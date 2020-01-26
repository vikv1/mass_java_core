package edu.uw.bothell.css.dsl.MASS;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore // TODO: Fix this test
public class VertexPlaceTest {
    private final String graph_filename = "../matsim/network-pt-simple.xml";

    @Test
    public void getNeighborsReturnsAList() {
        List<VertexPlace.Tuple> neighbors = VertexPlace.getNeighbors(graph_filename, 1);

        assertTrue(neighbors != null);
    }

    @Test
    public void getNeighborsReturnsTheCorrectNumberOfNeighbors() {
        List<VertexPlace.Tuple> neighbors = VertexPlace.getNeighbors(graph_filename, 1);

        assertEquals(1, neighbors.size());
    }

    @Test
    public void getNeighborsReturnsTheCorrectNeighbors() {
        List<VertexPlace.Tuple> neighbors = VertexPlace.getNeighbors(graph_filename, 1);

        List<VertexPlace.Tuple> realNeighbors = new ArrayList<>(Arrays.asList(
                new VertexPlace.Tuple(2, 10)
        ));

        assertEquals("Has 1 neighbor", 1, neighbors.size());
        assertEquals("Neighbor is index 2", 2, neighbors.get(0).index);
        assertEquals("Neighbor id 2 has weight 10", 10.0, neighbors.get(0).weight, 0.05);
    }
}
