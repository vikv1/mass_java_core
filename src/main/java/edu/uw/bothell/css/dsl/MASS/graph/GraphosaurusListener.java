/*

 	MASS Java Software License
	© 2012-2025 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2025 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.graph;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.Gson;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.GraphPlaces;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.VertexPlace;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphosaurusMessage;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 * WebSocket client that sends agent location updates to Graphosaurus visualization server
 */
public class GraphosaurusListener implements MASSListener {

    protected static final String DEFAULT_WEBSOCKET_URL = "ws://localhost:8080";
    protected static final long DEFAULT_POLL_INTERVAL_MS = 500;
    protected static final String DEFAULT_AGENT_SHAPE = "sphere";
    protected static final double DEFAULT_MOVE_SPEED = 1.0;

    protected final Log4J2Logger massLogger;
    protected final Graph graph;
    protected final GraphPlaces graphPlaces;
    protected final AgentLocationTracker tracker;
    protected final Gson gson;
    protected final String websocketUrl;
    protected final long pollIntervalMs;

    protected final boolean partialLoading;

    protected final ConcurrentLinkedQueue<String> messageQueue;

    protected GraphosaurusWebSocketClient wsClient;
    protected Thread pollingThread;
    protected Thread senderThread;
    protected volatile boolean running = false;

    /**
     * Constructor with default settings
     * 
     * @param graphPlaces The GraphPlaces instance to monitor
     */
    public GraphosaurusListener(GraphPlaces graphPlaces) {
        this(graphPlaces, DEFAULT_WEBSOCKET_URL, DEFAULT_POLL_INTERVAL_MS, false);
    }

    /**
     * Constructor with custom settings
     * 
     * @param graphPlaces The GraphPlaces instance to monitor
     * @param websocketUrl WebSocket server URL
     * @param pollIntervalMs Polling interval in milliseconds
     */
    public GraphosaurusListener(GraphPlaces graphPlaces, String websocketUrl, long pollIntervalMs) {
        this(graphPlaces, websocketUrl, pollIntervalMs, false);
    }

    /**
     * Constructor with custom settings and partial loading option
     * 
     * @param graphPlaces The GraphPlaces instance to monitor
     * @param websocketUrl WebSocket server URL
     * @param pollIntervalMs Polling interval in milliseconds
     * @param partialLoading If true, skip sending the full graph on connect;
     *                       nodes and edges are only sent as agents visit them
     */
    public GraphosaurusListener(GraphPlaces graphPlaces, String websocketUrl, long pollIntervalMs, boolean partialLoading) {
        this.massLogger = MASSBase.getLogger();
        this.graph = graphPlaces;
        this.graphPlaces = graphPlaces;
        this.tracker = new AgentLocationTracker();
        this.gson = new Gson();
        this.websocketUrl = websocketUrl;
        this.pollIntervalMs = pollIntervalMs;
        this.partialLoading = partialLoading;
        this.messageQueue = new ConcurrentLinkedQueue<>();

        initializeConnection();
    }

    /**
     * Initialize WebSocket connection and start polling
     */
    protected void initializeConnection() {
        try {
            URI serverUri = new URI(websocketUrl);
            wsClient = new GraphosaurusWebSocketClient(serverUri);
            wsClient.connect();

            massLogger.debug("Graphosaurus listener connecting to: " + websocketUrl);

            // Wait briefly for connection to establish, then send full graph (unless partial loading)
            if (!partialLoading) {
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        sendFullGraph();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                massLogger.debug("Partial loading enabled - skipping initial full graph send");
            }

            running = true;

            // Start async message sender thread
            senderThread = new Thread(new MessageSenderRunnable(), "GraphosaurusSenderThread");
            senderThread.setDaemon(true);
            senderThread.start();

            // Start polling thread
            pollingThread = new Thread(new PollingRunnable(), "GraphosaurusPollingThread");
            pollingThread.setDaemon(true);
            pollingThread.start();

        } catch (Exception e) {
            massLogger.error("Failed to initialize Graphosaurus connection", e);
        }
    }

