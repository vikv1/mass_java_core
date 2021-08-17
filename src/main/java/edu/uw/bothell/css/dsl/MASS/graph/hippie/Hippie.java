package edu.uw.bothell.css.dsl.MASS.graph.hippie;

import java.util.*;
import java.io.*;
import java.nio.file.*;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.GraphPlaces;
import edu.uw.bothell.css.dsl.MASS.graph.HIPPIETABFormatLineParts;
import edu.uw.bothell.css.dsl.MASS.graph.hippie.HippieVertex.*;
import edu.uw.bothell.css.dsl.MASS.Message;
import edu.uw.bothell.css.dsl.MASS.MNode;

// Hippie is a class that extends GraphPlaces for use with HIPPIE graph data.
public class Hippie extends GraphPlaces implements Serializable {
    /**
     * HippieOp is a compressed representation of a graph operation for use
     * with batching remote graph operations and sending them in bulk to
     * distributed nodes.
     */
    public static class HippieOp implements Serializable {
        public static final long serialVersionUID = 0L;
        public static final int FUNC_ADD_VERTEX = 0;
        public static final int FUNC_ADD_EDGE = 1;

        public int funcID = -1;
        public int sourceID = -1;
        public String[] sourceKeys;
        public int destID = -1;
        public double edgeWeight = 0;
        public String extendedAttribs = "";

        public HippieOp(int funcID, int sourceID, String[] sourceKeys, int destID, double edgeWeight, String extendedAttribs) {
            this.funcID = funcID;
            this.sourceID = sourceID;
            this.sourceKeys = sourceKeys;
            this.destID = destID;
            this.edgeWeight = edgeWeight;
            this.extendedAttribs = extendedAttribs;
        }

        public String toString() {
            return "[" + this.sourceID + ", " + this.destID + ", " + this.edgeWeight + ", " + this.extendedAttribs + "]";
        }
    }

    // MAX_OPERATIONS_BUFFER indicates the max number of Graph operations
    // that can be stored in the operations cache before a flush is 
    // required.
    public static final int MAX_OPERATIONS_BUFFER = 500000;

    // cachedVertexOperations and cachedEdgeOperations are operation caches used to record graph operations
    // for future distribution to cluster nodes.
    protected ArrayList<ArrayList<HippieOp>> cachedVertexOperations = new ArrayList<ArrayList<HippieOp>>();
    protected ArrayList<ArrayList<HippieOp>> cachedEdgeOperations = new ArrayList<ArrayList<HippieOp>>();

    // Empty HIPPIE constructor. Mostly used for instantiating new HPPIE graphs on remote nodes.
    public Hippie() {
        super();
    }

    /**
     * Instantiates a new HIPPIE graph instance, using HippieVertex as the vertex class.
     * 
     * @param handle The reference handle for the HippieGraph. Must be unique to this instance.
     */
    public Hippie(int handle) {
        super(handle, HippieVertex.class.getName());
    }

    /**
     * Instantiates a new HIPPIE graph instance using the provided className
     * as the vertex class.
     * 
     * @param handle The reference handle for the HippieGraph. Must be unique to this instance.
     * @param className The class name of the vertex class to be used with the graph.
     */
    public Hippie(int handle, String className) {
        super(handle, className);
    }

