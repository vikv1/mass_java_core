/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uwca;

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
         //   Thread.sleep(10000);
            if(j != null){
                // run the calculations
                j.executeJob();
                // update job status
                j.setStatus("Completed");
            }
        }catch(Exception e){
            // TODO: error reporting here
        }
    }
    
}
