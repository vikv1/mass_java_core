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

import java.util.Arrays;

/**
 * MatrixUtilities contains helper methods designed to centralize matrix operations
 * @since 1.0.1
 */
public class MatrixUtilities {
	
	/**
	 * Given an array of integers representing dimensions of a matrix, determine the total number of elements
	 * @param size Matrix dimensions and sizes
	 * @return The total number of elements (places) in the matrix
	 */
	public static int getMatrixSize( int[] size ) {
		
		// obvious calculations
		if ( size == null ) return 0;
		if ( size.length == 0 ) return 0;
		
		int placeTotal = 1;

		// iterate through each dimension and multiply
		for ( int x : size ) {
			placeTotal *= x;
		}
		
		return placeTotal;
		
	}
	
	/**
	 * Given matrix dimensions and a position within the matrix, calculate the global index position
	 * based on a single linear representation of all elements
	 * @param size Matrix dimensions
	 * @param index A position within the matrix
	 * @return The index position, as represented in a global, flattened, single-dimensional array, or
	 * minimum integer value if the position could not be calculated (problem in size or index arrays) 
	 */
	public static int getLinearIndex( int[] size, int[] index ) {
	
		// bounds checks
		if ( size == null || index == null ) return Integer.MIN_VALUE;
		if ( size.length != index.length ) return Integer.MIN_VALUE;
		
		// single dimension bounds check
		if ( size.length == 1 && index[ 0 ] > size[ 0 ] - 1 ) return Integer.MIN_VALUE;
		
    	int linearIndex = 0;
    	int columnWeight = 1;

    	// determine last "column" (leftmost) weight (all column sizes except for rightmost)
    	for ( int i = index.length - 1; i > 0; i-- ) {
    		columnWeight *= size[ i ];
    	}
    	
    	// determine position by iterating through each dimension and adding each position within the dimension
    	for ( int i = 0; i < index.length - 1; i ++ ) {

    		// invalid index or size at this position?
    		if ( size[i] <= 0 || index[ i ] < 0 || ( index[i] > size[ i ] - 1 ) ) return Integer.MIN_VALUE;

    		// multiply column weight by column value to get this column contributor to the linear index 
    		linearIndex += columnWeight * index[ i ];
    		
    		// column weight is now ready for the next iteration
    		columnWeight /= size[ i ];

    	}

    	// add the very last (rightmost) column value, column weight is one
    	linearIndex += index[ size.length - 1 ];

    	return linearIndex;

	}
	
    /** 
     * Converts a given global index into a multidimensional index representation
     * @param size Matrix dimensions
     * @param linearIndex An index in a linear single dimension that will be converted into a multidimensional index
     * @return A multidimensional index corresponding to the given linear index position 			   
     */
    public static int[] getIndex( int[] size, int linearIndex ) {

    	int[] index = new int[ size.length ];

    	// calculate starting with "leftmost" dimension
    	for ( int i = size.length - 1; i >= 0  ; i-- ) {
    		
    		index[ i ] = linearIndex % size[ i ];
    		linearIndex /= size[ i ];
    	
    	}

    	return index;
    	
    }

    /**
     * Return the sum of all elements in an integer array
     * @param array The source array
     * @return The total of all integers within the source array
     */
    public static int sumArrayElements( int[] array ) {
    	
    	if ( array == null ) return 0;
    	
    	return Arrays.stream( array ).reduce( 0, ( x, y ) -> x + y );
    
    }
    
	/**
	 * Given an index, referenced to a global index of Places, return the "rank"
	 * or node number where the Place should be located
	 * @param globalLinearIndex The array index for which to obtain the node or rank number
	 * @param size Matrix dimensions
	 * @param systemSize The number of nodes in the cluster
	 * @return The node/rank number associated with that Place
	 */
	public static int getRankFromGlobalLinearIndex( int globalLinearIndex, int[] size, int systemSize ) {

		// short circuit obvious answers for performance
		if ( systemSize == 1 ) return 0;			// system size of one means everything on node 0
		if ( globalLinearIndex == 0 ) return 0;		// first Place always on node 0

		int matrixSize = MatrixUtilities.getMatrixSize( size );

		// calculate where the index is in relation to the total simulation space
		double indexPositionRelative = ( double ) globalLinearIndex / ( double ) matrixSize; 
		
		// linear relationship between index position and number of nodes in the system
		return ( int ) ( systemSize * indexPositionRelative );
   		
    }
	
}