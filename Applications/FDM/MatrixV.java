/******************************************************************************
  
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
******************************************************************************/

package FDM;

/**
 * Defines Matrices u, v, and w 
 */
public class MatrixV extends MatrixBase {

    private final int           mComponent; // _u = 0, _v = 1, _w = 2
    
    /**
     * Is the constructor that creates a matrix of u, v, or w
     * @param component either _u (0), _v (1), or _w (2)
     */
    public MatrixV( int[] index, int[] size, int component ){
	super( index, size );
        mComponent = component;
    }

    /**
     * Computes uu, uv, uw, vu, vv, vw, wu, wv, and ww direction's velocity at
     * 0-th step.
     */
    protected double represent( int currentComponent ){
        return represent( currentComponent, 0 );
    }

    /**
     * Computes uu, uv, uw, vu, vv, vw, wu, wv, and ww direction's velocity at
     * a given step.
     */
    protected double represent( int currentComponent, int step ){
	double rep = 9999;
        switch( mComponent ){
	case _u:
	    switch( currentComponent ){
	    case _u: // uu direction
		rep = q(m_i+step, m_j, m_k);
		break;
	    case _v: // uv direction
		rep = (q(m_i+step, m_j, m_k)   + q(m_i+step+1, m_j, m_k)
			+ q(m_i+step, m_j-1, m_k) + q(m_i+step+1, m_j-1, m_k)) 
		    * 0.25;
		break;
	    case _w: // uw direction
		rep = (q(m_i+step, m_j, m_k)   + q(m_i+step+1, m_j, m_k)
			+ q(m_i+step, m_j, m_k-1) + q(m_i+step+1, m_j, m_k-1))
		    * 0.25;
		break;
	    case _cellCenter: // center 
		rep = ( q(m_i+step, m_j, m_k) + q(m_i+step+1, m_j, m_k) ) 
		    * 0.5;
		break;
	    }
	    break;
	case _v:
	    switch( currentComponent ){
	    case _u:
		rep = (q(m_i, m_j+step, m_k) + q(m_i, m_j+step+1, m_k)
			+ q(m_i-1, m_j+step, m_k) + q(m_i-1, m_j+step+1, m_k))
		    * 0.25;
		break;
	    case _v:
		rep = q(m_i, m_j+step, m_k);
		break;
	    case _w:
		rep = (q(m_i, m_j+step, m_k)   + q(m_i, m_j+step+1, m_k)
			+ q(m_i, m_j+step, m_k-1) + q(m_i, m_j+step+1, m_k-1))
		    * 0.25;
		break;
	    case _cellCenter:
		rep = ( q(m_i, m_j+step, m_k) + q(m_i, m_j+step+1, m_k) ) 
		    * 0.5;
		break;
	    }
	    break;
	case _w:
	    switch( currentComponent ){
	    case _u:
		rep = (q(m_i, m_j, m_k+step) + q(m_i, m_j, m_k+step+1)
			+ q(m_i-1, m_j, m_k+step) + q(m_i-1, m_j, m_k+step+1))
		    * 0.25;
		break;
	    case _v:
		rep = (q(m_i, m_j, m_k+step)   + q(m_i, m_j, m_k+step+1)
			+ q(m_i, m_j-1, m_k+step) + q(m_i, m_j-1, m_k+step+1))
		    * 0.25;
		break;
	    case _w:
		rep = q(m_i, m_j, m_k+step);
		break;
	    case _cellCenter:
		rep = (q(m_i, m_j, m_k+step) + q(m_i, m_j, m_k+step+1) ) 
		    * 0.5;
		break;
	    }
	    break;
        }
	return rep;
        // return 9999;
    }

