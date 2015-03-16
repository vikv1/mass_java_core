package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class Program {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-mandelbrot-0.8.2-SNAPSHOT-jar-with-dependencies.jar";
  public static final int MAX_ITERATION = 200000;
  public static int MATRIX_SIZE = 5024, NTHREADS = 4;
	
	public static void main(String[] args) {
    if(args.length > 1) {
      MATRIX_SIZE = Integer.parseInt(args[0]);
      NTHREADS = Integer.parseInt(args[1]);
    }

		// init MASS library
		MASS.addLibrary(JAR_FILE_NAME);
		MASS.setNodeFilePath(NODE_FILE);
		MASS.setCommunicationPort(50951);
		MASS.setNumThreads(NTHREADS);

    String startStr = "START - " + (new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date()));
    System.out.println(startStr);
    long massStart = System.nanoTime();
    // start MASS
    MASS.init();
		
		int[][] colors = new int[MATRIX_SIZE][MATRIX_SIZE];
		
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Matrix", (Object) new Integer(0), MATRIX_SIZE, MATRIX_SIZE);
		System.err.println("Places init done");
		// create Agents (number of Agents = y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Colorer", null, places, MATRIX_SIZE);
		System.err.println("Agents init done");
    long syncStart = System.nanoTime();
		Object[] agentsCallAllObjs = new Object[MATRIX_SIZE];
		for(int i = 0; i < MATRIX_SIZE; i++){
		  agentsCallAllObjs[i] = i;
		}
		System.err.println("callAll sync start");
		Object[] calledAgentsResults = (Object[]) agents.callAll(Colorer.INIT_MIGRATE, agentsCallAllObjs);
		agents.manageAll();
		calledAgentsResults = (Object[]) agents.callAll(Colorer.CALCULATE_COLOR, agentsCallAllObjs);
		for(int i = 0; i < MATRIX_SIZE; i++)
		{
		  colors[i][0] = (int)calledAgentsResults[i];
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
	      colors[j][i] = (int)calledAgentsResults[j];
	    }
		}
		long syncEnd = System.nanoTime();
		
		// orderly shutdown
		MASS.finish();
    
    long massEnd = System.nanoTime();
    String endStr = "END - " + (new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date()));
    System.out.println(endStr);

    double massTime = (double)(massEnd - massStart) / 1000000000.0;
    double syncTime = (double)(syncEnd - syncStart) / 1000000000.0;
    String durationStr = "DONE: massTime = " + massTime + " sec; async Time = " + syncTime + " sec";
    System.out.println(durationStr);
    saveToFile(startStr+ " " + endStr + " " + durationStr, colors);
	 }
	
private static void saveToFile(String firstline, int[][] result) {
    FileWriter fw;
    BufferedWriter bw = null;
    try {
      fw = new FileWriter("result-sync.txt");
      bw = new BufferedWriter(fw);
      bw.write(firstline);
      bw.newLine();
      
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
