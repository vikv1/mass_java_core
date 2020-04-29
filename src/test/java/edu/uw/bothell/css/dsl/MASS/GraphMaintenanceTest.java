package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;
import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.After;
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

    @After
    public void shutdownMASS() {
        MASS.finish();
    }

    @Before
    public void initGraph() {
        MASS.init();

        graph = new GraphPlaces(0, VertexPlace.class.getName(), 120);

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
    public void testRemoveEdge() {
        final String vertexA = "A";
        final String vertexB = "B";

        graph.addVertex(vertexA);
        graph.addVertex(vertexB);

        boolean added = graph.addEdge(vertexA, vertexB, 0.9);

        assertTrue("Add edge returns true", added);

        VertexModel vertex = graph.getGraph().getVertices().stream().filter(v -> v.id.equals(vertexA)).findFirst().get();

        assertTrue("Edge exists with correct neighbor", vertex.neighbors.contains(vertexB));
        
        graph.removeEdge(vertexA, vertexB);
        
        vertex = graph.getGraph().getVertices().stream().filter(v -> v.id.equals(vertexA)).findFirst().get();
        
        assertTrue(!vertex.neighbors.stream().filter(n -> n.equals(vertexB)).findFirst().isPresent());
    }

    @Test
    public void testAddVertex() {
        final String vertexKey = "ABC";
        
        int vertexId = graph.addVertex(vertexKey);

        assertTrue("Created vertex with valid id", vertexId >= 0);

        List<VertexModel> vertices = graph.getGraph().getVertices();

        VertexModel vertex = vertices.stream().filter(v -> v.id.equals(vertexKey)).findFirst().get();

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
}
