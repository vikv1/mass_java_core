package edu.uw.bothell.css.dsl.MASS.graph.hippie;

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

public class HippieTest extends AbstractTest {
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
    public void hippieNetworkIsCreated() {
        Hippie graph = new Hippie(0);
        assertNotNull(graph);
    }

    @Test
    public void hippieNetworkIsComplete() throws Exception {
        Hippie graph = new Hippie(0);
        graph.loadFromFile("test-files/test-hippie.tsv");
        

        Place [] places = graph.getPlaces();

        assertEquals(14, places.length);
    }
}