    /**
     * Send the entire graph structure to Graphosaurus
     */
    protected void sendFullGraph() {
        try {
            GraphModel graphModel = graph.getGraph();
            if (graphModel == null || graphModel.getVertices() == null) {
                System.out.println("[Graphosaurus] No graph data to send");
                return;
            }

            System.out.println("[Graphosaurus] Sending full graph: " + graphModel.getVertices().size() + " vertices");

            // First pass: send all vertices
            for (VertexModel vertex : graphModel.getVertices()) {
                if (vertex.id != null) {
                    sendVertex(vertex);
                    tracker.markVertexAsSent(String.valueOf(vertex.id));
                }
            }

            // Second pass: send all edges (resolve neighbor IDs to model-level IDs)
            List<VertexModel> allVertices = graphModel.getVertices();
            int edgeCount = 0;
            for (VertexModel vertex : allVertices) {
                if (vertex.id != null && vertex.neighbors != null) {
                    String fromId = String.valueOf(vertex.id);
                    for (Object neighborId : vertex.neighbors) {
                        String toId = resolveNeighborId(neighborId, allVertices);
                        if (!tracker.hasEdgeBeenSent(fromId, toId)) {
                            sendEdge(fromId, toId);
                            tracker.markEdgeAsSent(fromId, toId);
                            edgeCount++;
                        }
                    }
                }
            }

            System.out.println("[Graphosaurus] Sent " + edgeCount + " edges");

        } catch (Exception e) {
            massLogger.error("Error sending full graph", e);
        }
    }

    @Override
    public void finish() {
        running = false;
        
        if (pollingThread != null) {
            try {
                pollingThread.join(5000);
            } catch (InterruptedException e) {
                massLogger.error("Error stopping polling thread", e);
            }
        }

        if (senderThread != null) {
            try {
                senderThread.join(5000);
            } catch (InterruptedException e) {
                massLogger.error("Error stopping sender thread", e);
            }
        }

        // Drain any remaining messages before closing
        drainQueue();

        if (wsClient != null) {
            wsClient.close();
        }

        massLogger.debug("Graphosaurus listener stopped");
    }

    private void drainQueue() {
        if (wsClient == null || !wsClient.isOpen()) return;
        String json;
        while ((json = messageQueue.poll()) != null) {
            try {
                wsClient.send(json);
            } catch (Exception e) {
                massLogger.error("Error draining message queue", e);
                break;
            }
        }
    }

    @Override
    public void registerProcessor(String key, GraphRequest requestProcessor) {
        // Not used for WebSocket client mode
    }

    /**
     * Enqueue a message for async delivery to the Graphosaurus server.
     * The dedicated sender thread drains the queue and sends over WebSocket.
     * 
     * @param message The message object to send
     */
    protected void sendMessage(GraphosaurusMessage message) {
        try {
            String json = gson.toJson(message);
            messageQueue.offer(json);
        } catch (Exception e) {
            massLogger.error("Error serializing message for Graphosaurus", e);
        }
    }

    /**
     * Send graph structure (vertices and edges) for a specific vertex and its neighbors
     * This implements on-demand graph loading strategy
     * 
     * @param vertexId The vertex ID to send
     */
    protected void sendGraphStructureForVertex(Object vertexId) {
        try {
            GraphModel graphModel = graph.getGraph();
            
            if (graphModel == null || graphModel.getVertices() == null) {
                return;
            }

            List<VertexModel> allVertices = graphModel.getVertices();
            String vertexIdStr = String.valueOf(vertexId);

            for (VertexModel vertex : allVertices) {
                if (vertex.id == null || !String.valueOf(vertex.id).equals(vertexIdStr)) {
                    continue;
                }

                // 1) Send the vertex itself
                if (!tracker.hasVertexBeenSent(vertexIdStr)) {
                    sendVertex(vertex);
                    tracker.markVertexAsSent(vertexIdStr);
                }

                if (vertex.neighbors == null) break;

                // 2) Send all neighbor vertices first (so the frontend knows
                //    both endpoints before it receives the edge)
                List<String> resolvedNeighborIds = new ArrayList<>();
                for (Object rawNeighborId : vertex.neighbors) {
                    String neighborIdStr = resolveNeighborId(rawNeighborId, allVertices);
                    resolvedNeighborIds.add(neighborIdStr);

                    if (!tracker.hasVertexBeenSent(neighborIdStr)) {
                        for (VertexModel neighbor : allVertices) {
                            if (neighbor.id != null && String.valueOf(neighbor.id).equals(neighborIdStr)) {
                                sendVertex(neighbor);
                                tracker.markVertexAsSent(neighborIdStr);
                                break;
                            }
                        }
                    }
                }

                // 3) Now send all edges
                for (String neighborIdStr : resolvedNeighborIds) {
                    if (!tracker.hasEdgeBeenSent(vertexIdStr, neighborIdStr)) {
                        sendEdge(vertexIdStr, neighborIdStr);
                        tracker.markEdgeAsSent(vertexIdStr, neighborIdStr);
                    }
                }

                break;
            }
        } catch (Exception e) {
            massLogger.error("Error sending graph structure for vertex: " + vertexId, e);
        }
    }

