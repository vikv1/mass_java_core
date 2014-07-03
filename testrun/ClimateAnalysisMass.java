// ClimateAnalysisMass.java   -- Cherie Wasous 3.5.2014

import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import MASS.Agents;
import MASS.Constants;
import MASS.MASS; // Library for Multi-Agent Spatial Simulation
import MASS.Places;

public class ClimateAnalysisMass {

	// just runMode 0 !!!  No enhanced agents

	private static final int USE_NO_AGENTS = 0;

	public static void main(String[] args) throws Exception {

		Date startMassInitTime = new Date();
		Date stopMassInitTime, stopInitPlacesTime, stopPlacesComputeTime, startAgentsLoopTime, stopPlacesCollectTime, stopUserSearchTime, startMassFinishTime, stopMassFinishTime, stopPlacesMyMaxGatherTime;

		// Verify number of arguments
		if (args.length < 9) {
			System.err
					.println("\nUsage:\n\tjava ClimateAnalysisMass login "
							+ "pass port nAgents nProc nThrds nTimeSlots nDays runMode");
			System.exit(-1);
		}

		// Set variables with the user input data
		String login = args[0];
		String pass = args[1];
		String port = args[2];
		int nAgents = Integer.parseInt(args[3]);
		int nProcesses = Integer.parseInt(args[4]);
		int nThreads = Integer.parseInt(args[5]);

		// usually 4 timeslots (midnite, 6am, noon, 6pm) --> this is X dimension
		int nTimeSlots = Integer.parseInt(args[6]);
		// usually 30 or 364 --> this is Y dimension
		int nDays = Integer.parseInt(args[7]);

		// runMode (see definitions at top of file)
		int runMode = Integer.parseInt(args[8]);

		// prepare MASS arguments
		String[] massArgs = new String[4];
		massArgs[0] = login; // user login
		massArgs[1] = pass; // user password
		massArgs[2] = "machinefile.txt"; // machine file
		massArgs[3] = port; // port

		// Start the MASS library
		MASS.init(massArgs, nProcesses, nThreads);


		// Create the ClimateData Places array: nTimeSlots x nDays
		// typically 4 times per day (midnight, 6am, noon, 6pm), but
		// programmable
		// typically a month of data (30 days), but programmable
		//
		// Each place element contains a grid for the Pacific NW, which consists
		// of 123 x 162 locations. So at each location in this grid is the
		// climate information that that particular timeSlot and day.
		int chunk = nTimeSlots / nProcesses;
		Places climateData = new Places(1, "ClimateData", chunk, nTimeSlots,
				nDays);

		// Each climateData element "reads its data", then computes values
		climateData.callAll(ClimateData.compute_, null);

		// **********************************************************************************
		// runMode Switch statement
		//
		// **********************************************************************************
		switch (runMode) {

		// ****************************************************************************
		// USE_NO_AGENTS
		//
		// .runMode of using no agents, so just via place callAll gather all
		// values and do sequential sort on this Master node
		//
		// .uses original MASS Agents constructor
		//
		// ****************************************************************************
		case USE_NO_AGENTS: // do all work without using any agents

			// gather the max values from each place element

			Object[] tempArgs = new Object[nDays * nTimeSlots];
			Object[] temp = climateData.callAll(ClimateData.myMax_, tempArgs);


			// look thru returned values and find the max
			MaxClimateData overallMax = new MaxClimateData();
			overallMax.mcdFlux = 0.0; // set to very low value
			MASS.log("Length of returned value is " + temp.length);
			for ( int i = 0; i < temp.length; i++ ) // fukuda debugging
			    System.out.println( "temp[" + i + "] = " + temp[i] ); // fukuda debugging

			for (int i = 0; i < temp.length; i++) {
				MaxClimateData nextValue = (MaxClimateData) temp[i];
				MASS.log("Going to do comparison # " + i);
				if (overallMax.mcdFlux < nextValue.mcdFlux) {
					overallMax = nextValue;
				}
			}

			System.out.println("\nMax value found at day=" + (overallMax.mcdDay +1)
					+ " and time=" + (overallMax.mcdTime +1)
					+ ", with flux=" + overallMax.mcdFlux
					+ ", direction=" + overallMax.mcdDir
					+ ", at x=" + overallMax.mcdX + ", y="
					+ overallMax.mcdY);
			if ( ( ( overallMax.mcdDay + 1 ) == nDays ) &&
			     ( ( overallMax.mcdTime + 1 ) == nTimeSlots ) &&
			     ( ( overallMax.mcdX ) == 122 ) &&
			     ( ( overallMax.mcdY ) == 161 ) ) {
		    	 MASS.printResult("$.$.$.$.$.$.$.$.$.$.$.$.$.$      CORRECT !!!  :-)     $.$.$.$.$.$.$.$.$.$.$.$.$.$");
		     } else {
		    	 MASS.printResult("~~~~~~~~~~~~~~~~~~~~~~~~~~~   NOT correct...  :-(  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		     }
			break;


		default:
			System.out.println("\n$$$ that runMode=" + runMode
					+ " not yet supported, come back later !! ");
			break;
		}

		startMassFinishTime = new Date();

		// Gracefully shut-down MASS
		MASS.finish();

		// Terminate the JVM
		System.exit(0);

	}
}
