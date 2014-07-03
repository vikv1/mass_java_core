/******************************************************************************
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.

  Parallelized with the MASS library, University of Washington Bothell
******************************************************************************/

package FDM;

/**
 * Defines a three-dimensional array. This becomes a basis for MatrixBase
 */
public class Grid extends BasicInfo{

    // q[x + i][y][z]: the value on x-axis at the i-th step of element (x,y,z)
    // q[x][y + i][z]: the value on y-axis at the i-th step of element (x,y,z)
    // q[x][y][z + i]: the value on z-axis at the i-th step of element (x,y,z)
    protected float[][][]       q;              // three dimensional array
    protected int        m_i,m_j,m_k;

    protected int index[];    // MASS cubicle index
    protected int size[];     // MASS array size
    

    /**
     * Is a MASS-specific function that accesses both real and shadow spaces of q[][][].
     */
    public float getQ( int i, int j, int k ) {
	return q[i + 2][j + 2][k + 2];
    }

    public void putQ( int i, int j, int k, float value ) {
	q[i + 2][j + 2][k + 2] = value;
    }

    /**                                                                                    
     * Find the direction (east, west, north, south, top, and bottom) of the               
     * neighbor with a given index, (i.e., int[] src).                                     
     *                       
     * @param src a given neighbor's index.                                                
     * @return east, west, north, south, top, or bottom                                    
     */
    private int dirSrc( int[] src ) {
        if ( src[_x] == index[_x] + 1 )
            return east;
        if ( src[_x] == index[_x] - 1 )
            return west;
        if ( src[_y] == index[_y] + 1 )
            return north;
        if ( src[_y] == index[_y] - 1 )
            return south;
        if ( src[_z] == index[_z] + 1 )
            return top;
        if ( src[_z] == index[_z] - 1 )
            return bottom;
        return -1;
    }

    private float[][][] getB( int xBoundary, int yBoundary, int zBoundary, 
		       int xOffset, int yOffset, int zOffset, boolean print ) {
	float[][][] border = new float[xBoundary][yBoundary][zBoundary];
	for ( int x= 0; x < xBoundary; x++ )
	    for ( int y = 0; y < yBoundary; y++ )
		for ( int z = 0; z < zBoundary; z++ ) {
		    border[x][y][z] = getQ( x + xOffset, y + yOffset, z + zOffset );
		    if ( print )
			System.err.printf( "getB from bottom: cubicle[%d,%d,%d]: element[%d,%d,%d] = %f\n", index[_x], index[_y], index[_z],
					   index[_x] * unit + x + xOffset, index[_y] * unit + y + yOffset, index[_z] * unit + z + zOffset, border[x][y][z] );
		}
	//System.out.printf( "getB from index[%d][%d][%d]: border[%d][%d][%d] = %s\n",
	//	   index[0], index[1], index[2], xBoundary, yBoundary, zBoundary, border );
	return border;
    }

    public float[][][] getBoundary( int[] src ) {

	switch( dirSrc( src ) ) {
	case east: 
	    return getB( 2, unit, unit, unit - 2, 0, 0, false );
	case west: 
	    return getB( 2, unit, unit, 0, 0, 0, false );
	case north:
	    return getB( unit, 2, unit, 0, unit - 2, 0, false );
	case south:
	    return getB( unit, 2, unit, 0, 0, 0, false );
	case top:  
	    return getB( unit, unit, 2, 0, 0, unit - 2, false );
	case bottom:
	    return getB( unit, unit, 2, 0, 0, 0, false );
	}

	return null;
    }

    private void putB( int xBoundary, int yBoundary, int zBoundary, 
		       int xOffset, int yOffset, int zOffset, 
		       float[][][] border, boolean print ) {
	//System.out.printf( "putB to index[%d][%d][%d]: border[%d][%d][%d] = %s\n",
	//		   index[0], index[1], index[2], xBoundary, yBoundary, zBoundary, border );
	if ( border == null )
	    return;
	for ( int x= 0; x < xBoundary; x++ )
	    for ( int y = 0; y < yBoundary; y++ )
		for ( int z = 0; z < zBoundary; z++ ) {
		    putQ( x + xOffset, y + yOffset, z + zOffset, border[x][y][z] );
		    if ( print )
			System.err.printf( "putB to top: cubicle[%d,%d,%d]: element[%d,%d,%d] = %f\n", index[_x], index[_y], index[_z],
					   index[_x] * unit + x + xOffset, index[_y] * unit + y + yOffset, index[_z] * unit + z + zOffset, border[x][y][z] );
		}
    }

