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

import java.util.Arrays;
import java.util.List;
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

    private int nextPlaceIndex = 0;

    // Graph maintenance
    private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

    private Vector<Vector<VertexPlace>> placesVector = new Vector<>(1);

    /**
     * Instantiate a PlacesBase for this node
     *
     * @param handle         The Handle ID identifying this PlacesBase
     * @param className      The class that represents a Place
     */
    public GraphPlaces(int handle, String className, String[] graphArgs, Object[] initArgs) {
        super(handle, className, graphArgs, initArgs);

        init_algorithm = GraphInitAlgorithm.FULL_LIST;
        filename = "graph_n.txt";
        input_format = GraphInputFormat.CSV;
    }

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
     * Constructor for empty graph
     */
    public GraphPlaces(int handle, String className, int size) {
        super(handle, className, size, new int[] { size });

        // Should use a different indicator for empty graph
        this.init_algorithm = GraphInitAlgorithm.FULL_LIST;
        this.filename = "";
        this.input_format = GraphInputFormat.CSV;
    }
    
    /**
     * Constructor for empty graph - remote node
     */
    public GraphPlaces(int handle, String className, int size, boolean _remote_node) {
        super(handle, className);

        // Should use a different indicator for empty graph
        this.init_algorithm = GraphInitAlgorithm.FULL_LIST;
        this.filename = "";
        this.input_format = GraphInputFormat.CSV;
        
        init_all_graph_blank(size);
    }

    protected void reinitialize() {
        super.reinitialize();

        nextPlaceIndex = 0;
        placesVector = new Vector<>(1);
    }

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
     * Send the place_initialize_graph message to all nodes
     * @param argument
     * @param boundaryWidth - unused
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


    @Override
    protected void init_all(Object argument) {
        if (argument instanceof Integer) {
            init_all_graph_blank((Integer) argument);
        } else {
            Object[] arguments = (Object[]) argument;

            // TODO: This is failing in kotlin
//            String[] graphArguments = (String[]) Arrays.copyOfRange(arguments, 0, 2);
//            Object[] initArguments = (Object[]) Arrays.copyOfRange(arguments, 2, arguments.length);

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

        // TODO: Missing weights
        newGraph.getVertices()
                .forEach(vertex -> vertex.neighbors
                        .forEach(neighbor -> addEdge(vertex.id, neighbor, 1.0)));
    }

    public void merge(GraphModel source, GraphModel remoteGraphs) {
        source.getVertices().addAll(remoteGraphs.getVertices());
    }

    private static List<Integer> mapNeighborIndicesToAttributes(Vector<Integer> neighborIndices) {
        return neighborIndices
                .stream()
                .map(MASSBase.distributed_map::reverseLookup)
                .map(Integer.class::cast)
                .collect(Collectors.toList());
    }

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
                
                //graph.addVertex(vPlace.getIndex()[0], vPlace.neighbors);
                // TODO: Missing weights
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
            //graph.merge(getRemoteGraphs());
            merge(graph, getRemoteGraphs());
        }

        return graph;
    }

    private GraphModel getRemoteGraphs() {
        GraphModel graph = new GraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES, getHandle(), null));

            Message m = node.receiveMessage();

            if (m.getAction() != Message.ACTION_TYPE.MAINTENANCE_GET_PLACES_RESPONSE) {
                throw new RuntimeException("Received incorrect response from node");
            } else {
                GraphModel model = (GraphModel) m.getArgument();

                //graph.merge(model);
                merge(graph, model);
            }
        }

        return graph;
    }

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

    @Override
    public boolean addEdge(Object vertexId, Object neighborId, double weight) {
        boolean added = false;

        if (!validNeighbor(vertexId, neighborId)) {
            return false;
        }

        Log4J2Logger logger = MASSBase.getLogger();

        logger.error(String.format("addEdge [vertexId=%s; neighborId=%s; weight=%f]", vertexId.toString(), neighborId.toString(), weight));

        int globalIndex = MASSBase.distributed_map.getOrDefault(vertexId, -1);

        if (globalIndex < 0) {
            return false;
        }

        int ownerId = getNodeIdFromGlobalLinearIndex(globalIndex);

        if (ownerId == MASSBase.getMyPid()) {
            logger.error("addEdge->myPlaces");

            added = addEdgeLocally(vertexId, neighborId, weight);
        } else {
            logger.error("addEdge->remotePlace");

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

    public boolean addEdgeLocally(Object vertexId, Object neighborId, double weight) {
        int globalIndex = MASSBase.getGlobalIndexForKey(vertexId);

        int spanSize = getSize()[0];
        int chunkSize = spanSize / MASS.getSystemSize();

        int placesIndex = globalIndex / spanSize - 1;

        boolean added = false;
        
        if (placesIndex >= 0 && placesVector.size() >= 0 && placesVector.size() > placesIndex) {
            int localPlaceIndex = globalIndex % chunkSize;

            VertexPlace place = placesVector.get(placesIndex).get(localPlaceIndex);

            place.addNeighbor(neighborId, weight);
            
            added = true;
        } else {
            MASSBase.getLogger().error("Error trying to add edge [placesIndex=" + placesIndex + "]");
        }

        return added;
    }

    public boolean removeEdgeLocally(Object vertexId, Object neighborId) {
        int globalIndex = MASSBase.getGlobalIndexForKey(vertexId);

        int spanSize = getSize()[0];
        int chunkSize = spanSize / MASS.getSystemSize();

        int placesIndex = globalIndex / spanSize - 1;

        if (placesIndex >= 0 && placesVector.size() >= 0 && placesVector.size() > placesIndex) {
            int localPlaceIndex = globalIndex % chunkSize;

            VertexPlace place = placesVector.get(placesIndex).get(localPlaceIndex);

            place.removeNeighbor(neighborId);
        } else {
            MASSBase.getLogger().error("Error trying to add edge [placesIndex=" + placesIndex + "]");
        }

        return false;
    }

    @Override
    public boolean removeEdge(Object vertexId, Object neighborId) {
        if (MASSBase.distributed_map.getOrDefault(vertexId, -1) == -1) {
            return false;
        }

        boolean removed = false;

        Log4J2Logger logger = MASSBase.getLogger();

        logger.error(String.format("removeEdge [vertexId=%s; neighborId=%s]", vertexId, neighborId));

        int globalIndex = MASSBase.distributed_map.getOrDefault(vertexId, -1);

        if (globalIndex < 0) {
            return false;
        }

        int ownerId = getNodeIdFromGlobalLinearIndex(globalIndex);

        if (ownerId == MASSBase.getMyPid()) {
            logger.error("addEdge->myPlaces");

            removed = removeEdgeLocally(vertexId, neighborId);
        } else {
            logger.error("addEdge->remotePlace");

            VertexMetaValues values = getVertexMetaValues(vertexId);

            int owner = values.OwnerPid;

            if (owner != -1) {
                for (MNode node : MASSBase.getRemoteNodes()) {
                    if (node.getPid() == owner) {
                        node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_REMOVE_EDGE, getHandle(), new Object[] { vertexId, neighborId, null }));
                    }
                }
            }

            logger.warning("Cannot add edge: source is out of range(" + vertexId + ")");
        }

        return false;
    }

