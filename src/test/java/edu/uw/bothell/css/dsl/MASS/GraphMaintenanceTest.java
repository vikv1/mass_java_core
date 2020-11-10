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

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;
import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.After;
import org.junit.Before;
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
    
    @Test
    public void testRemoveVertex() {
        final String vertexA = "A";
        final String vertexB = "B";
        final String vertexC = "C";
        
        graph.addVertex(vertexA);
        graph.addVertex(vertexB);
        graph.addVertex(vertexC);
        
        graph.addEdge(vertexA, vertexB, 0.9);
        graph.addEdge(vertexA, vertexC, 0.9);

        VertexModel vertexAModel = graph.getGraph().getVertices()
                .stream()
                .filter(v -> v.id.equals(vertexA))
                .findFirst()
                .get();

        assertTrue(vertexAModel.neighbors.size() == 2);
        
        assertTrue(graph.getGraph().getVertices().size() == 3);
        
        graph.removeVertex(vertexB);
        
        assertTrue(graph.getGraph().getVertices().size() == 2);

        vertexAModel = graph.getGraph().getVertices()
                .stream()
                .filter(v -> v.id.equals(vertexA))
                .findFirst()
                .get();
        
        assertTrue(vertexAModel.neighbors.size() == 1);
        assertTrue(vertexAModel.neighbors.contains(vertexC));
    }
}
