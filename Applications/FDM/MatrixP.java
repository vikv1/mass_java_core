/******************************************************************************
  
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
******************************************************************************/

package FDM;

public class MatrixP extends MatrixBase {

    protected final double  _A;             // 1 / ( 2 / dx^2 + 2 / dy^y + 2 / dz^z )
    public final static double _MAX_ITERATION = 500; // repetition for P's convergence

    public Grid              Q;          // a 3D velocity array
    
    /**
     * Is the constructor that initializes _A, _MAX_ITERATION (for P's convergence),
     * and creates a three dimensinal velocity array, Q.
     */
    public MatrixP( int[] index, int[] size ) {
	super( index, size );

        // P_ijk = 1 / ( 2 / dx^2 + 2 / dy^y + 2 / dz^z ) * 
        //        (   ( P_(i-1) + P_(i+1) ) / delta_x^2
        //          + ( P_(j-1) + P_(j+1) ) / delta_y^2
	//          + ( P_(k-1) + P_(k-1) ) / delta_z^2 - Q )
        //
	// where _A = 1 / ( 2 / dx^2 + 2 / dy^y + 2 / dz^z )

        _A = 1.0 / ( 2.0*( m_h2Inverse[_x] + m_h2Inverse[_y] + m_h2Inverse[_z] ) );
        Q =     new Grid( index, size );
    }

    /**
     * Calculates a next pressure for each cell.
     *
     * @param V a 3D array of velocities
     */
    public double calc( MatrixV3D V ){
	double error = 0.0;              // for convergence
        double pD=0.0;                   // temporary pressure

	setBoundaryCondition( V );   // calculae effect of walls and air-conditioning
	
	// calculate each cell's pressure
	for( int i=0; i < unit; i++ ){
	    for( int j=0; j < unit; j++ ){
		for( int k=0; k < unit; k++ ){
		    if ( index[_x] == 0 && i == 0 || index[_y] == 0 && j == 0 || index[_z] == 0 && k == 0 ||
			 index[_x] == size[_x]-1 && i >= unit-2 || 
			 index[_y] == size[_y]-1 && j >= unit-2 || 
			 index[_z] == size[_z]-1 && k >= unit-2 )
			continue;
		    setPosition( i,j,k ); Q.setPosition( i,j,k );
		    pD = calc();
		    error+=Math.abs( q()-pD );
		    //System.err.printf( "MatrixP[%d, %d, %d]=%f\n", index[_x] * unit + i, index[_y] * unit + j, 
		    //	       index[_z] * unit + k, pD );
		    setValue( pD );
		}
	    }
	}
	return error;
    }

    /**
     * @return
     * P_ijk = 1 / ( 2 / dx^2 + 2 / dy^y + 2 / dz^z ) * 
     *        (   ( P_(i-1) + P_(i+1) ) / delta_x^2
     *          + ( P_(j-1) + P_(j+1) ) / delta_y^2
     *          + ( P_(k-1) + P_(k-1) ) / delta_z^2 - Q )
     *
     * where 
     * _A = 1 / ( 2 / dx^2 + 2 / dy^y + 2 / dz^z )
     * ppDifference(_x) = ( P_(i-1) + P_(i+1) ) / delta_x^2
     * ppDifference(_y) = ( P_(i-1) + P_(i+1) ) / delta_y^2
     * ppDifference(_z) = ( P_(i-1) + P_(i+1) ) / delta_z^2
     * Q = Q.q( )
     */
    private double calc(){


        return _A * ( ppDifference(_x) + ppDifference(_y) + ppDifference(_z) - Q.q() );
    }

    /**
     * @param iDirection x, y, or z
     * @return ( P_(i-1) + P_(i+1) ) / delta_(x, y, or z)^2
     */
    private double ppDifference( int iDirection ){
        return ( q( iDirection, +1 ) + q( iDirection, -1 ) ) * m_h2Inverse[ iDirection ];
    }

