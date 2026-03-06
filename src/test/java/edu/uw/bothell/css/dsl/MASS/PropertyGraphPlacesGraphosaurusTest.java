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

package edu.uw.bothell.css.dsl.MASS;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration test for PropertyGraphPlaces with Graphosaurus visualization.
 * 
 * This test creates a sample property graph with nodes having labels and properties,
 * and edges with relationship types and properties. The graph is then visualized
 * using the PropertyGraphosaurusListener.
 * 
 * Prerequisites:
 * 1. Start the graphosaurus server: cd C:\Users\lakes\graphosaurus && node server.js
 * 2. Open viewer.html in a browser: C:\Users\lakes\graphosaurus\viewer.html
 * 3. Run this test
 * 4. Click on nodes and edges in the visualization to see their properties
 */
public class PropertyGraphPlacesGraphosaurusTest extends AbstractTest {
    
    @BeforeAll
    public static void beforeAll() {
        resetMASSBase();
        MNode masterNode = new MNode();
        masterNode.setHostName(randomString());
        masterNode.setMaster(true);
        MASSBase.addNode(masterNode);
        MASSBase.initMASSBase(masterNode);
    }

    @AfterAll
    public static void afterAll() {
        resetMASSBase();
    }

    /**
     * Test creating a property graph and visualizing it with Graphosaurus.
     * 
     * This creates a social network graph with:
     * - Person nodes with labels [Person] and properties (name, age, city)
     * - Company nodes with labels [Company] and properties (name, industry)
     * - Edges with relationship types (KNOWS, WORKS_AT) and properties (since, role)
     */
    @Test
    public void propertyGraphWithGraphosaurus() throws Exception {
        System.out.println("=== Property Graph Graphosaurus Test ===\n");
        
        // Create PropertyGraphPlaces
        System.out.println("Creating PropertyGraphPlaces...");
        PropertyGraphPlaces graph = new PropertyGraphPlaces(0, PropertyVertexPlace.class.getName());
        
        // Enable property-aware Graphosaurus visualization
        System.out.println("Enabling Property Graphosaurus visualization...");
        graph.enablePropertyGraphosaurusVisualization();
        assertTrue(graph.isGraphosaurusEnabled(), "Graphosaurus should be enabled");
        
        // Create Person nodes
        System.out.println("\nCreating Person nodes...");
        
        Map<String, String> aliceProps = new HashMap<>();
        aliceProps.put("name", "Alice");
        aliceProps.put("age", "30");
        aliceProps.put("city", "Seattle");
        graph.addPropertyVertex("alice", Arrays.asList("Person", "Developer"), aliceProps);
        
        Map<String, String> bobProps = new HashMap<>();
        bobProps.put("name", "Bob");
        bobProps.put("age", "35");
        bobProps.put("city", "Portland");
        graph.addPropertyVertex("bob", Arrays.asList("Person", "Manager"), bobProps);
        
        Map<String, String> charlieProps = new HashMap<>();
        charlieProps.put("name", "Charlie");
        charlieProps.put("age", "28");
        charlieProps.put("city", "San Francisco");
        graph.addPropertyVertex("charlie", Arrays.asList("Person", "Designer"), charlieProps);
        
        Map<String, String> dianaProps = new HashMap<>();
        dianaProps.put("name", "Diana");
        dianaProps.put("age", "32");
        dianaProps.put("city", "Seattle");
        graph.addPropertyVertex("diana", Arrays.asList("Person", "Developer"), dianaProps);
        
        Map<String, String> eveProps = new HashMap<>();
        eveProps.put("name", "Eve");
        eveProps.put("age", "40");
        eveProps.put("city", "New York");
        graph.addPropertyVertex("eve", Arrays.asList("Person", "CTO"), eveProps);
        
        // Create Company nodes
        System.out.println("Creating Company nodes...");
        
        Map<String, String> techCorpProps = new HashMap<>();
        techCorpProps.put("name", "TechCorp");
        techCorpProps.put("industry", "Technology");
        techCorpProps.put("size", "500");
        graph.addPropertyVertex("techcorp", Arrays.asList("Company", "Enterprise"), techCorpProps);
        
        Map<String, String> startupIncProps = new HashMap<>();
        startupIncProps.put("name", "StartupInc");
        startupIncProps.put("industry", "Software");
        startupIncProps.put("size", "50");
        graph.addPropertyVertex("startupinc", Arrays.asList("Company", "Startup"), startupIncProps);
        
        Map<String, String> designCoProps = new HashMap<>();
        designCoProps.put("name", "DesignCo");
        designCoProps.put("industry", "Design");
        designCoProps.put("size", "100");
        graph.addPropertyVertex("designco", Arrays.asList("Company", "Agency"), designCoProps);
        
        // Create KNOWS relationships between people
        System.out.println("Creating KNOWS relationships...");
        
        Map<String, String> knowsProps1 = new HashMap<>();
        knowsProps1.put("since", "2020");
        knowsProps1.put("context", "college");
        graph.setRelationEdge("alice", "bob", Arrays.asList("KNOWS", "FRIEND"), knowsProps1);
        
        Map<String, String> knowsProps2 = new HashMap<>();
        knowsProps2.put("since", "2021");
        knowsProps2.put("context", "work");
        graph.setRelationEdge("alice", "charlie", Arrays.asList("KNOWS"), knowsProps2);
        
        Map<String, String> knowsProps3 = new HashMap<>();
        knowsProps3.put("since", "2019");
        knowsProps3.put("context", "meetup");
        graph.setRelationEdge("bob", "diana", Arrays.asList("KNOWS", "COLLEAGUE"), knowsProps3);
        
        Map<String, String> knowsProps4 = new HashMap<>();
        knowsProps4.put("since", "2018");
        knowsProps4.put("context", "conference");
        graph.setRelationEdge("diana", "eve", Arrays.asList("KNOWS", "MENTOR"), knowsProps4);
        
        Map<String, String> knowsProps5 = new HashMap<>();
        knowsProps5.put("since", "2022");
        knowsProps5.put("context", "project");
        graph.setRelationEdge("charlie", "diana", Arrays.asList("KNOWS"), knowsProps5);
        
        // Create WORKS_AT relationships
        System.out.println("Creating WORKS_AT relationships...");
        
        Map<String, String> worksAtProps1 = new HashMap<>();
        worksAtProps1.put("role", "Senior Developer");
        worksAtProps1.put("since", "2020");
        worksAtProps1.put("department", "Engineering");
        graph.setRelationEdge("alice", "techcorp", Arrays.asList("WORKS_AT"), worksAtProps1);
        
        Map<String, String> worksAtProps2 = new HashMap<>();
        worksAtProps2.put("role", "Engineering Manager");
        worksAtProps2.put("since", "2019");
        worksAtProps2.put("department", "Engineering");
        graph.setRelationEdge("bob", "techcorp", Arrays.asList("WORKS_AT", "MANAGES"), worksAtProps2);
        
        Map<String, String> worksAtProps3 = new HashMap<>();
        worksAtProps3.put("role", "Lead Designer");
        worksAtProps3.put("since", "2021");
        worksAtProps3.put("department", "Design");
        graph.setRelationEdge("charlie", "designco", Arrays.asList("WORKS_AT"), worksAtProps3);
        
        Map<String, String> worksAtProps4 = new HashMap<>();
        worksAtProps4.put("role", "Full Stack Developer");
        worksAtProps4.put("since", "2022");
        worksAtProps4.put("department", "Product");
        graph.setRelationEdge("diana", "startupinc", Arrays.asList("WORKS_AT"), worksAtProps4);
        
        Map<String, String> worksAtProps5 = new HashMap<>();
        worksAtProps5.put("role", "Chief Technology Officer");
        worksAtProps5.put("since", "2018");
        worksAtProps5.put("department", "Executive");
        graph.setRelationEdge("eve", "techcorp", Arrays.asList("WORKS_AT", "LEADS"), worksAtProps5);
        
        // Print graph summary
        System.out.println("\n=== Graph Summary ===");
        System.out.println("Total vertices: " + graph.size());
        
        assertNotNull(graph);
        assertTrue(graph.size() > 0, "Graph should have nodes");
        
        // Print the property graph
        System.out.println("\n=== Property Graph Details ===");
        graph.printGraph();
        
        // Keep the test running so the visualization can be viewed
        System.out.println("\n=== Visualization Active ===");
        System.out.println("Open the Graphosaurus frontend at ws://localhost:8080 to view the graph.");
        System.out.println("Click on nodes to see their labels and properties.");
        System.out.println("Click on edges to see their relationship types and properties.");
        System.out.println("\nNode types (click to see properties):");
        System.out.println("  - Person nodes: Alice, Bob, Charlie, Diana, Eve");
        System.out.println("  - Company nodes: TechCorp, StartupInc, DesignCo");
        System.out.println("\nRelationship types (click to see properties):");
        System.out.println("  - KNOWS: connections between people");
        System.out.println("  - WORKS_AT: employment relationships");
        System.out.println("\nWaiting 60 seconds for frontend inspection...");
        
        Thread.sleep(60000);  // Wait 60 seconds
        
        // Cleanup
        graph.disableGraphosaurusVisualization();
        System.out.println("\nTest completed.");
    }

