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

package edu.uw.bothell.css.dsl.MASS.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;

/**
 * Perform a series of unit tests against the MatrixUtilities class to verify proper
 * and consistent behavior of the class / methods
 */
public class MatrixUtilitiesTest extends AbstractTest {

	@Test
	public void getMatrixSizeBadArrays() {
		
		// should not result in an Exception
		assertEquals( 0, MatrixUtilities.getMatrixSize( null ) );
		assertEquals( 0, MatrixUtilities.getMatrixSize( new int[ 0 ] ) );
		
	}
	
	@Test
	public void getMatrixSize() {
		
		// single dimension
		assertEquals( 1, MatrixUtilities.getMatrixSize( new int[]{ 1 } ) );
		
		// multiple dimensions
		assertEquals( 1, MatrixUtilities.getMatrixSize( new int[]{ 1, 1, 1 } ) );
		assertEquals( 27, MatrixUtilities.getMatrixSize( new int[]{ 3, 3, 3 } ) );
		
	}
	
	@Test
	public void getLinearIndexBadArrays() {
		
		// mismatched sizes or null arrays should return min integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( null, null ) );
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( null, new int[ 0 ] ) );
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[ 0 ], null ) );
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[ 1 ], new int[ 2 ] ) );
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[ 2 ], new int[ 1 ] ) );
		
	}

	@Test
	public void getLinearIndex() {
		
		// one-dimensional matrix, first element, index should be zero
		assertEquals( 0, MatrixUtilities.getLinearIndex( new int[]{ 1 }, new int[]{ 0 } ) );
		
		// two-dimensional matrix, first element, index should be zero
		assertEquals( 0, MatrixUtilities.getLinearIndex( new int[]{ 1, 1 }, new int[]{ 0, 0 } ) );
		
		// three-dimensional matrix, first element, index should be zero
		assertEquals( 0, MatrixUtilities.getLinearIndex( new int[]{ 1, 1, 1 }, new int[]{ 0, 0, 0 } ) );
		
		// one-dimensional matrix, last element, index should be 1
		assertEquals( 1, MatrixUtilities.getLinearIndex( new int[]{ 2 }, new int[]{ 1 } ) );
		
		// two-dimensional matrix, last element, index should be 3
		assertEquals( 3, MatrixUtilities.getLinearIndex( new int[]{ 2, 2 }, new int[]{ 1, 1 } ) );
		
		// three-dimensional matrix, last element, index should be 7
		assertEquals( 7, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 1, 1, 1 } ) );

		// one-dimensional matrix, last element, index should be 2
		assertEquals( 2, MatrixUtilities.getLinearIndex( new int[]{ 3 }, new int[]{ 2 } ) );
		
		// two-dimensional matrix, second element, index should be 1
		assertEquals( 1, MatrixUtilities.getLinearIndex( new int[]{ 2, 2 }, new int[]{ 0, 1 } ) );
		
		// three-dimensional matrix, seventh element, index should be 6
		assertEquals( 6, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 1, 1, 0 } ) );

	}
	
	@Test
	public void getIndexBoundsChecking() {

		// one-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2 }, new int[]{ 2 } ) );

		// two-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2, 2 }, new int[]{ 2, 2 } ) );

		// three-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 2, 2, 2 } ) );

		// negative location should return minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 1, -1, 1 } ) );
		
	}

	@Test
	public void getIndexExtremes() {
		
		// an index at zero in all dimensions is the first linear index
		int[] firstElement = MatrixUtilities.getIndex( new int[]{ 10, 10, 10 }, 0 );
		assertEquals( 0, firstElement[ 0 ] );
		assertEquals( 0, firstElement[ 1 ] );
		assertEquals( 0, firstElement[ 2 ] );
		
		// an index at maximum in all dimensions is the last liner index
		int[] lastElement = MatrixUtilities.getIndex( new int[]{ 10, 10, 10 }, 999 );
		assertEquals( 9, lastElement[ 0 ] );
		assertEquals( 9, lastElement[ 1 ] );
		assertEquals( 9, lastElement[ 2 ] );
	
	}

	@Test
	public void getIndexColumnMax() {

		// maximum index for first dimension
		int[] firstDimension = MatrixUtilities.getIndex( new int[]{ 10, 10, 10 }, 9 );
		assertEquals( 0, firstDimension[ 0 ] );
		assertEquals( 0, firstDimension[ 1 ] );
		assertEquals( 9, firstDimension[ 2 ] );

		// maximum index for second dimension
		int[] secondDimension = MatrixUtilities.getIndex( new int[]{ 10, 10, 10 }, 90 );
		assertEquals( 0, secondDimension[ 0 ] );
		assertEquals( 9, secondDimension[ 1 ] );
		assertEquals( 0, secondDimension[ 2 ] );

		// maximum index for third dimension
		int[] thirdDimension = MatrixUtilities.getIndex( new int[]{ 10, 10, 10 }, 900 );
		assertEquals( 9, thirdDimension[ 0 ] );
		assertEquals( 0, thirdDimension[ 1 ] );
		assertEquals( 0, thirdDimension[ 2 ] );

	}

	@Test
	public void getIndexTwoDimensionDetailedTest() {

		// regression tests for a bug between getIndex and getLinearIndex that Prof. Fukuda observed on 3/2021
		// for size [ 2, 2 ], index 0 should be [ 0, 0 ]
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 0 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 0 )[ 1 ] );
		
		// for size [ 2, 2 ], index 1 should be [ 0, 1 ]
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 1 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 1 )[ 1 ] );
		
		// for size [ 2, 2 ], index 2 should be [ 1, 0 ]
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 2 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 2 )[ 1 ] );

		// for size [ 2, 2 ], index 3 should be [ 1, 1 ]
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 3 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 2, 2 }, 3 )[ 1 ] );

	}

	@Test
	public void getIndexTwoDimensionOddSizesDetailedTest() {

		// another set of regression tests for matrices with differing number of elements in each dimension
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 0 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 0 )[ 1 ] );

		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 1 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 1 )[ 1 ] );
		
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 2 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 2 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 3 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 3 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 4 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 4 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 5 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 5 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 6 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 6 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 7 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 7 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 8 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 8 )[ 1 ] );

		assertEquals( 3, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 9 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 9 )[ 1 ] );

		assertEquals( 3, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 10 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 10 )[ 1 ] );

		assertEquals( 3, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 11 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 11 )[ 1 ] );

		assertEquals( 4, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 12 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 12 )[ 1 ] );

		assertEquals( 4, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 13 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 13 )[ 1 ] );

		assertEquals( 4, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 14 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 5, 3 }, 14 )[ 1 ] );

	}

	@Test
	public void getIndexTwoDimensionOddSizesReverseOrderDetailedTest() {

		// another set of regression tests for matrices with differing number of elements in each dimension
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 0 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 0 )[ 1 ] );

		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 1 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 1 )[ 1 ] );
		
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 2 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 2 )[ 1 ] );

		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 3 )[ 0 ] );
		assertEquals( 3, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 3 )[ 1 ] );

		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 4 )[ 0 ] );
		assertEquals( 4, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 4 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 5 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 5 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 6 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 6 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 7 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 7 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 8 )[ 0 ] );
		assertEquals( 3, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 8 )[ 1 ] );

		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 9 )[ 0 ] );
		assertEquals( 4, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 9 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 10 )[ 0 ] );
		assertEquals( 0, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 10 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 11 )[ 0 ] );
		assertEquals( 1, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 11 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 12 )[ 0 ] );
		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 12 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 13 )[ 0 ] );
		assertEquals( 3, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 13 )[ 1 ] );

		assertEquals( 2, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 14 )[ 0 ] );
		assertEquals( 4, MatrixUtilities.getIndex( new int[]{ 3, 5 }, 14 )[ 1 ] );

	}

	@Test
	public void checkLinearSpaceConversion() {
	
		int[] matrix = new int[]{ 10, 10, 10 };
		
		// check every linear location index to make sure conversions are equal
		for ( int i = 0; i < 1000; i ++ ) {
			
			// get the array index equivalent of the linear address
			int[] globalIndex = MatrixUtilities.getIndex( matrix,  i );
			
			// from that array index, get the linear equivalent
			int linearIndex = MatrixUtilities.getLinearIndex( matrix, globalIndex );
			
			// should match
			assertEquals( i, linearIndex );
			
		}
		
	}

	@Test
	public void sumArrayElements() {
		
		// should not result in an Exception
		assertEquals( 0, MatrixUtilities.sumArrayElements( null ) );
		
		// simple additive operations
		assertEquals( 3, MatrixUtilities.sumArrayElements( new int[]{ 3 } ) );
		assertEquals( 3, MatrixUtilities.sumArrayElements( new int[]{ 1, 1, 1 } ) );
		
	}
	
	@Test
	public void getRankFromGlobalLinearIndexSingleNode() {

		// test matrix of 27 places
		int matrix[] = new int[]{ 3, 3, 3 };
		
		// all Places should be located on the master node for this test configuration
		for ( int i = 0; i < 27; i ++ ) {
			assertEquals( 0, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 1 ) );
		}
		
	}

	@Test
	public void getRankFromGlobalLinearIndexMultipleNodes() {

		// test matrix of 27 places
		int matrix[] = new int[]{ 3, 3, 3 };
		
		// indexes 0-8 should be on master node
		for ( int i = 0; i < 9; i ++ ) {
			assertEquals( 0, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 3 ) );
		}
		
		// indexes 9-17 should be on second node
		for ( int i = 9; i < 18; i ++ ) {
			assertEquals( 1, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 3 ) );
		}
		
		// indexes 18-26 should be on third node
		for ( int i = 18; i < 27; i ++ ) {
			assertEquals( 2, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 3 ) );
		}
		
	}

	@Test
	public void getRankFromGlobalLinearIndexMultipleNodesOddPlaceSize() {

		// test matrix of 23 places, to be "distributed" across 4 nodes (all nodes except the last should receive 6 places)
		int matrix[] = new int[]{ 23 };
		
		// indexes 0-5 should be on master node (6 Places)
		for ( int i = 0; i < 6; i ++ ) {
			assertEquals( 0, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 4 ) );
		}
		
		// indexes 6-11 should be on second node (6 Places)
		for ( int i = 6; i < 12; i ++ ) {
			assertEquals( 1, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 4 ) );
		}
		
		// indexes 12-17 should be on third node (6 Places)
		for ( int i = 12; i < 18; i ++ ) {
			assertEquals( 2, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 4 ) );
		}

		// indexes 18-23 should be on fourth node (5 Places)
		for ( int i = 18; i < 23; i ++ ) {
			assertEquals( 3, MatrixUtilities.getRankFromGlobalLinearIndex( i, matrix, 4 ) );
		}

	}

}
