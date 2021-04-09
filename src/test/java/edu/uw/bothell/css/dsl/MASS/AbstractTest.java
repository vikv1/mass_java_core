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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Random;

import org.easymock.EasyMockExtension;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

/**
 * Abstract Test contains helper classes and methods used by unit test classes
 */
@TestMethodOrder( MethodOrderer.Random.class )
@ExtendWith( EasyMockExtension.class )
public class AbstractTest extends EasyMockSupport {

    // Toggle for enabling Unicode in unit Tests
    private static final boolean ENABLE_UNICODE = false;
    
    // when not specified, length of a random string
    private static final int DEFAULT_RANDOM_STRING_LENGTH = 32;

    private static Random random = new Random(System.currentTimeMillis());

    /**
     * Perform actions that will reset MASSBase for another test
     */
    public static void resetMASSBase() {
    	
    		MASSBase.getAllNodes().clear();
    		MASSBase.setCurrentArgument( null );
    		MASSBase.setCurrentFunctionId( 0 );
    		MASSBase.setCurrentMsgType( null );
    		MASSBase.setCurrentAgentsBase( null );
    		MASSBase.setCurrentAgentsBase( null );
    		MASSBase.setCurrentReturns( null );
    		MASSBase.setDestinationPlaces( null );
    		MASSBase.getAgentsMap().clear();
    		MASSBase.getPlacesMap().clear();
    		MASSBase.getRemoteNodes().clear();
    		MASSBase.getHosts().clear();
    		MASSBase.getLogger().setLogLevel(LogLevel.OFF);
    		
    }
    
    /**
     * Perform any necessary operations before every unit test
     */
    @BeforeEach
    public void abstractSetUp() {
    	
    	MASSBase.getLogger().setLogLevel( LogLevel.DEBUG );

    }
    
	/**
	 * Perform any necessary cleanup and final verifications after EVERY test 
	 */
	@AfterEach
	public void abstractTearDown() {

		// make sure all mock objects were called as expected
		verifyAll();

		// reset all mock objects to prepare for next test
		resetAll();
		
	}

	/**
     * Is the specified character a valid character for an XML document? (According to W3C specs)
	 *
     * @param t The character to test
     * @return TRUE if the character can be included within the body of an XML document, FALSE if not
     */
    protected static boolean isValidXMLCharacter(char t) {
        if (((t < 32 || t > 55295) && (t != 9) && (t != 10) && (t != 13) && (t < 57344 || t > 65533))) return false;
        return true;
    }

    /**
     * Generate a random boolean value
     * @return A random boolean value
     */
    protected static boolean randomBoolean() {
        return random.nextBoolean();
    }

    /**
     * Generate a random byte value
     * @return A random byte value
     */
    protected static byte randomByte() {
        byte returnByte = (byte) ((255 * random.nextDouble()) - 128);
        return returnByte;
    }
	
    /**
     * Generate a random double value
     * @return A random double value
     */
    protected static double randomDouble() {
        return random.nextDouble();       
    }

    /**
     * Generate a random float value
     * @return A random float value
     */
    protected static float randomFloat() {
        return random.nextFloat();       
    }
	
    /**
     * Generate a random integer value
     * @return A random integer value
     */
    protected static int randomInt() {
        return random.nextInt();       
    }

    /**
     * Generate a ranged random integer value
     * @param minValue The minimum value to generate
     * @param maxValue The maximum value to generate
     * @return A random integer value
     */
    protected static int randomInt(int minValue, int maxValue) {
    	
    	double val = random.nextDouble();
    	
    	val = (val * ((double) maxValue - (double) minValue)) + (double) minValue;
    	
    	return (int) val;
    	
    }
	
    /**
     * Generate a random long value
     * @return A random long value
     */
    protected static long randomLong() {
        return random.nextLong();
    }

    /**
     * Generate a random short value
     * @return A random short value
     */
    protected static short randomShort() {
        
        // generate a value from 32767 to -32768
        short returnValue = (short) ((65535 * random.nextDouble()) - 32768); 
        
        // return the value
        return returnValue;
        
    }

    /**
     * Generate a string of random characters of default length
     *@returns A string of random characters
    */
    protected static String randomString() {
    	return randomString( DEFAULT_RANDOM_STRING_LENGTH );
    }
    
    /**
     * Generate a string of random characters of specified length
     *
     *@param length The length to the string to be generated
     *@returns A string of random characters of the length specified
    */
    protected static String randomString(int length) {
    
        StringBuffer returnString = new StringBuffer("");
        
        // loop to build string
        for (int i = 0; i < length; i ++) {

            char t = 0;
            double randomValue = 0;
            
            // generate character "t" based on desire to use full range of characters
            if (ENABLE_UNICODE) {
            
                do {

                    // assign a random character value
                    randomValue = random.nextDouble();
                    t = (char) (65535 * randomValue);
                    
                }
                
                // is the character value within normal UNICODE character set? (excludes invalid xml characters and CR)
                while (isValidXMLCharacter(t) == false || (t == 13));
            
            }
            
            // use only ASCII character set
            else {

                do {

                    // assign a random character value
                    randomValue = random.nextDouble();
                    t = (char) (256 * randomValue);
                    
                }
                
                // is the character value within normal ASCII character set? (upper/lowercase letters and numbers)
                while ((t < 48 || t >57) && (t < 65 || t > 90) && (t < 97 || t > 122));
                
            }
            
            // append the newly generated character
            returnString.append(t);
            
        }
        
        // convert the buffer to a string and return it
        return returnString.toString();
        
    }

    /**
     * Using Java native serialization, return an array of bytes representing
     * the serialized form of the supplied object
     * @param objectToSerialize The object to serialize
     * @return An array of bytes representing the serialized object
     * @throws IOException
     */
    protected static byte[] serializeObject( Object objectToSerialize ) throws IOException {

    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ObjectOutput out = new ObjectOutputStream( bos );

    	// serialize to the byte array
    	out.writeObject( objectToSerialize );
    	out.flush();

    	// return the array
    	return bos.toByteArray();

    }
    
}