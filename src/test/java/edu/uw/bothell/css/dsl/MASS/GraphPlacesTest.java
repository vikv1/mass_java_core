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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GraphPlacesTest extends AbstractTest {
    @BeforeAll
    public static void beforeAll() {
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
    public void networkIsCreated() {
        String [] graphArguments = new String[] {
                "test-files/network-triangles.xml",
                "something-else.txt"
        };

        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), "dummy-name.txt",
                GraphInputFormat.CSV, GraphInitAlgorithm.FULL_LIST, 6, graphArguments);

        assertNotNull(graph);
    }

    @Test
    @Disabled // FIXME(Issue #150): test is failing.
    public void neighborsArePopulated() {
        String [] graphArguments = new String[] {
                "test-files/network-triangles.xml",
                "something-else.txt"
        };

        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), "dummy-name.txt",
                GraphInputFormat.CSV, GraphInitAlgorithm.FULL_LIST, 6, graphArguments);

        Place place = graph.getPlaces()[0];

        assertTrue(place instanceof VertexPlace);

        VertexPlace vertexPlace = (VertexPlace) place;

        assertTrue(vertexPlace.neighbors.size() == 1);
        assertTrue(vertexPlace.neighbors.get(0).equals(1));
    }

    @Test
    @Disabled // FIXME(Issue #151): Test is failing.
    public void networkContainsATriangle() {
        String [] graphArguments = new String[] {
                "test-files/network-triangles.xml",
                "something-else.txt"
        };

        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), "dummy-name.txt",
                GraphInputFormat.CSV, GraphInitAlgorithm.FULL_LIST, 6, graphArguments);

        VertexPlace vertexPlace1 = (VertexPlace) graph.getPlaces()[0];
        VertexPlace vertexPlace2 = (VertexPlace) graph.getPlaces()[1];
        VertexPlace vertexPlace3 = (VertexPlace) graph.getPlaces()[2];

        assertTrue( vertexPlace1.neighbors.contains(1) );
        assertTrue( vertexPlace2.neighbors.contains(2) );
        assertTrue( vertexPlace3.neighbors.contains(0) );
    }

    @Test
    public void addEdgeWithoutWeight() {
        // Setup graph and vertices.
        int sourceID = 0;
        int destinationID = 1;
        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), 2);
        graph.addVertex(sourceID);
        graph.addVertex(destinationID);

        // Add an edge without specifying weight.
        graph.addEdge(sourceID, destinationID);

        // Check that the edge was added successfully and that its weight is 1.0.
        int sourceGlobalIndex = graph.getVertexMetaValues(sourceID).Id;
        VertexPlace vert = graph.getVertexPlace(sourceGlobalIndex);

        // validate weights and neighbors list
        assertEquals(vert.neighbors.size(), 1);
        assertEquals(vert.weights.size(), 1);

        int neighbor = (int) vert.neighbors.get(0);
        // GraphPlaces signature requires a double for weight but VertexPlace
        // casts it to an int.
        int neighborWeight = (int) vert.weights.get(0);
        
        assertEquals(neighbor, destinationID);
        assertEquals(neighborWeight, 1);
    }

    @Test
    public void hippieNetworkIsCreated() {
        String [] graphArguments = new String[] {
                "test-files/complete-small.tsv",
                "/dev/null"
        };

        // TODO: Cleanup the constructor for graphplaces to something more like this
//        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), graphArguments[0],
//                GraphInputFormat.HIPPIE, GraphInitAlgorithm.FULL_LIST);

        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), "dummy-name.txt",
                GraphInputFormat.HIPPIE, GraphInitAlgorithm.FULL_LIST, 6, graphArguments);

        assertNotNull(graph);
    }

    @Test
    @Disabled // Maybe the listeners are not closing correctly?
    public void hippieNetworkIsComplete() {
        String [] graphArguments = new String[] {
                "test-files/complete-small.tsv",
                "/dev/null"
        };

        // TODO: Cleanup the constructor for graphplaces to something more like this
//        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), graphArguments[0],
//                GraphInputFormat.HIPPIE, GraphInitAlgorithm.FULL_LIST);

        GraphPlaces graph = new GraphPlaces(0, VertexPlace.class.getName(), "dummy-name.txt",
                GraphInputFormat.HIPPIE, GraphInitAlgorithm.FULL_LIST, 6, graphArguments);

        Place [] places = graph.getPlaces();

        assertEquals(7, places.length);

        assertEquals(VertexPlace.class.getName(), places[0].getClass().getName());

        int [] vertices = {
                0, 1, 2, 3, 4, 5, 6
        };

        for (Place place : places) {
            VertexPlace vPlace = (VertexPlace) place;
            int id = place.getIndex()[0];

            int [] expectedNeighbors = Arrays.stream(vertices).filter(pid -> pid != id).toArray();

            Object [] neighbors = vPlace.getNeighbors();

            for (int i = 0; i < expectedNeighbors.length; i++) {
                assertEquals(expectedNeighbors[i], neighbors[i]);
            }
        }
    }

//    @Test
//    public void hippieNetworkIs
}
