/*

 	MASS Java Software License
	© 2012-2017 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class MASSTest extends AbstractTest {

	@Test
	public void getSetNumThreads() throws Exception {
		
		// by default, only one
		assertEquals( 1, MASS.getNumThreads() );
		
		// change it
		MASS.setNumThreads( 2 );
		assertEquals( 2, MASS.getNumThreads() );
		
		// shouldn't be able to set to zero or less
		MASS.setNumThreads( 0 );
		assertEquals( 2, MASS.getNumThreads() );
		MASS.setNumThreads( -1 );
		assertEquals( 2, MASS.getNumThreads() );
		
		// revert
		MASS.setNumThreads( 1 );
		assertEquals( 1, MASS.getNumThreads() );
		
	}

	@Test
	public void getSetDefaultUsername() throws Exception {
		
		String newUsername = randomString();
		String originalUsername = MASS.getDefaultUsername();
		
		// set new value, and test
		MASS.setDefaultUsername( newUsername );
		assertEquals( newUsername, MASS.getDefaultUsername() );
		
		// revert back
		MASS.setDefaultUsername( originalUsername );
		
	}

	@Test
	public void getSetNodeFilePath() throws Exception {
		
		String newPath = randomString();
		String originalPath = MASS.getNodeFilePath();
		
		// set new value, and test
		MASS.setNodeFilePath( newPath );
		assertEquals( newPath, MASS.getNodeFilePath() );
		
		// revert back
		MASS.setNodeFilePath( originalPath );
		
	}

	@Test
	public void isConsoleLoggingEnabled() throws Exception {
		
		// should NOT be enabled!
		assertFalse( MASS.isConsoleLoggingEnabled() );
		
	}
	
}