    public void putBoundary( Object[] border ) {
	// from east
	putB( 2, unit, unit, unit, 0, 0, ( float[][][] )border[east], false );
	// from west
	putB( 2, unit, unit, -2, 0, 0, ( float[][][] )border[west], false );
	// from north
	putB( unit, 2, unit, 0, unit, 0, ( float[][][] )border[north], false );
	// from south
	putB( unit, 2, unit, 0, -2, 0, ( float[][][] )border[south], false );
	// from top
	putB( unit, unit, 2, 0, 0, unit, ( float[][][] )border[top], false );
	// from bottom
	putB( unit, unit, 2, 0, 0, -2, ( float[][][] )border[bottom], false );
    }

    /**
     * Creates a 3D array of floats, each undefined.
     * Note that mMaxX, mMaxY, and mMaxZ must be set by 
     * BasicInfo.setBasicInfo( ) before this constructor will be called.
     *
     * @param index MASS cubilcle index
     * @param size  MASS array size
     */
    public Grid( int[] index, int[] size ) {
	init( index, size, 0.0f );
    }

    /**
     * Creates a 3D array of floats, each defined as value
     *
     * @param index MASS cubilcle index
     * @param size  MASS array size
     * @param value an initial value for all array elements
     */
    public Grid( int[] index, int[] size, float value ) {
	init( index, size, value );
    }
    private void init( int[] index, int[] size, float value ) {
	this.index = index;         // MASS cubicle index
	this.size  = size;          // MASS array size
        q = new float[unit + 4][unit + 4][unit + 4]; // mMaxX,Y,Z + 1 replaced with unit + 4

        for( int k=0; k <= unit - 1; k++ ){  // mMaxZ replaced with unit -1
            for( int j=0; j <= unit - 1; j++ ){
                for( int i=0; i <= unit - 1; i++ ){
		    putQ( i, j, k, value );  // initialize only the real space
                }
            }
        }
    }

    /**
     * Sets the array element specified with i, j, and k.
     * @param i indexes the x axis.
     * @param j indexes the y axis.
     * @param k indexes the z axis.
     */
    public void setPosition( int i, int j, int k ){
        m_i = i;
        m_j = j;
        m_k = k;
    }

    /**
     * Sets the value to the element previously located by setPosition( )
     * @param value is a value to be set to this element.
     */
    public void setValue( double value ){
        putQ( m_i, m_j, m_k, (float)value ); // q[m_i][m_j][m_k] = (float)value;
    }

    /**
     * Retrieve the value of the element specified with i, j, and k.
     * @param i indexes the x axis.
     * @param j indexes the y axis.
     * @param k indexes the z axis.
     * @return the value of the element specified with i, j, and k.
     */
    public double q( int i, int j, int k ){
	//System.out.printf( "q(%d, %d, %d)\n", i, j, k );
        return getQ( i, j, k ); // q[i][j][k];
    }

    /**
     * Retrieve the value of the element previously located by setPosition( )
     * @return the value of the element previously located by setPosition( )
     */
    protected double q(){
        return getQ( m_i, m_j, m_k );  // q[m_i][m_j][m_k];
    }

    /**
     * Retrieves the value at i-th step of the current element in a given
     * (i, j, k) direction.
     *
     * @param iDirection either i, j, or k direction
     * @param iStep      i-th step of the current element
     * @return the value at i-th step of the current element.
     */
    protected double q( int iDirection, int iStep ){
        switch( iDirection ){
	case _x:
	    return getQ( m_i+iStep, m_j, m_k ); // q[m_i+iStep][m_j][m_k];
	case _y:
	    return getQ( m_i, m_j+iStep, m_k ); // q[m_i][m_j+iStep][m_k];
	case _z:
	    return getQ( m_i, m_j, m_k+iStep ); // q[m_i][m_j][m_k+iStep];
        }
        return 9999; // error code
    }

}
