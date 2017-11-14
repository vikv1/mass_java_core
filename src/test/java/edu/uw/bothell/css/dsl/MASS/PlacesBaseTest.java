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
import static org.junit.Assert.assertNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

public class PlacesBaseTest extends AbstractTest {

	// class under test
	private PlacesBase placesBase;
	
	// the matrix of Places
	private int[] placesMatrix = new int[]{ 10, 10, 10 };
	
	// the handle ID for the test PlacesBase
	private int handle = randomInt();
	
	@BeforeClass
	public static void beforeAll() {
		
		// MASSBase should be made ready for use before any test runs
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		MASSBase.addNode( masterNode );
		MASSBase.initMASSBase( masterNode );
		
	}

	@AfterClass
	public static void afterAll() {
		
		// clean up MASSBase
		MASSBase.getAllNodes().clear();
		MASSBase.setCurrentArgument( null );
		MASSBase.setCurrentFunctionId( 0 );
		MASSBase.setCurrentMsgType( null );
		MASSBase.setCurrentAgentsBase( null );
		MASSBase.setCurrentAgentsBase( null );
		MASSBase.setCurrentReturns( null );
		MASSBase.setDestinationPlaces( null );
		MASSBase.getPlacesMap().clear();
		MASSBase.getRemoteNodes().clear();
		MASSBase.getHosts().clear();
		MASSBase.getLogger().setLogLevel(LogLevel.OFF);
		
	}

	@Before
	public void onSetUp() {
		
		// start with a new instance for each test
		placesBase = new PlacesBase( handle, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		
	}
	
	@Test
	public void getSize() throws Exception {
		
		// size should be the same as the init array
		assertEquals( placesMatrix, placesBase.getSize() );
		
	}
	
	@Test
	public void getPlacesSize() throws Exception {
		
		// should be multiple of all matrix sizes
		assertEquals( 1000, placesBase.getPlacesSize() );
		
	}
	
	@Test
	public void masterNodeNoShadowSpaces() throws Exception {
		
		// default Places base as used in this test class only assigned to master node
		assertNull( placesBase.getLeftShadow() );
		assertNull( placesBase.getRightShadow() );
		
	}

	@Test
	public void getShadowSize() throws Exception {
		
		// one "stripe"
		assertEquals( 100, placesBase.getShadowSize() );

	}
	
	@Test
	public void getClassName() throws Exception {
		
		assertEquals( SimpleTestPlace.class.getName(), placesBase.getClassName() );
		
	}
	
	@Test
	public void getLowerUpperBoundary() throws Exception {
		
		// the test matrix is contained on one node, so limits are the boundaries
		assertEquals( 0, placesBase.getLowerBoundary() );
		assertEquals( 999, placesBase.getUpperBoundary() );
		
	}

	@Test
	public void getRankFromGlobalLinearIndexSingleNode() throws Exception {
		
		// on a single node, all Places are located at rank #0
		for ( int i = 0; i < placesBase.getPlacesSize(); i ++ ) {
			assertEquals( 0, placesBase.getRankFromGlobalLinearIndex( i ) );
		}
		
	}
	
	@Test
	public void getPlaces() throws Exception {
		
		Place[] places = placesBase.getPlaces();
		
		// all Places should be populated for the test case
		for ( int i = 0; i < placesBase.getPlacesSize(); i ++ ) {
			assertNotNull( places[ i ] );
		}
		
	}
	
	@Test
	public void getGlobalLinearIndexFromGlobalArrayIndex() throws Exception {
		
		// one-dimensional matrix, first element, index should be zero
		assertEquals( 0, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 0 }, new int[]{ 1 } ) );
		
