/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.uwbothell.entities;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jwoodrin
 * JobManger class manages the jobs to be submitted by the user, 
 * and executed by the JobRunner
 */
public class JobManager {
    private static volatile JobManager instance = null;
    int jobId = 0;
    List<Job> jobs;
    
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
    public synchronized void submitJob(String mgmtVar, String model){
        Job job = new Job();
        switch(mgmtVar){
            case "tmax":       
                String mgmtVarName = "analytics.Tmax";
                job.setVar(mgmtVarName);
                job.setVarName(mgmtVar);
                job.setDataModel(model);
                job.setStatus("Queued");
                break;
            default:
                break;
        }
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
            returnData[i][1] = j.getDataModel();
            returnData[i][2] = "";
            returnData[i][3] = j.getStatus();
            i++;
        }
        // return json formatted data for the browser
        Gson gson = new Gson();
        String json = gson.toJson(returnData);        
        return json;        
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
}
