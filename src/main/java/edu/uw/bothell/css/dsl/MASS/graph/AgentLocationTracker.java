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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.uw.bothell.css.dsl.MASS.Agent;

/**
 * Tracks agent locations on graph vertices and detects state changes
 * for Graphosaurus visualization.
 */
public class AgentLocationTracker {

    /**
     * Tracks current location of each agent: agentId -> vertexId
     */
    private final Map<Integer, Object> agentLocations;

    /**
     * Tracks which vertices have been sent to Graphosaurus (for on-demand loading)
     */
    private final Set<Object> sentVertices;

    /**
     * Tracks which edges have been sent to Graphosaurus (for on-demand loading)
     */
    private final Set<String> sentEdges;

    /**
     * Ordered visit history for each agent: agentId -> list of visited vertex IDs
     */
    private final Map<Integer, List<Object>> agentVisitHistory;

    /**
     * Assigned color for each agent: agentId -> color
     */
    private final Map<Integer, Integer> agentColors;

    /**
     * IDs of agents that were removed (but whose history we still keep)
     */
    private final Set<Integer> removedAgents;

    /**
     * Random color generator for agents
     */
    private final Random random;

    /**
     * Constructor initializes tracking data structures
     */
    public AgentLocationTracker() {
        this.agentLocations = new ConcurrentHashMap<>();
        this.sentVertices = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.sentEdges = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.agentVisitHistory = new ConcurrentHashMap<>();
        this.agentColors = new ConcurrentHashMap<>();
        this.removedAgents = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.random = new Random();
    }

    /**
     * Update agent location and return the change type
     * 
     * @param agent The agent to track
     * @param vertexId The vertex ID where the agent is located
     * @return AgentChange indicating what happened (SPAWNED, MOVED, or UNCHANGED)
     */
    public AgentChange updateAgentLocation(Agent agent, Object vertexId) {
        int agentId = agent.getAgentId();
        Object previousLocation = agentLocations.get(agentId);

        if (previousLocation == null) {
            agentLocations.put(agentId, vertexId);
            agentVisitHistory.computeIfAbsent(agentId, k -> new ArrayList<>()).add(vertexId);
            return new AgentChange(AgentChangeType.SPAWNED, agentId, vertexId, null);
        } else if (!previousLocation.equals(vertexId)) {
            agentLocations.put(agentId, vertexId);
            agentVisitHistory.computeIfAbsent(agentId, k -> new ArrayList<>()).add(vertexId);
            return new AgentChange(AgentChangeType.MOVED, agentId, vertexId, previousLocation);
        } else {
            return new AgentChange(AgentChangeType.UNCHANGED, agentId, vertexId, previousLocation);
        }
    }

    /**
     * Mark agent as removed from the system.
     * Visit history and color are preserved so the agent list can show
     * every agent that has ever existed in the graph.
     * 
     * @param agentId The ID of the removed agent
     * @return The vertex where the agent was last located, or null if not tracked
     */
    public Object removeAgent(int agentId) {
        removedAgents.add(agentId);
        return agentLocations.remove(agentId);
    }

    /**
     * @return true if the agent was tracked at some point but has since been removed
     */
    public boolean isRemoved(int agentId) {
        return removedAgents.contains(agentId);
    }

    /**
     * Get IDs of all agents that have ever been tracked (active + removed).
     */
    public Set<Integer> getAllEverTrackedAgentIds() {
        Set<Integer> all = new HashSet<>(agentLocations.keySet());
        all.addAll(removedAgents);
        return all;
    }

    /**
     * Check if a vertex has been sent to Graphosaurus
     * 
     * @param vertexId The vertex ID to check
     * @return true if vertex data has been sent
     */
    public boolean hasVertexBeenSent(Object vertexId) {
        return sentVertices.contains(vertexId);
    }

    /**
     * Mark a vertex as sent to Graphosaurus
     * 
     * @param vertexId The vertex ID to mark as sent
     */
    public void markVertexAsSent(Object vertexId) {
        sentVertices.add(vertexId);
    }

    /**
     * Check if an edge has been sent to Graphosaurus
     * 
     * @param fromVertex Source vertex ID
     * @param toVertex Target vertex ID
     * @return true if edge data has been sent
     */
    public boolean hasEdgeBeenSent(Object fromVertex, Object toVertex) {
        String edgeKey = getEdgeKey(fromVertex, toVertex);
        return sentEdges.contains(edgeKey);
    }

