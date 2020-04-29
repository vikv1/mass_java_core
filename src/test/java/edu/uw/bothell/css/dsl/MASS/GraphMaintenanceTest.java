package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;
import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class tests GraphPlaces features related to graph maintenance
 *   - addVertex
 *   - removeVertex
 *   - addEdge
 *   - removeEdge
 */
@Category(IntegrationTest.class)
public class GraphMaintenanceTest {
    private Graph graph;

    @BeforeClass
    public static void setupMASS() {
        MASS.init();
    }

    @AfterClass
    public static void shutdownMASS() {
        MASS.finish();
    }

    @Before
    public void initGraph() {
        String [] graphArguments = new String[] {
                "test-files/network-triangles.xml",
                "something-else.txt"
        };

        graph = new GraphPlaces(0, VertexPlace.class.getName(), "dummy-name.txt",
                GraphInputFormat.CSV, GraphInitAlgorithm.FULL_LIST, 6, graphArguments);

    }

    @Test
    public void testAddEdge() {
        final String vertexA = "A";
        final String vertexB = "B";
        
        graph.addVertex(vertexA);
        graph.addVertex(vertexB);
        
        boolean added = graph.addEdge(vertexA, vertexB, 0.9);
        
        assertTrue("Add edge returns true", added);

        VertexModel vertex = graph.getGraph().getVertices().stream().filter(v -> v.id.equals(vertexA)).findFirst().get();
        
        assertTrue("Edge exists with correct neighbor", vertex.neighbors.contains(vertexB));
    }

    @Test
    @Ignore
    public void testRemoveEdge() {

    }

    @Test
    public void testAddVertex() {
        GraphModel model = graph.getGraph();

        List<VertexModel> vertices = model.getVertices();

        assertTrue("Sanity check for non-existing vertex 6", vertices.size() == 6);

        int vertexId = graph.addVertex(101);

        assertTrue("Created vertex with valid id", vertexId >= 0);

        vertices = graph.getGraph().getVertices();

        assertTrue("Vertex is created", vertexId == 6);

        VertexModel vertex = vertices.get(vertexId);

        assertTrue(vertex != null);
        assertEquals(0, vertex.neighbors.size());
    }

    @Test
    public void testAddEdgeWithNewVertices() {
        int vertexIdA = 101;
        int vertexIdB = 102;
        
        graph.addVertex(vertexIdA);
        graph.addVertex(vertexIdB);

        boolean added = graph.addEdge(vertexIdA, vertexIdB, 0.9);

        assertTrue("Created edge with new vertices", added);

        List<VertexModel> vertices = graph.getGraph().getVertices();

        VertexModel vertexA = vertices.stream().filter(v -> v.id.equals(vertexIdA)).findFirst().get();

        assertTrue(vertexA != null);
        assertEquals(1, vertexA.neighbors.size());
        assertEquals(vertexIdB, vertexA.neighbors.get(0));
    }

    @Test
    /**
     * Considering a mass cluster with 3 nodes available: mass0 (master) mass1 mass2
     */
//    public void createNetworkOnMultipleNodes() {
//        Integer [] topology = ((GraphPlaces) graph).getTopology();
//
//        for (int i = 0; i < topology.length; i++) {
//            assertTrue(topology[i] == 2);
//        }
//    }

    @Category(IntegrationTest.class)
    public void testTopographyIsRetrieved() {

    }
}
