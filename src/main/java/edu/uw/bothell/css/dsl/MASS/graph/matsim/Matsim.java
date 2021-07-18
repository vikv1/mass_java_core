package edu.uw.bothell.css.dsl.MASS.graph.matsim;

import java.util.*;
import java.io.*;
import java.nio.*;

import javax.xml.stream.*;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Message;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.GraphPlaces;
import edu.uw.bothell.css.dsl.MASS.graph.MATSimNetworkNode;
import edu.uw.bothell.css.dsl.MASS.graph.MATSimNetworkLink;
import edu.uw.bothell.css.dsl.MASS.graph.MATSimNetworkModel;
import edu.uw.bothell.css.dsl.MASS.graph.matsim.MatsimVertex.*;

// Matsim is a class that extends GraphPlaces for use with MATSim graph data.
public class Matsim extends GraphPlaces implements Serializable {

    /**
     * MatsimOp is a compressed representation of a graph operation for use
     * with batching remote graph operations and sending them in bulk to 
     * distributed nodes.
     */
    public static class MatsimOp implements Serializable {
        public static final long serialVersionUID = 0L;
        public static final int FUNC_ADD_VERTEX = 0;
        public static final int FUNC_ADD_EDGE = 1;

        public int funcID = -1;

        // Vertex attributes
        public int sourceID = -1;
        public long vertID = 0;
        public double vertX = 0;
        public double vertY = 0;
        public int vertType = 0;

        // Edge attributes
        public int destID = -1;
        public MatsimEdge edge;

        public MatsimOp() { }

        // Constructor for vertex operations.
        public MatsimOp(MATSimNetworkNode node) {
            this.funcID = FUNC_ADD_VERTEX;
            this.vertID = node.id;
            this.vertX = node.x;
            this.vertY = node.y;
            this.vertType = node.type;
        }

        // Constructor for edge operations
        public MatsimOp(MATSimNetworkLink link) {
            this.funcID = FUNC_ADD_EDGE;
            this.edge = new MatsimEdge(link);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            if (funcID == 0) {
                sb.append("VERTEX");
                sb.append("] - {");
                sb.append("souceID=" + this.sourceID + ", ");
                sb.append("vertID=" + this.vertID + ", ");
                sb.append("vertX=" + this.vertX + ", ");
                sb.append("vertY=" + this.vertY + ", ");
                sb.append("vertType=" + this.vertType + "}");

            } else {
                sb.append("EDGE");
                sb.append("] - {");
                sb.append("souceID=" + this.sourceID + ", ");
                sb.append("destID=" + this.destID + ", ");
                sb.append("edge=" + this.edge.toString() + "}");
            }
            
            return sb.toString();
        }
    }

    // MAX_OPERATIONS_BUFFER indicates the max number of Graph operations
    // that can be stored in the operations cache before a flush is 
    // required.
    public static final int MAX_OPERATIONS_BUFFER = 500000;

    // cachedOperations is an operations cache used to record graph operations
    // for future distribution to cluster nodes.
    protected ArrayList<ArrayList<MatsimOp>> cachedVertexOperations = new ArrayList<ArrayList<MatsimOp>>();
    protected ArrayList<ArrayList<MatsimOp>> cachedEdgeOperations = new ArrayList<ArrayList<MatsimOp>>();

    // Empty Matsim constructor. Mostly used for instantiating new Matsim graphs on remote nodes.
    public Matsim() {
        super();
    }

    public Matsim(int handle) {
        super(handle, MatsimVertex.class.getName());
    }

    public Matsim(int handle, String className) {
        super(handle, className);
    }

    public Matsim(int handle, String className, String filePath) throws IOException,FileNotFoundException,XMLStreamException {
        super(handle, className);

        loadFromFile(filePath);
    }

    public void loadFromFile(String filePath) throws IOException,FileNotFoundException,XMLStreamException {
        for (int i = 0; i < MASS.getSystemSize(); i++) {
            cachedVertexOperations.add(new ArrayList<MatsimOp>());
            cachedEdgeOperations.add(new ArrayList<MatsimOp>());
        }

        populate_graph(filePath);
    }

    // getVertex returns the MatsimVertex associated with the provided vertex ID.
    public MatsimVertex getVertex(Object vertex) {
        return (MatsimVertex) super.getVertex(vertex);
    }

    public boolean addEdgeOnNode(int nodeID, int vertexID, int neighborID, MatsimEdge edge) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add the edge.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteEdge(nodeID, vertexID, neighborID, edge);
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        MatsimVertex vert = (MatsimVertex) places.get(localIndex);
        vert.addEdge(neighborID, edge);
        places.set(localIndex, vert);