		// two-dimensional matrix, first element, index should be zero
		assertEquals( 0, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 0, 0 }, new int[]{ 1, 1 } ) );
		
		// three-dimensional matrix, first element, index should be zero
		assertEquals( 0, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 0, 0, 0 }, new int[]{ 1, 1, 1 } ) );
		
		// one-dimensional matrix, last element, index should be 1
		assertEquals( 1, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1 }, new int[]{ 2 } ) );
		
		// two-dimensional matrix, last element, index should be 3
		assertEquals( 3, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1, 1 }, new int[]{ 2, 2 } ) );
		
		// three-dimensional matrix, last element, index should be 7
		assertEquals( 7, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1, 1, 1 }, new int[]{ 2, 2, 2 } ) );

		// one-dimensional matrix, middle element, index should be 2
		assertEquals( 2, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 2 }, new int[]{ 3 } ) );
		
		// two-dimensional matrix, middle element, index should be 2
		assertEquals( 2, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1, 0 }, new int[]{ 2, 2 } ) );
		
		// three-dimensional matrix, middle element, index should be 6
		assertEquals( 6, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1, 1, 0 }, new int[]{ 2, 2, 2 } ) );
		
		// one-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 2 }, new int[]{ 2 } ) );

		// two-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 2, 2 }, new int[]{ 2, 2 } ) );

		// three-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 2, 2, 2 }, new int[]{ 2, 2, 2 } ) );

		// zero size for a dimension should be ignored
		assertEquals( 3, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1, 0, 1 }, new int[]{ 2, 0, 2 } ) );
		
		// negative location should return minimum integer value
		assertEquals( Integer.MIN_VALUE, placesBase.getGlobalLinearIndexFromGlobalArrayIndex( new int[]{ 1, -1, 1 }, new int[]{ 2, 2, 2 } ) );
		
	}
	
	@Test
	public void getGlobalArrayIndex() throws Exception {
		
		// using "placesMatrix" as the definition of the matrix for this test
		
		// an index at zero in all dimensions is the first linear index
		int[] firstElement = placesBase.getGlobalArrayIndex( 0 );
		assertEquals( 0, firstElement[ 0 ] );
		assertEquals( 0, firstElement[ 1 ] );
		assertEquals( 0, firstElement[ 2 ] );
		
		// an index at maximum in all dimensions is the last liner index
		int[] lastElement = placesBase.getGlobalArrayIndex( 999 );
		assertEquals( 9, lastElement[ 0 ] );
		assertEquals( 9, lastElement[ 1 ] );
		assertEquals( 9, lastElement[ 2 ] );
		
		// maximum index for first dimension
		int[] firstDimension = placesBase.getGlobalArrayIndex( 9 );
		assertEquals( 0, firstDimension[ 0 ] );
		assertEquals( 0, firstDimension[ 1 ] );
		assertEquals( 9, firstDimension[ 2 ] );

		// maximum index for second dimension
		int[] secondDimension = placesBase.getGlobalArrayIndex( 90 );
		assertEquals( 0, secondDimension[ 0 ] );
		assertEquals( 9, secondDimension[ 1 ] );
		assertEquals( 0, secondDimension[ 2 ] );

		// maximum index for third dimension
		int[] thirdDimension = placesBase.getGlobalArrayIndex( 900 );
		assertEquals( 9, thirdDimension[ 0 ] );
		assertEquals( 0, thirdDimension[ 1 ] );
		assertEquals( 0, thirdDimension[ 2 ] );

	}
	
	@Test
	public void checkLinearGlobalSpaceConversion() throws Exception {
	
		// check every linear location index to make sure conversions are equal
		for ( int i = 0; i < 1000; i ++ ) {
			
			// get the array index equivalent of the linear address
			int[] globalIndex = placesBase.getGlobalArrayIndex( i );
			
			// from that array index, get the linear equivalent
			int linearIndex = placesBase.getGlobalLinearIndexFromGlobalArrayIndex( globalIndex, placesMatrix );
			
			// should match
			assertEquals( i, linearIndex );
			
		}
		
	}
		
	@Test
	public void getHandle() throws Exception {
		
		assertEquals( handle, placesBase.getHandle() );
		
	}
	
	
	
}