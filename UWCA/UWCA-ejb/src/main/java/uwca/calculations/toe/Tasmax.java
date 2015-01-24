package uwca.calculations.toe;

import edu.uw.bothell.css.dsl.MASS.*;
//import MASS.Agents;
//import masstoe.calculations.toe.places.TasmaxPlace;
//import masstoe.climatemodels.Tasmax_1;
//import MASS.MASS;
//import MASS.Places;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import uwca.NetCdf;
import uwca.calculations.toe.agents.TasmaxAgent;
import uwca.calculations.toe.places.TasmaxPlace;
import uwca.climatemodels.ClimateModelInterface;
import uwca.climatemodels.Tasmax_1;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author jwoodrin
 */
public class Tasmax extends AbstractToe{    
    
    public int jobNumber;
    
    int x;
    int y;
    int z;
    
    Places places; // our data grid
    Agents agents;
    
 
    ClimateModelInterface inputClimateModel;
    
    // step 2 vars
    double[][] minHistTolVals;
    double[][] maxHistTolVals;            
    
    // step 4 vars
    double[][] slopes;
    double[][] confidenceInterval;
    
    // step 5 vars
    double[][] climatologies;
    double[][] slopeMinusConInt;
    double[][] slopePlusConInt;
    double[][] climaSlope1; // clima + reg slope
    double[][] climaSlope2; // clima + reg slope + con int
    double[][] climaSlope3; // clima + reg slope - con int
    
    // final vars
    int[][] toeReg;
    int[][] toePls;
    int[][] toeMin;
    
    // params
    float climateTempThreshold;
    double minMaxTol;
    int numOfYears;
    int toeThreshold;
    
    /**
     * Main constructor
     * Accepts params arguments from web page
     * @param params 
     */
    public Tasmax(String[] params){
        
//        climateTempThreshold = Float.parseFloat(params[0]);
//        minMaxTol = Double.parseDouble(params[1]);
//        numOfYears = Integer.parseInt(params[2]);
//        toeThreshold = Integer.parseInt(params[3]);
        
        climateTempThreshold = 30.0f;
        // min max tolerance will be specified by a percentage such ad 90%... which will translate
        // into +/- 5% from 100 (95%) and 0 (5%)
        // for instance, to get the 10% value below 80% would have to have been specified in the GUI
        minMaxTol = 0.10;
        numOfYears = 200;
        toeThreshold = 120;
    }
    
    /**
     * This method possibly un-neccessary
     * 
     * @param args
     * @param numProc
     * @param numThr 
     */
    public void setArgs(ClimateModelInterface inputModel, int jobNum){

        inputClimateModel = inputModel;
        jobNumber =  jobNum;  
        
        // create job directory
        File dir = new File("jobs/"+Integer.toString(jobNumber));
        dir.mkdirs();
        File tmp = new File(dir, "log.txt");
        try {
            tmp.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(Tasmax.class.getName()).log(Level.SEVERE, null, ex);
        }    
    }
    
    /**
     * Inits the Places and Agents for the MASS Tasmax calculation
     */
    private void massInit(){

        int interv = 0;

        int[][] grid = inputClimateModel.getDimensions();
        x = grid[0][0]; // longitude(east / west)
        y = grid[0][1]; // latitude (north / south)
        z = 150;        // time    
        
        x = 2;
        y = 2;
        
             
        
        // instanciate our places
        places = new Places(jobNumber, "uwca.calculations.toe.places.TasmaxPlace", (Object)interv, x, y, z);  
        // set the input climate model
  //      places.callAll(TasmaxPlace.setClimateModel, (Object)inputClimateModel);
   //     places.callAll(TasmaxPlace.setClimateModel);
        
        agents = new Agents(jobNumber, "uwca.calculations.toe.agents.TasmaxAgent", null, places, x * y * z); 
    }
    