        return true;
    }

    private boolean addRemoteEdge(int nodeID, int vertexID, int neighborID, MatsimEdge edge) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to remove the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_MATSIM_ADD_EDGE,
            getHandle(),
            new Object[]{vertexID, neighborID, edge}
        );
        
        // Send message and wait for reply
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // Message systems currently only returns ACK if successful
        // so if we do not receive one, assume failure.
        if (replyMsg.getAction() != Message.ACTION_TYPE.ACK) {
            return false;
        }

        return true;
    }

    private void populate_graph(String path) throws IOException,FileNotFoundException,XMLStreamException {
        FileInputStream fis = new FileInputStream(path);
        XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(fis);
        int eventCode = -1;
        // Read graph data
        while(xsr.hasNext()) {
            eventCode = xsr.next();

            // node (vertex) elements
            if (XMLStreamConstants.START_ELEMENT == eventCode &&
                xsr.getLocalName().equalsIgnoreCase("nodes")) {

                MATSimNetworkNode node = new MATSimNetworkNode();
                
                // Parse all nodes (vertices)
                while (xsr.hasNext()) {
                    eventCode = xsr.next();
                    
                    // Check if we've moved past the last node
                    if (XMLStreamConstants.END_ELEMENT == eventCode &&
                        xsr.getLocalName().equalsIgnoreCase("nodes")) {
                        
                        break;
                    }

                    if (XMLStreamConstants.START_ELEMENT == eventCode &&
                        xsr.getLocalName().equalsIgnoreCase("node")) {
                        
                        // extract node data
                        String val = xsr.getAttributeValue(null, "id");
                        node.id = val != null ? Long.valueOf(val) : 0;

                        val = xsr.getAttributeValue(null, "x");
                        node.x = val != null ? Double.valueOf(val) : 0;

                        val = xsr.getAttributeValue(null, "y");
                        node.y = val != null ? Double.valueOf(val) : 0;

                        val = xsr.getAttributeValue(null, "type");
                        node.type = val != null ? Integer.valueOf(val) : 0;

                        MatsimOp op = new MatsimOp(node);
                        cacheVertex(op);
                    }
                }
            }

            // link (edge) elements
            if (XMLStreamConstants.START_ELEMENT == eventCode &&
                xsr.getLocalName().equalsIgnoreCase("links")) {

                MATSimNetworkLink link = new MATSimNetworkLink();
            
                // Parse all nodes (vertices)
                while (xsr.hasNext()) {
                    eventCode = xsr.next();
                    
                    // Check if we've moved past the last node
                    if (XMLStreamConstants.END_ELEMENT == eventCode &&
                        xsr.getLocalName().equalsIgnoreCase("links")) {
                        
                        break;
                    }

                    if (XMLStreamConstants.START_ELEMENT == eventCode &&
                        xsr.getLocalName().equalsIgnoreCase("link")) {
                        
                        // extract link data

                        String val = xsr.getAttributeValue(null, "id");
                        link.id = val != null ? val : "";

                        val = xsr.getAttributeValue(null, "from");
                        link.from = val != null ? Long.valueOf(val) : 0;

                        val = xsr.getAttributeValue(null, "to");
                        link.to = val != null ? Long.valueOf(val) : 0;

                        val = xsr.getAttributeValue(null, "length");
                        link.length = val != null ? Double.valueOf(val) : 0.0;

                        val = xsr.getAttributeValue(null, "freespeed");
                        link.freespeed = val != null ? Double.valueOf(val) : 0.0;

                        val = xsr.getAttributeValue(null, "capacity");
                        link.capacity = val != null ? Float.valueOf(val) : 0.0f;
                        
                        val = xsr.getAttributeValue(null, "permlanes");
                        link.permlanes = val != null ? Float.valueOf(val) : 0.0f;

                        val = xsr.getAttributeValue(null, "oneway");
                        link.oneway = val != null ? Integer.valueOf(val) : 0;

                        val = xsr.getAttributeValue(null, "modes");
                        link.modes = val != null ? val : "";

                        MatsimOp op = new MatsimOp(link);
                        cacheEdge(link);
                    }
                }
            }
        }

        // flush remaining operations to nodes.
        flushCachedVertexOps();
        flushCachedEdgeOps();
    }

    // cacheVertex caches the provided vertex key and ID
    protected int cacheVertex(MatsimOp op) {
        // Check if vertex already exists in the dist map.
        int sourceID = MASS.distributed_map.getOrDefault(op.vertID, -1).intValue();

        // If we get a value then the vertex already exists, no need to cache it.
        if (sourceID != -1) { 
            return sourceID; 
        }
        
        // get the owner of the vertex and cache it.
        int vertexOwner = getOwnerID(nextVertexID);
        op.sourceID = nextVertexID;
        cachedVertexOperations.get(vertexOwner).add(op);
        sourceID = nextVertexID;
        nextVertexID++;

        // Add primary and secondary keys to the dist map
        MASS.distributed_map.put(op.vertID, sourceID);
        
        if (getCacheVertexSize() > MAX_OPERATIONS_BUFFER) {
            flushCachedVertexOps();
        }

        return sourceID;
    }

    // flushCachedVertexOps flushes all of the cached graph operations to 
    // their respective nodes.
    protected void flushCachedVertexOps() {
        // Send to each node
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_BULK_GRAPH_MATSIM_OPS,
            getHandle(),
            cachedVertexOperations.get(node.getPid())
        )));

        // Handle local operations
        int myPid = MASS.getMyPid();
        for (MatsimOp op : cachedVertexOperations.get(myPid)) {
            if (op.funcID == MatsimOp.FUNC_ADD_VERTEX) {
                MatsimVertexInitData vid = new MatsimVertexInitData(op.vertID, op.vertX, op.vertY, op.vertType);
                addVertexOnNode(myPid, op.sourceID, vid);
            } else if (op.funcID == MatsimOp.FUNC_ADD_EDGE) {
                // addEdgeOnNode(myPid, op.sourceID, op.destID, op.edgeWeight, op.extendedAttribs);
            }
        }

        // Wait for acknowledgment of remote operations
        MASS.getRemoteNodes().forEach(node -> {
            Message msg = node.receiveMessage();
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASS.getLogger().debug("failed to bulk apply operations");
            }
        });

        // Clear operations cache
        cachedVertexOperations.clear();
        for (int i = 0; i < cachedVertexOperations.size(); i++) {
            cachedVertexOperations.add(new ArrayList<MatsimOp>());
        }
    }

    // cacheEdge caches the provided edge data.
    protected void cacheEdge(MATSimNetworkLink link) {
        // get source ID
        int sourceID = MASS.distributed_map.getOrDefault(link.from, -1);
        if (sourceID == -1) {
            MASS.getLogger().error("[cacheEdge] source vertex doesn't exist");
            return;
        }

        int destID = MASS.distributed_map.getOrDefault(link.to, -1);
        if (destID == -1) {
            MASS.getLogger().error("[cacheEdge] dest vertex doesn't exist");
            return;
        }

        int sourceOwner = getOwnerID(sourceID);
        MatsimOp op = new MatsimOp(link);
        op.sourceID = sourceID;
        op.destID = destID;

        cachedEdgeOperations.get(sourceOwner).add(op);

        if (getCacheEdgeSize() > MAX_OPERATIONS_BUFFER) {
            flushCachedEdgeOps();
        }
    }

    // flushCachedOps flushes all of the cached graph operations to 
    // their respective nodes.
    protected void flushCachedEdgeOps() {
        // Send to each node
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_BULK_GRAPH_MATSIM_OPS,
            getHandle(),
            cachedEdgeOperations.get(node.getPid())
        )));

        // Handle local operations
        int myPid = MASS.getMyPid();
        for (MatsimOp op : cachedEdgeOperations.get(myPid)) {
            if (op.funcID == MatsimOp.FUNC_ADD_EDGE) {
                addEdgeOnNode(myPid, op.sourceID, op.destID, op.edge);
            }
        }

        // Wait for acknowledgment of remote operations
        MASS.getRemoteNodes().forEach(node -> {
            Message msg = node.receiveMessage();
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASS.getLogger().debug("failed to bulk apply operations");
            }
        });

        // Clear operations cache
        cachedEdgeOperations.clear();
        for (int i = 0; i < cachedEdgeOperations.size(); i++) {
            cachedEdgeOperations.add(new ArrayList<MatsimOp>());
        }
    }

    protected int getCacheVertexSize() {
        int opsSum = 0;
        for (int i = 0; i < cachedVertexOperations.size(); i++) {
            opsSum += cachedVertexOperations.get(i).size();
        }

        return opsSum;
    }

    protected int getCacheEdgeSize() {
        int opsSum = 0;
        for (int i = 0; i < cachedEdgeOperations.size(); i++) {
            opsSum += cachedEdgeOperations.get(i).size();
        }

        return opsSum;
    }
}