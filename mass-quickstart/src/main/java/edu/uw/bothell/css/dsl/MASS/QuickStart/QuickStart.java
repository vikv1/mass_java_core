package edu.uw.bothell.css.dsl.MASS.QuickStart;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class QuickStart {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-quickstart-0.8.2-SNAPSHOT.jar";
	
	public static void main(String[] args) {

		// init MASS library
		MASS.addLibrary(JAR_FILE_NAME);
		MASS.setNodeFilePath(NODE_FILE);
		
		// start MASS
		MASS.init();
		
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
