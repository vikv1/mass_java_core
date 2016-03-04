package edu.uw.bothell.css.dsl.MASS;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.stream.NcStreamProto;

import java.io.BufferedReader;
import java.io.IOException;

import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Place Test
 *
 * @author Michael O'Keefe
 */
public class PlaceTest extends AbstractTest {


    @TestSubject
    private Place place1 = new Place(); // create a place object to test with

    @TestSubject
    private Place place2 = new Place();


    @Test
    public void testOpenWithTxt() throws Exception {
        String filePath = "/Users/Michael/mass_java_core/testTxt.txt";

        String filePath2 = "/Users/Michael/mass_java_core/testTxt2.txt";

        Object descriptor = null;
        Object descriptor2 = null;
        Object descriptor3 = null;

        descriptor = place1.open(filePath, 0);

        descriptor = place2.open(filePath, 0);

        descriptor2 = place1.open(filePath2, 0);

        descriptor2 = place2.open(filePath2, 0);

        descriptor3 = place1.open(filePath, 0);

        descriptor3 = place2.open(filePath, 0);



        if (descriptor instanceof FileChannel) {

            // close the txt file
            try {
                ((FileChannel) descriptor).close();
            } catch (IOException ioe) {
                System.err.println( ioe );
            }
        }

        if (descriptor2 instanceof FileChannel) {

            // close the txt file
            try {
                ((FileChannel) descriptor2).close();
            } catch (IOException ioe) {
                System.err.println( ioe );
            }
        }

        if (descriptor3 instanceof FileChannel) {

            // close the txt file
            try {
                ((FileChannel) descriptor3).close();
            } catch (IOException ioe) {
                System.err.println( ioe );
            }
        }
    }

    // TODO: slf4j logging dependencies for NetdfFiles
    @Test
    public void testOpenWithNetcdf() throws Exception {
        String filePath = "/testNetcdf.nc";

        // call places open method with the given file name and check if returned
        // descriptor object is returned as expected
        Object descriptor = place1.open(filePath, 0);

        // check if descriptor is a NetcdfFile
        if (descriptor instanceof NetcdfFile) {

            // for storing Netcdf Variables
            List<Variable> netcdfVariables = new ArrayList<>();

            // retrive the variable "data" from the descriptor and add it to list of Variables
            netcdfVariables.add( ( ( NetcdfFile ) descriptor ).findVariable( "data" ) );

            // obtain an Array of the "data" Variable
            List<Array> readResults = ( ( NetcdfFile ) descriptor ).readArrays(netcdfVariables);

            // make sure results were returned
            if (readResults.isEmpty()) {
                System.out.println("Empty Results: FAILED");
            }

            // print results: 0 through 71 expected
            else {
                System.out.println("testOpenWithNetcdf SUCCESS: " + readResults.get(0));
            }

            // close the NetcdfFile
            try {
                ((NetcdfFile) descriptor).close();
            } catch (IOException ioe) {
                System.err.println( ioe );
            }
        }
    }
}
