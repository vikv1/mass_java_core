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
import java.util.Optional;
import java.util.Vector;

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
     * @param boundary_width The width of the boundary between nodes, used to calculate shadow space
     * @param argument       The argument to supply to the Place during initialization
     * @param size           Matrix dimensions, as an array of integers representing dimension sizes
     */
    private GraphPlaces(int handle, String className, int boundary_width, Object argument, int[] size) {
        super(handle, className, boundary_width, argument, size);

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
        Object [] arguments = (Object[]) argument;

        String [] graphArguments = (String [])Arrays.copyOfRange(arguments, 0, 2);
        Object [] initArguments = (Object [])Arrays.copyOfRange(arguments, 2, arguments.length);

        init_all_graph(graphArguments, initArguments);
    }

    /**
     * Graph interface implementation
     */
    @Override
    public GraphModel getGraph() {
        return getGraph(true);
    }

    @Override
    public GraphModel getGraph(boolean all) {
        GraphModel graph = new GraphModel();

        if (!(getPlaces()[0] instanceof VertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not VertexPlaces.");

            return graph;
        }

        // Places on master node
        for (Place place : getPlaces()) {
            VertexPlace vPlace = (VertexPlace) place;

            graph.addVertex(vPlace.getIndex()[0], vPlace.neighbors);
        }

//        if (all) {
//            graph.merge(getRemoteGraphs());
//        }

        return graph;
    }

    private GraphModel getRemoteGraphs() {
        GraphModel graph = new GraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES));

            Message m = node.receiveMessage();

            if (m.getAction() != Message.ACTION_TYPE.MAINTENANCE_GET_PLACES_RESPONSE) {
                throw new RuntimeException("Received incorrect response from node");
            } else {
                GraphModel model = (GraphModel) m.getArgument();

                graph.merge(model);
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

        PlacesBase myPlaces = MASSBase.getCurrentPlacesBase();

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
                        node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_EDGE, new Object[] { vertexId, neighborId, weight }));
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

    @Override
    public boolean removeEdge(int vertexId, int neighborId) {
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

        Integer [] result = getTopology();

        int smallestIndex = 0;
        int smallestSize = result[0];

        for (int i = 1; i < result.length; i++) {
            if (result[i] < smallestSize) {
                smallestIndex = i;
                smallestSize = result[i];
            }
        }

        return addVertexPlace(getHosts().get(smallestIndex), vertexId);
    }

    private int addVertexPlace(String host, int vertexId) {
        if (MASSBase.getMyHostname().equals(host)) {
            return addPlaceLocally(vertexId);
        }

        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_ADD_PLACE, vertexId);

        Optional<MNode> hostOption = MASS.getAllNodes().stream().filter(node -> node.getHostName().equals(host)).findFirst();

        if (hostOption.isPresent()) {
            hostOption.get().sendMessage(message);

            Message m = hostOption.get().receiveMessage();

            return (int) m.getArgument();
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

            int globalIndex = getSize()[0] + nextPlaceIndex;

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
        return false;
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
}
