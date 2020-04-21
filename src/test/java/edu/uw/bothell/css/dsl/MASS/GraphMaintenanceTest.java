package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;
import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.*;
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
        GraphModel graphModel = graph.getGraph();

        List<VertexModel> vertices = graphModel.getVertices();

        assertTrue(!vertices.get(5).neighbors.contains(0));

        assertTrue("Add edge returns true", graph.addEdge(0, 1, 0.9));

        vertices = graph.getGraph().getVertices();

        assertTrue("Edge exists with correct neighbor", vertices.get(0).neighbors.contains(1));
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
        int vertexIdA = graph.addVertex(101);
        int vertexIdB = graph.addVertex(101);

        boolean added = graph.addEdge(vertexIdA, vertexIdB, 0.9);

        assertTrue("Created edge with new vertices", added);

        List<VertexModel> vertices = graph.getGraph().getVertices();

        VertexModel vertexA = vertices.get(vertexIdA);

        assertTrue(vertexA != null);
        assertEquals(1, vertexA.neighbors.size());
        assertEquals(vertexIdB, (long) vertexA.neighbors.get(0));
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
