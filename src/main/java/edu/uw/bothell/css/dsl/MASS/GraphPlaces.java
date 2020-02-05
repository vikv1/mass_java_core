package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.monitoring.MonitorConnector;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.*;

public class GraphPlaces extends Places implements Graph {
    private final GraphInitAlgorithm init_algorithm;
    private final String filename;
    private final GraphInputFormat input_format;

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
        GraphModel graph = new GraphModel();

        if (!(getPlaces()[0] instanceof VertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not VertexPlaces.");

            return graph;
        }

        for (Place place : getPlaces()) {
            VertexPlace vPlace = (VertexPlace) place;

            graph.addVertex(vPlace.getIndex()[0], vPlace.neighbors);
        }

        return graph;
    }

    @Override
    public boolean addEdge(int vertexId, int neighborId, double weight) {
        boolean added = false;

        Log4J2Logger logger = MASSBase.getLogger();

        if (vertexId < 0 || vertexId < getPlaces().length) {
            VertexPlace vertexPlace = (VertexPlace) getPlaces()[vertexId];

            try {
                vertexPlace.addNeighbor(neighborId);

                added = true;
            } catch (IllegalArgumentException iae) {
                logger.warning("Exception encountered addingEdge: " + iae);
            }
        } else {
            logger.warning("Cannot add edge: source is out of range(" + vertexId + ")");
        }

        return added;
    }

    @Override
    public boolean removeEdge(int vertexId, int neighborId) {
        return false;
    }

    private int [] placeSizes;

    private int getPlaceCountForHost(String host, Vector<Integer> locks, int index) {
        // TODO: where can we get the monitoring port
        String resource = String.format("ws://%s:%d/", host, MonitorConnector.getInstance().getPort());

        int size = -1;

        Log4J2Logger logger = MASSBase.getLogger();

        CountDownLatch latch = new CountDownLatch(1);

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                logger.trace("open");

                WebSocket.Listener.super.onOpen(webSocket);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                logger.trace("text");

                placeSizes[index] = 10;

                latch.countDown();

                return WebSocket.Listener.super.onText(webSocket, data, last);
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                logger.trace("Binary");

                return null;
            }

            @Override
            public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
                logger.trace("ping");

                return null;
            }

            @Override
            public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                logger.trace("pong");

                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                logger.trace("close");

                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                logger.trace("error: " + error);
            }
        };
        WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(resource), listener).join();

        try {
            socket.sendText("{ \"action\": \"FETCH\", \"handle\": \"STATUS\" }", false);

            latch.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("sendText exception: ", e);
        }

        return size;
    }

    private Object getTopology() {
        Vector<String> hosts = getHosts();

        Vector<Integer> locks = new Vector<>(hosts.size());

        for (int i = 0; i < hosts.size(); i++) {
            String host = hosts.get(i);

            int placeCount = getPlaceCountForHost(host, locks, i);
        }

//        try {
//            locks.wait();
//        } catch (InterruptedException e) {
//            MASSBase.getLogger().error("Error waiting for websocket locks", e);
//        }

        return null;
    }

    @Override
    public int addVertex() {
        Object result = getTopology();

        return addPlace();
    }

    @Override
    public boolean removeVertex(int vertexId) {
        return false;
    }
}
