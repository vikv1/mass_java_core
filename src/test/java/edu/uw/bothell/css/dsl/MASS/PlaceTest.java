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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Place Test
 *
 * @author Michael O'Keefe
 */
public class PlaceTest extends AbstractTest {

    @TestSubject
    private Place place = new Place(); // create a place object to test with

    @Test
    public void testOpenWithTxt() throws Exception {
        String fileName = "testTxt.txt";

        // call places open method with the given files name and check if
        // returned descriptor object is returned as expected
        Object descriptor = place.open(fileName);

        // check if descriptor is a BufferedReader object
        if (descriptor instanceof BufferedReader) {

            // txt file will read "SUCCESS" if returned properly
            System.out.println(((BufferedReader) descriptor).readLine());

            // close the txt file
            try {
                ((BufferedReader) descriptor).close();
            } catch (IOException ioe) {
                System.err.println( ioe );
            }
        }
    }

    // TODO: Ask Matt about solving slf4j logging dependencies for NetdfFiles
    @Test
    public void testOpenWithNetcdf() throws Exception {
        String fileName = "testNetcdf.nc";

        // call places open method with the given file name and check if returned
        // descriptor object is returned as expected
        Object descriptor = place.open(fileName);

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