    /**
     * Mark an edge as sent to Graphosaurus
     * 
     * @param fromVertex Source vertex ID
     * @param toVertex Target vertex ID
     */
    public void markEdgeAsSent(Object fromVertex, Object toVertex) {
        String edgeKey = getEdgeKey(fromVertex, toVertex);
        sentEdges.add(edgeKey);
    }

    /**
     * Generate a random color for an agent and store it.
     * 
     * @param agentId The agent to assign a color to
     * @return Color as integer (e.g., 0xFF0000 for red)
     */
    public int generateAndStoreColor(int agentId) {
        int color = random.nextInt(0xFFFFFF);
        agentColors.put(agentId, color);
        return color;
    }

    /**
     * Generate a random color (without storing).
     * 
     * @return Color as integer
     */
    public int generateRandomColor() {
        return random.nextInt(0xFFFFFF);
    }

    /**
     * Get the stored color for an agent.
     * 
     * @param agentId The agent ID
     * @return Color as integer, or -1 if not assigned
     */
    public int getAgentColor(int agentId) {
        return agentColors.getOrDefault(agentId, -1);
    }

    /**
     * Get the current location of an agent
     * 
     * @param agentId The agent ID
     * @return The vertex ID where the agent is located, or null if not tracked
     */
    public Object getAgentLocation(int agentId) {
        return agentLocations.get(agentId);
    }

    /**
     * Get all currently tracked agents
     * 
     * @return Set of all tracked agent IDs
     */
    public Set<Integer> getTrackedAgents() {
        return new HashSet<>(agentLocations.keySet());
    }

    /**
     * Get the visit history for a specific agent.
     * 
     * @param agentId The agent ID
     * @return Ordered list of visited vertex IDs, or empty list if none
     */
    public List<Object> getAgentVisitHistory(int agentId) {
        return agentVisitHistory.getOrDefault(agentId, new ArrayList<>());
    }

    /**
     * Get all agent locations (snapshot).
     * 
     * @return Map of agentId to current vertexId
     */
    public Map<Integer, Object> getAllAgentLocations() {
        return new HashMap<>(agentLocations);
    }

    /**
     * Get all agent visit histories (snapshot).
     * 
     * @return Map of agentId to ordered list of visited vertex IDs
     */
    public Map<Integer, List<Object>> getAllAgentHistories() {
        Map<Integer, List<Object>> snapshot = new HashMap<>();
        for (Map.Entry<Integer, List<Object>> entry : agentVisitHistory.entrySet()) {
            snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return snapshot;
    }

    /**
     * Get all agent colors (snapshot).
     * 
     * @return Map of agentId to color
     */
    public Map<Integer, Integer> getAllAgentColors() {
        return new HashMap<>(agentColors);
    }

    /**
     * Clear all tracking data
     */
    public void clear() {
        agentLocations.clear();
        sentVertices.clear();
        sentEdges.clear();
        agentVisitHistory.clear();
        agentColors.clear();
        removedAgents.clear();
    }

    /**
     * Clear only graph structure transmission state.
     * Useful after websocket reconnect so nodes/edges can be resent without
     * losing tracked agents or visit history.
     */
    public void clearSentGraphState() {
        sentVertices.clear();
        sentEdges.clear();
    }

    /**
     * Create a unique key for an edge (order-independent for undirected graphs)
     * 
     * @param fromVertex Source vertex ID
     * @param toVertex Target vertex ID
     * @return Unique edge key
     */
    private String getEdgeKey(Object fromVertex, Object toVertex) {
        // Create consistent key regardless of direction (for undirected graphs)
        String from = String.valueOf(fromVertex);
        String to = String.valueOf(toVertex);
        if (from.compareTo(to) < 0) {
            return from + "->" + to;
        } else {
            return to + "->" + from;
        }
    }

    /**
     * Represents a change in agent state
     */
    public static class AgentChange {
        private final AgentChangeType type;
        private final int agentId;
        private final Object currentVertex;
        private final Object previousVertex;

        public AgentChange(AgentChangeType type, int agentId, Object currentVertex, Object previousVertex) {
            this.type = type;
            this.agentId = agentId;
            this.currentVertex = currentVertex;
            this.previousVertex = previousVertex;
        }

        public AgentChangeType getType() {
            return type;
        }

        public int getAgentId() {
            return agentId;
        }

        public Object getCurrentVertex() {
            return currentVertex;
        }

        public Object getPreviousVertex() {
            return previousVertex;
        }
    }

    /**
     * Types of agent state changes
     */
    public enum AgentChangeType {
        SPAWNED,    // Agent newly appeared
        MOVED,      // Agent moved to different vertex
        UNCHANGED   // Agent still at same vertex
    }
}

