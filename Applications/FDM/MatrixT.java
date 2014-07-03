/******************************************************************************
  
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
******************************************************************************/

package FDM;

public class MatrixT extends MatrixBase {

    float WALL_TEMP;

    /**
     * Is the constructor that initialize all air temperature with 0.5.
     */
    public MatrixT( int[] index, int[] size ) {
        super( index, size, 0.5f );
    }

    public float[][][] retrieveAll( ) {
	float[][][] temperature = new float[unit][unit][unit];
	for ( int i = 0; i < unit; i++ )
	    for ( int j = 0; j < unit; j++ )
		for ( int k = 0; k < unit; k++ )
		    temperature[i][j][k] = getQ( i, j, k );
	return temperature;
    }

    /**
     * Calculates air temperature for all cells based on an array of velocities.
     * @param V a 3D array of velocities
     */
    public void calcAll( MatrixV3D V ){
        setBoundaryCondition(); // calculate the effect of walls and air-conditioning.
        for( int k=0; k < unit; k++ ){          // z
            for( int j=0; j < unit; j++ ){      // y
                for( int i=0; i < unit; i++ ){  // x
		    if ( index[_x] == 0 && i == 0 || index[_y] == 0 && j == 0 ||
			 index[_z] == 0 && k == 0 )
			continue;               // left/front/bottom wall
		    if ( index[_x] == size[_x] - 1 && i >= unit - 2 ||
			 index[_y] == size[_y] - 1 && j >= unit - 2 ||
			 index[_z] == size[_z] - 1 && k >= unit - 2 )
			continue;               // right/back/top wall and beyond
                    setPosition( i,j,k ); V.setPosition( i,j,k );
                    setValue( calc( V ) ); // calculate each cell's temperature
                }
            }
        }
    }
    
    /**
     * Calcuate each cell's air temperature based on an array of velocities.
     * @param V a 3D array of velocities
     */
    private double calc( MatrixV3D V ){
	return q() + m_dt * ( -( V.uRep(_cellCenter) * centralDifference1(_x)
				 + V.vRep(_cellCenter) * centralDifference1(_y)
				 + V.wRep(_cellCenter) * centralDifference1(_z) )
			      + mRePrInverce * ( centralDifference2(_x)
						 +centralDifference2(_y)
						 +centralDifference2(_z)));
    }
    
    /*
     * Assumes "unit" must be six or larger.
     */
    private void setBoundaryCondition(){
        WALL_TEMP=0.5f; // assume that wall temperature is always 0.5
        //x section
        for( int j=0; j < unit; j++ ){
            for( int k=0; k < unit; k++ ){
                //max
		if ( index[_x] == size[_x] - 1 )
		    putQ(unit - 1, j, k, 0.0f);       // beyond the wall
                //max-1
		if ( index[_x] == size[_x] - 1 ) 
		    putQ(unit - 2, j, k, WALL_TEMP);  // on the wall
                //zero
		if ( index[_x] == 0 )
		    putQ(   0   , j, k, WALL_TEMP);  // on the wall
            }
        }

        if(mCaseNo==2){
            //air conditioning OUT
	    for ( int j = 0; j < unit ; j++ ) {
		if ( index[_y] * unit + j >= 2*mMaxY/6 &&
		     index[_y] * unit + j <= 3*mMaxY/6 &&
		     index[_x] == size[_x] - 1 && index[_z] == size[_z] - 1 )
		    putQ(unit-2, j, unit-6,  0.0f);   // cool air conditionning.
            }
        }
        if(mCaseNo==4){
            //air conditioning OUT
	    if ( index[_y] == 0 && index[_z] == size[_z] - 1 ) {
		for( int j=1; j <= 3; j++ )
		    for( int k=unit-5; k <= unit-3; k++ )
			putQ(unit-2, j, k, 1.0f);   // warm air conditioning.
            }
        }
        //y section
        for( int i=0; i <= unit; i++ ){
            for( int k=0; k <= unit; k++ ){
                //max
		if ( index[_y] == size[_y] - 1 )
		    putQ(i, unit - 1, k, 0.0f);
                //max-1
		if ( index[_y] == size[_y] - 1 )
		    putQ(i, unit - 2, k, WALL_TEMP);
                //zero
		if ( index[_y] == 0 )
		    putQ(i,    0   , k, WALL_TEMP);
            }
        }
        //z section
        for( int i=0; i <= unit; i++ ){
            for( int j=0; j <= unit; j++ ){
                //max
		if ( index[_z] == size[_z] - 1 )
		    putQ(i, j, unit - 1, 0.0f);
                //max-1
		if ( index[_z] == size[_z] - 1 )
		    putQ(i, j, unit - 2, WALL_TEMP);
                //zero
		if ( index[_z] == 0 )
		    putQ(i, j,    0   ,  WALL_TEMP);
            }
        }

        if(mCaseNo==2){
            //z section heating
	    for ( int i = 0; i < unit; i++ )
		for ( int j = 0; j < unit; j++ ) {
		    if ( //index[_x] * unit + i >= 2*mMaxX/3 && index[_x] * unit + i <= mMaxX-3 &&
			index[_x] * unit + i >= 2*(mMaxX-1)/3 && index[_x] * unit + i <= mMaxX-4 &&
			 index[_y] * unit + j >= mMaxY/3   && index[_y] * unit + i <= 2*mMaxY/3 &&
			 index[_z] == 0 )
			//zero
			putQ(i, j,    0   , 1.0f);  // warmed by sunlight
                }
	}
        if(mCaseNo==3){
            //z section heating
	    for ( int i = 0; i < unit; i++ )
		for ( int j = 0; j < unit; j++ ) {
		    if ( index[_x] * unit + i >= (mMaxX-1)/2-1 
			 && index[_x] * unit + i <= (mMaxX-1)/2+1 &&
			 index[_y] * unit + i >= (mMaxY-1)/2-1 
			 && index[_y] * unit + i <= (mMaxY-1)/2+1 &&
			 index[_z] == 0 )
			//zero
			putQ(i, j,    0   , 1.0f);  // warmed by sunlight
                }
        }
    }
}

