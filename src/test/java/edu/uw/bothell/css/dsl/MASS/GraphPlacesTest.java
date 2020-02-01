package edu.uw.bothell.css.dsl.MASS;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.*;

public class GraphPlacesTest {
    @BeforeClass
    public static void setupMASS() {
        MASS.init();
    }

    @AfterClass
    public static void shutdownMASS() {
        MASS.finish();
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
        assertTrue(vertexPlace.neighbors.get(0) == 1);
    }

    @Test
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

        assertTrue("0 neighbors 1", vertexPlace1.neighbors.contains(1));
        assertTrue("1 neighbors 2", vertexPlace2.neighbors.contains(2));
        assertTrue("2 neighbors 0", vertexPlace3.neighbors.contains(0));
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

            int [] neighbors = vPlace.getNeighbors();

            for (int i = 0; i < expectedNeighbors.length; i++) {
                assertEquals(expectedNeighbors[i], neighbors[i]);
            }
        }
    }

//    @Test
//    public void hippieNetworkIs
}
