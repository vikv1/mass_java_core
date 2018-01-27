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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Perform a series of unit tests against the Agents class to verify proper
 * and consistent behavior of the class / methods
 */
public class AgentsTest extends AbstractTest {

	private static final int PLACES_HANDLE = randomInt();
	private static final int AGENTS_HANDLE = randomInt();

	// class under test
	private Agents agents;

	private Places places;
	private Object originalMThreadLock;

	@BeforeClass
	public static void beforeAll() {
		
		// MASSBase should be made ready for use before tests are run
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		MASSBase.addNode( masterNode );
		MASSBase.initMASSBase( masterNode );

	}

	@Before
	public void onSetUp() {
		
		// MASSBase should be made ready for use before tests are run
		if ( MASSBase.getSystemSize() == 0 ) {
			MNode masterNode = new MNode();
			masterNode.setHostName( randomString() );
			masterNode.setMaster( true );
			MASSBase.addNode( masterNode );
			MASSBase.initMASSBase( masterNode );
		}

		Places places = new Places( 0, SimpleTestPlace.class.getName(), null, 1, 1, 1 );

		// start with new instances for each test
//		agentsBase = new AgentsBase( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, PLACES_HANDLE, 1 );
		agents = new Agents( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, places, 1 );
		
		// remember initial MThread parameters for reset later
		originalMThreadLock = MThread.getLock();

	}

	@After
	public void tearDown() {

		// reset MThread back to original state
		MThread.setLock( originalMThreadLock );

	}

	@Test
	public void nAgents() throws Exception {
		
		// should only be one agent
		assertEquals( 1, agents.nAgents() );
		
	}
	
	@AfterClass
	public static void afterAll() {
		
		// clean up MASSBase
		resetMASSBase();
		
	}

}
