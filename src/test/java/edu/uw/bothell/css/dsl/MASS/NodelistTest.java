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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import edu.uw.bothell.css.dsl.test.IntegrationTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Perform a series of unit tests against the Nodelist class to verify proper
 * and consistent behavior of the class / methods
 */
@Category(IntegrationTest.class)
public class NodelistTest extends AbstractTest {

	// class under test
	Nodelist nodelist = new Nodelist();
	
	@Test
	public void getSetNodes() throws Exception {
		
		// node collection should not start off NULL
		assertNotNull( nodelist.getNodes() );

		// should be able to replace it
		List<MNode> nodes = new ArrayList<>();
		
		nodelist.setNodes( nodes );
		assertEquals( nodes, nodelist.getNodes() );
		
	}

	@Test
	public void importNodesList() throws Exception {
		MASS.init();
	}
	
}