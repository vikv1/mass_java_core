package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CytoscapeListener implements MASSListener {
    private static final int LISTENER_PORT = 8165;
    private final Log4J2Logger massLogger;
    private Thread listenerThread;

    private Map<String, GraphRequest> baseProcessors = new HashMap<>();

    public CytoscapeListener(Graph graph) {
        massLogger = MASSBase.getLogger();

        initThreads(graph);
    }

    private void initThreads(Graph graph) {
        listenerThread = new Thread(new ListenerRunner(graph, LISTENER_PORT));
        
        listenerThread.start();
    }

    @Override
    public void finish() {
        try {
            listenerThread.join();
        } catch (InterruptedException e) {
            massLogger.error("Exception trying to join listener threads", e);
        }
    }

    @Override
    public void registerProcessor(String key, GraphRequest requestProcessor) {
        baseProcessors.put(key, requestProcessor);
    }

    private class ListenerRunner implements Runnable {
        private final int port;
        private Graph graph;

        private Map<String, Supplier<Object>> requestProcessors;

        ListenerRunner(Graph graph, int port) {
            this.graph = graph;
            this.port = port;

            initProcessors();
        }

        private void initProcessors() {
            requestProcessors = new HashMap<>();

            requestProcessors.put("getGraph", graph::getGraph);
        }

        @Override
        public void run() {
            try {
                ServerSocket socket = new ServerSocket(port);

                while (true) {
                    Socket remoteSocket = socket.accept();

                    handleRequest(remoteSocket);
                }
            } catch (IOException e) {
                massLogger.error("Exception creating socket", e);
            }
        }

        private void handleRequest(Socket remoteSocket) {
            try {
                ObjectInputStream inStream = new ObjectInputStream(remoteSocket.getInputStream());
                ObjectOutputStream outStream = new ObjectOutputStream(remoteSocket.getOutputStream());

                String request = (String) inStream.readObject();

                GraphRequest graphRequest = parseRequest(request, inStream);

                processRequest(graphRequest, outStream);
            } catch (IOException | ClassNotFoundException e) {
                massLogger.error("Error handling remote request", e);
            }
        }

        private GraphRequest parseRequest(String request, ObjectInputStream inStream) {
            GraphRequest graphRequest = null;

            switch (request) {
                case "getGraph":
                    graphRequest = () -> {
                        Supplier<Object> processor = requestProcessors.get(request);

                        return processor.get();
                    };

                    break;
                case "setGraph":
                    graphRequest = new GraphRequest() {
                        private ObjectInputStream stream = inStream;

                        public Object process() {
                            try {
                                GraphModel model = (GraphModel) stream.readObject();

                                graph.setGraph(model);

                                if (baseProcessors.containsKey("countTriangles")) {
                                    baseProcessors.get("countTriangles").process();
                                }

                                return "Success";
                            } catch (IOException e) {
                                massLogger.error("Exception processing setGraph", e);
                            } catch (ClassNotFoundException e) {
                                massLogger.error("Java exception processing setGraph", e);
                            }

                            return "Failure";
                        }
                    };

                    break;
                default:
                    graphRequest = baseProcessors.getOrDefault(request, () -> "Operation not implemented: " + request);
            }

            return graphRequest;
        }

        private void processRequest(GraphRequest request, ObjectOutputStream outStream) {
            try {
                outStream.writeObject(request.process());
            } catch (IOException e) {
                massLogger.error("Error sending result to client", e);
            } catch (Exception e) {
                massLogger.error("Exception encountered processing request", e);
            }
        }
    }
}