    /**
     * Calculates a next velocity from V for all cells
     * @param V the current velocity (u, v, w)
     */
    public void resetQ( MatrixV3D V ){
	for( int i=0; i < unit; i++ ){
	    for( int j=0; j < unit; j++ ){
		for( int k=0; k < unit; k++ ){
		    if ( index[_x] == 0 && i == 0 || index[_y] == 0 && j == 0 || index[_z] == 0 && k == 0 ||
			 index[_x] == size[_x]-1 && i >= unit-2 || 
			 index[_y] == size[_y]-1 && j >= unit-2 || 
			 index[_z] == size[_z]-1 && k >= unit-2 )
			continue;
		    // System.out.printf( "MatrixP.resetQ: setPosition(%d, %d, %d)\n", i, j, k ); 
                    Q.setPosition( i,j,k ); V.setPosition( i,j,k );
                    Q.setValue( calcQ( V ) );
                }
            }
        }
    }

    /**
     * Calculate a next velocity for each cell.
     */
    private double calcQ( MatrixV3D V ){
        return ( (  V.v[_u].forwardDifference(_x)
                    +V.v[_v].forwardDifference(_y)
                    +V.v[_w].forwardDifference(_z) ) * m_dtInverse
		 - (  Math.pow( V.v[_u].forwardDifference(_x), 2.0 )
		      +Math.pow( V.v[_v].forwardDifference(_y), 2.0 )
		      +Math.pow( V.v[_w].forwardDifference(_z), 2.0 )
		      + ( V.vRepForwardDifference(_x) * V.uRepForwardDifference(_y)
			  +V.wRepForwardDifference(_x) * V.uRepForwardDifference(_z)
			  +V.wRepForwardDifference(_y) * V.vRepForwardDifference(_z) ) * 2.0 ) );
    }
    
    private void setBoundaryCondition( MatrixV3D V ){
        //x section
        for( int j=0; j < unit; j++ ){
            for( int k=0; k < unit; k++ ){
                //max
		if ( index[_x] == size[_x] - 1 )
		    putQ(unit-1, j, k,  (float)0.0 );
                //max-1
		if ( index[_x] == size[_x] - 1 )
		    putQ(unit-2, j, k, (float)(q(unit-3, j, k) - mReInverse*2.0*m_hInverse[_x] * V.v[_u].q(unit-3, j, k)) );
                //zero
		if ( index[_x] == 0 )
		    putQ(   0   , j, k, (float)(q(   1   , j, k) - mReInverse*2.0*m_hInverse[_x] * V.v[_u].q(   2   , j, k) ) );
            }
        }
        //case 1
        //air conditioning IN
	for ( int j = 0; j < unit; j++ )
	    if ( index[_y] * unit + j >=2*mMaxY/6 && index[_y] * unit + j <=3*mMaxY/6 &&
		 index[_x] == size[_x] - 1 && index[_z] == size[_z] - 1 ) {
		//max-1
		putQ(unit-2, j, unit-4,  (float)q(unit-3, j, unit-4));
		putQ(unit-2, j, unit-5,  (float)q(unit-3, j, unit-5));
		putQ(unit-2, j, unit-6,  (float)q(unit-3, j, unit-6));
        }
        
        //y section
        for( int i=0; i < unit; i++ ){
            for( int k=0; k < unit; k++ ){
                //max
		if ( index[_y] == size[_y] - 1 )
		    putQ(i, unit-1, k,  (float)0.0 );
                //max-1
		if ( index[_y] == size[_y] - 1 )
		    putQ(i, unit-2, k,  (float)(q(i, unit-3, k) - mReInverse*2.0*m_hInverse[_y] * V.v[_v].q(i, unit-3, k)) );
                //zero
		if ( index[_y] == 0 )
		    putQ(i,    0   , k,  (float)(q(i,    1   , k) - mReInverse*2.0*m_hInverse[_y] * V.v[_v].q(i,    2   , k)) );
            }
        }
        //z section
        for( int i=0; i < unit; i++ ){
            for( int j=0; j < unit; j++ ){
                //max
		if ( index[_z] == size[_z] - 1 )
		    putQ(i, j, unit-1,  (float)0.0 );
                //max-1
		if ( index[_z] == size[_z] - 1 )
		    putQ(i, j, unit-2,  (float)(q(i, j, unit-3) - mReInverse*2.0*m_hInverse[_z] * V.v[_w].q(i, j, unit-3)) );
                //zero
		if ( index[_z] == 0 )
		    putQ(i, j,    0   ,  (float)(q(i, j,    1   ) - mReInverse*2.0*m_hInverse[_z] * V.v[_w].q(i, j,    2   ) ) );
            }
        }
    }
}
