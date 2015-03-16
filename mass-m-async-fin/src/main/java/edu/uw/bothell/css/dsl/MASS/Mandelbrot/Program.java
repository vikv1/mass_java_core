package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class Program {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-m-async-fin-0.8.2-SNAPSHOT-jar-with-dependencies.jar";
	public static final int MAX_ITERATION = 200000;
	public static int MATRIX_SIZE = 4032, NTHREADS = 4, CALC_PER_AGENT, AGENT_SIZE = 4032, NODE_PER_ROW;
	
	public static void main(String[] args) {
	  if(args.length > 1) {
	    MATRIX_SIZE = Integer.parseInt(args[0]);
	    NTHREADS = Integer.parseInt(args[1]);
	    AGENT_SIZE = Integer.parseInt(args[2]);
	  }
	  NODE_PER_ROW = AGENT_SIZE / MATRIX_SIZE;
    CALC_PER_AGENT = MATRIX_SIZE / NODE_PER_ROW;
	  
		// init MASS library
		MASS.addLibrary(JAR_FILE_NAME);
		MASS.setNodeFilePath(NODE_FILE);
    MASS.setCommunicationPort(50951);
    MASS.setNumThreads(NTHREADS);
		
		int[][] colors = new int[MATRIX_SIZE][MATRIX_SIZE];

    String startStr = "START async - " + (new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date())) + " - MAX ITER = " + MAX_ITERATION;
    System.out.println(startStr);
		long massStart = System.nanoTime();
		// start MASS
		MASS.init();
		
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Cell", (Object) new Integer(0), MATRIX_SIZE, MATRIX_SIZE);
		System.err.println((new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date())) + " Places init done");
		// create Agents (number of Agents = y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Calculator", null, places, AGENT_SIZE);
    System.err.println((new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date())) + " Agents init done");
		long asyncStart = System.nanoTime();
		Object[] agentsCallAllObjs = new Object[AGENT_SIZE];
		for(int i = 0; i < AGENT_SIZE; i++){
		  Point p = new Point();
		  p.x = (i / NODE_PER_ROW);
		  p.y = (i % NODE_PER_ROW) * CALC_PER_AGENT;
		  agentsCallAllObjs[i] = p;
		}
		
		int[] funcIds = new int[CALC_PER_AGENT * 2];
		funcIds[0] = Calculator.INIT_MIGRATE_HORIZON;
		funcIds[1] = Calculator.CALCULATE_COLOR;
    for (int i = 1; i < CALC_PER_AGENT; i ++) {
      funcIds[2 * i] = Calculator.MIGRATE_HORIZON;
      funcIds[2 * i + 1] = Calculator.CALCULATE_COLOR;
    }
		
    System.err.println("callAllAsync start");
    List<Agent> results = null;
    try {
      results = agents.callAllAsync(funcIds, agentsCallAllObjs);
    }catch(Throwable e) {
      MASS.logException(null, e);
    }
		
		long asyncEnd = System.nanoTime();
		System.err.println("callAllAsync end");
		
		// orderly shutdown
		MASS.finish();
		
		long massEnd = System.nanoTime();
    String endStr = "END - " + (new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date()));
    System.out.println(endStr);
		System.out.println("Collecting results");
		int i = 0, j = 0, agentIdx = 0;
		Iterator<Agent> resultsIter = results.iterator();
		while(resultsIter.hasNext()) {
		 // System.out.println("agentId = " + agentIdx);
		  Object[] resultIter = resultsIter.next().getAsyncResults();
		  for(int k = 0; k < resultIter.length; k++) {
		   // System.out.print("i = " + i + ", j = " + j + ", ");
		    colors[i][j] = (int)resultIter[k];
		    ++j;
		    if(j  == MATRIX_SIZE) {
		      ++i;
		      j = 0;
		    }
		  }
		  ++agentIdx;
		}

    double massTime = (double)(massEnd - massStart) / 1000000000.0;
    double asyncTime = (double)(asyncEnd - asyncStart) / 1000000000.0;
    String durationStr = "DONE: massTime = " + massTime + " sec; async Time = " + asyncTime + " sec";
    System.out.println(durationStr);
		saveToFile(startStr+ " " + endStr + " " + durationStr, colors);
	 }
	private static void saveToFile(String firstline, int[][] result) {
    FileWriter fw;
    BufferedWriter bw = null;
    try {
      fw = new FileWriter("result-async.txt");
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
