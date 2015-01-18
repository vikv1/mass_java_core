package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class Program {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-quickstart-0.8.2-SNAPSHOT.jar";
	public static final int MATRIX_SIZE = 200, MAX_ITERATION = 200;
	
	public static void main(String[] args) {

		// init MASS library
		MASS.addLibrary(JAR_FILE_NAME);
		MASS.setNodeFilePath(NODE_FILE);
		
		double[][] colors = new double[MATRIX_SIZE][MATRIX_SIZE];
		
		// start MASS
		MASS.init();
		
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Matrix", (Object) new Integer(0), MATRIX_SIZE, MATRIX_SIZE);
		
		// create Agents (number of Agents = y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.Maldelbrot.Colorer", null, places, MATRIX_SIZE);
		Object[] agentsCallAllObjs = new Object[MATRIX_SIZE];
		for(int i = 0; i < MATRIX_SIZE; i++){
		  agentsCallAllObjs[i] = i;
		}
		Object[] calledAgentsResults = (Object[]) agents.callAll(Colorer.INIT_MIGRATE, agentsCallAllObjs);
		agents.manageAll();
		calledAgentsResults = (Object[]) agents.callAll(Colorer.CALCULATE_COLOR, agentsCallAllObjs);
		for(int i = 0; i < MATRIX_SIZE; i++)
		{
		  colors[0][i] = (double)calledAgentsResults[i];
		}
		
		// move all Agents four times to cover all dimensions in Places
		for (int i = 1; i < MATRIX_SIZE; i ++) {
			
			// tell Agents to move
			agents.callAll(Colorer.MIGRATE);
			
			// sync all Agent status
			agents.manageAll();
			
			// find out where they live now
			calledAgentsResults = (Object[]) agents.callAll(Colorer.CALCULATE_COLOR, agentsCallAllObjs);
			for(int j = 0; j < MATRIX_SIZE; j++)
	    {
	      colors[i][j] = (double)calledAgentsResults[j];
	    }
		}
		
		// orderly shutdown
		MASS.finish();
		System.out.println("Result is :");
		for(int i = 0; i < MATRIX_SIZE; i++){
		  for(int j = 0; j < MATRIX_SIZE; j++)
		  {
		    System.out.print(colors[i][j] + " ");
		  }
		  System.out.println();
		}
	 }
	 
}
