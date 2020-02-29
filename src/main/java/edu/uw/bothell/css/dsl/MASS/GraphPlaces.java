package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.VertexMetaValues;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.monitoring.FetchPlacesListener;
import edu.uw.bothell.css.dsl.MASS.monitoring.MonitorConnector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

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

    public GraphPlaces(int handle, String className, int size) {
        super(handle, className, size, new int[] { size });

        // Should use a different indicator for empty graph
        this.init_algorithm = GraphInitAlgorithm.FULL_LIST;
        this.filename = "";
        this.input_format = GraphInputFormat.CSV;
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

            String[] graphArguments = (String[]) Arrays.copyOfRange(arguments, 0, 2);
            Object[] initArguments = (Object[]) Arrays.copyOfRange(arguments, 2, arguments.length);

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

        if (getPlaces() != null && !(getPlaces()[0] instanceof VertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not VertexPlaces.");

            return graph;
        }

        // Places on master node
        if (getPlaces() != null) {
            for (Place place : getPlaces()) {
                VertexPlace vPlace = (VertexPlace) place;

                //graph.addVertex(vPlace.getIndex()[0], vPlace.neighbors);
                graph.addVertex((Integer) vPlace.getAttribute(), vPlace.neighbors);
            }
        }

        for (Vector<VertexPlace> places : placesVector) {
            for (VertexPlace place : places) {
                Integer attribute = (Integer) MASSBase.distributed_map.reverseLookup(place.getIndex()[0]);

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

    public boolean validNeighbor(final int vertexId, final int neighborId) {
        if (MASSBase.distributed_map.getOrDefault(vertexId, -1) == -1
            || MASSBase.distributed_map.getOrDefault(neighborId, -1) == -1) {
            return false;
        }

        return true;
    }

    @Override
    public boolean addEdge(int vertexId, int neighborId, double weight) {
        boolean added = false;

        if (!validNeighbor(vertexId, neighborId)) {
            return false;
        }

        Log4J2Logger logger = MASSBase.getLogger();

        logger.error(String.format("addEdge [vertexId=%d; neighborId=%d; weight=%f]", vertexId, neighborId, weight));

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
                    }
                }
            }

            logger.warning("Cannot add edge: source is out of range(" + vertexId + ")");
        }

        return added;
    }

    public boolean addEdgeLocally(int vertexId, int neighborId, double weight) {
        int globalIndex = MASSBase.getGlobalIndexForKey(vertexId);

        int spanSize = getSize()[0];
        int chunkSize = spanSize / MASS.getSystemSize();

        int placesIndex = globalIndex / spanSize - 1;

        if (placesIndex >= 0 && placesVector.size() >= 0 && placesVector.size() > placesIndex) {
            int localPlaceIndex = globalIndex % chunkSize;

            VertexPlace place = placesVector.get(placesIndex).get(localPlaceIndex);

            place.addNeighbor(neighborId, weight);
        } else {
            MASSBase.getLogger().error("Error trying to add edge [placesIndex=" + placesIndex + "]");
        }

        return false;
    }

    public boolean removeEdgeLocally(int vertexId, int neighborId) {
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
    public boolean removeEdge(int vertexId, int neighborId) {
        if (MASSBase.distributed_map.getOrDefault(vertexId, -1) == -1) {
            return false;
        }

        boolean removed = false;

        Log4J2Logger logger = MASSBase.getLogger();

        logger.error(String.format("removeEdge [vertexId=%d; neighborId=%d]", vertexId, neighborId));

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

    private int getPlaceCountForHost(String host) {
        // TODO: where can we get the monitoring port
        String resource = String.format("ws://%s:%d/", host, MonitorConnector.getInstance().getPort());

        int size = -1;

        Log4J2Logger logger = MASSBase.getLogger();

        FetchPlacesListener listener = new FetchPlacesListener();

        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(resource), listener).join();

        try {
            socket.sendText("{ \"action\": \"FETCH\", \"handle\": \"PLACES\" }", false);

            if (listener.await()) {
                size = listener.response.message.get(1).placesSize;
            }
        } catch (Exception e) {
            logger.error("sendText exception: ", e);
        }

        return size;
    }

    public Integer[] getTopology() {
        Vector<String> hosts = getHosts();

        Integer [] placesSizes = new Integer[hosts.size()];

        for (int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);

            placesSizes[i] = getPlaceCountForHost(host);
        }

//        try {
//            locks.wait();
//        } catch (InterruptedException e) {
//            MASSBase.getLogger().error("Error waiting for websocket locks", e);
//        }

        return placesSizes;
    }

    @Override
    public int addVertex(int vertexId) {
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


        return addVertexPlace(getHosts().get(nodeId), vertexId);
    }

    private int addVertexPlace(String host, int vertexId) {
        if (MASSBase.getMyHostname().equals(host)) {
            return addPlaceLocally(vertexId);
        }

        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_PLACE, getHandle(), (Object) vertexId);

        Optional<MNode> hostOption = MASS.getAllNodes().stream().filter(node -> node.getHostName().equals(host)).findFirst();

        if (hostOption.isPresent()) {
            hostOption.get().sendMessage(message);

            Message m = hostOption.get().receiveMessage();

            return m.getAgentPopulation();
        } else {
            MASSBase.getLogger().error("Failed to send addPlace message to " + host + "; host not found");
        }

        return -1;
    }

    public int addPlaceLocally(int vertexId) {
        Log4J2Logger logger = MASSBase.getLogger();

        int [] placesIndex = getPlacesIndex();

        int chunkSize = getSize()[0] / MASS.getSystemSize();

        int lowerBoundary = chunkSize * MASS.getMyPid() * placesIndex[0];

        int upperBoundary = lowerBoundary + chunkSize;

        if (placesVector.size() <= 0 || placesVector.size() < placesIndex[0]) {
            placesVector.add(new Vector<>(chunkSize));
        } else if (placesIndex[1] < lowerBoundary || placesIndex[1] >= upperBoundary) {
            logger.error("Place index outside of bounds: " + placesIndex[1]);

            return -1;
        }

        try {
            VertexPlace newPlace = objectFactory.getInstance(getClassName(), null);

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
    public boolean removeVertex(int vertexId) {
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

    public void removeVertexLocally(int vertexId) {
        int globalIndex = MASS.distributed_map.get(vertexId);

        Place vertexPlace = null;

        // Remove this vertex as a neighbor from all places owned
        for (Vector<VertexPlace> places : placesVector) {
            for (VertexPlace place : places) {
                if (place.getIndex()[0] == globalIndex) {
                    vertexPlace = place;
                }
                place.removeNeighbor(vertexId);
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

    public VertexMetaValues getVertexMetaValues(int vertexId) {
        int id = -1;
        int pid = -1;

        int globalIndex = MASSBase.distributed_map.getOrDefault(vertexId, -1);

        if (globalIndex != -1) {
            final int chunkSize = this.getSize()[0] / MASSBase.getSystemSize();

            id = globalIndex;

            pid = getNodeIdFromGlobalLinearIndex(globalIndex);
        }

        return new VertexMetaValues(id, pid);
    }

    // TODO: Should we make a globallinearindex type?
    // then we could do index.getNode()
    public int getNodeIdFromGlobalLinearIndex(final int globalLinearIndex) {
        // indices per places
        int spanSize = getSize()[0];

        int chunkSize = spanSize / MASS.getSystemSize();

        return globalLinearIndex % spanSize / chunkSize;
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

        int placeIndex = globalLinearIndex % chunkSize;

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
                int [] neighbors = place.getNeighbors();

                place.prepareForExchangeAll();

                for (int neighborKey : neighbors) {
                    int owner = getNodeIdFromGlobalLinearIndex(neighborKey);

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
}
