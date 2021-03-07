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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.VertexMetaValues;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

public class GraphPlaces extends Places implements Graph {
    private final GraphInitAlgorithm init_algorithm;
    private final String filename;
    private final GraphInputFormat input_format;

    // nextVertexID tracks the vertex ID associated vertices added to the graph.
    // It is kept up to date such that it's currently value represents the ID
    // to be assigned tot he next Vertex.
    private int nextVertexID = 0;

    // idQueue is used to store the IDs of vertices that have been removed 
    // so that they may be reused for newly added nodes.
    // Note: If this isn't accessed concurrently we can replace this with 
    // a linked list. Similarly with the VertexPlace vectors.
    private Queue<Integer> idQueue = new ConcurrentLinkedQueue<Integer>();

    // localNextPlaceIndex is a local tracker for the next places index.
    private int localNextPlaceIndex = 0;

    // globalNextPlaceIndex is a global tracker for the next places index. 
    // Its value is only meaningful on the master node.
    private int globalNextPlaceIndex = 0;

    // objectFactory is used to generate objects of the places class provided
    // when instantiating GraphPlaces.
    private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

    // placesVector is used to stored VertexPlaces added after instantiating
    // GraphPlaces.
    private Vector<Vector<VertexPlace>> placesVector = new Vector<>(1);
    private Vector<VertexPlace> places = new Vector<VertexPlace>();

    /**
     * Constructs a GraphPlaces object populated with data from the 
     * "graph_n.txt" CSV text file.
     * 
     * @param handle The Handle ID identifying this GraphPlaces.
     * @param className The class that represents a VertexPlace.
     * @param graphArgs 
     * @param initArgs 
     */
    public GraphPlaces(int handle, String className, String[] graphArgs, Object[] initArgs) {
        super(handle, className, graphArgs, initArgs);

        init_algorithm = GraphInitAlgorithm.FULL_LIST;
        filename = "graph_n.txt";
        input_format = GraphInputFormat.CSV;
    }

    /**
     * Constructs a GraphPlaces object populated with data contained in
     * the provided filename.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param filename The filename of the file with which to extract graph data.
     * @param format The format of the file (e.g., CSV, HIPPIE, etc..).
     * @param init_algorithm The initialization algorithm used 
     * (e.g., FULL_LIST or PARTITIONED_LIST).
     */
    public GraphPlaces(int handle, String className, String filename, GraphInputFormat format,
                       GraphInitAlgorithm init_algorithm) {
        super(handle, className, new Object[] { filename, format, init_algorithm });

        if (init_algorithm != GraphInitAlgorithm.FULL_LIST || init_algorithm != GraphInitAlgorithm.PARTITIONED_LIST) {
            init_algorithm = GraphInitAlgorithm.FULL_LIST;
        }

        this.init_algorithm = init_algorithm;
        this.filename = filename;
        this.input_format = format;
    }

    /**
     * Constructs a GraphPlaces object populated with data contained in
     * the provided filename.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param filename The filename of the file with which to extract graph data.
     * @param format The format of the file (e.g., CSV, HIPPIE, etc..).
     * @param init_algorithm The initialization algorithm used 
     * (e.g., FULL_LIST or PARTITIONED_LIST).
     * @param nVertices This is unused.
     * @param argument The arguments to be supplied to the VertexPlace during
     * initialization.
     */
    public GraphPlaces(int handle, String className, String filename, GraphInputFormat format,
                       GraphInitAlgorithm init_algorithm, int nVertices, Object argument) {
        super(handle, className, argument);

        if (init_algorithm != GraphInitAlgorithm.FULL_LIST || init_algorithm != GraphInitAlgorithm.PARTITIONED_LIST) {
            init_algorithm = GraphInitAlgorithm.FULL_LIST;
        }

        this.init_algorithm = init_algorithm;
        this.filename = filename;
        this.input_format = format;
    }

    /**
     * Constructs an empty GraphPlaces object.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param size The number of vertices in the graph.
     */
    public GraphPlaces(int handle, String className, int size) {
        super(handle, className, size, new int[] { size });

        // Should use a different indicator for empty graph
        this.init_algorithm = GraphInitAlgorithm.FULL_LIST;
        this.filename = "";
        this.input_format = GraphInputFormat.CSV;
    }

