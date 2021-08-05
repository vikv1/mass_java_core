package edu.uw.bothell.css.dsl.MASS.graph.matsim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.MNode;

public class MatsimTest extends AbstractTest {
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
    public void matsimNetworkIsCreated() {
        Matsim graph = new Matsim(0);
        assertNotNull(graph);
    }

    @Test
    public void matsimNetworkIsComplete() throws Exception {
        Matsim graph = new Matsim(0);
        graph.loadFromFile("test-files/test-matsim.xml");
        

        Place [] places = graph.getPlaces();

        assertEquals(6, places.length);
    }
}