    /**
     * Test partial loading mode where only agent-visited nodes/edges are sent.
     * Builds a 200-node grid graph, then spawns several agents that walk
     * random paths, revealing nodes and edges incrementally.
     */
    @Test
    public void propertyGraphWithPartialLoading() throws Exception {
        System.out.println("=== Partial Loading Graphosaurus Test (200 nodes) ===\n");
        
        PropertyGraphPlaces graph = new PropertyGraphPlaces(0, PropertyVertexPlace.class.getName());
        
        System.out.println("Enabling Property Graphosaurus visualization with partial loading...");
        graph.enablePropertyGraphosaurusVisualization("ws://localhost:8080", 500, true);
        assertTrue(graph.isGraphosaurusEnabled(), "Graphosaurus should be enabled");
        
        // --- Build a 200-node graph in a 20x10 grid ---------------------------
        int cols = 10, rows = 10;
        int totalNodes = cols * rows;
        String[] nodeIds = new String[totalNodes];
        
        System.out.println("Creating " + totalNodes + " nodes...");
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                String id = "n" + idx;
                nodeIds[idx] = id.toLowerCase();
                Map<String, String> props = new HashMap<>();
                props.put("name", "Node " + idx);
                props.put("row", String.valueOf(r));
                props.put("col", String.valueOf(c));
                graph.addPropertyVertex(id, Arrays.asList("Grid"), props);
            }
        }
        
        System.out.println("Creating grid edges...");
        int edgeCount = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                // right neighbor
                if (c + 1 < cols) {
                    graph.setRelationEdge(nodeIds[idx], nodeIds[idx + 1],
                            Arrays.asList("GRID"), new HashMap<>());
                    edgeCount++;
                }
                // bottom neighbor
                if (r + 1 < rows) {
                    graph.setRelationEdge(nodeIds[idx], nodeIds[(r + 1) * cols + c],
                            Arrays.asList("GRID"), new HashMap<>());
                    edgeCount++;
                }
            }
        }
        System.out.println("Graph ready: " + totalNodes + " nodes, " + edgeCount + " edges (not yet sent to visualizer).\n");
        
        // --- Spawn 3 agents and walk them along random grid paths -------------
        Random rng = new Random(42);
        int agentCount = 10;
        int stepsPerAgent = 100;
        TestAgent[] agents = new TestAgent[agentCount];
        int[] agentRow = new int[agentCount];
        int[] agentCol = new int[agentCount];
        
        for (int a = 0; a < agentCount; a++) {
            int startR = rng.nextInt(rows);
            int startC = rng.nextInt(cols);
            agentRow[a] = startR;
            agentCol[a] = startC;
            
            String startId = nodeIds[startR * cols + startC];
            VertexPlace startVertex = graph.getVertex(startId);
            assertNotNull(startVertex, "Start vertex " + startId + " should exist");
            
            agents[a] = new TestAgent(a + 1);
            agents[a].setPlacePublic(startVertex);
            startVertex.getAgents().add(agents[a]);
            System.out.println("Spawned agent-" + (a + 1) + " at " + startId);
        }
        
        Thread.sleep(2000);
        
        for (int step = 0; step < stepsPerAgent; step++) {
            for (int a = 0; a < agentCount; a++) {
                // Pick a random adjacent cell (up/down/left/right)
                int nr = agentRow[a], nc = agentCol[a];
                switch (rng.nextInt(4)) {
                    case 0: nr = Math.max(0, nr - 1); break;
                    case 1: nr = Math.min(rows - 1, nr + 1); break;
                    case 2: nc = Math.max(0, nc - 1); break;
                    case 3: nc = Math.min(cols - 1, nc + 1); break;
                }
                if (nr == agentRow[a] && nc == agentCol[a]) continue;
                
                String fromId = nodeIds[agentRow[a] * cols + agentCol[a]];
                String toId = nodeIds[nr * cols + nc];
                
                VertexPlace from = graph.getVertex(fromId);
                VertexPlace to = graph.getVertex(toId);
                
                from.getAgents().remove(agents[a]);
                agents[a].setPlacePublic(to);
                to.getAgents().add(agents[a]);
                
                agentRow[a] = nr;
                agentCol[a] = nc;
                
                if (step % 5 == 0) {
                    System.out.println("  step " + step + ": agent-" + (a + 1) + " " + fromId + " -> " + toId);
                }
            }
            Thread.sleep(1000);
        }
        
        // Remove two agents and keep one alive to test agent-history persistence
        for (int a = 0; a < agentCount - 1; a++) {
            String locId = nodeIds[agentRow[a] * cols + agentCol[a]];
            VertexPlace loc = graph.getVertex(locId);
            loc.getAgents().remove(agents[a]);
            System.out.println("Removed agent-" + (a + 1) + " from " + locId);
        }
        
        System.out.println("\nWaiting 20 seconds for frontend inspection...");
        Thread.sleep(20000);
        
        // Remove last agent
        String lastLocId = nodeIds[agentRow[agentCount - 1] * cols + agentCol[agentCount - 1]];
        VertexPlace lastLoc = graph.getVertex(lastLocId);
        lastLoc.getAgents().remove(agents[agentCount - 1]);
        System.out.println("Removed last agent");
        
        Thread.sleep(5000);
        
        graph.disableGraphosaurusVisualization();
        System.out.println("\nPartial loading test completed.");
    }

    /**
     * Simple agent subclass for test-driven visualization.
     */
    static class TestAgent extends Agent {
        public TestAgent(int id) {
            setAgentId(id);
        }

        @Override
        public Object callMethod(int functionId, Object argument) {
            return null;
        }

        public void setPlacePublic(Place place) {
            super.setPlace(place);
        }
    }

    /**
     * Test creating a larger property graph for stress testing visualization.
     */
    @Test
    public void largerPropertyGraphWithGraphosaurus() throws Exception {
        System.out.println("=== Larger Property Graph Graphosaurus Test ===\n");
        
        // Create PropertyGraphPlaces
        System.out.println("Creating PropertyGraphPlaces...");
        PropertyGraphPlaces graph = new PropertyGraphPlaces(0, PropertyVertexPlace.class.getName());
        
        // Enable property-aware Graphosaurus visualization
        System.out.println("Enabling Property Graphosaurus visualization...");
        graph.enablePropertyGraphosaurusVisualization();
        
        // Create nodes in a grid pattern with different types
        String[] nodeTypes = {"Server", "Database", "Cache", "Queue", "API", "Worker"};
        int nodeCount = 0;
        
        System.out.println("Creating infrastructure nodes...");
        for (int i = 0; i < 20; i++) {
            String type = nodeTypes[i % nodeTypes.length];
            String nodeId = type.toLowerCase() + "_" + i;
            
            Map<String, String> props = new HashMap<>();
            props.put("name", type + " " + i);
            props.put("type", type);
            props.put("status", i % 3 == 0 ? "active" : (i % 3 == 1 ? "idle" : "busy"));
            props.put("cpu", String.valueOf(20 + (i * 3) % 80));
            props.put("memory", String.valueOf(30 + (i * 5) % 70));
            
            graph.addPropertyVertex(nodeId, Arrays.asList(type, "Infrastructure", "Node"), props);
            nodeCount++;
        }
        
        System.out.println("Created " + nodeCount + " nodes");
        
        // Create relationships
        System.out.println("Creating connections...");
        int edgeCount = 0;
        
        for (int i = 0; i < 20; i++) {
            String type = nodeTypes[i % nodeTypes.length];
            String fromId = type.toLowerCase() + "_" + i;
            
            // Connect to 2-3 other nodes
            for (int j = 1; j <= 3; j++) {
                int targetIndex = (i + j * 3) % 20;
                String targetType = nodeTypes[targetIndex % nodeTypes.length];
                String toId = targetType.toLowerCase() + "_" + targetIndex;
                
                if (!fromId.equals(toId)) {
                    Map<String, String> relProps = new HashMap<>();
                    relProps.put("latency", String.valueOf(1 + (i + j) % 10) + "ms");
                    relProps.put("bandwidth", String.valueOf(100 + (i * j) % 900) + "Mbps");
                    relProps.put("protocol", j % 2 == 0 ? "HTTP" : "gRPC");
                    
                    String relType = getRelationshipType(type, targetType);
                    graph.setRelationEdge(fromId, toId, Arrays.asList(relType, "CONNECTS"), relProps);
                    edgeCount++;
                }
            }
        }
        
        System.out.println("Created " + edgeCount + " edges");
        
        // Print summary
        System.out.println("\n=== Infrastructure Graph Summary ===");
        System.out.println("Total vertices: " + graph.size());
        System.out.println("Total edges: " + edgeCount);
        
        assertNotNull(graph);
        assertTrue(graph.size() > 0, "Graph should have nodes");
        
        System.out.println("\n=== Visualization Active ===");
        System.out.println("Open the Graphosaurus frontend to view the infrastructure graph.");
        System.out.println("Nodes are color-coded by type (Server, Database, Cache, etc.)");
        System.out.println("Click nodes/edges to see detailed properties.");
        System.out.println("\nWaiting 60 seconds for frontend inspection...");
        
        Thread.sleep(60000);  // Wait 60 seconds
        
        // Cleanup
        graph.disableGraphosaurusVisualization();
        System.out.println("\nTest completed.");
    }
    
    /**
     * Helper to determine relationship type based on node types
     */
    private String getRelationshipType(String fromType, String toType) {
        if (fromType.equals("API") || toType.equals("API")) {
            return "CALLS";
        } else if (fromType.equals("Database") || toType.equals("Database")) {
            return "QUERIES";
        } else if (fromType.equals("Cache") || toType.equals("Cache")) {
            return "CACHES";
        } else if (fromType.equals("Queue") || toType.equals("Queue")) {
            return "PUBLISHES";
        } else {
            return "CONNECTS";
        }
    }
}
