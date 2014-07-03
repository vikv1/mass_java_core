// SugarScapeMass.java

import MASS.*;
import java.util.*;

public class SugarScapeMass
{

    public static void main( String[] args ) throws Exception {

	// Verify user provide enough input data
	if( args. length < 9 ) {
		System.err.println( "\nUsage:\n\tjava SugarScape login pass port "
				  + "size nAgents maxTime interval nProc nThrds show");
		System.exit( -1 );
	}

	// Set variables with the user input data
        String login 	= args[0];
        String pass 	= args[1];
        String port 	= args[2];
        int size 	= Integer.parseInt( args[3] );
        int nAgents 	= Integer.parseInt( args[4] );
        int maxTime 	= Integer.parseInt( args[5] );
        int interval 	= Integer.parseInt( args[6] );
        int nProcesses 	= Integer.parseInt( args[7] );
        int nThreads 	= Integer.parseInt( args[8] );
        boolean showGraphics = args.length == 10 ? true : false;
		int vDist = 1;	

        // Start MASS
        String[] massArgs = new String[4];
        massArgs[0] 	  = login;   			// user login
        massArgs[1] 	  = pass;          		// user password
        massArgs[2] 	  = "machinefile.txt";    	// machine file
        massArgs[3] 	  = port;                   	// port 

		// Start the MASS librarly
        MASS.init( massArgs, nProcesses, nThreads );		

        // Create a Land array.
        Places land = new Places( 1, "Land", null, size, size );				//<< exchangeAll()
        //Places land = new Places( 1, "Land", null, vDist, false, size, size );	//<< exchangeBoundary()

        // Populate Agents (unit) on the Land array
        Agents unit = new Agents( 2, "Unit", null, land, nAgents );


        // Define the neighbors of each cell   
        Vector<int[]> neighbors = new Vector<int[]>( );

		for( int x = 0 - vDist; x <= vDist; x++ ) {
       	    for( int y = 0 - vDist; y <= vDist; y++ ) {

           		if( !(x == 0 && y == 0) ) 
		    	neighbors.add( new int[]{ x, y } );
       	    }
		}

		// Inialize all Land locations
        land.callAll( Land.init_, null );

        // Start graphics
        if ( interval > 0 && showGraphics )
       		land.callSome( Land.startGraphics_, (Object)null, 0, 0 );

		// Timer variables
		Date startTime = new Date( );
        long timer_total = 0;


        // Start simulatin time
		// ---------------------
        for ( int time = 0; time < maxTime; time++ ) 
		{
			Date timer_start = new Date();

            // Exchange #agents with neighbors
            land.exchangeAll( 1, Land.exchange_, neighbors );			// << exchangeAll()
            //land.exchangeBoundary( 1, Land.exchange_, neighbors );	// << exchangeBoundary()

            land.callAll( Land.update_ );

            // Move agents to a neighbor with the least population
            unit.callAll( Unit.decideNewPosition_, (Object)null );
            unit.manageAll( );

			// Stop timer and add time to timer total
			Date timer_stop = new Date();
			timer_total += (timer_stop.getTime() - timer_start.getTime());

			// Collect and print (if selected) the local data
            if ( time % interval == 0 )
            {
                Object[] locData = land.callAll( Land.collectLocData_, (Object[])null );
                if (showGraphics) land.callSome( Land.writeToGraphics_, ( Object )locData, 0, 0 );
            }
        }

		Date endTime = new Date( );
        System.out.println( "\n *** Total Time (ms)    : " + ( endTime.getTime( ) - startTime.getTime( ) ) );
        System.out.println(   " *** Execution Time (ms): " + timer_total );

		// End graphics
		if( showGraphics ) land.callSome(Land.finishGraphics_, (Object)null, 0, 0);

        // finish MASS
        MASS.finish( );
    }


}