    /**
     * Resolve a raw neighbor ID (often a MASS-internal integer) to the same
     * format used as vertex.id in the GraphModel.  For PropertyGraphPlaces the
     * model uses the reverse-looked-up attribute string (e.g. "nodea") while
     * neighbors still store the raw integer index; this helper bridges the gap.
     */
    protected String resolveNeighborId(Object rawNeighborId, List<VertexModel> vertices) {
        // The distributed_map is keyed Object -> Integer, where the value is
        // the MASS-internal vertex index.  reverseLookup takes an Integer value
        // and returns the original Object key (e.g. "nodea").
        Integer intKey = null;
        if (rawNeighborId instanceof Integer) {
            intKey = (Integer) rawNeighborId;
        } else if (rawNeighborId instanceof Number) {
            intKey = ((Number) rawNeighborId).intValue();
        }

        if (intKey != null) {
            Object resolved = MASSBase.distributed_map.reverseLookup(intKey);
            if (resolved != null) {
                return String.valueOf(resolved);
            }
        }

        // Fallback: if the rawId already matches a vertex id in the model, use it as-is
        String rawStr = String.valueOf(rawNeighborId);
        for (VertexModel v : vertices) {
            if (v.id != null && String.valueOf(v.id).equals(rawStr)) {
                return rawStr;
            }
        }
        return rawStr;
    }

    /**
     * Send a single vertex to Graphosaurus
     * 
     * @param vertex The vertex to send
     */
    protected void sendVertex(VertexModel vertex) {
        // Generate random position for the vertex (Graphosaurus will layout in 3D)
        double[] position = new double[3];
        position[0] = Math.random() * 4 - 2;  // Random x between -2 and 2
        position[1] = Math.random() * 4 - 2;  // Random y between -2 and 2
        position[2] = Math.random() * 4 - 2;  // Random z between -2 and 2

        int color = 0x888888;  // Gray color for vertices

        GraphosaurusMessage.AddNodeMessage message = 
            new GraphosaurusMessage.AddNodeMessage(
                String.valueOf(vertex.id), 
                color, 
                position
            );
        
        message.addData("vertexId", vertex.id);
        sendMessage(message);
    }

    /**
     * Send a single edge to Graphosaurus
     * 
     * @param fromVertex Source vertex ID
     * @param toVertex Target vertex ID
     */
    protected void sendEdge(Object fromVertex, Object toVertex) {
        int edgeColor = 0xCCCCCC;  // Light gray for edges

        String fromId = String.valueOf(fromVertex);
        String toId = String.valueOf(toVertex);
        
        System.out.println("[Graphosaurus] Sending edge: " + fromId + " -> " + toId);

        GraphosaurusMessage.AddEdgeMessage message = 
            new GraphosaurusMessage.AddEdgeMessage(fromId, toId, edgeColor);
        
        sendMessage(message);
    }

    /**
     * Runnable that drains the message queue and sends over WebSocket.
     * Batches up to MAX_DRAIN messages per cycle, then sleeps briefly
     * to allow natural batching when many messages are produced at once.
     */
    private class MessageSenderRunnable implements Runnable {
        private static final int MAX_DRAIN_PER_CYCLE = 50;
        private static final long SEND_INTERVAL_MS = 20;

