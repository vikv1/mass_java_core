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
import java.util.HashSet;
import java.util.Set;

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

    protected GraphosaurusWebSocketClient wsClient;
    protected Thread pollingThread;
    protected volatile boolean running = false;

    /**
     * Constructor with default settings
     * 
     * @param graphPlaces The GraphPlaces instance to monitor
     */
    public GraphosaurusListener(GraphPlaces graphPlaces) {
        this(graphPlaces, DEFAULT_WEBSOCKET_URL, DEFAULT_POLL_INTERVAL_MS);
    }

    /**
     * Constructor with custom settings
     * 
     * @param graphPlaces The GraphPlaces instance to monitor
     * @param websocketUrl WebSocket server URL
     * @param pollIntervalMs Polling interval in milliseconds
     */
    public GraphosaurusListener(GraphPlaces graphPlaces, String websocketUrl, long pollIntervalMs) {
        this.massLogger = MASSBase.getLogger();
        this.graph = graphPlaces;
        this.graphPlaces = graphPlaces;
        this.tracker = new AgentLocationTracker();
        this.gson = new Gson();
        this.websocketUrl = websocketUrl;
        this.pollIntervalMs = pollIntervalMs;

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

            // Wait briefly for connection to establish, then send full graph
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Wait for WebSocket to connect
                    sendFullGraph();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

            // Start polling thread
            running = true;
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

            // Second pass: send all edges
            int edgeCount = 0;
            for (VertexModel vertex : graphModel.getVertices()) {
                if (vertex.id != null && vertex.neighbors != null) {
                    String fromId = String.valueOf(vertex.id);
                    for (Object neighborId : vertex.neighbors) {
                        String toId = String.valueOf(neighborId);
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

        if (wsClient != null) {
            wsClient.close();
        }

        massLogger.debug("Graphosaurus listener stopped");
    }

    @Override
    public void registerProcessor(String key, GraphRequest requestProcessor) {
        // Not used for WebSocket client mode
    }

    /**
     * Send a message to Graphosaurus server
     * 
     * @param message The message object to send
     */
    protected void sendMessage(GraphosaurusMessage message) {
        if (wsClient != null && wsClient.isOpen()) {
            try {
                String json = gson.toJson(message);
                wsClient.send(json);
                System.out.println("[Graphosaurus] Sent: " + message.getType());
            } catch (Exception e) {
                massLogger.error("Error sending message to Graphosaurus", e);
            }
        } else {
            System.out.println("[Graphosaurus] WebSocket not connected, can't send: " + message.getType());
        }
    }

    /**
     * Send graph structure (vertices and edges) for a specific vertex and its neighbors
     * This implements on-demand graph loading strategy
     * 
     * @param vertexId The vertex ID to send
     */
    private void sendGraphStructureForVertex(Object vertexId) {
        try {
            // Get the full graph model
            GraphModel graphModel = graph.getGraph();
            
            if (graphModel == null || graphModel.getVertices() == null) {
                return;
            }

            // Find the vertex in the model (use string comparison for reliable ID matching)
            String vertexIdStr = String.valueOf(vertexId);
            for (VertexModel vertex : graphModel.getVertices()) {
                if (vertex.id != null && String.valueOf(vertex.id).equals(vertexIdStr)) {
                    // Send vertex if not already sent
                    if (!tracker.hasVertexBeenSent(vertexIdStr)) {
                        sendVertex(vertex);
                        tracker.markVertexAsSent(vertexIdStr);
                    }

                    // Send edges to neighbors if not already sent
                    if (vertex.neighbors != null) {
                        for (Object neighborId : vertex.neighbors) {
                            String neighborIdStr = String.valueOf(neighborId);
                            if (!tracker.hasEdgeBeenSent(vertexIdStr, neighborIdStr)) {
                                sendEdge(vertexIdStr, neighborIdStr);
                                tracker.markEdgeAsSent(vertexIdStr, neighborIdStr);
                            }

                            // Also send neighbor vertex if not already sent
                            if (!tracker.hasVertexBeenSent(neighborIdStr)) {
                                // Find and send the neighbor vertex
                                for (VertexModel neighbor : graphModel.getVertices()) {
                                    if (neighbor.id != null && String.valueOf(neighbor.id).equals(neighborIdStr)) {
                                        sendVertex(neighbor);
                                        tracker.markVertexAsSent(neighborIdStr);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            massLogger.error("Error sending graph structure for vertex: " + vertexId, e);
        }
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
     * Runnable for polling agent locations
     */
    private class PollingRunnable implements Runnable {
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

                // Iterate through all vertices in the graph
                for (VertexModel vertex : graphModel.getVertices()) {
                    Object vertexId = vertex.id;
                    
                    // Get the actual VertexPlace to access agents
                    // Note: This requires access to the underlying PlacesBase
                    // We'll need to get agents through the graph interface
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
                                    // Send graph structure for this vertex
                                    sendGraphStructureForVertex(vertexId);
                                    
                                    // Send spawn agent message
                                    int color = tracker.generateRandomColor();
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
                                    // Send graph structure for destination vertex
                                    sendGraphStructureForVertex(vertexId);
                                    
                                    // Send move agent message
                                    GraphosaurusMessage.MoveAgentMessage moveMsg = 
                                        new GraphosaurusMessage.MoveAgentMessage(
                                            "agent-" + agentId,
                                            String.valueOf(vertexId),
                                            DEFAULT_MOVE_SPEED
                                        );
                                    sendMessage(moveMsg);
                                    break;

                                case UNCHANGED:
                                    // No action needed
                                    break;
                            }
                        }
                    }
                }

                // Check for removed agents
                Set<Integer> trackedAgents = tracker.getTrackedAgents();
                for (Integer agentId : trackedAgents) {
                    if (!currentAgents.contains(agentId)) {
                        // Agent was removed
                        tracker.removeAgent(agentId);
                        GraphosaurusMessage.RemoveAgentMessage removeMsg = 
                            new GraphosaurusMessage.RemoveAgentMessage("agent-" + agentId);
                        sendMessage(removeMsg);
                    }
                }

                // Log polling summary (use System.out for visibility during testing)
                if (totalAgentsFound > 0) {
                    System.out.println("[Graphosaurus] Found " + totalAgentsFound + " agents across " + vertexCount + " vertices");
                }

            } catch (Exception e) {
                massLogger.error("Error polling agent locations", e);
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