    /**
     * Constructs a basic GraphPlaces object with no pre-allocated space.
     * @param handle
     * @param className
     */
    public GraphPlaces(int handle, String className) {
        super(handle, className);

        // Note(bluger-02/20/2021) - This seems to be required. I'm not sure why yet.
        this.init_algorithm = GraphInitAlgorithm.FULL_LIST;
        this.filename = "";
        this.input_format = GraphInputFormat.CSV;
    }
    
    /**
     * Constructs an empty GraphPlaces object.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param size The number of vertices in the graph.
     * @param _remote_node This is unused.
     */
    public GraphPlaces(int handle, String className, int size, boolean _remote_node) {
        super(handle, className);

        // Should use a different indicator for empty graph
        this.init_algorithm = GraphInitAlgorithm.FULL_LIST;
        this.filename = "";
        this.input_format = GraphInputFormat.CSV;
        
        init_all_graph_blank(size);
    }

    // reinitialize reinitializes the GraphPlaces object by setting the 
    // local index trackers and the placesVector to 0.
    @Override
    protected void reinitialize() {
        super.reinitialize();

        localNextPlaceIndex = 0;
        globalNextPlaceIndex = 0;
        placesVector = new Vector<Vector<VertexPlace>>(1);

        nextVertexID = 0;
        places = new Vector<VertexPlace>();
    }

    // reinitializeGraph calls reinitialize locally and sends MAINTENANCE_REINITIALIZE
    // messages to each of the worker nodes to reinitialize them as well.
    private void reinitializeGraph() {
        // TODO: This feels like something that could be handled by an internal callAll or something similarly
        // utilizing the infrastructure the code already has
        reinitialize();

        //Send reinitialize message to all remote nodes
        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_REINITIALIZE, getHandle(), null);

