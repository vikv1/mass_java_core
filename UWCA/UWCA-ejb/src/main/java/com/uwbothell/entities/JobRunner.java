/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.entities;

import analytics.Tmax;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;
import javax.ejb.Schedule;
import javax.ejb.Singleton;

/**
 *
 * @author jwoodrin
 * Our JobRunner grabs jobs from the JobManager and executes them using MASS
 */
@Singleton
public class JobRunner {
    
    // our reference to our singleton job manager class
 //   @Inject
  //  @EJB
  //  private JobManagerSingleton jobMgr;
    JobManager jobMgr;
    
    /**
     * Our default Constructor
     */
    public JobRunner(){
        jobMgr = JobManager.getInstance();
    }
    
    /**
     *  Our scheduled job runner
     */
    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    public void doWork(){
        try{
            // Location for machine file using Glassfish
            // C:\glassfish4\glassfish\domains\domain1\config
       
            Job j = jobMgr.getNextJob();
            Thread.sleep(10000);
            if(j != null){
                // read in Machine file to determine how many machines we have
                // TODO: READ FILE
                // processors are x1 per machine (unless we can use herc)
                // threads are x2 per processor
                // we determine the places by the number of overall threads
                
                // we determine how much data we can read in at once by looking @ file sizes and 
                
                // need to mark the file changeovers for places to handle as they are reading in the data chunks
                
                
                // since there's a job to execute prepare Mass here
                String[] massArgs = new String[4];
                int numProc = 1;
                int numThr = 2;
                massArgs[0] = "var1";
                massArgs[1] = "var2";
                massArgs[2] = "machinefile.txt";
                massArgs[3] = "45454"; // port
                
                MASS.init(massArgs, numProc, numThr);
                int interv = 0;              
                
                String var = j.getVar();
                
                // figure out how many processors and threads we have based off of 
                
                // PLACES ARGS:
                // handle
                // class to run
                // argument
                // Places dimension x
                // Places dimension y 
                // Places dimension z // not used
                Places places = new Places(1, var, (Object)(new Integer(interv)), 10, 10);
                // TODO: replace with java reflection
                int[] functions = Tmax.getFunctions();
                // after getting the function list, execute each one sequentially
                for (int i : functions){
                     places.callAll(i, 0); // not full read TODO: make dynamic
                }
                // complete calculation               
                MASS.finish();
                // update job status
                j.setStatus("Completed");
            }
        }catch(Exception e){
            // TODO: error reporting here
        }
    }
    
}
