package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;

public class Program {

	private static final String NODE_FILE = "nodes.xml";
	private static final String JAR_FILE_NAME = "mass-m-async-fin-0.8.2-SNAPSHOT-jar-with-dependencies.jar";
	public static final int MAX_ITERATION = 300000;
	public static int MATRIX_SIZE = 4032, NTHREADS = 4;
	
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
		
		int[][] colors = new int[MATRIX_SIZE][MATRIX_SIZE];

    String startStr = "START - " + (new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS").format(new Date()));
    System.out.println(startStr);
		long massStart = System.nanoTime();
		// start MASS
		MASS.init();
		
		Places places = new Places(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Cell", (Object) new Integer(0), MATRIX_SIZE, MATRIX_SIZE);
		System.err.println("Places init done");
		// create Agents (number of Agents = y in this case), in Places
		Agents agents = new Agents(1, "edu.uw.bothell.css.dsl.MASS.Mandelbrot.Calculator", null, places, MATRIX_SIZE);
    System.err.println("Agents init done");
		long asyncStart = System.nanoTime();
		Object[] agentsCallAllObjs = new Object[MATRIX_SIZE];
		for(int i = 0; i < MATRIX_SIZE; i++){
		  agentsCallAllObjs[i] = i;
		}
		
		LinkedList<Integer> funcIds = new LinkedList<Integer>();
		funcIds.add(Calculator.INIT_MIGRATE_HORIZON);
		funcIds.add(Calculator.CALCULATE_COLOR);
    for (int i = 1; i < MATRIX_SIZE; i ++) {
      funcIds.add(Calculator.MIGRATE_HORIZON);
      funcIds.add(Calculator.CALCULATE_COLOR);
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
		int i = 0;
		Iterator<Agent> resultsIter = results.iterator();
		while(resultsIter.hasNext()) {
		  Iterator<Object> resultIter = resultsIter.next().getAsyncResults().iterator();
		  int j = 0;
		  while(resultIter.hasNext()){
		    colors[i][j] = (int)resultIter.next();
		    ++j;
		  }
      ++i;
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