    /**
     * STEP 1: Read NetCDF year into TasmaxPlaces
     *     Read in each year into a place by long (x) && lat (y) coordinates
     *     - each place in the time dimension will have 365 - 366 days
     *     Each place iterates through the 365-6 days to find days over threshold
     *     Each place iterates through the 365-6 days to find days over threshold (PARAM 1). The result will be     
     *     the number of days over threshold
     *     - one int # kept by place class output 
     */
    private void readDataIntoPlaces(){
  
        // first set the climate threshold
        places.callAll(TasmaxPlace.setClimateTempThreshold, this.climateTempThreshold);
        // set climate model
        // places.callAll(TasmaxPlace.setClimateModel, (Object)inputClimateModel);
        int[][] yearIndices =   inputClimateModel.findYearReadIndexes(); 
        this.readFullYear(x, y, z, yearIndices);
         //      this.readLocalizedYear();
    
    }
    
    /**
     * STEP 2:
     * Find Historical tolerance 1950 - 1999. Find the min / max %'s (PARAM 2 && 3)
     *  Find Historical tolerance 1950 - 1999. Find the min / max %'s (PARAM 2 && 3)
     *  OUTPUT: 2 2 dim array's (x * y) of min and max values
     *      - this output is used in the final step for finding the ToE
     */
    private void findHistoricalTolerance(){

        // AGENTS
        /**
         * We need to tell the agents where to start their journey for step 3
         */
        int[] dims = new int[2];
        dims[0] = x;
        dims[1] = y;
        agents.callAll(TasmaxAgent.setInitialHistoricalTolerancePosition, (Object)dims);
   
        // update agent statuses (need to do after each migration)
        agents.manageAll();            
        
        // get our historical min / max values
        for(int i = 0; i < 56; i++){
            agents.callAll(TasmaxAgent.gatherHistoricalTolerance);
            agents.manageAll();
        }
        agents.callAll(TasmaxAgent.calculateHistoricalTolerance, minMaxTol);
        agents.manageAll();  
        
        // return the values        
        Object[] historicalToleranceVals = (Object[])agents.callAll(TasmaxAgent.getHistoricalToleranceVals, new Object[x*y]);
        
        /**
         * The return objects contain 1-d int arrays containing
         * historicalToleranceVals[0] = min historical value
         * historicalToleranceVals[1] = max historical value
         * historicalToleranceVals[2] = agent place x index
         * historicalToleranceVals[3] = agent place y index
         */
        minHistTolVals = new double[x][y];
        maxHistTolVals = new double[x][y];
        for(int i = 0; i < historicalToleranceVals.length; i++){
            double[] vals = (double[]) historicalToleranceVals[i];
            int xIndex = (int)vals[2];
            int yIndex = (int)vals[3];
            // set our array vals
            minHistTolVals[xIndex][yIndex] = vals[0];
            maxHistTolVals[xIndex][yIndex] = vals[1];
        }
    }
    
    /**
     * STEP 3:
     * Find Climatology - 1980 - 2010 average 
     * -  1 2 dim (x*y) double array output representing the average days over threshold for 1980 - 2010
     */
    private void findClimatology(){

        agents.callAll(TasmaxAgent.setClimatologyInitPosition, 29);
        agents.manageAll();
        
        // get our historical min / max values
        for(int i = 0; i < 30; i++){
            agents.callAll(TasmaxAgent.gatherClimatologyValues);
            agents.manageAll();
        }
        // calculate our historical tolerance
        agents.callAll(TasmaxAgent.calculateClimatology);
        agents.manageAll();   
        
        // get and format the climatologies into a local array
        climatologies = new double[x][y];
        Object[] agentClimatologies = (Object[]) agents.callAll(TasmaxAgent.getClimatologyValue, new Object[x*y]);
        for(int i = 0; i < agentClimatologies.length; i++){
            int xIndex = i % x;
            int yIndex = i / x;
            climatologies[xIndex][yIndex] = (double)agentClimatologies[i];
        }
    }    
    
