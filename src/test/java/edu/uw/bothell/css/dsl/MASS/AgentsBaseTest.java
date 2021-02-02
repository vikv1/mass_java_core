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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Perform a series of unit tests against the AgentsBase class to verify proper
 * and consistent behavior of the class / methods
 */
public class AgentsBaseTest extends AbstractTest {

	// class under test
	private AgentsBase agentsBase;

	private static PlacesBase placesBase;
	private static final int PLACES_HANDLE = randomInt();
	private static final int AGENTS_HANDLE = randomInt();
	private Object originalMThreadLock;
	
	@BeforeAll
	public static void beforeAll() {

		// force MASSBase to contain a single node
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		MASSBase.addNode( masterNode );
		MASSBase.initMASSBase( masterNode );

		int[] placesMatrix = new int[]{ 1, 1, 1 };
		placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		MASSBase.setCurrentPlacesBase( placesBase );

	}

	@AfterAll
	public static void afterAll() {
		
		// clean up MASSBase
		resetMASSBase();
		
	}

	@BeforeEach
	public void onSetUp() {
		
		// MASSBase should be made ready for use before tests are run
		if ( MASSBase.getSystemSize() == 0 ) {
			MNode masterNode = new MNode();
			masterNode.setHostName( randomString() );
			masterNode.setMaster( true );
			MASSBase.addNode( masterNode );
			MASSBase.initMASSBase( masterNode );
		}

		// start with new instances for each test
		agentsBase = new AgentsBase( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, PLACES_HANDLE, 1 );

		// remember initial MThread parameters for reset later
		originalMThreadLock = MThread.getLock();

	}

	@AfterEach
	public void tearDown() {

		// reset MThread back to original state
		MThread.setLock( originalMThreadLock );

	}

	@Test
	public void getClassName() {
		
		assertEquals( SimpleTestAgent.class.getName(), agentsBase.getClassName() );
		
	}

	@Test
	public void getLocalPopulation() {
		
		// TODO - two methods to return the same thing?
		
		// Only one place, so should be only one Agent present on this node
		assertEquals( 1, agentsBase.getLocalPopulation() );
		assertEquals( 1, agentsBase.nLocalAgents() );
		
	}
	
	@Test
	public void getPlacesHandle() {
		
		assertEquals( PLACES_HANDLE, agentsBase.getPlacesHandle() );
		
	}
	
	@Test
	public void getInitPopulation() {
		
		// Only one place, so should have been only one Agent present on this node
		assertEquals( 1, agentsBase.getInitPopulation() );
		
	}
	
	@Test
	public void getHandle() {
		
		assertEquals( AGENTS_HANDLE, agentsBase.getHandle() );
		
	}
	
	@Test
	public void getAgents() {
		
		// not testing AgentList here - only that it can be retrieved from AgentsBase
		assertNotNull( agentsBase.getAgents() );
		
	}
	
	@Test
	@Disabled
	public void manageAllLockRelease() {

		// must provide something for MThread to lock against
		String lockObj = new String();
		MThread.setLock( lockObj );
		
		// should not result in an Exception (locks should release!)
		agentsBase.manageAll( 0 );
		
	}

	@Test
	@Disabled
	public void manageAllSingleLiveAgent() {

		// must provide something for MThread to lock against
		String lockObj = new String();
		MThread.setLock( lockObj );
		
		// need an Agent to "manage"
		Agent agent = new SimpleTestAgent( new String() );
		MThread.setAgentBagSize( 1 );
		agentsBase.getAgents().add( agent );
		
		// make the call (should not throw an Exception)
		agentsBase.manageAll( 0 );
		
		// Agent bag size should not be zero in MThread
		assertEquals( 0, MThread.getAgentBagSize() );

		// remove test agent
		agentsBase.getAgents().clear();
		
	}

}