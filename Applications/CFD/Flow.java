/******************************************************************************
  
  Copyright(C) 2007 DeepDigital Co.,Ltd. All Rights Reserved.
  
******************************************************************************/
package CFD;

import FDM.Solver3D;
import MASS.*;

public class Flow {

    public static void main(String[] args) {

        int caseNo = 2;
        int size = 2;
	int unit = 15;
	double re=140;
	double pr=0.7;
	double gr=20000.0;
	int interval = 50;
	int nCores = 1;
        int nProcs = 1;

	if ( args.length == 9 ) {
	    caseNo = Integer.parseInt( args[0] );
	    size   = Integer.parseInt( args[1] );
	    unit   = Integer.parseInt( args[2] );
	    re     = Double.parseDouble( args[3] );
	    pr     = Double.parseDouble( args[4] );
	    gr     = Double.parseDouble( args[5] );
	    interval = Integer.parseInt( args[6] );
            nProcs = Integer.parseInt(args[7]);
	    nCores = Integer.parseInt(args[8]);
	}  
	else if ( args.length == 2 ) 
        {
            nProcs = Integer.parseInt(args[0]);
	    nCores = Integer.parseInt(args[1]);

	}
        else
        {
            System.err.println( "usage: java -cp MASS.jar:. Flow caseNo size unit re pr gr nCores" );
	    System.exit( -1 );
        }
            

	String[] massArgs = new String[4];
	massArgs[0] = "dslab";            // user name
	massArgs[1] = "ds1ab-302";          // password
        massArgs[2] = "machinefile.txt";    // machine file
        massArgs[3] = "";                   // optional proc
	MASS.init( massArgs, nProcs, nCores );

        Solver3D solver = null;
	try{
	    switch( caseNo ){
	    case 1:
		solver = new Solver3D( size,size,size,unit,
				       100,1.0,0.0, interval,
				       caseNo );
		break;
	    case 2:
		solver = new Solver3D( size,size,size,unit,
				       //350,0.7,10000.0,interval
				       re,pr,gr,interval,
				       caseNo );
		break;
	    case 3:
		solver = new Solver3D( size,size,size,unit,
				       500,7.0,10000.0,interval,
				       caseNo );
		break;
	    default:
		solver = new Solver3D( size,size,size,unit,
				       100,1.0,0.0,interval,
				       caseNo );
		break;
	    }
	} catch( Exception e ) {
	    e.printStackTrace( );
	}
        
	if ( solver != null )
	    solver.calc();
	MASS.finish( );
    }
}
