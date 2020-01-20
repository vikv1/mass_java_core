package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
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

                processRequest(request, outStream);
            } catch (IOException | ClassNotFoundException e) {
                massLogger.error("Error handling remote request", e);
            }
        }

        private void processRequest(String request, ObjectOutputStream outStream) {
            Supplier<Object> processor = requestProcessors.get(request);

            try {
                outStream.writeObject(processor.get());
            } catch (IOException e) {
                massLogger.error("Error sending result to client", e);
            }
        }
    }
}
