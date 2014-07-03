/*
 * RandomWalk_Parallel.java
 * (RandomWalk.java modified to test Parallel_NetCDF.java)
 * Modified by Kelsey Weingartner
 */


import MASS.*;             // Library for Multi-Agent Spatial Simulation

import java.util.Date;
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

		long startElapsedTime = new Date().getTime();	// Start timer
		
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
		String[] massArgs = new String[5];
		massArgs[0] = login;  				// user name
		massArgs[1] = pass;  				// password
		massArgs[2] = "machinefile.txt";    // machine file
		massArgs[3] = port;       			// optional proc
		massArgs[4] = "netcdfAll-4.2.jar";   // optional jar to be loaded with MASS, must be in the same working directory
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

			if ( time % interval == 0 ) {
				// Update graphics
				Object[] agents = land.callAll( Land.collectAgents_, (Object[])null );
				if (showGraphics) land.callSome( Land.writeToGraphics_, ( Object )agents, 0, 0 );

				// Set up arguments for each write
				Object[] saveArgs = new Object[2];
				saveArgs[0] = new int[] {0};

				int count = 0;
				for (int row = 0; row < size; row ++) {
					for (int col = 0; col < size; col++) {
						if (count >= agents.length) break;	// Break if no agents left
						
						saveArgs[1] = agents[count];	// Replace args[1] with the current value to save			
						writer.callSome(Parallel_NetCDF.write_, (Object)saveArgs, row, col);
						count++;
					}
				}

				// flush to file every other interval
//				if (time % 2 == 0)	{
//					writer.callAll(Parallel_NetCDF.flush_, (Object)null);
//				}
			}            
		}

		// Get final state of the simulation
		Object[] simContent = land.callAll( Land.collectAgents_, (Object[])null );
		
		// Set up arguments for write
		Object[] saveArgs = new Object[2];
		saveArgs[0] = new int[] {0};

		int count = 0;
		for (int row = 0; row < size; row ++) {
			for (int col = 0; col < size; col++) {
				if (count >= simContent.length) break;	// Break if no agents left
				
				saveArgs[1] = simContent[count];	// Replace args[1] with the current value to save			
				writer.callSome(Parallel_NetCDF.write_, (Object)saveArgs, row, col);
				count++;
			}
		}
		 
		writer.callAll(Parallel_NetCDF.flush_, (Object)null);	// Write to disk 
		
		if ( interval > 0 && showGraphics)
			land.callSome(Land.finishGraphics_, (Object)null, 0, 0);	// Finish graphics
		
		// Stop timer and display elapsed time
		long endElapsedTime = new Date().getTime();	
		System.out.println("Total elapsed time = " + (endElapsedTime - startElapsedTime));
		
		// Get file contents for comparison
		Object[] indices = new Object[size * size];
		for (int i = 0; i < (size * size); i++)
			indices[i] = new int[] {0};
		Object[] fileContent = writer.callAll(Parallel_NetCDF.read_, indices);
		
		compare(simContent, fileContent);
		writer.callAll(Parallel_NetCDF.close_, (Object)null); // close out file
		
		// finish MASS
		MASS.finish( );
	}

	private static void compare(Object[] simContent, Object[] fileContent) {
		if (simContent.length != fileContent.length) {
			System.out.println("ERROR: \t Different number of contents");
			return;
		}
		
		// Dump simulation contents to console
		for (int i = 0; i < simContent.length; i++) {
			double currSimVal = ((Double)simContent[i]).doubleValue();
			double currFileVal = ((Double)fileContent[i]).doubleValue();
			
			if (currSimVal != currFileVal) {
				System.out.println("ERROR: \t Index " + i + " Values do not match");
				return;
			}
		}
		
		System.out.println("SUCCESS: \t All values match");
	}
}
