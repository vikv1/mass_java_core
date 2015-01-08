

import edu.uw.bothell.css.dsl.MASS.*;             // Library for Multi-Agent Spatial Simulation
import java.util.Vector;   // for Vector


// Simulation Scenario
public class RandomWalk 
{
    /**
     * Starts a RandomWalk application with the MASS library
     * @param receives the Land array size, the number of initial agents, and
     *                 the maximum simaution time.
     */
    public static void main( String[] args ) throws Exception {
	// validate teh arguments
	if ( args.length < 9 ) {
	    System.err.println( "usage: " + 
				"java RanodomWalk loginId pass port size nAgents maxTime interval nProcs nThreads show" );
	    System.exit( -1 );
	}
        String login = args[0];
        String pass = args[1];
        String port = args[2];
	int size = Integer.parseInt( args[3] );
	int nAgents = Integer.parseInt( args[4] );
	int maxTime = Integer.parseInt( args[5] );
        int interval = Integer.parseInt( args[6] );
	int nProcesses = Integer.parseInt( args[7] );
	int nThreads = Integer.parseInt( args[8] );
        boolean showGraphics = args.length == 10 ? true : false;
        
	// start MASS
	// MASS.init( args, nProcesses, nThreads );
	String[] massArgs = new String[4];
	massArgs[0] = login;            // user name
	massArgs[1] = pass;          // password
        massArgs[2] = "machinefile.txt";    // machine file
        massArgs[3] = port;                   // optional proc
	MASS.init( massArgs, nProcesses, nThreads );

	// create a Land array.
	Places land = new Places( 1, "Land", null, size, size );

	// populate Nomda agents on the land.
	Agents nomad = new Agents( 2, "Nomad", null, land, nAgents );

	// define the four neighbors of each cell
	Vector<int[]> neighbors = new Vector<int[]>( );
	int[] north = { 0, -1 }; neighbors.add( north );
	int[] east  = { 1,  0 }; neighbors.add( east );
	int[] south = { 0,  1 }; neighbors.add( south );
	int[] west  = { -1, 0 }; neighbors.add( west );
	
        land.callAll( Land.init_, null );
	
	// start graphics
	if ( interval > 0 && showGraphics )
	//  land.callSome( Land.startGraphics_, (Object)null, 0, 0 );
        
	// now go into a cyclic simulation
	for ( int time = 0; time < maxTime; time++ ) {
	    // exchange #agents with four neighbors
	    land.exchangeAll( 1, Land.exchange_, neighbors );
	    land.callAll( Land.update_ );

	    // move agents to a neighbor with the least population
	    nomad.callAll( Nomad.decideNewPosition_, (Object)null );
	    nomad.manageAll( );
            if ( time % interval == 0 )
            {
             //   Object[] agents = land.callAll( Land.collectAgents_, (Object[])null );
             //   if (showGraphics) land.callSome( Land.writeToGraphics_, ( Object )agents, 0, 0 );
            }            
	}

    //    land.callSome(Land.finishGraphics_, (Object)null, 0, 0);
	// finish MASS
	MASS.finish( );
    }
    
}
