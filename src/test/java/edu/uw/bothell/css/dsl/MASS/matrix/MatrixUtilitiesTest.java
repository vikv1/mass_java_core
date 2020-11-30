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

		// one-dimensional matrix, middle element, index should be 2
		assertEquals( 2, MatrixUtilities.getLinearIndex( new int[]{ 3 }, new int[]{ 2 } ) );
		
		// two-dimensional matrix, middle element, index should be 2
		assertEquals( 2, MatrixUtilities.getLinearIndex( new int[]{ 2, 2 }, new int[]{ 1, 0 } ) );
		
		// three-dimensional matrix, middle element, index should be 6
		assertEquals( 6, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 1, 1, 0 } ) );
		
		// one-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2 }, new int[]{ 2 } ) );

		// two-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2, 2 }, new int[]{ 2, 2 } ) );

		// three-dimensional matrix, element out of bounds, index should be minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 2, 2, 2 } ) );

		// zero size for a dimension should be ignored
		assertEquals( 3, MatrixUtilities.getLinearIndex( new int[]{ 2, 0, 2 }, new int[]{ 1, 0, 1 } ) );
		
		// negative location should return minimum integer value
		assertEquals( Integer.MIN_VALUE, MatrixUtilities.getLinearIndex( new int[]{ 2, 2, 2 }, new int[]{ 1, -1, 1 } ) );
		
	}

	@Test
	public void getIndex() {
		
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

}