    protected void setBoundaryCondition(){
        switch( mComponent ){
	case _u:
	    //x section
	    for( int j=0; j < unit; j++ ){
		for( int k=0; k < unit; k++ ){
		    //max:   beyond the right wall = the front of the right wall
		    if ( index[_x] == size[_x] - 1 )
			 putQ( unit - 1, j, k,  getQ( unit - 3, j, k ) );
		    //max-1: right wall
		    if ( index[_x] == size[_x] - 1 )
			 putQ( unit - 2, j, k,  0.0f);
		    //zero:  beyond the left wall = the fronst of the left wall
		    if ( index[_x] == 0 )
			putQ(   0   , j, k, getQ(   2   , j, k) );
		    //one:   right wall
		    if ( index[_x] == 0 )
			putQ(   1   , j, k,    0.0f );
		}
	    }
	    
	    if(mCaseNo==2){
		//air conditioning IN
		for ( int j = 0; j < unit; j++ )
		    if ( index[_y] * unit + j >= 2*mMaxY/6 && index[_y] * unit + j <= 3*mMaxY/6 &&
			 index[_x] == size[_x] - 1 && index[_z] == size[_z] - 1 ) {
			//max-1:   from upper right wall
			putQ(unit-1 , j, unit-4,  0.5f);
			putQ(unit-2, j, unit-4,  0.5f);
			putQ(unit-1 , j, unit-5,  0.5f);
			putQ(unit-2, j, unit-5,  0.5f);
		    }
		
		//air conditioning OUT
		for ( int j = 0; j < unit; j++ )
		    if ( index[_y] * unit + j >= 2*mMaxY/6 && index[_y] * unit + j <= 3*mMaxY/6 &&
			 index[_x] == size[_x] - 1 && index[_z] == size[_z] - 1 ) {
			//max-1:   to upper right wall but slighter lower than IN
			putQ(unit-1, j, unit-6,  -1.0f);
			putQ(unit-2, j, unit-6,  -1.0f);
		    }
	    }
	    if(mCaseNo==4){
		//air conditioning OUT
		if ( index[_x] == size[_x] - 1 && index[_y] == 0 && index[_z] == size[_z] - 1 ) {
		    for( int j=1; j <= 3; j++ ){
			for( int k=unit-5; k <= unit-3; k++ ){
			    putQ(unit-1, j, k, -1.0f);
			    putQ(unit-2, j, k, -1.0f);
			}
		    }
		}
	    }
	    //y section
	    for( int k=0; k < unit; k++ ){
		for( int i=0; i < unit; i++ ){
		    //max: the corner of the right wall 
		    if ( index[_y] == size[_y] - 1 )
			putQ(i, unit - 1, k, 0.0f);
		    //max-1: parallel to the right wall 
		    if ( index[_y] == size[_y] - 1 )
			putQ(i, unit-2, k, -getQ(i, unit-3, k ) );
		    //zero:  parallel to the left wall
		    if ( index[_y] == 0 )
			putQ(i,   0,   k, -getQ(i,   1   , k));
		}
	    }

	    //z section
	    for( int i=0; i < unit; i++ ){
		for( int j=0; j < unit; j++ ){
		    //max:   the corner of the right wall
		    if ( index[_z] == size[_z] - 1 )
			putQ(i, j, unit - 1, 0.0f);
		    //max-1: parallel to the right wall
		    if ( index[_z] == size[_z] - 1 )
			putQ(i, j, unit - 2, -getQ(i, j, unit-3) );
		    if(mCaseNo==1){
			if ( index[_z] == size[_z] - 1 )
			    putQ(i, j, unit - 2, 1.0f);
		    }
		    //zero:  parallel to the left wall
		    if ( index[_z] == 0 )
			putQ(i, j,   0,  -getQ(i, j,   1   ) );
		}
	    }
	    break;
	case _v:
	    //x section
	    for( int j=0; j < unit; j++ ){
		for( int k=0; k < unit; k++ ){
		    //max
		    if ( index[_x] == size[_x] - 1 )
			putQ(unit - 1  , j, k,      0.0f);
		    //max-1
		    if ( index[_x] == size[_x] - 1 )
			putQ(unit-2, j, k,    -getQ(unit-3, j, k) );
		    //zero
		    if ( index[_x] == 0 )
			putQ(   0   , j, k,    -getQ(   1   , j, k) );
		}
	    }
	    //y section
	    for( int k=0; k < unit; k++ ){
		for( int i=0; i < unit; i++ ){
		    //max:   beyond the back wall = the front of the back wall
		    if ( index[_y] == size[_y] - 1 )
		    putQ(i, unit - 1  , k, getQ(i, unit-3, k) );
		    //max-1: back wall
		    if ( index[_y] == size[_y] - 1 )
		    putQ(i, unit-2, k,    0.0f );
		    //zero:  beyond the front wall = the front of the front wall
		    if ( index[_y] == 0 )
		    putQ(i,    0   , k,    getQ(i,    2   , k) );
		    //one:   front wall
		    if ( index[_y] == 0 )
		    putQ(i,    1   , k,    0.0f );
		}
	    }
	    //z section
	    for( int i=0; i < unit; i++ ){
		for( int j=0; j < unit; j++ ){
		    //max
		    if ( index[_z] == size[_z] - 1 )
			putQ(i, j, unit-1,    0.0f);
		    //max-1
		    if ( index[_z] == size[_z] - 1 )
		    putQ(i, j, unit-2, -getQ(i, j, unit-3));
		    //zero
		    if ( index[_z] == 0 )
			putQ(i, j,    0, -getQ(i, j,    1   ));
		}
	    }
	    break;
	case _w:
	    //x section
	    for( int j=0; j < unit; j++ ){
		for( int k=0; k < unit; k++ ){
		    //max
		    if ( index[_x] == size[_x] - 1 )
			putQ(unit-1  , j, k,      0.0f);
		    //max-1
		    if ( index[_x] == size[_x] - 1 )
			putQ(unit-2, j, k,     -getQ(unit-3, j, k));
		    //zero
		    if ( index[_x] == 0 )
			putQ(   0   , j, k,     -getQ(   1   , j, k));
		}
	    }
	    //y section
	    for( int k=0; k < unit; k++ ){
		for( int i=0; i < unit; i++ ){
		    //max
		    if ( index[_y] == size[_y] - 1 )
			putQ(i, unit-1  , k,      0.0f);
		    //max-1
		    if ( index[_y] == size[_y] - 1 )
			putQ(i, unit-2, k,     -getQ(i, unit-3, k));
		    //zero
		    if ( index[_y] == 0 )
			putQ(i,    0   , k,     -getQ(i,    1   , k));
		}
	    }
	    //z section
	    for( int i=0; i < unit; i++ ){
		for( int j=0; j < unit; j++ ){
		    //max:   beyond the ceil = below the ceil
		    if ( index[_z] == size[_z] - 1 )
			putQ(i, j, unit-1  ,      getQ(i, j, unit-3));
		    //max-1: the ceil
		    if ( index[_z] == size[_z] - 1 )
			putQ(i, j, unit-2,      0.0f);
		    //zero   beyond the floor = above the floor
		    if ( index[_z] == 0 )
			putQ(i, j,    0   ,      getQ(i, j,    2   ));
		    //one:   the floor
		    if ( index[_z] == 0 )
			putQ(i, j,    1   ,      0.0f);
		}
	    }

	    if(mCaseNo==4){
		//air conditioning IN
		//max-1
		for ( int i = 0; i < unit; i++ )
		    for ( int j = 0; j < unit; j++ ) {
			if ( index[_x] * unit + i >= (mMaxX-2)/2-1 && index[_x] * unit + i <= (mMaxX-2)/2+1 && 
			     index[_y] * unit + i >= (mMaxY-2)/2-1 && index[_y] * unit + i <= (mMaxY-2)/2+1 &&
			     index[_z] == size[_z] - 1 ) {
			    putQ(i, j, unit - 1,  1.0f);
			    putQ(i, j, unit - 2,  1.0f);
			}
		    }
	    }
	    break;
        }
    }
}