//    private int getPlaceCountForHost(String host) {
//        // TODO: where can we get the monitoring port
//        String resource = String.format("ws://%s:%d/", host, MonitorConnector.getInstance().getPort());
//
//        int size = -1;
//
//        Log4J2Logger logger = MASSBase.getLogger();
//
//        FetchPlacesListener listener = new FetchPlacesListener();
//
//        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
//                .buildAsync(URI.create(resource), listener).join();
//
//        try {
//            socket.sendText("{ \"action\": \"FETCH\", \"handle\": \"PLACES\" }", false);
//
//            if (listener.await()) {
//                size = listener.response.message.get(1).placesSize;
//            }
//        } catch (Exception e) {
//            logger.error("sendText exception: ", e);
//        }
//
//        return size;
//    }

//    public Integer[] getTopology() {
//        Vector<String> hosts = getHosts();
//
//        Integer [] placesSizes = new Integer[hosts.size()];
//
//        for (int i = 0; i < hosts.size(); i++) {
//            String host = hosts.get(i);
//
//            placesSizes[i] = getPlaceCountForHost(host);
//        }
//
////        try {
////            locks.wait();
////        } catch (InterruptedException e) {
////            MASSBase.getLogger().error("Error waiting for websocket locks", e);
////        }
//
//        return placesSizes;
//    }

    @Override
    public int addVertex(Object vertexId) {
        if (MASS.distributed_map.containsKey(vertexId)) {
            return -1;
        }

        // TODO: use MASS Monitoring
//        Integer [] result = getTopology();
//
//        int smallestIndex = 0;
//        int smallestSize = result[0];
//
//        for (int i = 1; i < result.length; i++) {
//            if (result[i] < smallestSize) {
//                smallestIndex = i;
//                smallestSize = result[i];
//            }
//        }

        int nodeId = getNodeIdFromGlobalLinearIndex(nextPlaceIndex);


        return addVertexPlace(getHosts().get(nodeId), vertexId, null);
    }

    @Override
    public int addVertex(Object vertexId, Object vertexInitParam) {
        if (MASS.distributed_map.containsKey(vertexId)) {
            return -1;
        }
        
        // Placement
        int nodeId = getNodeIdFromGlobalLinearIndex(nextPlaceIndex);
        
        return addVertexPlace(getHosts().get(nodeId), vertexId, vertexInitParam);
    }

    private int addVertexPlace(String host, Object vertexId, Object vertexInitParam) {
        if (MASSBase.getMyHostname().equals(host)) {
            return addPlaceLocally(vertexId, vertexInitParam);
        }

        Object [] param = new Object[] { vertexId, vertexInitParam };
        
        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_PLACE, getHandle(), param);

        Optional<MNode> hostOption = MASS.getAllNodes().stream().filter(node -> node.getHostName().equals(host)).findFirst();
    
        if (hostOption.isPresent()) {
            hostOption.get().sendMessage(message);

            Message m = hostOption.get().receiveMessage();

            nextPlaceIndex++; //added line jonathan
            MASS.distributed_map.put(vertexId, m.getAgentPopulation()); // added jonathan Acoltzi
            MASSBase.getLogger().debug("in addVertex Place m.getAgentPopulation(): " + m.getAgentPopulation());
            return m.getAgentPopulation();
        } else {
            System.out.println("no host found: " + host);
            MASSBase.getLogger().error("Failed to send addPlace message to " + host + "; host not found");
        }

        return -1;
    }

    public int addPlaceLocally(Object vertexId, Object vertexInitParam) {
        Log4J2Logger logger = MASSBase.getLogger();

        int [] placesIndex = getPlacesIndex();

        int chunkSize = getSize()[0] / MASS.getSystemSize();

        int lowerBoundary = chunkSize * MASS.getMyPid() * placesIndex[0];

        int upperBoundary = lowerBoundary + chunkSize;

        if (placesVector.size() <= 0 || placesVector.size() - 1 < placesIndex[0]) {
            placesVector.add(new Vector<>(chunkSize));
        } else if (placesIndex[1] < lowerBoundary || placesIndex[1] >= upperBoundary) {
            logger.error("Place index outside of bounds: " + placesIndex[1]);

            return -1;
        }

        try {
            VertexPlace newPlace = objectFactory.getInstance(getClassName(), vertexInitParam);

            int globalIndex = getSize()[0] + chunkSize * MASS.getMyPid() + nextPlaceIndex;

            // Index starts after the initial set
            newPlace.setIndex(new int[] { globalIndex });

            placesVector.get(placesIndex[0]).add(placesIndex[1], newPlace);

            MASS.distributed_map.put(vertexId, globalIndex);

            nextPlaceIndex++;

            return globalIndex;
        } catch (Exception e) {
            logger.error("Exception adding new vertex place locally", e);
        }

        return -1;
    }

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

        return false;
    }

    public void removeVertexLocally(Object vertexId) {
        int globalIndex = MASS.distributed_map.get(vertexId);

        Place vertexPlace = null;

        // Remove this vertex as a neighbor from all places owned
        for (Vector<VertexPlace> places : placesVector) {
            for (VertexPlace place : places) {
                if (place.getIndex()[0] == globalIndex) {
                    vertexPlace = place;
                }
                
                place.removeNeighborSafely(vertexId);
            }

            if (vertexPlace != null) {
                places.remove(vertexPlace);
            }
        }
    }

    private int [] getPlacesIndex() {
        int linearSize = getSize()[0];

        return new int [] { nextPlaceIndex / linearSize, nextPlaceIndex % linearSize };
    }

    public VertexMetaValues getVertexMetaValues(Object vertexId) {
        int id = -1;
        int pid = -1;

        int globalIndex = MASSBase.distributed_map.getOrDefault(vertexId, -1);

        if (globalIndex != -1) {
            final int chunkSize = this.getSize()[0] / MASSBase.getSystemSize(); //not used

            id = globalIndex;

            pid = getNodeIdFromGlobalLinearIndex(globalIndex);
        }

        return new VertexMetaValues(id, pid);
    }

    // TODO: Should we make a globallinearindex type?
    // then we could do index.getNode()
    public int getNodeIdFromGlobalLinearIndex(final int globalLinearIndex) {
        // Calculate layer and relative index.
        int layer = globalLinearIndex / getSize()[0];
        int relativeIdx = globalLinearIndex - layer * getSize()[0];

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
     * Get the place associated with a global linear index
     * @param globalLinearIndex
     * @return
     */
    public VertexPlace getVertexPlace(int globalLinearIndex) {
        
        int networkSize = getSize()[0];
        int localPlacesIndex = globalLinearIndex / networkSize - 1;
        int chunkSize = networkSize / MASS.getSystemSize();
       
        int placeIndex = (globalLinearIndex  )% chunkSize;

        return placesVector.get(localPlacesIndex).get(placeIndex);
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
        int networkSize = getSize()[0];
        int chunkSize = networkSize / MASS.getSystemSize();
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
