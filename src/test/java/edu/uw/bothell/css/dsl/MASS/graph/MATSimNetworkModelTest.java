package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.Nodelist;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class MATSimNetworkModelTest {
    private MATSimNetworkModel network = null;

    @Before
    public void importNetwork() {
        String networkFilename = "../matsim/network-pt-simple.xml";

        File networkFile = new File(networkFilename);

        try {
            System.setProperty("javax.xml.accessExternalDTD", "all");

            JAXBContext jaxbContext = JAXBContext.newInstance(MATSimNetworkModel.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            network = (MATSimNetworkModel) jaxbUnmarshaller.unmarshal(networkFile);
        } catch (JAXBException e) {
            System.err.println( "Error initializing JAXB parser..." );
            e.printStackTrace();

            MASS.getLogger().error( "Error initializing JAXB parser...", e );
        }
    }

    @Test
    public void theNetworkIsImported() {
        assertNotNull(network);
    }

    @Test
    public void theNetworkIsTheCorrectShape() {
        assertEquals(6, network.getNodes().size());
        assertEquals(5, network.getLinks().size());
    }

    @Test
    public void theNetworkNodesAreIdentifiedCorrectly() {
        List<Long> nodeIds = network.getNodes().stream().map(n -> n.id).collect(Collectors.toList());

        Set<Long> nodesIdsSet = network.getNodes().stream().map(n -> n.id).collect(Collectors.toSet());

        assertTrue(nodesIdsSet.containsAll(nodeIds));
    }
}
