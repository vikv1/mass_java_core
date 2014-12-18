package edu.uw.bothell.css.dsl.MASS.QuickStart;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class QuickStart {

	private static final String DEFAULT_MACHINE_FILE = "src/main/resources/machines.xml";
	private static final int DEFAULT_NUM_PROCESSES = 2;
	private static final int DEFAULT_NUM_THREADS = 1;
	private static final String DEFAULT_PASSWORD = "ds1ab-302";
	private static final int DEFAULT_PORT = 3232;
	private static final String DEFAULT_USERID = "dslab";
	
	public static void main(String[] args) {

		int nProcesses = 0;
		int nThreads = 0;
		String[] massArgs = new String[4];

		// use command-line arguments if possible
		if (args.length == 5) {

			// use arguments provided
			massArgs[0] = args[0]; // user name
			massArgs[1] = args[1]; // password
			massArgs[2] = "machinefile.txt"; // machine file
			massArgs[3] = args[2]; // port

			nProcesses = Integer.parseInt(args[3]);
			nThreads = Integer.parseInt(args[4]);

		}
		
		else {

			// use defaults where necessary
			massArgs[0] = DEFAULT_USERID; // user name
			massArgs[1] = DEFAULT_PASSWORD; // password
			massArgs[2] = DEFAULT_MACHINE_FILE; // machine file
			massArgs[3] = String.valueOf(DEFAULT_PORT); // port

			nProcesses = DEFAULT_NUM_PROCESSES;
			nThreads = DEFAULT_NUM_THREADS;
			
//			System.out.println("usage: java -cp MASS.jar:jsch-0.1.44.jar:. "
//					+ "Project userid password port nProcs nThrs");
//			System.exit(-1);

		}
		
		// start MASS
		MASS.init(massArgs, nProcesses, nThreads);
		
		// create all Places (having dimensions of x, y, and z)
		// the total number of Place objects that will be created is x*y*z
		int x = 5;
		int y = 5;
		int z = 5;
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.QuickStart.Matrix", (Object) new Integer(0), x, y, z);
		
		// as a test, instruct all places to return the hostnames of the machines where they are located
		Object[] placeCallAllObjs = new Object[x*y*z];
		Object[] calledPlacesResults = (Object[]) places.callAll(Matrix.GET_HOSTNAME, placeCallAllObjs);
		
		// create Agents (number of Agents = x*y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.QuickStart.Nomad", null, places, x * y);
		Object[] agentsCallAllObjs = new Object[x*y];
		Object[] calledAgentsResults = (Object[]) agents.callAll(Nomad.GET_HOSTNAME, agentsCallAllObjs);
		
		// move all Agents four times to cover all dimensions in Places
		for (int i = 0; i < 4; i ++) {
			
			// tell Agents to move
			agents.callAll(Nomad.MIGRATE);
			
			// sync all Agent status
			agents.manageAll();
			
			// find out where they live now
			calledAgentsResults = (Object[]) agents.callAll(Nomad.GET_HOSTNAME, agentsCallAllObjs);
			
		}
		
		// find out where all of the Agents wound up when all movements complete
		calledAgentsResults = (Object[]) agents.callAll(Nomad.GET_HOSTNAME, agentsCallAllObjs);
		
		// orderly shutdown
		MASS.finish();
		
	 }
	 
}
