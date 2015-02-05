/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uwca;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import uwca.calculations.toe.Tasmax;
import uwca.calculations.toe.ToeInterface;
import uwca.climatemodels.ClimateModelInterface;

/**
 *
 * @author jason
 */
public class Job {
    
    private int jobNumber;
    
    // main calculation to be done
    private ToeInterface variable;
    private String varName;

    // input climate model
    private ClimateModelInterface inputModel;
    private String inputModelName;
    
    private String status;
    
    // our array list which collects prov information
    List<String> provCollector = new ArrayList<String>();
    
    String provFile = "";
    String toeRegFile = "";
    String toeMinFile = "";
    String toeMaxFile = "";
    
    private String[] outputFiles = new String[4];
    
    private String jobsDirectory = "";
    
    /**
     * public constructor
     */
    public Job(String jobsDir){
        jobsDirectory = jobsDir;
        Random rn = new Random();
        jobNumber =  rn.nextInt(9999);
        
        // create job directory for this specific job
        File dir = new File(jobsDir+"/"+Integer.toString(jobNumber));
        dir.mkdirs();
        File tmp = new File(dir, "provlog.txt");
        try {
            tmp.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Tasmax.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Starts the main calculations for the job
     */
    public void executeJob(){
        ProvAdapter provLogger = new ProvAdapter("jobs/"+jobNumber + "/provlog.txt");
        
        String msg =  " Job " + Integer.toString(jobNumber) + " Started " ;
        provLogger.logProvenance(msg);
        
        // log files used
        msg = "Input Model used: " + this.inputModelName + "\n";
        
        String[] files = inputModel.getFiles();
        for(int i = 1; i <= files.length; i++){
            msg = msg + "File " + i + " " + files[i-1] + "\n";
        }
        provLogger.logProvenance(msg);
        
        variable.setArgs(inputModel, jobNumber, provLogger);
        variable.executeCalculations();
        
        this.writeDataToFile();
        msg =  " Job " + Integer.toString(jobNumber) + " Finished " ;
        provLogger.logProvenance(msg);
    }

    /**
     * Creates the job number directory within the jobs
     */
    public void createJobDirectory(){
             // create job directory
        File dir = new File("jobs/"+jobNumber);
        dir.mkdirs();
        File tmp = new File(dir, "log.txt");
        try {
            tmp.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Tasmax.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    private void writeDataToFile(){
        
        toeRegFile = jobsDirectory+"/"+jobNumber+"/toeReg.nc";
        toeMinFile = jobsDirectory+"/"+jobNumber+"/toePls.nc";
        toeMaxFile = jobsDirectory+"/"+jobNumber+"/toeMin.nc";
        variable.writeNetCdfFiles(toeRegFile, toeMinFile, toeMaxFile);    
    }
    
    public int getNumToeYears(){
        return variable.getNumToeYears();
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the variable
     */
    public ToeInterface getVariable() {
        return variable;
    }

    /**
     * @param variable the variable to set
     */
    public void setVariable(ToeInterface variable) {
        this.variable = variable;
    }

    /**
     * @return the inputModel
     */
    public ClimateModelInterface getInputModel() {
        return inputModel;
    }

    /**
     * @param inputModel the inputModel to set
     */
    public void setInputModel(ClimateModelInterface inputModel) {
        this.inputModel = inputModel;
    }

    /**
     * @return the varName
     */
    public String getVarName() {
        return varName;
    }

    /**
     * @param varName the varName to set
     */
    public void setVarName(String varName) {
        this.varName = varName;
    }

    /**
     * @return the inputModelName
     */
    public String getInputModelName() {
        return inputModelName;
    }

    /**
     * @param inputModelName the inputModelName to set
     */
    public void setInputModelName(String inputModelName) {
        this.inputModelName = inputModelName;
    }

    /**
     * @return the outputFiles
     */
    public String[] getOutputFiles() {
        
        String current = "";
                
        try {
            current   = new java.io.File( "." ).getCanonicalPath();
        } catch (IOException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }   
        
//        outputFiles[0] = current +"\\"+ jobsDirectory + "\\" + jobNumber + "\\" + "provLog.txt";
//        outputFiles[1] = current +"\\"+ jobsDirectory + "\\" + jobNumber + "\\" + "toeReg.nc";
//        outputFiles[2] = current +"\\"+ jobsDirectory + "\\" + jobNumber + "\\" + "toeMin.nc";
//        outputFiles[3] = current +"\\"+ jobsDirectory + "\\" + jobNumber + "\\" + "toePls.nc";   
        
        outputFiles[0] = current +"/"+ jobsDirectory + "/" + jobNumber + "/" + "provLog.txt";
        outputFiles[1] = current +"/"+ jobsDirectory + "/" + jobNumber + "/" + "toeReg.nc";
        outputFiles[2] = current +"/"+ jobsDirectory + "/" + jobNumber + "/" + "toeMin.nc";
        outputFiles[3] = current +"/"+ jobsDirectory + "/" + jobNumber + "/" + "toePls.nc"; 
        return outputFiles;
    }


}