    /**
    * STEP 4:
    * LSR - Least Squared Regression
    *    For 2006-2099 range produce 3 2d arrays (full long and lat coordinates)
    *       - 2d array of slopes
    *       - 2d array of Confidence interval (Error term * tvalue) + slope
    *       - 2d array of Confidence interval (Error term * tvalue) - slope
    */
    private void leastSquaredRegression(){

        slopes = new double[x][y];
        slopePlusConInt = new double[x][y];
        slopeMinusConInt = new double[x][y];
        // set the initial position for step 5
        int lsrStartPosition = 2006 - inputClimateModel.getStartYear();
        int lsrEndPosition = z - 1;
        agents.callAll(TasmaxAgent.setInitialLsrPosition, (Integer)lsrStartPosition);
        // update agent statuses
        agents.manageAll();
        // move the agents along the z axis and gather values
        for(int i = 0; i < lsrEndPosition; i++){
            agents.callAll(TasmaxAgent.gatherLsrValue);
            agents.callAll(TasmaxAgent.migrateZDimension);
            agents.manageAll();            
        }        

        // calculate the lsr values
        agents.callAll(TasmaxAgent.calculateLsrValues);
        
        // get slope values
        Object[] agentSlopes = (Object[]) agents.callAll(TasmaxAgent.getSlopes, new Object[x*y]);
        // get confidence
        Object[] agentErrorTerm = (Object[]) agents.callAll(TasmaxAgent.getErrorTerm, new Object[x*y]);
         
        int cnt = 0;
        
        for(int i = 0; i < agentSlopes.length; i++){
            
            int xIndex = i % x;
            int yIndex = i / x;
            slopes[xIndex][yIndex] = (double)agentSlopes[i];
            
            double confidenceInterval = 
            
            slopePlusConInt[xIndex][yIndex] = (double)agentSlopes[i] + ((double)agentErrorTerm[i] * inputClimateModel.getTvalue());    
            slopeMinusConInt[xIndex][yIndex] = (double)agentSlopes[i] - ((double)agentErrorTerm[i] * inputClimateModel.getTvalue());
        }  
    }
    
    /**
     * STEP 5
     */
    private void climatologyManipulations(){

        /**
         * 
         * 6. Confidence / Slope arrays / add climatology
         *      6a. 2d array of just regular slopes (output from 5a)
         *      6b. 2d array of slope + confidence interval
         *      6c. 2d array of slope - confidence interval
         */

        slopeMinusConInt = new double[x][y];
        slopePlusConInt = new double[x][y];
        climaSlope1 = new double[x][y]; // clima + reg slope
        climaSlope2 = new double[x][y]; // clima + reg slope + con int
        climaSlope3 = new double[x][y]; // clima + reg slope - con int
        for(int i = 0; i < x; i++){
            for(int k = 0; k < y; k++){
                
                slopeMinusConInt[i][k] = slopes[i][k] - confidenceInterval[i][k];
                slopePlusConInt[i][k] = slopes[i][k] + confidenceInterval[i][k];
                
                climaSlope1[i][k] = slopes[i][k] + climatologies[i][k];
                climaSlope2[i][k] = slopePlusConInt[i][k] - confidenceInterval[i][k] + climatologies[i][k];
                climaSlope3[i][k] = slopeMinusConInt[i][k] + confidenceInterval[i][k] + climatologies[i][k];
            }        
        }
    }
    
    /**
     * STEPS 6
     */
    private void findToe(){
           /**
         * STEP 7
         * Add slope for each year (specify # of years (PARAM 4)
         */
        /**
         * STEP 8: Find ToE
         * ToE
         *    3 2d array output of what year temperature rises above user defined threshold (PARAM 5)
         */
        
        double[][][] reg = new double[x][y][numOfYears];
        double[][][] min = new double[x][y][numOfYears];
        double[][][] pls = new double[x][y][numOfYears];
                
        toeReg = new int[x][y];
        toePls = new int[x][y];
        toeMin = new int[x][y];
        
        for(int i = 0; i < x; i++){
            for(int k = 0; k < y; k++){
                for(int j = 0; j < numOfYears; j++){
                    if(j == 0){
                        reg[i][k][j] = climaSlope1[i][k];
                        pls[i][k][j] = climaSlope2[i][k];
                        min[i][k][j] = climaSlope3[i][k];
                        
                        if(reg[i][k][j] >= toeThreshold)
                            toeReg[i][k] = 1950 + j;
                        else 
                            toeReg[i][k] = 0;
                        if(pls[i][k][j] >= toeThreshold)
                            toePls[i][k] = 1950 + j;
                        else 
                            toePls[i][k] = 0;
                        if(min[i][k][j] >= toeThreshold)
                            toeMin[i][k] = 1950 + j;
                        else 
                            toeMin[i][k] = 0;
                    }
                    else{
                        reg[i][k][j] = reg[i][k][j-1] + slopes[i][k];
                        pls[i][k][j] = reg[i][k][j-1] + slopePlusConInt[i][k];
                        min[i][k][j] = reg[i][k][j-1] + slopeMinusConInt[i][k];
                        
                        if(reg[i][k][j] != 0 && reg[i][k][j] >= toeThreshold)
                            toeReg[i][k] = 1950 + j;
                        if(pls[i][k][j] != 0 && pls[i][k][j] >= toeThreshold)
                            toePls[i][k] = 1950 + j;
                        if(min[i][k][j] != 0 && min[i][k][j] >= toeThreshold)
                            toeMin[i][k] = 1950 + j;
                        
                        
                    }
                }
            }        
        }    
    }
    
