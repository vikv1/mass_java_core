/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca;

//import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import uwca.calculations.toe.Tasmax;
import uwca.climatemodels.Tasmax_1;

/**
 *
 * @author jason
 */
public class JobManager {
    private static volatile JobManager instance = null;
    int jobId = 0;
    List<Job> jobs;
    
    // the name of the directory where all the job information will be stored
    private String jobsDirectory = "jobs";
    
    /**
     * Default Constructor
     */
    private JobManager() { 
        jobs = new ArrayList<Job>();
    }
    
    /**
     * 
     * @return 
     */
    public static JobManager getInstance() {
        if (instance == null) {
            synchronized (JobManager.class) {
                if (instance == null) {
                    instance = new JobManager();
                }
            }
        }
        return instance;
    }
    /**
     * Submits job, called by servlets
     * @param mgmtVar the var to calculate
     * @param model  the model to use
     */
    public synchronized void submitJob(String var, String model, String[] params){
        Job job = new Job(this.getJobsDirectory());
        switch(var){
            case "tmax":       
                job.setVariable(new Tasmax(params, jobsDirectory)); 
                job.setVarName(var);
                job.setStatus("Queued");
                break;
            default:
                break;
        }
        switch(model){
            case "conus_c5":
                job.setInputModel(new Tasmax_1());
                job.setInputModelName(model);
                break;
            default:                    
                 break;
        
        }
        // add the job to the queue
        jobs.add(job);
    }
    
    /**
     * 
     * @return 
     */
    public synchronized String getStatusUpdates(){
        // format the data for table construction on client side
        String[][] returnData = new String[jobs.size()][5];
        int i = 0;
        for(Job j : jobs){
            returnData[i][0] = j.getVarName();
            returnData[i][1] = j.getInputModelName();
            returnData[i][2] = "";
            returnData[i][3] = j.getStatus();
            i++;
        }
        // return json formatted data for the browser
      //  Gson gson = new Gson();
     //   String json = gson.toJson(returnData);        
      //  return json; 
        return "";
    }
    
    /**
     * Retrieves the next job to be processed in the list. 
     * Called by the JobRunner
     * @return 
     */
    public synchronized Job getNextJob(){
        // find next job in the queue
        for(Job j : jobs){
            if(j.getStatus().equals("Queued")){
                j.setStatus("Running");
                return j;
            }           
        }
        // if there are no queued jobs
        return null;
    }    

    /**
     * @return the jobsDirectory
     */
    public String getJobsDirectory() {
        return jobsDirectory;
    }
}