        @Override
        public void run() {
            massLogger.debug("Graphosaurus sender thread started");

            while (running) {
                try {
                    int sent = 0;
                    String json;
                    while (sent < MAX_DRAIN_PER_CYCLE && (json = messageQueue.poll()) != null) {
                        if (wsClient != null && wsClient.isOpen()) {
                            wsClient.send(json);
                            sent++;
                        } else {
                            // Re-enqueue if not connected
                            messageQueue.offer(json);
                            break;
                        }
                    }
                    if (sent > 0) {
                        System.out.println("[Graphosaurus] Sender dispatched " + sent + " messages");
                    }
                    Thread.sleep(SEND_INTERVAL_MS);
                } catch (InterruptedException e) {
                    massLogger.debug("Sender thread interrupted");
                    break;
                } catch (Exception e) {
                    massLogger.error("Error in sender loop", e);
                }
            }

            massLogger.debug("Graphosaurus sender thread stopped");
        }
    }

    /**
     * Runnable for polling agent locations
     */
    private class PollingRunnable implements Runnable {
        private int pollCycleCount = 0;
        private static final int AGENT_LIST_SEND_INTERVAL = 5;

        @Override
        public void run() {
            massLogger.debug("Graphosaurus polling thread started");

            while (running) {
                try {
                    pollAgentLocations();
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException e) {
                    massLogger.debug("Polling thread interrupted");
                    break;
                } catch (Exception e) {
                    massLogger.error("Error in polling loop", e);
                }
            }

            massLogger.debug("Graphosaurus polling thread stopped");
        }

        /**
         * Poll all agents on VertexPlaces and send updates
         */
        private void pollAgentLocations() {
            try {
                GraphModel graphModel = graph.getGraph();
                if (graphModel == null || graphModel.getVertices() == null) {
                    massLogger.debug("GraphModel is null or has no vertices");
                    return;
                }

                Set<Integer> currentAgents = new HashSet<>();
                int vertexCount = graphModel.getVertices().size();
                int totalAgentsFound = 0;
                boolean hadChanges = false;

                for (VertexModel vertex : graphModel.getVertices()) {
                    Object vertexId = vertex.id;
                    
                    Set<Agent> agentsAtVertex = getAgentsAtVertex(vertexId);
                    
                    if (agentsAtVertex != null && !agentsAtVertex.isEmpty()) {
                        totalAgentsFound += agentsAtVertex.size();
                        for (Agent agent : agentsAtVertex) {
                            int agentId = agent.getAgentId();
                            currentAgents.add(agentId);

                            AgentLocationTracker.AgentChange change = 
                                tracker.updateAgentLocation(agent, vertexId);

                            switch (change.getType()) {
                                case SPAWNED:
                                    hadChanges = true;
                                    sendGraphStructureForVertex(vertexId);
                                    
                                    int color = tracker.generateAndStoreColor(agentId);
                                    GraphosaurusMessage.SpawnAgentMessage spawnMsg = 
                                        new GraphosaurusMessage.SpawnAgentMessage(
                                            String.valueOf(vertexId),
                                            "agent-" + agentId,
                                            color,
                                            DEFAULT_AGENT_SHAPE
                                        );
                                    spawnMsg.addData("agentId", agentId);
                                    sendMessage(spawnMsg);
                                    break;

                                case MOVED:
                                    hadChanges = true;
                                    sendGraphStructureForVertex(vertexId);
                                    
                                    GraphosaurusMessage.MoveAgentMessage moveMsg = 
                                        new GraphosaurusMessage.MoveAgentMessage(
                                            "agent-" + agentId,
                                            String.valueOf(vertexId),
                                            DEFAULT_MOVE_SPEED
                                        );
                                    sendMessage(moveMsg);
                                    break;

                                case UNCHANGED:
                                    break;
                            }
                        }
                    }
                }

                // Check for removed agents
                Set<Integer> trackedAgents = tracker.getTrackedAgents();
                for (Integer agentId : trackedAgents) {
                    if (!currentAgents.contains(agentId)) {
                        hadChanges = true;
                        tracker.removeAgent(agentId);
                        GraphosaurusMessage.RemoveAgentMessage removeMsg = 
                            new GraphosaurusMessage.RemoveAgentMessage("agent-" + agentId);
                        sendMessage(removeMsg);
                    }
                }

                // Send agent list periodically or when changes occurred
                pollCycleCount++;
                if (hadChanges || (pollCycleCount % AGENT_LIST_SEND_INTERVAL == 0 && !tracker.getAllEverTrackedAgentIds().isEmpty())) {
                    sendAgentList();
                }

                if (totalAgentsFound > 0) {
                    System.out.println("[Graphosaurus] Found " + totalAgentsFound + " agents across " + vertexCount + " vertices");
                }

            } catch (Exception e) {
                massLogger.error("Error polling agent locations", e);
            }
        }

