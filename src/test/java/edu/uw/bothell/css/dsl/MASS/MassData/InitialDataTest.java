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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;

/**
 * Perform a series of unit tests against the InitialData class to verify proper
 * and consistent behavior of the class / methods
 */
public class InitialDataTest extends AbstractTest {

	// class under test
	private InitialData initialData = new InitialData();
	
	@Test
	public void getSetAgentsName() throws Exception {
	
		String name = randomString();
		
		initialData.setAgentsName( name );
		
		assertEquals( name, initialData.getAgentsName() );
		
	}
	
	@Test
	public void toJSONString() throws Exception {
		
		// TODO - is there a useful test of the actual contents that can be performed?
		assertNotNull( initialData.toJSONString() );
		
	}
	
	@Test
	public void getSetPlacesX() throws Exception {
		
		int x = randomInt();
		
		initialData.setPlacesX( x );
		
		assertEquals( x, initialData.getPlacesX() );
		
	}
	
	@Test
	public void getSetPlacesY() throws Exception {
		
		int y = randomInt();
		
		initialData.setPlacesY( y );
		
		assertEquals( y, initialData.getPlacesY() );
		
	}
	
	@Test
	public void getSetNumberOfPlaces() throws Exception {
		
		int numPlaces = randomInt();
		
		initialData.setNumberOfPlaces( numPlaces );
		
		assertEquals( numPlaces, initialData.getNumberOfPlaces() );
		
	}
	
	@Test
	public void getSetNumberOfAgents() throws Exception {
		
		int numAgents = randomInt();
		
		initialData.setNumberOfAgents( numAgents );
		
		assertEquals( numAgents, initialData.getNumberOfAgents() );
		
	}

	@Test
	public void getSetPlacesName() throws Exception {
	
		String name = randomString();
		
		initialData.setPlacesName( name );
		
		assertEquals( name, initialData.getPlacesName() );
		
	}

	@Test
	public void placeOverloadsSetDebugData() throws Exception {
		
		boolean currentValue = initialData.placeOverloadsSetDebugData();

		// toggle the current value
		initialData.placeOverloadsSetDebugData( !currentValue );
		
		// check for the change
		assertEquals( !currentValue, initialData.placeOverloadsSetDebugData() );
		
	}

	@Test
	public void placeOverloadsGetDebugData() throws Exception {
		
		boolean currentValue = initialData.placeOverloadsGetDebugData();

		// toggle the current value
		initialData.placeOverloadsGetDebugData( !currentValue );
		
		// check for the change
		assertEquals( !currentValue, initialData.placeOverloadsGetDebugData() );
		
	}

	@Test
	public void agentOverloadsSetDebugData() throws Exception {
		
		boolean currentValue = initialData.agentOverloadsSetDebugData();

		// toggle the current value
		initialData.agentOverloadsSetDebugData( !currentValue );
		
		// check for the change
		assertEquals( !currentValue, initialData.agentOverloadsSetDebugData() );
		
	}

	@Test
	public void agentOverloadsGetDebugData() throws Exception {
		
		boolean currentValue = initialData.agentOverloadsGetDebugData();

		// toggle the current value
		initialData.agentOverloadsGetDebugData( !currentValue );
		
		// check for the change
		assertEquals( !currentValue, initialData.agentOverloadsGetDebugData() );
		
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void getSetAgentDataType() throws Exception {
	
		@SuppressWarnings("rawtypes")
		Class number = Integer.class; 
		
		initialData.setAgentDataType( number );
		
		assertEquals( number, initialData.getAgentDataType() );
		
	}

	@Test
	@SuppressWarnings("unchecked")
	public void getSetPlaceDataType() throws Exception {
	
		@SuppressWarnings("rawtypes")
		Class number = Integer.class; 
		
		initialData.setPlaceDataType( number );
		
		assertEquals( number, initialData.getPlaceDataType() );
		
	}
	
	@Test
	public void JSONConstructorDefaults() throws Exception {
		
		// create a defaults InitialData
		InitialData originalObj = new InitialData();

		// get the JSON representation
		String originalObjJSON = originalObj.toJSONString();
		
		// create a new InitialData for population from JSON
		InitialData newObj = new InitialData( originalObjJSON );
		
		// check all fields for correct default values
		assertEquals( "", newObj.getAgentsName() );
		assertEquals( "", newObj.getPlacesName() );
		assertEquals( 0, newObj.getPlacesX() );
		assertEquals( 0, newObj.getPlacesY() );
		assertEquals( 0, newObj.getNumberOfAgents() );
		assertEquals( 0, newObj.getNumberOfPlaces() );
		assertFalse( newObj.agentOverloadsGetDebugData() );
		assertFalse( newObj.agentOverloadsSetDebugData() );
		assertFalse( newObj.placeOverloadsGetDebugData() );
		assertFalse( newObj.placeOverloadsSetDebugData() );
		assertNull( newObj.getAgentDataType() );
		assertNull( newObj.getPlaceDataType() );
			
	}

}
