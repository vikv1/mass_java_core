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

	© 2012-2016 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.MassData;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;

/**
 * Perform a series of unit tests against the PlaceData class to verify proper
 * and consistent behavior of the class / methods
 */
public class PlaceDataTest extends AbstractTest {

	// class under test
	private PlaceData placeData = new PlaceData();

	@Test
	public void getSetThisPlaceData() throws Exception {
		
		Integer data = new Integer( randomInt() );
		
		placeData.setThisPlaceData( data );
		
		assertEquals( data, placeData.getThisPlaceData() );
		
	}

	@Test
	public void getSetHasAgents() throws Exception {
	
		boolean hasAgents = placeData.isHasAgents();
		
		// toggle value
		placeData.setHasAgents( !hasAgents );
		
		assertEquals( !hasAgents, placeData.isHasAgents() );
		
	}

	@Test
	public void getSetIndex() throws Exception {
		
		int index = randomInt();
		
		placeData.setIndex( index );
		
		assertEquals( index, placeData.getIndex() );
		
	}

	@Test
	public void getSetAgentDataOnThisPlace() throws Exception {
		
		AgentData[] agentData = new AgentData[ 0 ];
		
		placeData.setAgentDataOnThisPlace( agentData );
		
		assertArrayEquals( agentData, placeData.getAgentDataOnThisPlace() );
		
	}

	@Test
	public void toJSONString() throws Exception {
		
		AgentData[] agentData = new AgentData[]{ new AgentData(), new AgentData() };
		
		placeData.setAgentDataOnThisPlace( agentData );
		
		// TODO - is there a useful test of the actual contents that can be performed?
		assertNotNull( placeData.toJSONString() );
		
	}

}
