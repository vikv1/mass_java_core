/*

    MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public class PropertyGraphPlaces extends GraphPlaces {
		// sharedPlaceName is the identifier for a shared place
		protected String sharedPlaceName = null;

    public PropertyGraphPlaces() {
        super();
    }


    public PropertyGraphPlaces(int handle, String className) {
        super(handle, className);
    }

    @Override
    protected void init_graph_master() {
        MASSBase.getLogger().debug("PropertyGraphPlaces - init_graph_master");

        Vector<String> hosts = getHosts();

        InitArgs initArgs = new InitArgs(this.getHandle(), this.getClassName(), this.getClass().getName(), this.sharedPlaceName);

        Message message = new Message(Message.ACTION_TYPE.PROPERTY_GRAPH_PLACES_INITIALIZE_GRAPH, getSize(),
                getHandle(), getClassName(),
                initArgs, 0, hosts );

        init_master_base(message);
    }

    public int addPropertyVertex(Object itemID_Original, List<String> labels, Map<String, String> properties) {
        Object itemID = (Object) ((String) itemID_Original).trim().toLowerCase();
        if (itemID == null || MASS.distributed_map.containsKey(itemID)) {
            MASS.getLogger().debug("the provided vertexId already exists");
            return -1;
        }

		// store PropertyVertexPlace in MASS library, 
		// vertexId is the internal reference in MASS library
		int vertexId = this.addVertexWithParams(itemID, labels, properties); 
		if(vertexId == -1) {
			MASS.getLogger().error("At PropertyGraphPlaces's addPropertyVertex: Failed at getting mass library reference vertexId. ");
			return -1;
		}

		MASS.distributed_map.put(itemID, vertexId);
		MASS.getLogger().debug("At PropertyGraphPlaces's addPropertyVertex: Added ItemID: " + itemID + ", MASS library vertex ID " + Integer.toString((int) vertexId) + ", labels: " + labels + ": properties: " + properties);
		return vertexId;
	}
    
    protected int addVertexWithParams(Object itemID, List<String> labels, Map<String, String> properties) {
        int vertexID;
        boolean fromIDQueue = false;

        // Get a new vertexID
        try {
            vertexID = idQueue.remove();
            fromIDQueue = true;

        } catch (NoSuchElementException e) {
            vertexID = nextVertexID;
        }

        boolean success = addVertexOnNode(
            getOwnerID(vertexID),
            vertexID,
            itemID,
            labels,
            properties
        );

        // If unsuccessful
        if (!success) {
            // If we got the ID from our queue, re-enqueue it.
            if (fromIDQueue) {
                idQueue.add(vertexID);
            }
            
            // return -1 to indicate as such.
            return -1; 
        }

        // Otherwise, increment if needed and return the vertexID.
        if (!fromIDQueue) { nextVertexID++; }

        return vertexID;
    }

    protected boolean addVertexOnNode(int nodeID, int vertexID, Object itemID, List<String> labels, Map<String, String> properties) {
        
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { 
            return false; 
        }
        
        // If another node owns this vertex, send it a message to add it.
        if (nodeID != MASS.getMyPid()) {
            MASS.getLogger().debug("At PropertyGraphPlaces: adding remote vertex, node ID: " + nodeID);
            return addRemoteVertex(nodeID, vertexID, itemID, labels, properties);
        }
        // Get local index and size of places array.
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist,
        // return false.
        if (localIndex > localSize) { 
            return false; 
        }

        // Create new VertexPlace
        PropertyVertexPlace propertyVertexPlace;
        try {
            propertyVertexPlace = new PropertyVertexPlace(itemID, labels, properties);
            propertyVertexPlace.setIndex(new int[]{vertexID});
        } catch (Exception e) {
            MASS.getLogger().error("expection trying to instantiate a new vertex: ", e);
            return false;
        }


        // Set it at the appropriate index if this vertex is to occupy 
        // preallocated space or reclaiming space from a previously removed
        // vertex.
        if (localIndex < localSize) {
            places.set(localIndex, propertyVertexPlace);
            return true;
        }

        // Otherwise, add it to the back.
        places.add(propertyVertexPlace);
        return true;
    }

    protected boolean addRemoteVertex(int nodeID, int vertexID, Object itemID, List<String> labels, Map<String, String> properties) {
        // Get the remote node
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("At PropertyGraphPlaces: remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();
        MASS.getLogger().debug("At PropertyGraphPlaces : adding vertex at remote node with pid {} ", nodeID);
        // Create message to ask remote node to add vertex.
        Object[] msgContent = new Object[]{vertexID, itemID, labels, properties};
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_ADD_PROPERTY_VERTEX,
            getHandle(),
            msgContent
        );

        // Send message and wait for reply
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // getAgentPopulation is currently overloaded to return the success/failure
        // of adding the vertex to the remote node.
        if (replyMsg.getAgentPopulation() <= 0) {
            MASS.getLogger().debug("remote node with pid {} failed to add vertex", nodeID);
            return false;
        }

        return true;
    }

    // set relation properties for edge between fromName to toName
    public boolean setRelationEdge(Object fromItemID_Ori, Object toItemID_Ori, List<String> relationTypes, Map<String, String> relationProperties) {
        Object fromItemID = (Object) ((String) fromItemID_Ori).trim().toLowerCase();
        Object toItemID = (Object) ((String) toItemID_Ori).trim().toLowerCase();
                
        int sourceId = MASSBase.distributed_map.getOrDefault(fromItemID, -1);
        int destinationId = MASSBase.distributed_map.getOrDefault(toItemID, -1);

        if (sourceId == -1 || destinationId == -1) {
            MASS.getLogger().debug(" At PropertyGraphPlaces, setRelationEdge: fromName or toName doesn't exist");
            return false;
        }

        // Check vertexId exists
        if (MASS.getMyPid() == 0 && 
            sourceId >= nextVertexID || idQueue.contains(sourceId)) {  
            return false; 
        }

        // Check neighborId exists
        if (MASS.getMyPid() == 0 && 
        destinationId >= nextVertexID || idQueue.contains(destinationId)) {          
            return false; 
        }

        return addTOEdgeOnNode(
            getOwnerID(sourceId),
            sourceId,
            destinationId,
            fromItemID,
            toItemID,
            relationTypes,
            relationProperties
        ) && addFROMEdgeOnNode(
            getOwnerID(destinationId),
            sourceId,
            destinationId,
            fromItemID,
            toItemID,
            relationTypes,
            relationProperties
        );

    }

    protected boolean addTOEdgeOnNode(int nodeID, int sourceId, int destinationId, Object fromItemID, Object toItemID, List<String> relationTypes, Map<String, String> relationProperties) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add the edge.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteTOEdge(nodeID, sourceId, destinationId, fromItemID, toItemID, relationTypes, relationProperties);
        }

        // Get local index and size of places array
        int localIndex = sourceId / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        PropertyVertexPlace vertex = (PropertyVertexPlace) places.get(localIndex);
        vertex.setTONeighborRelation(toItemID, relationTypes, relationProperties);
        places.set(localIndex, vertex);

        return true;
    }

    protected boolean addRemoteTOEdge(int nodeID, int sourceId, int destinationId, Object fromItemID, Object toItemID, List<String> relationTypes, Map<String, String> relationProperties) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("At PropertyGrahPlaces: addRemoteEdge: remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to remove the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_ADD_PROPERTY_TO_EDGE,
            getHandle(),
            new Object[]{sourceId, destinationId, fromItemID, toItemID, relationTypes, relationProperties}
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

    protected boolean addFROMEdgeOnNode(int nodeID, int sourceId, int destinationId, Object fromItemID, Object toItemID, List<String> relationTypes, Map<String, String> relationProperties) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add the edge.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteFROMEdge(nodeID, sourceId, destinationId, fromItemID, toItemID, relationTypes, relationProperties);
        }

        // Get local index and size of places array
        int localIndex = destinationId / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        PropertyVertexPlace vertex = (PropertyVertexPlace) places.get(localIndex);
        vertex.setFROMNeighborRelation(fromItemID, relationTypes, relationProperties);
        places.set(localIndex, vertex);

        return true;
    }

    protected boolean addRemoteFROMEdge(int nodeID, int sourceId, int destinationId, Object fromItemID, Object toItemID, List<String> relationTypes, Map<String, String> relationProperties) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("At PropertyGrahPlaces: addRemoteEdge: remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to remove the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_ADD_PROPERTY_FROM_EDGE,
            getHandle(),
            new Object[]{sourceId, destinationId, fromItemID, toItemID, relationTypes, relationProperties}
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
    
    public Object[] propertyGraphCallAll(int functionId, Object argument[]) {

        // Allocate space for return values;
        Object[] returnValues = new Object[argument.length];

        // split arguments across available system nodes.
        int numNodes = MASS.getSystemSize();
        
        ArrayList<ArrayList<Object>> args = new ArrayList<ArrayList<Object>>();
        for (int i = 0; i < numNodes; i++) {
            args.add(new ArrayList<Object>());
        }

        for (int i = 0; i < argument.length; i++) {
            args.get(i % numNodes).add(argument[i]);
        }

        // Send arguments to respective nodes and have them call the appropriate
        // method.
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.PROPERTY_GRAPH_PLACES_CALL_ALL_RETURN_OBJECT,
            this.getHandle(),
            functionId,
            args.get(node.getPid())
        )));

        // callAll locally
        int myRank = MASS.getMyPid();
        Object[] retVals = callPropertyVertexPlaceMethod(functionId, args.get(myRank));

        // set appropriate return values
        for (int i = 0; i < retVals.length; i++) {
            returnValues[myRank + numNodes * i] = retVals[i];
        }

        // get the return values from remote nodes
        for (MNode node : MASS.getRemoteNodes()) {
            Message msg = node.receiveMessage();
            MASSBase.getLogger().debug("At PropertyGraphPlaces, remoteNodes return message received ");
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASSBase.getLogger().error("an unknown error occured during callAll. invalid message type received");
                return null;
            }

            int nodeRank = node.getPid();
            retVals = (Object[]) msg.getArgument();
            for (int i = 0; i < retVals.length; i++) {
                returnValues[nodeRank + numNodes * i] = retVals[i];
            }   
        }

        MASSBase.getLogger().debug("At PropertyGraphPlaces: returnValues size: " + returnValues.length);
        // for(int i = 0; i < returnValues.length; i++) {
        //     System.out.println("retVal " + i + " = " + returnValues[i]);
        // }
        return returnValues;
    }

    protected Object[] callPropertyVertexPlaceMethod(int functionId, ArrayList<Object> args) {
        MASSBase.getLogger().debug("At PropertyGraphPlaces: callVertexPlaceMethod: size = " + args.size() + ", functionId = " + functionId);
        if (args.size() != places.size()) {
            MASS.getLogger().warning("number of callAll args does not match number of places");
        }

        // Allocate memory for return values
        Object[] retVals = new Object[args.size()];
        
        // resume threads? Not sure why this is needed, it's copied from the Places
        // implementation.
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_CALLALL);
        
        int numPlaces = places.size();
        for (int i = 0; i < args.size() && i < numPlaces; i++) {
            if (places.get(i) == null) { continue; }
            PropertyVertexPlace vPlace = (PropertyVertexPlace) places.get(i);
            retVals[i] = vPlace.callMethod(functionId, args.get(i));
            MASSBase.getLogger().debug("At PropertyGraphPlaces, callVertexPlaceMethod, retVals result = " + retVals[i]);
        }

        // confirm all threads are done with callAll.
        MThread.barrierThreads( 0 );

        return retVals;
    }


    // get PropertyGraphModel, for printGraph purpose.
    public PropertyGraphModel getPropertyGraph() {
        PropertyGraphModel graph = new PropertyGraphModel();

        if (getPlaces() != null && getPlaces().length > 0 && !(getPlaces()[0] instanceof PropertyVertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not PropertyVertexPlaces.");

            return graph;
        }

        // Places on master node
        if (getPlaces() != null) {
            for (Place place : Arrays.stream(getPlaces()).filter(p -> p != null).collect(Collectors.toList())) {

                PropertyVertexPlace vPlace = (PropertyVertexPlace) place;
                
                graph.addPropertyModel(vPlace.getItemID(), vPlace.getLabels(),vPlace.getNodeProperties(),vPlace.getTONeighbors(), vPlace.getFROMNeighbors());
            }
        }

        merge(graph, getRemotePropertyGraphs());

        return graph;
    }

    // getRemoteGraphs sends messages to all worker nodes requesting the graph
    // models containing their respective vertices.
    private PropertyGraphModel getRemotePropertyGraphs() {
        PropertyGraphModel graph = new PropertyGraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PROPERTY_PLACES, getHandle(), null));

            Message m = node.receiveMessage();

            if (m.getAction() != Message.ACTION_TYPE.MAINTENANCE_GET_PROPERTY_PLACES_RESPONSE) {
                throw new RuntimeException("Received incorrect response from node");
            } else {
                PropertyGraphModel model = (PropertyGraphModel) m.getArgument();

                merge(graph, model);
            }
        }

        return graph;
    }

    private void merge(PropertyGraphModel source, PropertyGraphModel remoteGraphs) {
        source.getPropertyVertices().addAll(remoteGraphs.getPropertyVertices());
    }

    public void printGraph() {        
        MASS.getLogger().debug("Printing the graph with node properties and relationship information: ============");
        this.getPropertyGraph().print();
    }

    public Object[] getArguments(Object arg) {
        // calculate the total argument size for return-objects
		int total = MatrixUtilities.getMatrixSize( getSize() ); // the total number of place elements
        MASS.getLogger().debug("At PropertyGraphPlaces, getArguments, total number of places = " + total);
        Object[] args = new Object[total];
        Arrays.fill(args, arg);
        return args;
    }

    public int getThisGraphPlacesSize() {
        return this.places.size();
    }

    public void printVertex(Object uniqueID) {
        PropertyVertexPlace place = (PropertyVertexPlace) this.getVertex(uniqueID);
        if(place == null) {
            System.out.println("No such vertex found.");
            return;
        }
        PropertyVertexModel model = new PropertyVertexModel(place.getItemID(), place.getLabels(),place.getNodeProperties(),place.getTONeighbors(), place.getFROMNeighbors());
        model.print();
    }
}
