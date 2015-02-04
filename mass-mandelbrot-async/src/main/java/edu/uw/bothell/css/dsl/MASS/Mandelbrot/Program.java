package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class Program {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-mandelbrot-async-0.8.2-SNAPSHOT.jar";
	public static final int MATRIX_SIZE = 200, MAX_ITERATION = 200;
	
	public static void main(String[] args) {

		// init MASS library
		MASS.addLibrary(JAR_FILE_NAME);
		MASS.setNodeFilePath(NODE_FILE);
		
		int[][] colors = new int[MATRIX_SIZE][MATRIX_SIZE];
		
		// start MASS
		MASS.initAsync();
		
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Matrix", (Object) new Integer(0), MATRIX_SIZE, MATRIX_SIZE);
		
		// create Agents (number of Agents = y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.Maldelbrot.Colorer", null, places, MATRIX_SIZE);
		Object[] agentsCallAllObjs = new Object[MATRIX_SIZE];
		for(int i = 0; i < MATRIX_SIZE; i++){
		  agentsCallAllObjs[i] = i;
		}
		
		LinkedList<Integer> funcIds = new LinkedList<Integer>();
		funcIds.add(Colorer.INIT_MIGRATE);
		funcIds.add(Colorer.CALCULATE_COLOR);
    for (int i = 1; i < MATRIX_SIZE; i ++) {
      funcIds.add(Colorer.MIGRATE);
      funcIds.add(Colorer.CALCULATE_COLOR);
    }
		
		Agent[] results = agents.callAllAsync(funcIds, agentsCallAllObjs);
		// orderly shutdown
		MASS.finishAsync();
		System.out.println("Result is :");
		for(int j = 0; j < MATRIX_SIZE; j++){
		  Iterator<Object> resultIter = results[j].getAsyncResults().iterator();
		  int i = 0;
		  while(resultIter.hasNext()){
		    colors[i][j] = (int)resultIter.next();
		    ++i;
		  }
		}
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
        int j = 0;
        for(; j < result[i].length; j++)
        {
          bw.write(result[i][j] + "");
        }
        if(j < result[i].length - 1) {
          bw.write(" ");
        }
      }
      bw.newLine();
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
