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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GraphPlacesTest extends AbstractTest {
    @BeforeAll
    public static void beforeAll() {
        resetMASSBase();
        MNode masterNode = new MNode();
        masterNode.setHostName( randomString() );
        masterNode.setMaster( true );
        MASSBase.addNode( masterNode );
        MASSBase.initMASSBase( masterNode );
    }

    @AfterAll
    public static void afterAll() {
        resetMASSBase();
    }

    @Test
    public void dslNetworkIsCreated() throws Exception {
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        graph.loadDSLFile("src/resources/test-files/test-graph.dsl");

        assertNotNull(graph);
        assertEquals(10, graph.size());
    }

    @Test
    public void sarNetworkIsCreated() throws Exception {
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        graph.loadSARFile("src/resources/test-files/test-graph.sar");

        assertNotNull(graph);
        assertEquals(10, graph.size());
    }

    @Test
    public void neighborsArePopulated() throws Exception {
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        graph.loadDSLFile("src/resources/test-files/test-graph.dsl");

        Place place = graph.getPlaces()[0];

        assertTrue(place instanceof VertexPlace);

        VertexPlace vertexPlace = (VertexPlace) place;

        assertTrue(vertexPlace.neighbors.size() == 3);
        assertTrue(vertexPlace.neighbors.get(0).equals(8));
        assertTrue(vertexPlace.weights.get(0).equals(9));
    }

    @Test
    public void testAddVertexSingleNode() {
        // Setup graph and vertices.
        int sourceID = 0;
        int destinationID = 1;
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        
        // Add a few vertices
        graph.addVertex();
        graph.addVertex();
        graph.addVertex();
        assertEquals(3, graph.size());

        graph.removeVertex(1);
        assertEquals(2, graph.size());

        graph.addVertex();
        assertEquals(3, graph.size());

        graph.addVertex();
        assertEquals(4, graph.size());
    }

    @Test
    public void testGetVertexSingleNode() {
        // Setup graph and vertices.
        int sourceID = 0;
        int destinationID = 1;
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        
        // Add a few vertices
        int vertexID = graph.addVertexWithParams("D3AD10CC");
        assertEquals(1, graph.size());

        VertexPlace tut = graph.getVertex(vertexID);
        assertNotNull(tut);

        graph.removeVertex(vertexID);
        assertEquals(0, graph.size());

        tut = graph.getVertex(vertexID);
        assertNull(tut);
    }

    @Test
    public void testRemoveVertexSingleNode() {
        // Setup graph and vertices.
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());

        // Add a few vertices
        int vertID1 = graph.addVertex();
        int vertID2 = graph.addVertex();
        int vertID3 = graph.addVertex();
        assertEquals(3, graph.size());

        // remove a valid vertex
        assertTrue(graph.removeVertex(vertID1));
        assertEquals(2, graph.size());

        // remove a invalid vertex
        assertTrue(!graph.removeVertex(3));
        assertEquals(2, graph.size());

        // remove the remaining vertices
        graph.removeVertex(vertID2);
        graph.removeVertex(vertID3);
        assertEquals(0, graph.size());
    }

    @Test
    public void addEdgeWithoutWeight() {
        // Setup graph and vertices.
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        int sourceID = graph.addVertex();
        int destinationID = graph.addVertex();

        // Add an edge without specifying weight.
        assertTrue(graph.addEdge(sourceID, destinationID));

        // Check that the edge was added successfully and that its weight is 1.0.
        VertexPlace vert = graph.getVertex(sourceID);

        // validate weights and neighbors list
        assertEquals(1, vert.neighbors.size());
        assertEquals(1, vert.weights.size());

        int neighbor = (int) vert.neighbors.get(0);
        // GraphPlaces signature requires a double for weight but VertexPlace
        // casts it to an int.
        int neighborWeight = (int) vert.weights.get(0);
        
        assertEquals(destinationID, neighbor);
        assertEquals(1, neighborWeight);
    }

    @Test
    public void testRemoveEdge() {
        // Setup graph and vertices
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName());
        int sourceID = graph.addVertex();
        int destinationID = graph.addVertex();

        // Add an edge without specifying weight.
        assertTrue(graph.addEdge(sourceID, destinationID));

        // Check that the edge was added successfully and that its weight is 1.0.
        VertexPlace vert = graph.getVertex(sourceID);

        // validate weights and neighbors list
        assertEquals(1, vert.neighbors.size());
        assertEquals(1, vert.weights.size());

        assertTrue(graph.removeEdge(sourceID, destinationID));
        vert = graph.getVertex(sourceID);

        // validate that the edge is removed
        assertEquals(0, vert.neighbors.size());
        assertEquals(0, vert.weights.size());
    }
}
