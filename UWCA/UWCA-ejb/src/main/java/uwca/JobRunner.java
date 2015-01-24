/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uwca;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author jwoodrin
 * Our JobRunner grabs jobs from the JobManager and executes them using MASS
 */
@Singleton
public class JobRunner {
    
    // our reference to our singleton job manager class
    JobManager jobMgr;    
    
    int numProc = 1;
    int numThr = 2;
    
    private Boolean mass_init = false;
    
    /**
     * Our default Constructor
     */
    public JobRunner(){
        jobMgr = JobManager.getInstance();
        
    }
    
    /**
     * Starts up MASS on deployment with required libraries for clustering
     */
    @PostConstruct
    public void initMassLibrary(){    
        // attempt to clean out the jobs directory on each startup      
        File jobDir = new File(jobMgr.getJobsDirectory());
        try { 
            FileUtils.cleanDirectory(jobDir);
        } catch (IOException ex) {
            Logger.getLogger(JobRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
        /**
         * Init MASS for the calculations to execute
         */
        String massLib = "apachemath-3.3.3.jar";  
        MASS.addLibrary(massLib);
        MASS.setCommunicationPort(45454); // port # to use
         MASS.setNumThreads(1);            // # of threads to use
        MASS.init();
    }
    
    /**
     * Shut down mass on re-deploy
     */
    @PreDestroy
    public void finishMassLibrary(){
        // end mass
        MASS.finish();
    }
    /**
     *  Our scheduled job runner -- runs continuously
     */
    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    public void doWork(){
        try{
            // get the next job to be processed from the job manager
            Job j = jobMgr.getNextJob();
         //   Thread.sleep(10000);
            if(j != null){
                // run the calculations
                j.executeJob();
                // update job status
                j.setStatus("Completed");  
            }
        }catch(Exception e){
            // TODO: error reporting here
            String s = e.toString();
        }
    }
    
}