        // This needs to remove neighbors anyways so just send to everyone else
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(message));

        MASSBase.reinitializeMap();

        // Early clear is inconsequential. We just need to make sure we don't move forward before all nodes are done
        MASS.barrierAllSlaves();
    }

    /**
     * init_master initializes GraphPlaces on the master and sends messages
     * to all of the worker nodes to do the same.
     * 
     * @param argument The arguments to be supplied to the VertexPlace during
     * initialization.
     * @param boundaryWidth The width of the boundary between nodes, used to calculate shadow space.
     */
    @Override
    protected void init_master(Object argument, int boundaryWidth) {
        MASSBase.getLogger().debug("GraphPlaces - init_master");

        Vector<String> hosts = getHosts();

        Message message = new Message(Message.ACTION_TYPE.PLACES_INITIALIZE_GRAPH, getSize(),
                getHandle(), getClassName(),
                argument, boundaryWidth, hosts );

        init_master_base(message);
    }

    // init_all initializes GraphPlaces using the provided argument.
    @Override
    protected void init_all(Object argument) {
        if (argument instanceof Integer) {
            init_all_graph_blank((Integer) argument);
        } else {
            Object[] arguments = (Object[]) argument;

            // TODO: This is failing in kotlin
            // String[] graphArguments = (String[]) Arrays.copyOfRange(arguments, 0, 2);
            // Object[] initArguments = (Object[]) Arrays.copyOfRange(arguments, 2, arguments.length);

            String [] graphArguments = new String[2];
            Object [] initArguments = null;

            graphArguments[0] = arguments[0].toString();
            graphArguments[1] = arguments[1].toString();

            if (arguments.length > 2) {
                initArguments = new Object[arguments.length - 2];

                for (int i = 0; i < initArguments.length; i++) {
                    initArguments[i] = arguments[i + 2];
                }
            }

            init_all_graph(graphArguments, initArguments);
        }
    }

    /**
     * Graph interface implementation
     */
    @Override
    public GraphModel getGraph() {
        return getGraph(true);
    }

    /**
     * setGraph - replace in-memory graph with the supplied model
     */
    @Override
    public void setGraph(final GraphModel newGraph) {
        // Re-initialize graph across cluster
        reinitializeGraph();

        // apply the new model
        // TODO: This would be a good area for improvements. As it stands I will just use the new graph maintenance
        // functions to apply the model

        // Add all the vertices first
        newGraph.getVertices().forEach(vertex -> addVertex(vertex.id));

        // FIXME (#153): Missing weights
        newGraph.getVertices()
                .forEach(vertex -> vertex.neighbors
                        .forEach(neighbor -> addEdge(vertex.id, neighbor, 1.0)));
    }

    /**
     * merge merges the remoteGraphs into the source graph model.
     * 
     * @param source The source graph to merge nodes into.
     * @param remoteGraphs The remote graph to merge nodes from.
     */
    public void merge(GraphModel source, GraphModel remoteGraphs) {
        source.getVertices().addAll(remoteGraphs.getVertices());
    }

    
    /**
     * getGraph returns a GraphModel copy of the distributed graph.
     */
    @Override
    public GraphModel getGraph(boolean all) {
        GraphModel graph = new GraphModel();

        if (getPlaces() != null && getPlaces().length > 0 && !(getPlaces()[0] instanceof VertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not VertexPlaces.");

            return graph;
        }

        // Places on master node
        if (getPlaces() != null) {
            for (Place place : Arrays.stream(getPlaces()).filter(p -> p != null).collect(Collectors.toList())) {
                VertexPlace vPlace = (VertexPlace) place;

                Object attribute = MASSBase.distributed_map.reverseLookup(place.getIndex()[0]);

                if (attribute == null) {
                    attribute = place.getIndex()[0];
                }
                
                // FIXME (#154): Missing weights
                graph.addVertex(attribute, vPlace.neighbors);
            }
        }

        for (Vector<VertexPlace> places : placesVector) {
            for (VertexPlace place : places) {
                Object attribute = MASSBase.distributed_map.reverseLookup(place.getIndex()[0]);

                graph.addVertex(attribute, place.neighbors);
            }
        }

        if (all) {
            merge(graph, getRemoteGraphs());
        }

        return graph;
    }

    // getRemoteGraphs sends messages to all worker nodes requesting the graph
    // models containing their respective vertices.
    private GraphModel getRemoteGraphs() {
        GraphModel graph = new GraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES, getHandle(), null));

            Message m = node.receiveMessage();

            if (m.getAction() != Message.ACTION_TYPE.MAINTENANCE_GET_PLACES_RESPONSE) {
                throw new RuntimeException("Received incorrect response from node");
            } else {
                GraphModel model = (GraphModel) m.getArgument();

                merge(graph, model);
            }
        }

        return graph;
    }

    /**
     * validNeighbor returns whether both vertexId and neighborId represent existing
     * VertexPlaces.
     * 
     * @param vertexId The vertex ID of the source vertex.
     * @param neighborId The vertex ID of the neighbor/destination vertex.
     * @return true if an edge can be created between them, false otherwise.
     */
    public boolean validNeighbor(final Object vertexId, final Object neighborId) {
        if (MASSBase.distributed_map.getOrDefault(vertexId, -1) == -1
            || MASSBase.distributed_map.getOrDefault(neighborId, -1) == -1) {
            return false;
        }

        return true;
    }

    /**
     * addEdge adds an edge between the provided vertexId and neighborId.
     * The created edge is given a weight of 1.0.
     * 
     * @param vertexId the vertex ID of the source vertex.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    @Override
    public boolean addEdge(Object vertexId, Object neighborId) {
        // Weight is set as 1.0 to bring it in line with what the 
        // setGraph method does.
        return this.addEdge(vertexId, neighborId, 1.0);
    }

    /**
     * addEdge adds an edge between the provided vertexId and neighborId and
     * assigns it the provided weigth value.
     * 
     * @param vertexId the vertex ID of the source vertex.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * @param weight the weight of the edge.
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    @Override
    public boolean addEdge(Object vertexId, Object neighborId, double weight) {
        Log4J2Logger logger = MASSBase.getLogger();
        boolean added = false;

        // If both vertex IDs don't exist in the distributed map, return false.
        if (!validNeighbor(vertexId, neighborId)) {
            return false;
        }

        

        logger.debug(String.format("addEdge [vertexId=%s; neighborId=%s; weight=%f]", vertexId.toString(), neighborId.toString(), weight));

        int globalIndex = MASSBase.distributed_map.getOrDefault(vertexId, -1);
        if (globalIndex < 0) {
            return false;
        }

        int ownerId = getNodeIdFromGlobalLinearIndex(globalIndex);

        if (ownerId == MASSBase.getMyPid()) {
            logger.debug("addEdge->myPlaces");

            added = addEdgeLocally(vertexId, neighborId, weight);
        } else {
            logger.debug("addEdge->remotePlace");

            VertexMetaValues values = getVertexMetaValues(vertexId);

            int owner = values.OwnerPid;

            if (owner != -1) {
                for (MNode node : MASSBase.getRemoteNodes()) {
                    if (node.getPid() == owner) {
                        node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_EDGE, getHandle(), new Object[] { vertexId, neighborId, weight }));
                        node.receiveMessage(); // recieve ack jonathan modification 
                    }
                }
            }

            logger.warning("Cannot add edge: source is out of range(" + vertexId + ")");
        }

        return added;
    }

    /**
     * addEdgeLocally adds an edge between the provided local vertexId and 
     * the provided neighborId, and assigns the provided weigth as the edge 
     * weigth.
     * 
     * @param vertexId the vertex ID of the source vertex. Must be local to 
     * the calling node.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * @param weight the weight of the edge.
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    public boolean addEdgeLocally(Object vertexId, Object neighborId, double weight) {
        int globalIndex = MASSBase.getGlobalIndexForKey(vertexId);
        int owner = getNodeIdFromGlobalLinearIndex(globalIndex);
        if (globalIndex == -1 || owner != MASS.getMyPid()) { 
            return false; 
        }

        // Get the layer and local index associated with the global index.
        int layer = getLayer(globalIndex);
        int localIdx = getLocalIndex(globalIndex);

        VertexPlace place = placesVector.get(layer).get(localIdx);
        place.addNeighbor(neighborId, weight);

        return true;
    }

    /**
     * removeEdgeLocally removes an edge between the provided local vertexId
     * and the provided neighborId.
     * 
     * @param vertexId the vertex ID of the source vertex. Must be local to 
     * the calling node.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    public boolean removeEdgeLocally(Object vertexId, Object neighborId) {
        int globalIndex = MASSBase.getGlobalIndexForKey(vertexId);
        int owner = getNodeIdFromGlobalLinearIndex(globalIndex);

        if (globalIndex == -1 || owner != MASS.getMyPid()) { 
            return false; 
        }
        
        // Get the layer and local index associated with the global index.
        int layer = getLayer(globalIndex);
        int localIdx = getLocalIndex(globalIndex);

        VertexPlace place = placesVector.get(layer).get(localIdx);

        place.removeNeighbor(neighborId);

        return true;
    }

    /**
     * removeEdge removes the edge between the provided vertexId
     * and neighborId.
     * 
     * @param vertexId the vertex ID of the source vertex.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    @Override
    public boolean removeEdge(Object vertexId, Object neighborId) {
        Log4J2Logger logger = MASSBase.getLogger();
        int globalIndex = MASSBase.getGlobalIndexForKey(vertexId);
        if (globalIndex == -1) {
            return false;
        }

        logger.debug(String.format("removeEdge [vertexId=%s; neighborId=%s]", vertexId, neighborId));
        int ownerId = getNodeIdFromGlobalLinearIndex(globalIndex);
        
        // If this node does not own the vertex, send the request to the 
        // owning node and return false.
        if (ownerId != MASSBase.getMyPid()) {
            logger.debug("removeEdge->remotePlace");
            VertexMetaValues values = getVertexMetaValues(vertexId);
            int owner = values.OwnerPid;

            if (owner != -1) {
                for (MNode node : MASSBase.getRemoteNodes()) {
                    if (node.getPid() == owner) {
                        node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_REMOVE_EDGE, getHandle(), new Object[] { vertexId, neighborId, null }));
                    }
                }
            }
            
            return false;
        }
        
        logger.debug("addEdge->myPlaces");

        return removeEdgeLocally(vertexId, neighborId);
    }

    /**
     * addVertex creates a new vertex with the provided vertexId.
     * 
     * @param vertexId the ID of the vertex.
     * 
     * @return the global index of the newly created vertex.
     */
    @Override
    public int addVertex(Object vertexId) {
        if (MASS.distributed_map.containsKey(vertexId)) {
            return -1;
        }

        int nodeId = getNodeIdFromGlobalLinearIndex(globalNextPlaceIndex);

        return addVertexPlace(getHosts().get(nodeId), vertexId, null);
    }

    /**
     * addVertex adds an empty vertex to the graph.
     * 
     * @return The ID of the vertex if successful, -1 otherwise.
     */
    public int addVertex() {
        return addVertexWithParams(null);
    }

    /**
     * addVertexWithParams constructs a new vertex with the provided init params and 
     * adds it to the graph.
     * @param initParams The parameters to pass to the vertex constructor.
     * 
     * @return The vertexID if the vertex was successfully added, -1 otherwise.
     */
    public int addVertexWithParams(Object initParams) {
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
            initParams
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

    /**
     * addVertexOnNode creates a new VertexPlace at the node with the provided
     * nodeID and instantiates it with the provided vertex parameters.
     * 
     * @param nodeID The node ID of the node with which to add the vertex.
     * @param vertexInitParams The init paramters for the VertexPlace.
     * @param vertexID The ID of the vertex.
     * @return true if successful, false otherwise.
     */
    boolean addVertexOnNode(int nodeID, int vertexID, Object vertexInitParams) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add it.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteVertex(nodeID, vertexID, vertexInitParams);
        }

        // Get local index and size of places array.
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist,
        // return false.
        if (localIndex > localSize) { return false; }

        // Create new VertexPlace
        VertexPlace vertexPlace;
        try {
            vertexPlace = objectFactory.getInstance(getClassName(), vertexInitParams);
        } catch (Exception e) {
            MASS.getLogger().error("expection trying to instantiate a new vertex: ", e);
            return false;
        }

        // Set it at the appropriate index if this vertex is to occupy 
        // preallocated space or reclaiming space from a previously removed
        // vertex.
        if (localIndex < localSize) {
            places.set(localIndex, vertexPlace);
            return true;
        }

        // Otherwise, add it to the back.
        places.add(vertexPlace);
        return true;
    }

    /**
     * addRemoteVertex sends a message to the node with the provided nodeID to
     * add a vertex with the provided vertexID and init parameters.
     */
    private boolean addRemoteVertex(int nodeID, int vertexID, Object vertexInitParams) {
        // Get the remote node
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to add vertex.
        Object[] msgContent = new Object[]{vertexID, vertexInitParams};
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_ADD_PLACE,
            getHandle(),
            msgContent
        );

        // Send message and wait for reply
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // getAgentPopulation is currently overloaded to return the success/failure
        // of adding the vertex to the remote node.
        if (replyMsg.getAgentPopulation() < 0) {
            MASS.getLogger().debug("remote node with pid {} failed to add vertex", nodeID);
            return false;
        }

        return true;
    }

    /**
     * removeVertex removes the vertex associated with the provided vertexID
     * from the graph.
     * 
     * @param vertexID The ID of the vertex to be removed.
     * @return true if successful, false otherwise.
     */
    public boolean removeVertex(int vertexID) {
        // If the vertex to be deleted doesn't exist, return false.
        if (vertexID >= nextVertexID) { return false; }

        return removeVertexOnNode(
            getOwnerID(vertexID),
            vertexID
        );
    }

    /**
     * recycleID adds the provided vertexID to the idQueue
     * to be recycled in the next call to addVertex.
     * 
     * @param vertexID The ID of the vertex to be recycled.
     */
    private void recycleID(int vertexID) {
        // If we're the master node, add the vertex ID to our
        // idQueue to be recycled.
        if (MASS.getMyPid() == 0) {
            idQueue.add(vertexID);
        }
    }

    /**
     * removeVertexOnNode removes the vertex associated with the provided
     * vertexID from the node associated with the provided node ID.
     * @param nodeID The ID of the node with which to remove this vertex.
     * @param vertexID The ID of the vertex to be removed.
     * @return true if successful, false otherwise.
     */
    public boolean removeVertexOnNode(int nodeID, int vertexID) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to remove it.
        if (nodeID != MASS.getMyPid()) {
            boolean success = removeRemoteVertex(nodeID, vertexID);
            // If successful, enqueue ID for use with next added vertex.
            if (success) { recycleID(vertexID); }

            return success;
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        // TODO(#165) Get VertexPlace and traverse incoming edges to ensure
        // they're removed from the respective vertex places.

        // Set vertex as null to indicate it's unused and add it to the 
        // idQueue.
        places.set(localIndex, null);
        recycleID(vertexID);

        return true;
    }

    private boolean removeRemoteVertex(int nodeID, int vertexID) {
        // Get the remote ndoe
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
            Message.ACTION_TYPE.MAINTENANCE_REMOVE_PLACE,
            getHandle(),
            Integer.valueOf(vertexID)
        );

        // Send message and wait for reply.
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // Message system currently only returns an ACK if successful
        // so if we do not receive one, assume failure.
        if (replyMsg.getAction() != Message.ACTION_TYPE.ACK) {
            return false;
        }

        return true;
    }

    /**
     * getOwnerID returns the ID of the node that owns the provided
     * global index.
     * 
     * @param vertexID The vertex ID for which the owner is being requested.
     * @return the ID of the owning node.
     */
    public int getOwnerID(int vertexID) {
        return vertexID % MASS.getSystemSize();
    }

    /**
     * addVertex creates a new vertex, passing the constructor the provided
     * init paramters and assigns it the provided vertexId.
     * 
     * @param vertexId the ID of the vertex.
     * @param vertexInitParam the init parameters to be passed to the 
     * vertex constructor.
     * 
     * @return the global index of the newly created vertex.
     */
    @Override
    public int addVertex(Object vertexId, Object vertexInitParam) {
        if (MASS.distributed_map.containsKey(vertexId)) {
            return -1;
        }
        
        int nodeId = getNodeIdFromGlobalLinearIndex(globalNextPlaceIndex);
        
        return addVertexPlace(getHosts().get(nodeId), vertexId, vertexInitParam);
    }

    /**
     * addVertexPlace creates a new vertex on the provide host, passing the constructor the provided
     * init paramters and assigns it the provided vertexId.
     * 
     * @param host the hostname of the node on which to create the new vertex.
     * @param vertexId the ID of the vertex.
     * @param vertexInitParam the init parameters to be passed to the 
     * vertex constructor.
     * 
     * @return the global index of the newly created vertex.
     */
    private int addVertexPlace(String host, Object vertexId, Object vertexInitParam) {
        if (MASSBase.getMyHostname().equals(host)) {
            return addPlaceLocally(vertexId, vertexInitParam);
        }

        Object [] param = new Object[] { vertexId, vertexInitParam };
        
        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_PLACE, getHandle(), param);

        Optional<MNode> hostOption = MASS.getAllNodes().stream().filter(node -> node.getHostName().equals(host)).findFirst();

        if (!hostOption.isPresent()) {
            MASSBase.getLogger().error("Failed to send addPlace message to " + host + "; host not found");
            return -1;
        }
    
        hostOption.get().sendMessage(message);

        Message m = hostOption.get().receiveMessage();

        int globalIndex = m.getAgentPopulation();
        if (globalIndex == -1) { 
            MASSBase.getLogger().error("remote node " + host + " failed to add vertex: " + vertexId);
            return -1; 
        }

        // update distributed map and increment globalNextPlaceIndex.
        MASS.distributed_map.put(vertexId, m.getAgentPopulation());
        globalNextPlaceIndex++;
        
        return globalIndex;
    }

    /**
     * addPlaceLocally creates a new vertex on the calling node, passing the 
     * constructor the provided init paramters and assigns it the provided 
     * vertexId.
     * 
     * @param vertexId the ID of the vertex.
     * @param vertexInitParam the init parameters to be passed to the 
     * vertex constructor.
     * 
     * @return the global index of the newly created vertex.
     */
    public int addPlaceLocally(Object vertexId, Object vertexInitParam) {
        Log4J2Logger logger = MASSBase.getLogger();

        int stripe = getSize()[0] / MASS.getSystemSize();
        int remainder = getSize()[0] % MASS.getSystemSize();
        int chunkSize = MASS.getMyPid() < remainder ? stripe + 1 : stripe;
        
        int layer = localNextPlaceIndex / chunkSize;
        int relativeIndex = localNextPlaceIndex % chunkSize;

        // If we require a new layer to be created, do so.
        if (layer >= placesVector.size()) {
            placesVector.add(new Vector<>(chunkSize));
        }

        try {
            VertexPlace newPlace = objectFactory.getInstance(getClassName(), vertexInitParam);
            int leftIndex = getNodeLeftIndex(MASS.getMyPid(), stripe, remainder);
            int globalIndex = getSize()[0] * layer + leftIndex + relativeIndex;

            // Index starts after the initial set
            newPlace.setIndex(new int[] { globalIndex });

            placesVector.get(layer).add(relativeIndex, newPlace);

            MASS.distributed_map.put(vertexId, globalIndex);

            // Increment nextPlace indices.
            localNextPlaceIndex++;
            if (MASSBase.getMyPid() == 0) { globalNextPlaceIndex++; }

            return globalIndex;
        } catch (Exception e) {
            logger.error("Exception adding new vertex place locally", e);
        }

        return -1;
    }

    /**
     * removeVertex removes the vertex with the provided vertexId.
     * 
     * @param vertexId the ID of the vertex.
     * 
     * @return true if the vertex was successfully removed, false otherwise.
     */
    @Override
    public boolean removeVertex(Object vertexId) {
        if (MASS.distributed_map.getOrDefault(vertexId, -1) == -1) {
            return false;
        }

        // remove the neighbor from all neighbors
        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_REMOVE_PLACE, getHandle(), (Object) vertexId);

        // This needs to remove neighbors anyways so just send to everyone else
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(message));

        // remove locally
        removeVertexLocally(vertexId);

        return true;
    }

    /**
     * removeVertex removes the vertex with the provided vertexId from 
     * the calling node.
     * 
     * @param vertexId the ID of the vertex.
     * 
     * @return true if the vertex was successfully removed, false otherwise.
     */
    public void removeVertexLocally(Object vertexId) {
        int globalIndex = MASSBase.distributed_map.get(vertexId);

        Place vertexPlace = null;

        // Remove this vertex as a neighbor from all places owned
        for (Vector<VertexPlace> layer : placesVector) {
            for (VertexPlace place : layer) {
                if (place.getIndex()[0] == globalIndex) {
                    vertexPlace = place;
                }
                
                place.removeNeighborSafely(vertexId);
            }

            if (vertexPlace != null) {
                layer.remove(vertexPlace);
            }
        }
    }

    /**
     * getVertexMetaValues creates and returns a pairing fo the global
     * index and owning node for the provided vertexId.
     * 
     * @param vertexId The ID of the vertex to lookup.
     * @return The global index and ID of the node that owns it.
     */
    public VertexMetaValues getVertexMetaValues(Object vertexId) {
        int id = -1;
        int pid = -1;

        int globalIndex = MASSBase.distributed_map.getOrDefault(vertexId, -1);

        if (globalIndex != -1) {
            id = globalIndex;

            pid = getNodeIdFromGlobalLinearIndex(globalIndex);
        }

        return new VertexMetaValues(id, pid);
    }

    /**
     * getLocalIndex returns the local index into the places vector
     * given the globalLinearIndex.
     * @param globalLinearIndex The global linear index for which you'd like 
     * the local index.
     * @return the local index into the places vector
     * given the globalLinearIndex.
     */
    private int getLocalIndex(int globalLinearIndex) {
        int layer = getLayer(globalLinearIndex);
        int stripe = getSize()[0] / MASS.getSystemSize();
        int remainder = getSize()[0] % MASS.getSystemSize();
        int leftIndex = getNodeLeftIndex(MASS.getMyPid(), stripe, remainder);
        int relativeOffset = layer * getSize()[0];

        return globalLinearIndex - relativeOffset - leftIndex;
    }

    /**
     * getLayer returns the layer for which the global linear index should 
     * reside.
     * @param globalLinearIndex The global linear index for which you'd like the
     * layer.
     * @return The layer that contains global linear index.
     */
    private int getLayer(int globalLinearIndex) {
        return globalLinearIndex / getSize()[0];
    }

    /**
     * getNodeIdFromGlobalLinearIndex returns the ID of the node that owns
     * the provided global linear index.
     * @param globalLinearIndex The global linear index for which you would like
     * the owner.
     * @return the owner ID of the provided global linear index.
     */
    public int getNodeIdFromGlobalLinearIndex(final int globalLinearIndex) {
        int relativeIdx = globalLinearIndex % getSize()[0];

        return getNodeId(relativeIdx, MASS.getSystemSize(), getSize()[0]);
    }

    /**
     * getNodeId returns the appropriate node ID for the provided relativeIndex
     * given the number of nodes and the size of layers in the simulation space.
     * 
     * @param relativeIndex the index relative to the layer. For example, if our
     * simulation space was initialized to a size of 10, meaning we have 10 indices
     * per layer, and we wanted the owner of index 14. Its relative index would be 
     * index 4 of layer 1.
     * @param numNodes the number of nodes in the system.
     * @param size the size of the simulation space.
     * 
     * @return the node ID of the node that owns the provided global linear index.
     */
    public static int getNodeId(final int relativeIndex, int numNodes, int size) {
        // Calculate stripe, remainder, and the left and right indices of our node "array"
        int stripe = size / numNodes;
        int remainder = size % numNodes;
        int l = 0;
        int r = numNodes - 1;

        // Perform binary search over the node stripes to find which node this
        // global index belongs to.
        while (l <= r) {
            int m = l + (r - l) / 2;
            int left_i = getNodeLeftIndex(m, stripe, remainder);
            int right_i = getNodeRightIndex(m, left_i, stripe, remainder);

            if (relativeIndex >= left_i && relativeIndex <= right_i) {
                return m;
            }

            if (relativeIndex > right_i) {
                l = m + 1;
            } else {
                r = m - 1;
            }
        }
        
        // If we're unable to locate the node, return -1.
        return -1;
    }

    // getNodeLeftIndex retreives the left-side index of the simulation space owned
    // by the provided node ID.
    private static int getNodeLeftIndex(int node, int stripe, int remainder) {
        return node < remainder ? stripe * node + node : stripe * node + remainder;
    }
    
    // getNodeRightIndex retreives the right-side index of the simulation space owned
    // by the provided node ID.
    private static int getNodeRightIndex(int node, int left_i, int stripe, int remainder ) {
        return node < remainder ? left_i + stripe : left_i + stripe - 1;
    }

    /**
     * Get the VertexPlace associated with a global linear index. This
     * currently only works for local vertices.
     * 
     * TODO (#155): Implement ability to retrieve vertices from remote nodes.
     * @param globalLinearIndex The global index of the VertexPlace being
     * retrieved.
     * @return The VertexPlace associated with the global index. null is returned
     * if the VertexPlace cannot be found.
     */
    public VertexPlace getVertexPlace(int globalLinearIndex) {
        // Make sure the VertexPlace is owned by this node.
        int owner = getNodeIdFromGlobalLinearIndex(globalLinearIndex);
        if (owner != MASS.getMyPid()) {
            return null;
        }

        int layer = getLayer(globalLinearIndex);
        int localIdx = getLocalIndex(globalLinearIndex);

        return placesVector.get(layer).get(localIdx);
    }

    public void reallyCallAll(int functionId, Object argument, int tid) {
        for (Vector<VertexPlace> places : placesVector) {
            for (VertexPlace place : places) {
                place.callMethod( functionId, argument );
            }
        }
    }

    public int getExtendedPlacesSize() {
        return placesVector.stream().mapToInt(v -> v.size()).sum();
    }

    public void reallyCallAllWithReturns(int functionId, Object[] returns, Object[] arguments) {
        int bIndex = this.getSize()[0];

        for (int vIndex = 0; vIndex < placesVector.size(); vIndex++) {
            Vector<VertexPlace> places = placesVector.get(vIndex);

            for (int pIndex = 0; pIndex < places.size(); pIndex++) {
                int gIndex = bIndex + bIndex * vIndex + pIndex;

                if (arguments == null || (!(gIndex < arguments.length)))
                    returns[gIndex] = places.get(pIndex).callMethod(functionId, null);
                else
                    returns[gIndex] = places.get(pIndex).callMethod(functionId, arguments[gIndex]);
            }
        }
    }

    public void exchangeAll(int currentFunctionId) {
        int myRank = MASS.getMyPid();

        // do serially but this should be multi-threaded. Maybe we can just use a thread pool
        for (Vector<VertexPlace> places : placesVector) {
            for (VertexPlace place : places) {
                Object[] neighbors = place.getNeighbors();

                place.prepareForExchangeAll();

                for (Object neighborKey : neighbors) {
                    int neighborGlobalLinearIndex = MASSBase.distributed_map.getOrDefault(neighborKey, -1);

                    int owner = getNodeIdFromGlobalLinearIndex(neighborGlobalLinearIndex);

                    Object result = null;

                    if (owner == myRank) {
                        int globalIndex = MASSBase.getGlobalIndexForKey(neighborKey);
                        VertexPlace neighborPlace = getVertexPlace(globalIndex);

                        result = neighborPlace.callMethod(currentFunctionId, null);
                    } else {
                        // call remote
                        Message message = new Message(Message.ACTION_TYPE.GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT, getHandle(), (Object) neighborKey);

                        // This should really be a map
                        Optional<MNode> hostOption = MASS.getAllNodes().stream().filter(node -> node.getPid() == owner).findFirst();

                        if (hostOption.isPresent()) {
                            hostOption.get().sendMessage(message);

                            Message m = hostOption.get().receiveMessage();

                            result = m.getArgument();
                        } else {
                            MASSBase.getLogger().error("Failed to send addPlace message to " + owner + "; host not found");
                        }
                    }

                    place.setNeighborResult(neighborKey, result);
                }
            }
        }
    }

    public Object exchangeNeighbor(int functionId, int neighbor) {
        Optional<VertexPlace> option = placesVector
                .stream().flatMap(Vector::stream)
                .filter(vertexPlace -> vertexPlace.getIndex()[0] == neighbor)
                .findFirst();

        return option.isPresent() ? option.get().callMethod(functionId, null) : null;
    }

    public Vector<Vector<VertexPlace>> getPlacesVector() {
        return placesVector;
    }
}
