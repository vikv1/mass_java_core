package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@Category(IntegrationTest.class)
public class MATSimNetworkModelTest {
    // network-pt-simple.xml from Cytoscape repository
    private final String networkXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<!DOCTYPE network SYSTEM \"http://matsim.org/files/dtd/network_v1.dtd\">\n" +
            "<network>\n" +
            "<nodes>\n" +
            "<node x=\"0.0\" y=\"0.0\" id=\"1\" />\n" +
            "<node x=\"10.0\" y=\"0.0\" id=\"2\" />\n" +
            "<node x=\"20.0\" y=\"0.0\" id=\"3\" />\n" +
            "<node x=\"2000.0\" y=\"0.0\" id=\"4\" />\n" +
            "<node x=\"2010.0\" y=\"0.0\" id=\"5\" />\n" +
            "<node x=\"2020.0\" y=\"0.0\" id=\"6\" />\n" +
            "</nodes>\n" +
            "\n" +
            "<links capperiod=\"10:00:00\">\n" +
            "<link id=\"1-2\" modes=\"pt\" permlanes=\"1\" capacity=\"2000\" freespeed=\"22\" length=\"10\" to=\"2\" from=\"1\"/>\n" +
            "<link id=\"2-3\" modes=\"pt\" permlanes=\"1\" capacity=\"2000\" freespeed=\"22\" length=\"10\" to=\"3\" from=\"2\"/>\n" +
            "<link id=\"3-4\" modes=\"pt\" permlanes=\"1\" capacity=\"2000\" freespeed=\"22\" length=\"1980\" to=\"4\" from=\"3\"/>\n" +
            "<link id=\"4-5\" modes=\"pt\" permlanes=\"1\" capacity=\"2000\" freespeed=\"22\" length=\"10\" to=\"5\" from=\"4\"/>\n" +
            "<link id=\"5-6\" modes=\"pt\" permlanes=\"1\" capacity=\"2000\" freespeed=\"22\" length=\"10\" to=\"6\" from=\"5\"/>\n" +
            "</links>\n" +
            "</network>";

    private MATSimNetworkModel network = null;

    @Before
    public void importNetwork() {
        StringReader reader = new StringReader(networkXml);

        try {
            System.setProperty("javax.xml.accessExternalDTD", "all");

            JAXBContext jaxbContext = JAXBContext.newInstance(MATSimNetworkModel.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            network = (MATSimNetworkModel) jaxbUnmarshaller.unmarshal(reader);
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

    @Test
    public void theXPathAPI() {
        XPathFactory factory = XPathFactory.newInstance();

        XPath path = factory.newXPath();

        XPathExpression expression = null;

        try {
            expression = path.compile("//nodes/node/@id");

             NodeList nodeList = (NodeList) expression.evaluate(new InputSource(new StringReader(networkXml)),
                     XPathConstants.NODESET);

             for (int i = 0; i < nodeList.getLength(); i++) {
                 Node node = nodeList.item(i);

                 System.out.println("Item: " + i + ": " + node.getNodeValue());
             }

             assertEquals(nodeList.getLength(), 6);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }
}
