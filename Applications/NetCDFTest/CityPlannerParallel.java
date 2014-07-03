import MASS.*;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

public class CityPlannerParallel {
	public static final int NPROC = 1;			// Number of processors
	public static final int NTHRD = 1;			// Number of threads
//	public static final int NPROC = 2;
//	public static final int NTHRD = 4;
//	public static final int GEN_LENGTH = 1;	// Length of a generation

	public static String fileName;

	public static void main(String args[]) throws Exception {
		//		int size = Integer.parseInt( args[0] );		// Size of the city grid to create
		//		int nAgents = Integer.parseInt( args[1] );	// number of agents over simulation
		//		int maxGen = Integer.parseInt( args[2] );	// Max generations to run sim of
		//		int writeInterval = Integer.parseInt( args[3] );  // How frequently to write state
		//		fileName = args[4];  // Name of file state will be written to

		long startElapsedTime = new Date().getTime();	// Start timer

		int size = 100;		// Size of the city grid to create
		int nAgents = 1000;	// number of agents over simulation
		int maxGen = 100;	// Max generations to run sim of
		int writeInterval = 20;  // How frequently to write state
		fileName = "Parallel_Test.nc";  // Name of file state will be written to
		
		setUpSave( size );
		System.out.println("Finished creating file");

		// start MASS
		String[] massArgs = new String[5];
		massArgs[0] = "dslab";            	// user name
		massArgs[1] = "ds1ab-302";          // password
		massArgs[2] = "machinefile.txt";    // machine file
		massArgs[3] = "5000";               // port
		massArgs[4] = "netcdfAll-4.2.jar";   // optional jar to be loaded with MASS, must be in the same working directory

		// start MASS
		MASS.init( massArgs, NPROC, NTHRD);

		// create an array of cities
		Places city = new Places( 1, "CityPW_NoHold", fileName, size, size );

		// populate cities with citizens
		Agents citizens = new Agents( 2, "CitizenPW_NoHold", null, city, nAgents );

		// define the neighbors of each cell as the square of cells surrounding them 
		// * * *
		// * - *
		// * * *
		Vector<int[]> neighbors = new Vector<int[]>( );
		int[] north = { 0, -1 }; neighbors.add( north );
		int[] east  = { 1,  0 }; neighbors.add( east );
		int[] south = { 0,  1 }; neighbors.add( south );
		int[] west  = { -1, 0 }; neighbors.add( west );
		//		int[] nw = { -1, -1 }; neighbors.add( nw );
		//		int[] sw  = { -1,  1 }; neighbors.add( sw );
		//		int[] ne = { 1,  -1 }; neighbors.add( ne );
		//		int[] se  = { 1, 1 }; neighbors.add( se );

		printResults(city, size);

		// Begin simulation
		Date startTime = new Date( );

		for ( int years = 0; years < maxGen; years++ ) {
			// exchange #agents with four neighbors
			city.exchangeAll( 1, CityPW_NoHold.exchange_, neighbors );
			city.callAll( CityPW_NoHold.update_ );

			// move agents to a neighbor with the least population
			citizens.callAll( CitizenPW_NoHold.decideNewPosition_ );
			citizens.manageAll( );

//			printResults(city, size);
			
			if (years % writeInterval == 0)
				city.callAll(CityPW_NoHold.saveState_);
		}

		city.callAll(CityPW_NoHold.saveState_);		// Save final state
		Date endTime = new Date( );			// End timer

		// Print results and elapsed time
		printResults(city, size);
		System.out.println( "\tTime (ms): " +
				( endTime.getTime( ) - startTime.getTime( ) ) );

		long endElapsedTime = new Date().getTime();	// Stop timer
		System.out.println("Total elapsed time = " + (endElapsedTime - startElapsedTime));
		
		// finish MASS
		MASS.finish( );
	}

	// Sets up the file all saved state will be written to
	private static void setUpSave(int size) {
		NetcdfFileWriteable fileOut = null;
		try {
			fileOut = NetcdfFileWriteable.createNew(fileName);

			// Set the size of the array to the max number of agents, as for
			// now there will never be more agents than initially created
			// Use Max_Pop * 3 until fix more than Max_Pop agents moving to a place
			Dimension numAllowed = fileOut.addDimension("size", CityPW_NoHold.MAX_POP * 3); 
//			Dimension numAllowed = fileOut.addUnlimitedDimension("agents");
			
			// Create a variable for each City on the grid, represented by their indices
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					fileOut.addVariable((i + "-" + j), DataType.INT, new Dimension[] { numAllowed });
				}
			}

			fileOut.create();

		} catch (IOException e2) {
			System.out.println("Error creating file for City data");
		} finally {		// Close out the file no matter what happens
			if (null != fileOut)
				try {
					fileOut.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
	}

	// Prints number of agents per each place
	private static void printResults(Places city, int size) {
		Object[] data = city.callAll( CityPW_NoHold.exchange_, null );
		int index = 0;
		for ( int i = 0; i < size; i++ ) {
			for ( int j = 0; j < size; j++ ) 
				System.out.print( (( Integer )data[index++]).intValue() + " " );
			System.out.println();
		}	
		System.out.println();
	}
	
}