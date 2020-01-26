package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.util.Vector;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@Category(IntegrationTest.class)
@Ignore // TODO: Pathing is not working. run as classes dir. Copying leaves dependencies still missing
public class DistributedMapTest {
    private static final String graphFilename = "test-files/network-complete.xml";

    class Node extends VertexPlace {

    }

    class Crawler extends Agent implements Serializable {

    }

    @Before
    public void initMASS() {
        MASS.init();
    }

    @After
    public void finishMASS() {
        MASS.finish();
    }

    @Test
    public void graphFileForTestExists() {
        File f = new File(graphFilename);

        assertTrue(f.exists());
    }

    @Test
    public void nodesAreReachable() throws IOException, InterruptedException {
        Vector<String> hosts = MASS.getHosts();

        for (String host : hosts) {
            System.out.println("Pinging " + host + " ...");

            Process p = new ProcessBuilder("ping", "-c", "2", "-q", host).start();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            while (stdInput.ready()) {
                System.out.println(stdInput.readLine());

                if (stdError.ready()) {
                    System.err.println(stdError.readLine());
                }
            }

            p.waitFor();

            assertEquals(0, p.exitValue());
        }
    }

    @Test
    public void mapIsDistributed() {
        String graphArguments[] = {
                "graph.xml",
                "graph_weights.xml",
        };

        Places network = new GraphPlaces(0, Node.class.getName(),
                graphArguments[0], GraphInputFormat.MATSIM,
                GraphInitAlgorithm.FULL_LIST, 100, graphArguments);
    }
}
