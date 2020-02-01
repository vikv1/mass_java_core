package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;
import org.junit.*;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * This class tests GraphPlaces features related to graph maintenance
 *   - addVertex
 *   - removeVertex
 *   - addEdge
 *   - removeEdge
 */
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


}
