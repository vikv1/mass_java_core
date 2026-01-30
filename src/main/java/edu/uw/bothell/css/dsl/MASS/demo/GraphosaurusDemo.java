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

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.demo;

import edu.uw.bothell.css.dsl.MASS.*;

/**
 * Demo application to test Graphosaurus integration.
 * 
 * BEFORE RUNNING THIS DEMO:
 * 1. Start Graphosaurus server:
 *    cd C:\Users\lakes\graphosaurus
 *    npm run build   (if you haven't rebuilt after adding add_node/add_edge handlers)
 *    npm run server
 * 
 * 2. Open in browser:
 *    file:///C:/Users/lakes/graphosaurus/message-demo.html
 * 
 * 3. Run this demo application
 */
public class GraphosaurusDemo {

    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("Graphosaurus Integration Demo");
        System.out.println("===========================================");
        System.out.println();
        System.out.println("BEFORE RUNNING:");
        System.out.println("1. Start Graphosaurus server: cd C:\\Users\\lakes\\graphosaurus && npm run server");
        System.out.println("2. Open browser: file:///C:/Users/lakes/graphosaurus/message-demo.html");
        System.out.println();
        System.out.println("Starting in 3 seconds...");
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Initialize MASS
        System.out.println("Initializing MASS...");
        
        // Create master node (minimal setup for single-node demo)
        MNode masterNode = new MNode();
        masterNode.setHostName("localhost");
        masterNode.setMaster(true);
        MASSBase.addNode(masterNode);
        MASSBase.initMASSBase(masterNode);

        // Create GraphPlaces
        System.out.println("Creating graph...");
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());

        // Enable Graphosaurus visualization
        System.out.println("Enabling Graphosaurus visualization...");
        graph.enableGraphosaurusVisualization("ws://localhost:8080", 500);

        // Create vertices in a ring topology
        System.out.println("Adding vertices...");
        int numVertices = 8;
        int[] vertexIds = new int[numVertices];
        
        for (int i = 0; i < numVertices; i++) {
            vertexIds[i] = graph.addVertex();
            System.out.println("  Added vertex: " + vertexIds[i]);
        }

        // Create edges (ring + hub)
        System.out.println("Adding edges...");
        
        // Create a ring
        for (int i = 0; i < numVertices; i++) {
            int next = (i + 1) % numVertices;
            graph.addEdge(vertexIds[i], vertexIds[next]);
            System.out.println("  Added edge: " + vertexIds[i] + " -> " + vertexIds[next]);
        }
        
        // Add cross-connections for more interesting topology
        graph.addEdge(vertexIds[0], vertexIds[4]);
        graph.addEdge(vertexIds[2], vertexIds[6]);
        System.out.println("  Added cross-edges for more connections");

        // Give some time for the graph structure to be sent
        System.out.println("\nWaiting for graph to sync...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Now create some agents that will traverse the graph
        System.out.println("\nCreating agents on the graph...");
        System.out.println("(Agents should appear in Graphosaurus visualization)");
        
        // Manually add agents to vertices to simulate agent activity
        // In a real simulation, you would use Agents class
        simulateAgentActivity(graph, vertexIds);

        // Cleanup
        System.out.println("\nDemo complete. Press Ctrl+C to exit.");
        System.out.println("Watch the Graphosaurus visualization to see agents moving!");
        
        // Keep running so visualization stays active
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        graph.disableGraphosaurusVisualization();
    }

    /**
     * Simulate agent activity by adding agents to vertices
     */
    private static void simulateAgentActivity(GraphPlaces graph, int[] vertexIds) {
        // Since we can't easily create a full Agents setup in this demo,
        // we'll add agents directly to VertexPlaces
        
        try {
            // Spawn 4 agents at different starting positions
            System.out.println("\n--- Spawning agents ---");
            DemoAgent[] agents = new DemoAgent[4];
            int[] agentPositions = {0, 2, 4, 6}; // Start at even vertices
            
            for (int i = 0; i < agents.length; i++) {
                VertexPlace vertex = graph.getVertex(vertexIds[agentPositions[i]]);
                if (vertex != null) {
                    agents[i] = new DemoAgent(i);
                    agents[i].setPlacePublic(vertex);
                    vertex.getAgents().add(agents[i]);
                    System.out.println("  Spawned agent " + i + " at vertex " + vertexIds[agentPositions[i]]);
                }
                Thread.sleep(500); // Stagger spawns
            }

            Thread.sleep(2000);

            // Move agents around the ring - 8 rounds of movement
            for (int round = 1; round <= 8; round++) {
                System.out.println("\n--- Movement round " + round + " ---");
                
                for (int i = 0; i < agents.length; i++) {
                    if (agents[i] == null) continue;
                    
                    int currentPos = agentPositions[i];
                    int nextPos = (currentPos + 1) % vertexIds.length; // Move clockwise
                    
                    VertexPlace fromVertex = graph.getVertex(vertexIds[currentPos]);
                    VertexPlace toVertex = graph.getVertex(vertexIds[nextPos]);
                    
                    if (fromVertex != null && toVertex != null) {
                        fromVertex.getAgents().remove(agents[i]);
                        agents[i].setPlacePublic(toVertex);
                        toVertex.getAgents().add(agents[i]);
                        agentPositions[i] = nextPos;
                        System.out.println("  Agent " + i + ": " + vertexIds[currentPos] + " -> " + vertexIds[nextPos]);
                    }
                    
                    Thread.sleep(1000); // 1 second between each agent move
                }
            }

            // Remove agents one by one
            System.out.println("\n--- Removing agents ---");
            for (int i = 0; i < agents.length; i++) {
                if (agents[i] == null) continue;
                
                VertexPlace vertex = graph.getVertex(vertexIds[agentPositions[i]]);
                if (vertex != null) {
                    vertex.getAgents().remove(agents[i]);
                    System.out.println("  Removed agent " + i + " from vertex " + vertexIds[agentPositions[i]]);
                }
                Thread.sleep(1000);
            }

            System.out.println("\n--- Demo simulation complete ---");

        } catch (Exception e) {
            System.err.println("Error in agent simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Simple demo agent for visualization
     */
    static class DemoAgent extends Agent {
        public DemoAgent(int id) {
            setAgentId(id);
        }
        
        @Override
        protected void setAgentId(Integer agentId) {
            super.setAgentId(agentId);
        }

        // Expose protected setPlace as public for demo purposes
        public void setPlacePublic(Place place) {
            super.setPlace(place);
        }
    }
}

