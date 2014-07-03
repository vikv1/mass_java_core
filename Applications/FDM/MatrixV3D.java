/******************************************************************************
  
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
******************************************************************************/

package FDM;

public class MatrixV3D extends BasicInfo {

    MatrixV[]           v;

    int[] index;
    int[] size;
    
    public MatrixV3D( int index[], int size[] ) {
        this.index = index;
	this.size = size;

        v = new MatrixV[3];
        v[_u] = new MatrixV( index, size, _u );
        v[_v] = new MatrixV( index, size, _v );
        v[_w] = new MatrixV( index, size, _w );
        
    }

    private void setBoundaryCondition(){
        for( int vComponent=_x; vComponent<=_z; vComponent++ ){
            v[vComponent].setBoundaryCondition();
        }
    }
    
    public void setPosition( int i, int j, int k ){
        //Grid.setPosition( i,j,k );
	v[_u].setPosition( i, j, k ); v[_v].setPosition( i, j, k ); v[_w].setPosition( i, j, k );
    }
    
    public double v( int component ){
        return v[component].q();
    }

    public double uRep( int currentComponent ){
        return v[_u].represent( currentComponent );
    }
    public double vRep( int currentComponent ){
        return v[_v].represent( currentComponent );
    }
    public double wRep( int currentComponent ){
        return v[_w].represent( currentComponent );
    }

    public double uRepForwardDifference( int direction ){
        return ( v[_u].represent( direction, +1 ) - v[_u].represent( direction, 0 ) )
	    * m_hInverse[ direction ] * 0.5;
    }
    public double vRepForwardDifference( int direction ){
        return ( v[_v].represent( direction, +1 ) - v[_v].represent( direction, 0 ) )
	    * m_hInverse[ direction ] * 0.5;
    }
    public double wRepForwardDifference( int direction ){
        return ( v[_w].represent( direction, +1 ) - v[_w].represent( direction, 0 ) )
	    * m_hInverse[ direction ] * 0.5;
    }

    public void calc( MatrixP p, MatrixT T ){
        setBoundaryCondition();
	for( int i=0; i < unit; i++ ){
	    for( int j=0; j < unit; j++ ){
		for( int k=0; k < unit; k++ ){
		    if ( index[_x] == 0 && i == 0 || index[_y] == 0 && j == 0 || index[_z] == 0 && k == 0 ||
			 index[_x] == size[_x]-1 && i >= unit-2 ||
			 index[_y] == size[_y]-1 && j >= unit-2 ||
			 index[_z] == size[_z]-1 && k >= unit-2 )
			continue;
                    setPosition( i,j,k ); p.setPosition( i, j, k ); T.setPosition( i, j, k );
                    v[_u].setValue( calc( p, _u ) );
                    v[_v].setValue( calc( p, _v ) );
                    v[_w].setValue( calc( p, _w ) + m_dt * mGrPerRe2 * T.q() );
                }
            }
        }
    }

    private double calc( MatrixP p, int component ){
        return ( v(component)
                 +  m_dt * ( -( uRep( component ) * v[ component ].centralDifference1(_x)
				+vRep( component ) * v[ component ].centralDifference1(_y)
				+wRep( component ) * v[ component ].centralDifference1(_z) )
                             + mReInverse * ( v[ component ].centralDifference2(_x)
					      +v[ component ].centralDifference2(_y)
					      +v[ component ].centralDifference2(_z) )
                             - p.backwardDifference( component )  
			     )
		 );
    }
}
