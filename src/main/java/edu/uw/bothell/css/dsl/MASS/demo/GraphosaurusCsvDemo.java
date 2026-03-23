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
 * Demo that loads a graph from CSV files and visualizes it with Graphosaurus.
 *
 * Usage:
 *   java -cp target/mass-core.jar edu.uw.bothell.css.dsl.MASS.demo.GraphosaurusCsvDemo nodes.csv edges.csv
 *
 * If running on a remote node, SSH in with: ssh -R 8080:localhost:8080 user@remote-node
 */
public class GraphosaurusCsvDemo {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: GraphosaurusCsvDemo <nodes.csv> <edges.csv>");
            System.exit(1);
        }

        String nodesFile = args[0];
        String edgesFile = args[1];

        System.out.println("===========================================");
        System.out.println("Graphosaurus CSV Demo");
        System.out.println("===========================================");
        System.out.println("Nodes file: " + nodesFile);
        System.out.println("Edges file: " + edgesFile);
        System.out.println();

        // Initialize MASS (single-node, no nodes.xml needed)
        MNode masterNode = new MNode();
        masterNode.setHostName("localhost");
        masterNode.setMaster(true);
        MASSBase.addNode(masterNode);
        MASSBase.initMASSBase(masterNode);

        // Create graph and enable visualization
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        graph.enableGraphosaurusVisualization("ws://localhost:8080", 500);

        // Load graph from CSV files
        try {
            graph.loadCSVFiles(nodesFile, edgesFile);
        } catch (Exception e) {
            System.err.println("Failed to load CSV files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        int vertexCount = graph.getGraphPlaces().size();
        System.out.println("Loaded " + vertexCount + " vertices");
        System.out.println("Graph is now visible in Graphosaurus.");
        System.out.println();

        // Spawn agents on a few vertices and walk them around
        System.out.println("Spawning agents...");
        simulateAgentWalk(graph);

        System.out.println("\nDemo complete. Press Ctrl+C to exit.");
        try {
            while (true) { Thread.sleep(1000); }
        } catch (InterruptedException ignored) {}

        graph.disableGraphosaurusVisualization();
    }

    private static void simulateAgentWalk(GraphPlaces graph) {
        try {
            // Pick starting vertices: charlie, michael, rob
            String[] startNames = {"charlie", "michael", "rob"};
            DemoAgent[] agents = new DemoAgent[startNames.length];
            String[] currentPositions = new String[startNames.length];

            // Spawn agents
            for (int i = 0; i < startNames.length; i++) {
                int internalId = MASSBase.distributed_map.getOrDefault(startNames[i], -1);
                if (internalId == -1) {
                    System.out.println("  Vertex '" + startNames[i] + "' not found, skipping");
                    continue;
                }
                VertexPlace vertex = graph.getVertex(internalId);
                if (vertex == null) continue;

                agents[i] = new DemoAgent(i);
                agents[i].setPlacePublic(vertex);
                vertex.getAgents().add(agents[i]);
                currentPositions[i] = startNames[i];
                System.out.println("  Spawned agent " + i + " at " + startNames[i]);
                Thread.sleep(800);
            }

            Thread.sleep(2000);

            // Define a walk path for each agent through the graph
            String[][] walks = {
                {"charlie", "wallStreet", "martin", "thePresident", "rob"},
                {"michael", "thePresident", "martin", "charlie", "wallStreet"},
                {"rob", "martin", "wallStreet", "oliver"}
            };

            int maxSteps = 0;
            for (String[] walk : walks) maxSteps = Math.max(maxSteps, walk.length);

            for (int step = 1; step < maxSteps; step++) {
                System.out.println("\n--- Step " + step + " ---");

                for (int i = 0; i < agents.length; i++) {
                    if (agents[i] == null || step >= walks[i].length) continue;

                    String from = currentPositions[i];
                    String to = walks[i][step];

                    int fromId = MASSBase.distributed_map.getOrDefault(from, -1);
                    int toId = MASSBase.distributed_map.getOrDefault(to, -1);
                    if (fromId == -1 || toId == -1) continue;

                    VertexPlace fromVertex = graph.getVertex(fromId);
                    VertexPlace toVertex = graph.getVertex(toId);
                    if (fromVertex == null || toVertex == null) continue;

                    fromVertex.getAgents().remove(agents[i]);
                    agents[i].setPlacePublic(toVertex);
                    toVertex.getAgents().add(agents[i]);
                    currentPositions[i] = to;

                    System.out.println("  Agent " + i + ": " + from + " -> " + to);
                    Thread.sleep(1500);
                }
            }

            Thread.sleep(2000);

            // Remove agents
            System.out.println("\n--- Removing agents ---");
            for (int i = 0; i < agents.length; i++) {
                if (agents[i] == null) continue;
                int id = MASSBase.distributed_map.getOrDefault(currentPositions[i], -1);
                if (id == -1) continue;
                VertexPlace vertex = graph.getVertex(id);
                if (vertex != null) {
                    vertex.getAgents().remove(agents[i]);
                    System.out.println("  Removed agent " + i + " from " + currentPositions[i]);
                }
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("Error in simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class DemoAgent extends Agent {
        public DemoAgent(int id) { setAgentId(id); }
        @Override protected void setAgentId(Integer agentId) { super.setAgentId(agentId); }
        public void setPlacePublic(Place place) { super.setPlace(place); }
    }
}
