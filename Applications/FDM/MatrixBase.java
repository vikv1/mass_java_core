/******************************************************************************
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.

  Used as is with the MASS library for parallelization, UW Bothell.
******************************************************************************/

package FDM;

/**
 * Gives some differential-equation-based operations to Grid. This becomes 
 * a base class for:
 * MatrixV3D, MatrixP, and MatrixT
 */
public class MatrixBase extends Grid {

    /**
     * Is the default constructor that instantiates a 3D matrix without
     * element initialization
     */
    public MatrixBase( int[] index, int[] size ){
	super( index, size );
    }

    /**
     * Is a constructor that instantiates a 3D matrix with initializing
     * all elements with value
     */
    public MatrixBase( int[] index, int[] size, float value ){
        super( index, size, value );
    }

    /**
     * Computes the central difference of the current element:
     * du/dx = ( u_(i+1) - u_(i-1) ) / 2delta_x
     * du/dy = ( u_(i+1) - u_(i-1) ) / 2delta_y
     * du/dz = ( u_(i+1) - u_(i-1) ) / 2delta_z
     *
     * @param  direction _x, _y, or _z (i.e., 0, 1, or 2)
     * @return the central difference computed
     */
    protected double centralDifference1( int direction ){
        return ( q( direction, +1 ) - q( direction, -1 ) )
	    * m_hInverse[ direction ] * 0.5;
    }

    /**
     * Computes the 2nd central difference of the current element:
     * du^2/dx^2 = ( u_(i+1) - 2u_i + u_(i-1) ) / delta_x^2
     * du^2/dy^2 = ( u_(i+1) - 2u_i + u_(i-1) ) / delta_y^2
     * du^2/dz^2 = ( u_(i+1) - 2u_i + u_(i-1) ) / delta_z^2
     *
     * @param  direction _x, _y, or _z (i.e., 0, 1, or 2)
     * @return the 2nd central difference computed
     */
    protected double centralDifference2( int direction ){
        return ( q( direction, +1 ) - q()*2.0 + q( direction, -1 ) )
	    * m_h2Inverse[ direction ];
    }
    
    /**
     * Computes the forward difference of the current element:
     * du/dx = ( u_(i+1) - u_i ) / delta_x
     * du/dy = ( u_(i+1) - u_i ) / delta_y
     * du/dz = ( u_(i+1) - u_i ) / delta_z
     *
     * @param  direction _x, _y, or _z (i.e., 0, 1, or 2)
     * @return the forward difference computed
     */
    protected double forwardDifference( int direction ){
        return ( q( direction, +1 ) - q() ) * m_hInverse[ direction ];
    }

    /**
     * Computes the backword difference of the current element:
     * du/dx = ( u_i - u_(i-1) ) / delta_x
     * du/dy = ( u_i - u_(i-1) ) / delta_y
     * du/dz = ( u_i - u_(i-1) ) / delta_z
     *
     * @param  direction _x, _y, or _z (i.e., 0, 1, or 2)
     * @return the backward difference computed
     */
    protected double backwardDifference( int direction ){
        return ( q() - q( direction, -1 ) ) * m_hInverse[ direction ];
    }

}
