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

package edu.uw.bothell.css.dsl.MASS.graph.transport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Graphosaurus visualization messages
 */
public abstract class GraphosaurusMessage {
    protected String type;

    public GraphosaurusMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    /**
     * Spawn agent message for Graphosaurus
     */
    public static class SpawnAgentMessage extends GraphosaurusMessage {
        private String nodeId;
        private String id;
        private int color;
        private String shape;
        private Map<String, Object> data;

        public SpawnAgentMessage(String nodeId, String agentId, int color, String shape) {
            super("spawn_agent");
            this.nodeId = nodeId;
            this.id = agentId;
            this.color = color;
            this.shape = shape;
            this.data = new HashMap<>();
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getId() {
            return id;
        }

        public int getColor() {
            return color;
        }

        public String getShape() {
            return shape;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public void addData(String key, Object value) {
            this.data.put(key, value);
        }
    }

    /**
     * Move agent message for Graphosaurus
     */
    public static class MoveAgentMessage extends GraphosaurusMessage {
        private String agentId;
        private String targetNodeId;
        private double speed;

        public MoveAgentMessage(String agentId, String targetNodeId, double speed) {
            super("move_agent");
            this.agentId = agentId;
            this.targetNodeId = targetNodeId;
            this.speed = speed;
        }

        public String getAgentId() {
            return agentId;
        }

        public String getTargetNodeId() {
            return targetNodeId;
        }

        public double getSpeed() {
            return speed;
        }
    }

    /**
     * Remove agent message for Graphosaurus
     */
    public static class RemoveAgentMessage extends GraphosaurusMessage {
        private String agentId;

        public RemoveAgentMessage(String agentId) {
            super("remove_agent");
            this.agentId = agentId;
        }

        public String getAgentId() {
            return agentId;
        }
    }

    /**
     * Add node message for Graphosaurus (for on-demand graph loading)
     */
    public static class AddNodeMessage extends GraphosaurusMessage {
        private String id;
        private int color;
        private double[] position;
        private Map<String, Object> data;

        public AddNodeMessage(String id, int color, double[] position) {
            super("add_node");
            this.id = id;
            this.color = color;
            this.position = position;
            this.data = new HashMap<>();
        }

        public String getId() {
            return id;
        }

        public int getColor() {
            return color;
        }

        public double[] getPosition() {
            return position;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public void addData(String key, Object value) {
            this.data.put(key, value);
        }
    }

    /**
     * Add edge message for Graphosaurus (for on-demand graph loading)
     */
    public static class AddEdgeMessage extends GraphosaurusMessage {
        private String fromNodeId;
        private String toNodeId;
        private int color;
        private Map<String, Object> data;

        public AddEdgeMessage(String fromNodeId, String toNodeId, int color) {
            super("add_edge");
            this.fromNodeId = fromNodeId;
            this.toNodeId = toNodeId;
            this.color = color;
            this.data = new HashMap<>();
        }

        public String getFromNodeId() {
            return fromNodeId;
        }

        public String getToNodeId() {
            return toNodeId;
        }

        public int getColor() {
            return color;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public void setData(Map<String, Object> data) {
            this.data = data;
        }

        public void addData(String key, Object value) {
            this.data.put(key, value);
        }
    }

    /**
     * Agent list message containing all agents (active and removed) with their visit histories.
     * Sent periodically so the frontend can display the agent list panel.
     */
    public static class AgentListMessage extends GraphosaurusMessage {
        private List<AgentSummary> agents;

        public AgentListMessage(List<AgentSummary> agents) {
            super("agent_list");
            this.agents = agents;
        }

        public List<AgentSummary> getAgents() {
            return agents;
        }
    }

    /**
     * Summary of a single agent for the agent list message.
     */
    public static class AgentSummary {
        private String id;
        private String currentNode;
        private int color;
        private List<String> visitHistory;
        private boolean removed;

        public AgentSummary(String id, String currentNode, int color, List<String> visitHistory, boolean removed) {
            this.id = id;
            this.currentNode = currentNode;
            this.color = color;
            this.visitHistory = visitHistory;
            this.removed = removed;
        }

        public String getId() {
            return id;
        }

        public String getCurrentNode() {
            return currentNode;
        }

        public int getColor() {
            return color;
        }

        public List<String> getVisitHistory() {
            return visitHistory;
        }

        public boolean isRemoved() {
            return removed;
        }
    }
}

