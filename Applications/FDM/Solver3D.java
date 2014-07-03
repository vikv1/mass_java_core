/******************************************************************************
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
******************************************************************************/

package FDM;

import MASS.*;
import java.util.*;

//import graphic3D.RenderObjectForFlow;

public class Solver3D {

    Places cubicles;
    Vector<int[]> neighbors;
    Object[] args;

    int sizeX, sizeY, sizeZ, unit, interval;

    /**
     * Instantiates three 3D arrays, each corresponding to velocity, pressure
     * and temperature.
     * @param maxX   each 3D array's x axis
     * @param maxY   each 3D array's y axis
     * @param maxZ   each 3D array's z axis
     * @param caseNo test scenario number
     */
    public Solver3D(int sizeX, int sizeY, int sizeZ, int unit,
		    double re, double pr, double gr, int interval,
		    int caseNo ) throws Exception {

	this.sizeX = sizeX; this.sizeY = sizeY; this.sizeZ = sizeZ; this.unit = unit; this.interval = interval;
	int[] intParams = { unit, caseNo };
	double[] doubleParams = { re, pr, gr };
	Object[] args = { intParams, doubleParams };

	cubicles = new Places( 1, "FDM.Cubicle", ( Object )args, sizeX, sizeY, sizeZ );
	neighbors = Cubicle.createNeighbors( );

        // generate a set of dummy arguments that is used in callAll.
        args = new Object[sizeX * sizeY *sizeZ];
        for ( Object o : args )
            o = new Object( );
    }

    /**
     * Converts an Object array to a double array
     *
     * @param oArray an array of objects to be converted
     * @return a float array
     */
    private float[] object2floatArray( Object[] oArray ) {
        float[][][][][][] dArray6D
            = new float[sizeX][sizeY][sizeZ][][][];

        // Convert an array of objects, each with a 3D float array to
        // a 6D float array
        int element =0;
        for ( int x = 0; x < sizeX; x++ )
            for ( int y = 0; y < sizeY; y++ )
                for ( int z = 0; z < sizeZ; z++ )
                    dArray6D[x][y][z] = ( float[][][] )oArray[element++];

        float[] dArray1D
            = new float[sizeX*unit * sizeY*unit * sizeZ*unit];
        // Now covert a 6D float array to a 1D float array in the order of
        // [x][y][z] where z counts up first.
        element = 0;
        for ( int x = 0; x < sizeX; x++ )
            for ( int i = 0; i < unit; i++ )
                for ( int y = 0; y < sizeY; y++ )
                    for ( int j = 0; j < unit; j++ )
                        for ( int z = 0; z < sizeZ; z++ )
                            for ( int k = 0; k < unit; k++ )
                                dArray1D[element++]
                                    = dArray6D[x][y][z][i][j][k];

        return dArray1D;
    }

    /**
     * Is the simulation body that calculates the transition of velocity, pressure
     * and temperature over 100,000,000 cycles.
     */
    public void calc(){
        System.err.println( "start"  );

	long startMilisec = System.currentTimeMillis();

	// shadow initialization
	cubicles.exchangeAll( 1, Cubicle.exchangeP_, neighbors );
	cubicles.callAll( Cubicle.putP_ );
	cubicles.exchangeAll( 1, Cubicle.exchangeV_, neighbors );
	cubicles.callAll( Cubicle.putV_ );
	cubicles.exchangeAll( 1, Cubicle.exchangeT_, neighbors );
	cubicles.callAll( Cubicle.putT_ );

        for( int i=0; i<= 500; i++ ){
	    if ( i % interval == 0 ) {
		System.err.println( "loop " + i );

		float[] temperatures =
		    object2floatArray( cubicles.callAll( Cubicle.retrieveAllT_, args ) );
		
		// print them out
		int index = 0;
		for ( int x = 0; x < sizeX * unit; x++ )
		    for ( int y = 0; y < sizeY * unit; y++ )
			for ( int z = 0; z < sizeZ * unit; z++ ) {
			    float actual = temperatures[index++];
			    if ( x == 0 || y == 0 || z == 0 ||
				 x >= sizeX * unit -2 || y >= sizeY * unit -2 || z >= sizeZ * unit -2 )
				continue;

			    System.out.printf( "%d %d %d %f\n", x, y, z, actual );
			    if ( actual >= Float.MAX_VALUE )
				System.exit( -1 );
			}

	    }

	    // pressure calculation
	    double preError=-1.0, error=0.0;
	    cubicles.callAll( Cubicle.resetQ_ );

	    int iteration;
	    for ( iteration=0; iteration<MatrixP._MAX_ITERATION; iteration++ ) {
		Object[] errArray = cubicles.callAll( Cubicle.calcP_, args );
		cubicles.exchangeAll( 1, Cubicle.exchangeP_, neighbors );
		cubicles.callAll( Cubicle.putP_ );

		error=0.0;
		for ( int j = 0; j < errArray.length; j++ )
		    error+= ( ( Double )errArray[j] ).doubleValue( );
		// System.err.printf( "error(iter=%d) =%f\n", iteration, error );
		if ( error / preError > 0.9999 ) {
		    //System.err.printf( "iteration=%d error=%f / preError=%f ", iteration, error, preError );
		    break;
		}
		preError = error;
	    }
	    if ( iteration == MatrixP._MAX_ITERATION - 1 )
		System.err.println( "exceeded iteration loop" );

	    // velocity calculation
	    cubicles.callAll( Cubicle.calcV_ );
	    cubicles.exchangeAll( 1, Cubicle.exchangeV_, neighbors );
	    cubicles.callAll( Cubicle.putV_ );

	    // temperature calculation
	    cubicles.callAll( Cubicle.calcT_ );
	    cubicles.exchangeAll( 1, Cubicle.exchangeT_, neighbors );
	    cubicles.callAll( Cubicle.putT_ );
        }

	System.err.println( "time = " +  ( System.currentTimeMillis() -startMilisec ) );
    }
}
