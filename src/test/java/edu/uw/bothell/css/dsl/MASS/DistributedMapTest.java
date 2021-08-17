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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

//@Category(IntegrationTest.class)
@Disabled // TODO: Pathing is not working. run as classes dir. Copying leaves dependencies still missing
public class DistributedMapTest {
    private static final String graphFilename = "test-files/network-complete.xml";

    @SuppressWarnings("serial")
	class Node extends VertexPlace {

    }

    @SuppressWarnings("serial")
	class Crawler extends Agent implements Serializable {

    }

    @BeforeEach
    public void initMASS() {
        MASS.init();
    }

    @AfterEach
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

        Places network = new GraphPlaces(0, Node.class.getName());
    }
}
