/******************************************************************************
  Original Code:
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
  Parallelized with the MASS library, University of Washingto Bothell
******************************************************************************/

package FDM;

/**
 * Defines the basic information of a given simulation space.
 */

public class BasicInfo implements Global{
    protected static int        unit;                     // # cells on each axis in a MASS cubicle

    protected static int        mMaxX,mMaxY,mMaxZ;
    
    protected static double[]   m_hInverse,m_h2Inverse;   // 1/delta_X, (1 / delta_X)^2
    protected static double     mReInverse;               // 1/Re
    protected static double     mRePrInverce;             // Pressure/Re
    protected static double     mGrPerRe2;                // Gravity/Re
    protected static double     m_dt, m_dtInverse;        // delta_time, 1/delta_time
    protected static int        mCaseNo;                  // simulation scenario
    

    /**
     * @param maxX defines the size of X.
     * @param maxY defines the size of Y.
     * @param maxZ defines the size of Z.
     * @param re defines Reynold's number.
     * @param pr represents pressure
     * @param gr represents gravity
     * @param caseNo specifies the current simulation scenario.
     */
    public static void setBasicInfo( int maxX, int maxY, int maxZ,
				     double re, double pr, double gr, int caseNo,
				     int unit_param // added for MASS
				     ) {
	unit = unit_param;                         // added for MASS

        mMaxX = maxX;
        mMaxY = maxY;
        mMaxZ = maxZ;
        m_hInverse = new double[3];
        m_h2Inverse = new double[3];
        m_hInverse[_x] = (mMaxX-3);  // the size of x except left and right boundaries = 1/delta_X
        m_hInverse[_y] = (mMaxY-3);  // the size of y except left and right boundaries = 1/delta_Y
        m_hInverse[_z] = (mMaxZ-3);  // the size of z except left and right boundaries = 1/delta_Z

        for( int i=0; i<=2; i++)
            m_h2Inverse[i] = Math.pow( m_hInverse[i],2.0 );
        
        mReInverse      = 1.0/re;
        double max1     = Math.max(m_hInverse[_x],m_hInverse[_y]);
        double max2     = Math.max(max1,m_hInverse[_z]); // max non-boundary size
        m_dt            = 1.0/max2;
        m_dt            = 0.5/max2;        //temp delta_time
        m_dtInverse     = 1.0/m_dt;        // 1 / delta_time

        mRePrInverce    = mReInverse/pr;             // Pressure/Re
        mGrPerRe2       = gr*mReInverse*mReInverse;  // Gravity/Re
        
        mCaseNo         = caseNo;                    // Simulation scenario 

	//System.err.printf( "m_hInverse[_x]=%f mReInverse=%f, m_dt=%f, m_dtInverse=%f, mRePrInverce=%f\n", m_hInverse[_x], mReInverse, m_dt, m_dtInverse, mRePrInverce );
    }

}
