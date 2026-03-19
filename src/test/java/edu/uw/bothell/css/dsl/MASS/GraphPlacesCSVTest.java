/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Manual integration test: requires local CSV files and Graphosaurus frontend.")
public class GraphPlacesCSVTest extends AbstractTest {
    
    // Path to CSV files in Downloads folder
    private static final String NODES_FILE = "C:/Users/lakes/Downloads/synthetic_airline_nodes_30k.csv";
    private static final String EDGES_FILE = "C:/Users/lakes/Downloads/synthetic_airline_edges_30k.csv";
    
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

    @Test
    public void csvNetworkIsCreated() throws Exception {
        System.out.println("Creating GraphPlaces...");
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        
        System.out.println("Loading CSV files...");
        System.out.println("  Nodes file: " + NODES_FILE);
        System.out.println("  Edges file: " + EDGES_FILE);
        
        long startTime = System.currentTimeMillis();
        graph.loadCSVFiles(NODES_FILE, EDGES_FILE);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Load completed in " + (endTime - startTime) + " ms");
        System.out.println("Total nodes loaded: " + graph.size());
        
        // Debug: Print first few entries in distributed map
        System.out.println("\n--- Distributed Map Sample (first 5 entries) ---");
        int count = 0;
        for (Object key : MASSBase.distributed_map.keySet()) {
            if (count++ >= 5) break;
            System.out.println("  Key: '" + key + "' -> Value: " + MASSBase.distributed_map.get(key));
        }
        System.out.println("  Total keys in distributed map: " + MASSBase.distributed_map.size());
        
        assertNotNull(graph);
        assertTrue(graph.size() > 0, "Graph should have nodes loaded");
        
        // Print some stats
        Place[] places = graph.getPlaces();
        int totalEdges = 0;
        for (Place p : places) {
            if (p instanceof VertexPlace) {
                totalEdges += ((VertexPlace) p).neighbors.size();
            }
        }
        System.out.println("\nTotal edges loaded: " + totalEdges);
        
        // Print sample vertex with neighbors
        System.out.println("\n--- Sample Vertex ---");
         for (Place p : places) {
            if (p instanceof VertexPlace) {
                VertexPlace vp = (VertexPlace) p;
                if (vp.neighbors.size() > 0) {
                    System.out.println("  Vertex index: " + vp.getIndex()[0]);
                    System.out.println("  Neighbors: " + vp.neighbors);
                    System.out.println("  Weights: " + vp.weights);
                    break;
                }
            }
        }
    }

    @Test
    public void csvNetworkWithGraphosaurus() throws Exception {
        System.out.println("Creating GraphPlaces with Graphosaurus visualization...");
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        
        // Enable Graphosaurus visualization
        System.out.println("Enabling Graphosaurus visualization...");
        graph.enableGraphosaurusVisualization();
        assertTrue(graph.isGraphosaurusEnabled(), "Graphosaurus should be enabled");
        
        System.out.println("Loading CSV files...");
        long startTime = System.currentTimeMillis();
        graph.loadCSVFiles(NODES_FILE, EDGES_FILE);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Load completed in " + (endTime - startTime) + " ms");
        System.out.println("Total nodes loaded: " + graph.size());
        
        assertNotNull(graph);
        assertTrue(graph.size() > 0, "Graph should have nodes loaded");
        
        // Keep the test running for a bit so you can see the visualization
        System.out.println("Visualization active. Waiting 30 seconds for frontend inspection...");
        System.out.println("Open the Graphosaurus frontend at ws://localhost:8080 to view the graph.");
        
        Thread.sleep(90000);  // Wait 30 seconds
        
        // Cleanup
        graph.disableGraphosaurusVisualization();
        System.out.println("Test completed.");
    }
}

