package FDM;

import MASS.*;
import java.util.*;

public class Cubicle extends Place implements Global {
    MatrixV3D V;
    MatrixP   P;
    MatrixT   T;

    public Cubicle( Object args ) {
        outMessages = index; // passed to each neighbor through exchangeAll
        inMessages  = null; // return values from all my neighbors 

	int[] intParams = ( int[] )( ( Object[] )args )[0];
	double[] doubleParams = ( double[] )( ( Object[] )args )[1];

	int unit = intParams[0];
	int caseNo = intParams[1];
	double re = doubleParams[0];
	double pr = doubleParams[1];
	double gr = doubleParams[2];

	int maxX = unit * size[_x];
	int maxY = unit * size[_y];
	int maxZ = unit * size[_z];

	BasicInfo.setBasicInfo( maxX,maxY,maxZ,re,pr,gr,caseNo,unit );
	
	V = new MatrixV3D( index, size );
	P = new MatrixP( index, size );
	T = new MatrixT( index, size );
    }

    // ExchangeAll-related functions //////////////////////////////////////////
    /**
     * Creates a list of all my east, west, north, south, top, and bottom
     * neighbors.
     *
     * @return a list of all six neighbors.
     */
    public static Vector<int[]> createNeighbors( ) {
        Vector<int[]> neighbors = new Vector<int[]>( );
            //                east       west        north
	    //                south       top        bottom
	    int[][] dirs = { {1, 0, 0}, {-1, 0, 0}, {0, 1, 0},
			     {0, -1, 0}, {0, 0, 1}, {0, 0, -1} };
	    for ( int i = 0; i < dirs.length; i++ )
		neighbors.add( dirs[i] );
	    return neighbors;
    }

    public static final int calcP_ = 0;
    public static final int calcV_ = 1;
    public static final int calcT_ = 2;
    public static final int exchangeP_ = 3;
    public static final int exchangeV_ = 4;
    public static final int exchangeT_ = 5;
    public static final int exchangePQ_ = 6;
    public static final int putP_ = 7;
    public static final int putV_ = 8;
    public static final int putT_ = 9;
    public static final int putPQ_ = 10;
    public static final int retrieveAllT_ = 11;
    public static final int resetQ_ = 12;

    public Object callMethod( int functionId, Object arg ) {
        switch( functionId ) {
	case calcP_: return calcP( arg );
	case calcV_: return calcV( arg );
	case calcT_: return calcT( arg );
	case exchangeP_: return exchangeP( arg );
	case exchangeV_: return exchangeV( arg );
	case exchangeT_: return exchangeT( arg );
	case exchangePQ_: return exchangePQ( arg );
	case putP_: return putP( arg );
	case putV_: return putV( arg );
	case putT_: return putT( arg );
	case putPQ_: return putPQ( arg );
	case retrieveAllT_: return retrieveAllT( arg );
	case resetQ_: return resetQ( arg );
        }
        return null;
    }

    public Object calcP( Object arg ) {
	double error = P.calc( V );
	return ( Object )( new Double( error ) );
    }

    public Object calcV( Object arg ) {
	V.calc( P, T );
	return null;
    }

    public Object calcT( Object arg ) {
	T.calcAll( V );
	return null;
    }

    public Object exchangeP( Object src ) {
	return (Object)P.getBoundary( (int[])src );
    }

    public Object exchangeV( Object src ) {
	float[][][][] border = new float[3][][][];
	border[_u] = V.v[_u].getBoundary( (int[])src );
	border[_v] = V.v[_u].getBoundary( (int[])src );
	border[_w] = V.v[_u].getBoundary( (int[])src );
	
	return (Object)border;
    }

    public Object exchangeT( Object src ) {
	return (Object)T.getBoundary( (int[])src );
    }

    public Object exchangePQ( Object src ) {
	return (Object)P.Q.getBoundary( (int[])src );
    }

    public Object putP( Object arg ) {
	P.putBoundary( inMessages );
	return null;
    }

    public Object putV( Object arg ) {
        Object[][] border = new Object[3][6];

        for ( int msr = 0; msr < 3; msr++ ) { // _u, _v, _w
            for ( int nbr = 0; nbr < 6; nbr++ ) { // from my six neighbors
                float[][][][] neighboringData = ( float[][][][] )( inMessages[nbr] );
                if ( neighboringData != null ) // if I have a nieghbor in this direction
                    border[msr][nbr] = ( Object )( neighboringData[msr] );
		else
		    border[msr][nbr] = null;
            }
	    V.v[msr].putBoundary( border[msr] );
        }
        return null;
    }

    public Object putT( Object border ) {
	T.putBoundary( inMessages );
	return null;
    }

    public Object putPQ( Object border ) {
	P.Q.putBoundary( inMessages );
	return null;
    }

    public Object retrieveAllT( Object border ) {
	return ( Object )T.retrieveAll( );
    }

    public Object resetQ( Object arg ) {
	P.resetQ( V );
	return null;
    }
}
