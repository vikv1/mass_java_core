package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.Nodelist;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.File;

public class MATSimNetworkModelTest {
    @Test
    public void theNetworkIsImported() {
        String networkFilename = "../matsim/network-pt-simple.xml";

        File networkFile = new File(networkFilename);

        try {
            System.setProperty("javax.xml.accessExternalDTD", "all");

            JAXBContext jaxbContext = JAXBContext.newInstance(MATSimNetworkModel.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            MATSimNetworkModel network = (MATSimNetworkModel) jaxbUnmarshaller.unmarshal(networkFile);

            for (MATSimNetworkNode node : network.getNodes()) {
                System.out.println(node);
            }
        } catch (JAXBException e) {

            System.err.println( "Error initializing JAXB parser..." );
            e.printStackTrace();

            MASS.getLogger().error( "Error initializing JAXB parser...", e );
            System.exit( -1 );

        }
    }
}
