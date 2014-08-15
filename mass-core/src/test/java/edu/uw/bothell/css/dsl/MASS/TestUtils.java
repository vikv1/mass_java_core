package edu.uw.bothell.css.dsl.MASS;

import java.util.Random;

import org.junit.Ignore;

/**
 * TestUtils contains helper methods necessary for testing the MASS library
 * 
 * @author Matthew Sell
 *
 */
@Ignore
public class TestUtils {

    private static Random random = new Random(System.currentTimeMillis());

    // Toggle for enabling Unicode in unit Tests
    public static final boolean USE_UNICODE = false;

	/**
     * Is the specified character a valid character for an XML document? (According to W3C specs)
	 *
     * @param t The character to test
     * @return TRUE if the character can be included within the body of an XML document, FALSE if not
     */
    public static boolean isValidXMLCharacter(char t) {
        if (((t < 32 || t > 55295) && (t != 9) && (t != 10) && (t != 13) && (t < 57344 || t > 65533))) return false;
        return true;
    }

    /**
     * Generate a random boolean value
     * @return A random boolean value
     */
    public static boolean randomBoolean() {
        return random.nextBoolean();
    }

    /**
     * Generate a random byte value
     * @return A random byte value
     */
    public static byte randomByte() {
        byte returnByte = (byte) ((255 * random.nextDouble()) - 128);
        return returnByte;
    }
	
    /**
     * Generate a random double value
     * @return A random double value
     */
    public static double randomDouble() {
        return random.nextDouble();       
    }

    /**
     * Generate a random float value
     * @return A random float value
     */
    public static float randomFloat() {
        return random.nextFloat();       
    }
	
    /**
     * Generate a random integer value
     * @return A random integer value
     */
    public static int randomInt() {
        return random.nextInt();       
    }

    /**
     * Generate a ranged random integer value
     * @param minValue The minimum value to generate
     * @param maxValue The maximum value to generate
     * @return A random integer value
     */
    public static int randomInt(int minValue, int maxValue) {
    	
    	double val = random.nextDouble();
    	
    	val = (val * ((double) maxValue - (double) minValue)) + (double) minValue;
    	
    	return (int) val;
    	
    }
	
    /**
     * Generate a random long value
     * @return A random long value
     */
    public static long randomLong() {
        return random.nextLong();
    }

    /**
     * Generate a random short value
     * @return A random short value
     */
    public static short randomShort() {
        
        // generate a value from 32767 to -32768
        short returnValue = (short) ((65535 * random.nextDouble()) - 32768); 
        
        // return the value
        return returnValue;
        
    }

    /**
     * Generate a string of random characters
     *
     *@param length The length to the string to be generated
     *@returns A string of random characters of the length specified
    */
    public static synchronized String randomString(int length) {
    
        StringBuffer returnString = new StringBuffer("");
        
        // loop to build string
        for (int i = 0; i < length; i ++) {

            char t = 0;
            double randomValue = 0;
            
            // generate character "t" based on desire to use full range of characters
            if (USE_UNICODE) {
            
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

}