    /**
     * The main method which drives our calculations
     */
    public void executeCalculations(){
        // init MASS
        massInit();   
        
        // step 1 && 2 read data
    //    readDataIntoPlaces();
//        places.callAll(TasmaxPlace.falsifyDaysOverThreshold);
//        // step 3
//        findHistoricalTolerance();
//        // step 4
//        findClimatology();
//        // step 5
//        leastSquaredRegression();
//        // step 6
//        climatologyManipulations();
//        // step 7 && 8
//        findToe();
        
        this.placesTest();
        this.agentTest();
        
        /**
         * write the netcdf data to file
         */
        NetCdf fileWriter = new NetCdf();
//        fileWriter.writeToeFile("jobs/"+jobNumber+"/toeReg.nc", x, y, toeReg);
//        fileWriter.writeToeFile("jobs/"+jobNumber+"/toePls.nc", x, y, toePls);
//        fileWriter.writeToeFile("jobs/"+jobNumber+"/toeMin.nc", x, y, toeMin);

 
    }
    
    /**
     * Tester method for Places
     */
    public void placesTest(){
        
        Object[] placesIndexes = (Object[])places.callAll(TasmaxPlace.findHostName, new Object[x * y * z]);
    
        String s = "";
    
    }
    
    /**
     * Tester method for Agents
     */
    public void agentTest(){
        
        Object[] agentIndexes = (Object[])agents.callAll(TasmaxAgent.getPlaceIndex, new Object[x*y*z]);
        
        String s = "";
    }
    
    /**
     * reads in an entire lat * long * 365-6 data points and distributes the array to the places
     * @param x - longitude dimension
     * @param y - latitude dimension
     * @param z - time dimension
     */
    public void readFullYear(int x, int y, int z, int[][] yearIndices){
       
        int numYears = inputClimateModel.getNumYears();
        int startYear = inputClimateModel.getStartYear();
        // loop through the entire range of years
        for(int i = 0; i < numYears; i++){
            
            Object obj = inputClimateModel.readFullYear(x, y, i, yearIndices);
  
            float[][][] tempVals = (float[][][])obj;
            float[] yearData = tempVals[0][0];
            int year = i + startYear;
            Object[] placesArgs = new Object[x*y*z]; // the args we'll be sending to the places
            for(int a = 0; a < x; a++){
                for(int b = 0; b < y; b++){
                  /**
                   * We need to take each x&y coordinate from our temp values and put them
                   * into the proper index within a 1d object array to be sent to the proper places.
                   * The formula is:
                   * Index of array = m *z
                   * z = the number of elements in the z dimension of the array
                   * m = (xindex * ymax) + (yindex % ymax)
                   */
                    int m = (a * y) + b;
                    //  int m = (a * y) + (b % y);
                    int ind = (m * z) + i; // i being the year index modifier
                    try{
                    placesArgs[ind] = tempVals[a][b];
                    }catch(Exception e){
                        String s = e.toString();
                    }                    
                }
            }
            // set the temp year values
            places.callAll(TasmaxPlace.setDaysArray, placesArgs);
        }
    }
    
    /**
     * Uses the TasmaxPlace method to read in the 365-6 day float arrays from within each Place
     * This method is set up to read in certain slices of the time dimension (z) at a time to improve performance.
     */
    public void readLocalizedYear(){
        int numYears = inputClimateModel.getDimensions()[0][2];
        for(int i = 0 ; i < numYears; i++){   
            places.callAll(TasmaxPlace.readNetCdfDataFullYear, i);
        }
    }
}
