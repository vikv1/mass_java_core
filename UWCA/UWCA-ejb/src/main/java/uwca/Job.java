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
    
    String[] massArgs = new String[]{"","","machinefile.txt","45454"};  
    int numProc = 1;
    int numThr = 2;
              
    
    /**
     * public constructor
     */
    public Job(){
        Random rn = new Random();
        jobNumber =  rn.nextInt(9999);
    }
    
    /**
     * Starts the main calculations for the job
     */
    public void executeJob(){
        variable.setArgs(massArgs, numProc, numThr, inputModel, jobNumber);
        variable.executeCalculations();
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
    
    public void calculate(){
        
    }
    
    private void writeDataToFile(){
        
        
    
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


}