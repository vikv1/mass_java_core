package uwca.calculations.toe;

import edu.uw.bothell.css.dsl.MASS.Agents;
import edu.uw.bothell.css.dsl.MASS.Places;
import uwca.climatemodels.ClimateModelInterface;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author jwoodrin
 */
public abstract class AbstractToe implements ToeInterface{

    public int jobNumber;
    
    int x;
    int y;
    int z;
    
    public int numOfYears;
    
    Places places; // our data grid
    Agents agents;   
    
    ClimateModelInterface inputClimateModel;
    
    /**
     * 
     * 
     * @param args
     * @param numProc
     * @param numThr 
     */
    public void setArgs(ClimateModelInterface inputModel, int jobNum){
        inputClimateModel = inputModel;
        jobNumber =  jobNum;    
    }
    
    abstract void massInit();
    
    public int getNumToeYears(){
        return numOfYears;
    }
    
    @Override
    public abstract void executeCalculations();
    
    public abstract void placesTest();

    public abstract void agentTest();
}
