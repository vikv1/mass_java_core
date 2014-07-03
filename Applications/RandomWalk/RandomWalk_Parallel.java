/*
 * RandomWalk_Parallel.java
 * (RandomWalk.java modified to test Parallel_NetCDF.java)
 * Modified by Kelsey Weingartner
 */


import MASS.*;             // Library for Multi-Agent Spatial Simulation
import java.util.Vector;   // for Vector


// Simulation Scenario
public class RandomWalk_Parallel 
{
	/**
	 * Starts a RandomWalk application with the MASS library
	 * @param receives the Land array size, the number of initial agents, and
	 *                 the maximum simulation time.
	 */
	public static void main( String[] args ) throws Exception {
		// validate teh arguments
		if ( args.length < 10 ) {
			System.err.println( "usage: " + 
					"java RandomWalk loginId pass port size nAgents maxTime interval nProcs nThreads filename show" );
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
		String filename = args[9];
		boolean showGraphics = args.length == 11 ? true : false;

		// start MASS
		// MASS.init( args, nProcesses, nThreads );
		String[] massArgs = new String[4];
		massArgs[0] = login;  				// user name
		massArgs[1] = pass;  				// password
		massArgs[2] = "machinefile.txt";    // machine file
		massArgs[3] = port;       			// optional proc
		MASS.init( massArgs, nProcesses, nThreads );

		// create a Land array.
		Places land = new Places( 1, "Land", null, size, size );

		// populate Nomad agents on the land.
		Agents nomad = new Agents( 2, "Nomad", null, land, nAgents );
		
		// create writer
		Object[] writerArgs = new Object[4];
		writerArgs[0] = new Integer(nProcesses);
		writerArgs[1] = filename;
		writerArgs[2] = new Integer(Parallel_NetCDF.type_double);
		writerArgs[3] = new int[] {1};		
		Places writer = new Places( 3, "Parallel_NetCDF", null, size, size );
		// Initialize necessary variables
		writer.callAll(Parallel_NetCDF.init_, (Object)writerArgs);	
		// Prepare writer by creating/opening file and buffering contents
		// Passes false so file does not pre-convert netCDF arrays to java 
		writer.callAll(Parallel_NetCDF.open_, (Object)(new Boolean(false)));

		// define the four neighbors of each cell
		Vector<int[]> neighbors = new Vector<int[]>( );
		int[] north = { 0, -1 }; neighbors.add( north );
		int[] east  = { 1,  0 }; neighbors.add( east );
		int[] south = { 0,  1 }; neighbors.add( south );
		int[] west  = { -1, 0 }; neighbors.add( west );

		land.callAll( Land.init_, null );

		// start graphics
		if ( interval > 0 && showGraphics )
			land.callSome( Land.startGraphics_, (Object)null, 0, 0 );

		// now go into a cyclic simulation
		for ( int time = 0; time < maxTime; time++ ) {
			// exchange #agents with four neighbors
			land.exchangeAll( 1, Land.exchange_, neighbors );
			land.callAll( Land.update_ );

			// move agents to a neighbor with the least population
			nomad.callAll( Nomad.decideNewPosition_, (Object)null );
			nomad.manageAll( );

			int intervalCount = 0;
			if ( time % interval == 0 ) {
				// Update graphics
				Object[] agents = land.callAll( Land.collectAgents_, (Object[])null );
				if (showGraphics) land.callSome( Land.writeToGraphics_, ( Object )agents, 0, 0 );
				
				// Set up arguments for each write
				Object[] saveArgs = new Object[2];
				saveArgs[0] = new int[] {0};
				
				// Write each agent value to each place
				for (int i = 0; i < agents.length; i++) {
					saveArgs[1] = agents[i];	// Replace args[1] with the current value to save			
					writer.callSome(Parallel_NetCDF.write_, (Object)saveArgs, i);
				}
					
				// flush to file every other interval
				if (intervalCount % 2 == 0)	{
					writer.callAll(Parallel_NetCDF.flush_, (Object)null);
				}
				intervalCount++;	// Update interval counter
			}            
		}

		land.callSome(Land.finishGraphics_, (Object)null, 0, 0);
		// finish MASS
		MASS.finish( );
	}

}
