package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class Program {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-mandelbrot-0.8.2-SNAPSHOT-jar-with-dependencies.jar";
	public static final int MATRIX_SIZE = 100, MAX_ITERATION = 1000;
	
	public static void main(String[] args) {

		// init MASS library
		MASS.addLibrary(JAR_FILE_NAME);
		MASS.setNodeFilePath(NODE_FILE);
		MASS.setCommunicationPort(50951);
		MASS.setNumThreads(2);
    // start MASS
    MASS.init();
		
		int[][] colors = new int[MATRIX_SIZE][MATRIX_SIZE];
		
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Matrix", (Object) new Integer(0), MATRIX_SIZE, MATRIX_SIZE);
		
		// create Agents (number of Agents = y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Colorer", null, places, MATRIX_SIZE);
		Object[] agentsCallAllObjs = new Object[MATRIX_SIZE];
		for(int i = 0; i < MATRIX_SIZE; i++){
		  agentsCallAllObjs[i] = i;
		}
		Object[] calledAgentsResults = (Object[]) agents.callAll(Colorer.INIT_MIGRATE, agentsCallAllObjs);
		agents.manageAll();
		calledAgentsResults = (Object[]) agents.callAll(Colorer.CALCULATE_COLOR, agentsCallAllObjs);
		for(int i = 0; i < MATRIX_SIZE; i++)
		{
		  colors[0][i] = (int)calledAgentsResults[i];
		}
		
		// move all Agents four times to cover all dimensions in Places
		for (int i = 1; i < MATRIX_SIZE; i ++) {
			
			// tell Agents to move
			agents.callAll(Colorer.MIGRATE);
			
			// sync all Agent status
			agents.manageAll();
			
			calledAgentsResults = (Object[]) agents.callAll(Colorer.CALCULATE_COLOR, agentsCallAllObjs);
			for(int j = 0; j < MATRIX_SIZE; j++)
	    {
	      colors[i][j] = (int)calledAgentsResults[j];
	    }
		}
		
		// orderly shutdown
		MASS.finish();
		
		saveToFile(colors);
	 }
	

  private static void saveToFile(int[][] result) {
    FileWriter fw;
    BufferedWriter bw = null;
    try {
      fw = new FileWriter("result.txt");
      bw = new BufferedWriter(fw);
      for(int i = 0; i < result.length; i++)
      {
        for(int j = 0; j < result[i].length; j++)
        {
          bw.write(result[i][j] + "");
          if(j < result[i].length - 1) {
            bw.write(" ");
          }
        }
        bw.newLine();
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      if (bw != null) {
        try {
          bw.close();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }
	 
}