        private void sendAgentList() {
            Map<Integer, Object> locations = tracker.getAllAgentLocations();
            Map<Integer, List<Object>> histories = tracker.getAllAgentHistories();
            Map<Integer, Integer> colors = tracker.getAllAgentColors();
            Set<Integer> allIds = tracker.getAllEverTrackedAgentIds();

            List<GraphosaurusMessage.AgentSummary> summaries = new ArrayList<>();
            for (Integer agentId : allIds) {
                List<String> historyStrs = new ArrayList<>();
                List<Object> history = histories.getOrDefault(agentId, new ArrayList<>());
                for (Object v : history) {
                    historyStrs.add(String.valueOf(v));
                }
                Object currentLoc = locations.get(agentId);
                boolean removed = tracker.isRemoved(agentId);
                summaries.add(new GraphosaurusMessage.AgentSummary(
                    "agent-" + agentId,
                    currentLoc != null ? String.valueOf(currentLoc) : null,
                    colors.getOrDefault(agentId, 0xFFFF00),
                    historyStrs,
                    removed
                ));
            }

            if (!summaries.isEmpty()) {
                sendMessage(new GraphosaurusMessage.AgentListMessage(summaries));
            }
        }

        /**
         * Get agents at a specific vertex
         * This is a helper method to access agents from the graph
         * 
         * @param vertexId The vertex ID
         * @return Set of agents at the vertex, or null if not accessible
         */
        private Set<Agent> getAgentsAtVertex(Object vertexId) {
            try {
                VertexPlace vertex = null;
                
                // Convert to int for direct lookup (bypasses distributed_map)
                int intId = -1;
                if (vertexId instanceof Number) {
                    intId = ((Number) vertexId).intValue();
                } else if (vertexId instanceof String) {
                    try {
                        intId = Integer.parseInt((String) vertexId);
                    } catch (NumberFormatException e) {
                        // Not a numeric string, try object lookup
                        vertex = graphPlaces.getVertex(vertexId);
                    }
                }
                
                // Use int version if we have a valid int ID
                if (intId >= 0 && vertex == null) {
                    vertex = graphPlaces.getVertex(intId);
                }
                
                if (vertex != null) {
                    return vertex.getAgents();
                }
            } catch (Exception e) {
                massLogger.debug("Error getting agents at vertex " + vertexId + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * WebSocket client implementation
     */
    protected class GraphosaurusWebSocketClient extends WebSocketClient {

        public GraphosaurusWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            massLogger.debug("Connected to Graphosaurus server");
        }

        @Override
        public void onMessage(String message) {
            massLogger.debug("Received from Graphosaurus: " + message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            massLogger.debug("Disconnected from Graphosaurus server: " + reason);
            
            // Attempt to reconnect if disconnected unexpectedly
            if (running && remote) {
                massLogger.debug("Attempting to reconnect to Graphosaurus...");
                try {
                    Thread.sleep(5000);
                    reconnect();
                } catch (Exception e) {
                    massLogger.error("Failed to reconnect to Graphosaurus", e);
                }
            }
        }

        @Override
        public void onError(Exception ex) {
            massLogger.error("WebSocket error", ex);
        }
    }
}