    /**
     * Instantiates a new HIPPIE graph instance, using the provided className as the vertex
     * class and prepopulates the graph with Hippie data from the file at the provided
     * filePath.
     * 
     * @param handle The reference handle for the HippieGraph. Must be unique to this instance.
     * @param className The class name of the vertex class to be used with the graph.
     * @param filePath The path to a file containing HIPPIE graph data.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public Hippie(int handle, String className, String filePath) throws IOException,FileNotFoundException {
        super(handle, className);

        loadFromFile(filePath);
    }

    // loadFromFile loads HIPPIE graph data from the file found at the provided path.
    public void loadFromFile(String filePath) throws IOException,FileNotFoundException {
        // Initialize operations cache
        for (int i = 0; i < MASS.getSystemSize(); i++) {
            cachedVertexOperations.add(new ArrayList<HippieOp>());
            cachedEdgeOperations.add(new ArrayList<HippieOp>());
        }

        populate_graph(filePath);
    }

    // getVertex returns the HippieVertex associated with the provided vertex ID.
    public HippieVertex getVertex(Object vertex) {
        return (HippieVertex) super.getVertex(vertex);
    }

    @Override
    public int addVertexWithParams(Object vertexInitParam) {
        int id = super.addVertexWithParams(vertexInitParam);

        // Associate ID with HIPPIE Key/ID
        VertexInitData params = (VertexInitData)vertexInitParam;
        MASS.distributed_map.put(params.ID, id);
        MASS.distributed_map.put(params.Key, id);

        return id;
    }

    // addEdge adds an edge to the HippieGraph with the provided weight and
    // attributes.
    public boolean addEdge(int vertexID, int neighborID, double weight, String attributes) {
        // Check vertexId exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
            
            return false; 
        }

        // Check neighborId exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
                
            return false; 
        }

        return addEdgeOnNode(
            getOwnerID(vertexID),
            vertexID,
            neighborID,
            weight,
            attributes
        );
    }

    // addEdgeOnNode adds an edge on the provided node between the provided 
    // vertex and neighbor IDs, populated with the provided weight and 
    // attributes.
    public boolean addEdgeOnNode(int nodeID, int vertexID, int neighborID, double weight, String attributes) {
        if (!super.addEdgeOnNode(nodeID, vertexID, neighborID, weight)) {
            return false;
        }

        if (attributes == null || attributes.equalsIgnoreCase("")) {
            return true;
        }

        return addEdgeAttributesOnNode(nodeID, vertexID, neighborID, attributes);
    }

    // addEdgeAttributes adds the provided attributes to the edge between the
    // provided vertex and neighbor IDs.
    public boolean addEdgeAttributes(int vertexID, int neighborID, String attributes) {
        // Check vertexId exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
            
            return false; 
        }

        // Check neighborId exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
                
            return false; 
        }

        return addEdgeAttributesOnNode(
            getOwnerID(vertexID),
            vertexID,
            neighborID,
            attributes
        );
    }

    // addEdgeAttributesOnNode adds the provided attributes to the edge between the
    // provided vertex and neighbor IDs. nodeID is the node that owns the 
    // source vertex of the edge.
    public boolean addEdgeAttributesOnNode(int nodeID, int vertexID, int neighborID, String attributes) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add the edge.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteEdgeAttribute(nodeID, vertexID, neighborID, attributes);
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        HippieVertex vert = (HippieVertex) places.get(localIndex);
        vert.addEdgeAttributes(neighborID, attributes);
        places.set(localIndex, vert);

        return true;
    }

    // addRemoteEdgeAttribute adds the provided attributes to the edge owned by the 
    // remote node, between the provided vertex and neighbor IDs. 
    private boolean addRemoteEdgeAttribute(int nodeID, int vertexID, int neighborID, String attributes) {
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
            Message.ACTION_TYPE.MAINTENANCE_HIPPIE_ADD_EDGE_ATTRIB,
            getHandle(),
            new Object[]{vertexID, neighborID, attributes}
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

    // populate_graph populates the hippie graph with data from the HIPPIE file found
    // at the provided file path.
    private void populate_graph(String path) throws IOException,FileNotFoundException {
        Path filePath = Paths.get(path);

        BufferedReader br = new BufferedReader(new FileReader(filePath.toString()));
        String line = "";

        // Declare attribute variables
        String[] parts;
        String pKey = "";
        String pID = "";
        double edgeWeight = 0.0;
        String extendedAttribs = "";
        String iKey = "";
        String iID = "";

        // Declare vertex variables
        int sourceID = 0;
        int destID = 0;
        while ((line = br.readLine()) != null) {
            // Extract atributes
            parts = line.split("\t");
            pKey = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.PROTEIN_KEY);
            pID = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.PROTEIN_ID);
            extendedAttribs = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.EXTENDED_ATTRIBUTES);
            edgeWeight = Double.valueOf(HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.INTERACTION_ATTRIBUTE));
            iKey = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.INTERACTION_KEY);
            iID = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.INTERACTION_ID);

            // Process primary Keys/IDs
            sourceID = cacheVertex(pKey, pID);

            // Process interaction Keys/IDs
            destID = cacheVertex(iKey, iID);

            // Create edge between the primary and interaction vertices.
            cacheEdge(sourceID, destID, edgeWeight, extendedAttribs);
        }

        // flush remaining operations to nodes.
        flushCachedVertexOps();
        flushCachedEdgeOps();
    }

    // flushCachedOps flushes all of the cached graph operations to 
    // their respective nodes.
    protected void flushCachedVertexOps() {
        // Send to each node
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_BULK_GRAPH_HIPPIE_OPS,
            getHandle(),
            cachedVertexOperations.get(node.getPid())
        )));

        // Handle local operations
        int myPid = MASS.getMyPid();
        for (HippieOp op : cachedVertexOperations.get(myPid)) {
            VertexInitData vid = new VertexInitData(op.sourceKeys[0], op.sourceKeys[1]);
            addVertexOnNode(myPid, op.sourceID, vid);
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
            cachedVertexOperations.add(new ArrayList<HippieOp>());
        }
    }

    // flushCachedOps flushes all of the cached graph operations to 
    // their respective nodes.
    protected void flushCachedEdgeOps() {
        // Send to each node
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_BULK_GRAPH_HIPPIE_OPS,
            getHandle(),
            cachedEdgeOperations.get(node.getPid())
        )));

        // Handle local operations
        int myPid = MASS.getMyPid();
        for (HippieOp op : cachedEdgeOperations.get(myPid)) {
            addEdgeOnNode(myPid, op.sourceID, op.destID, op.edgeWeight, op.extendedAttribs);
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
            cachedEdgeOperations.add(new ArrayList<HippieOp>());
        }
    }

    // cacheVertex caches the provided vertex key and ID.
    protected int cacheVertex(String vertexKey, String vertexID) {
        // Check if vertex already exists in the dist map.
        int sourceID = MASS.distributed_map.getOrDefault(vertexID, -1).intValue();

        // If we get a value then the vertex already exists, no need to cache it.
        if (sourceID != -1) { 
            if (MASS.distributed_map.getOrDefault(vertexKey, -1).intValue() == -1) {
                MASS.distributed_map.put(vertexKey, sourceID);
            }
            return sourceID; 
        }
        
        // get the owner of the vertex and cache it.
        int vertexOwner = getOwnerID(nextVertexID);
        cachedVertexOperations.get(vertexOwner).add(new HippieOp(HippieOp.FUNC_ADD_VERTEX, nextVertexID, new String[]{vertexKey, vertexID}, -1, -1, ""));
        sourceID = nextVertexID;
        nextVertexID++;

        // Add primary and secondary keys to the dist map
        MASS.distributed_map.put(vertexKey, sourceID);
        MASS.distributed_map.put(vertexID, sourceID);
        
        if (getCacheVertexSize() > MAX_OPERATIONS_BUFFER) {
            flushCachedVertexOps();
        }

        return sourceID;
    }

    // cacheEdge caches the provided edge data.
    protected void cacheEdge(int sourceID, int destID, double weight, String extendedAttribs) {
        int sourceOwner = getOwnerID(sourceID);
        cachedEdgeOperations.get(sourceOwner).add(new HippieOp(HippieOp.FUNC_ADD_EDGE, sourceID, null, destID, weight, extendedAttribs));
    }

    // getCacheVertexSize retrieves the total size of all cached 
    // vertex operations.
    protected int getCacheVertexSize() {
        int opsSum = 0;
        for (int i = 0; i < cachedVertexOperations.size(); i++) {
            opsSum += cachedVertexOperations.get(i).size();
        }

        return opsSum;
    }
}