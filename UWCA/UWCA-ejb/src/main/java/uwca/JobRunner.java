/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uwca;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Places;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
     * Starts up MASS on deployment
     */
    @PostConstruct
    public void initMassLibrary(){
    
    String massLib = "apachemath-3.3.3.jar";  
     MASS.addLibrary(massLib);
        
 //   String filePath = "C:\\Users\\jwoodrin\\Documents\\NetBeansProjects\\MASS\\UWCA\\UWCA-ejb\\target\\classes\\nodes.xml";
 //    MASS.setNodeFilePath(filePath);  
        
    //    
    //   String s = System.getProperty("user.dir");
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

            // Location for machine file using Glassfish
            // C:\glassfish4\glassfish\domains\domain1\config
       
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
