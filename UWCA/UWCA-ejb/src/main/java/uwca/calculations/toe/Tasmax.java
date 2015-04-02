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
import org.joda.time.DateTime;
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
    
    private final int TOE_NULL_VAL = 0;

    // step 2 vars
    private double[][] minHistTolVals;
    private double[][] maxHistTolVals;            
    
    // step 4 vars
    private double[][] slopes;
    private double[][] confidenceInterval;
    
    // step 5 vars
    private double[][] climatologies;
    private double[][] slopeMinusConInt;
    private double[][] slopePlusConInt;
    private double[][][] climaSlope1;
    private double[][][] climaSlope2;
    private double[][][] climaSlope3;
    // the start year for the first year toe calculation
    private static final int TOE_START_YEAR = 2001;
    
    // final vars
    private int[][] toeReg;
    private int[][] toePls;
    private int[][] toeMin;
    
    // params
    private float climateTempThreshold;
    private double minMaxTol;
   
    private int toeThreshold;
    
    private String jobsDirectory = "";
    
    
    /**
     * Main constructor
     * Accepts params arguments from web page
     * @param params 
     */
    public Tasmax(String[] params, String jobsDir){
        
        jobsDirectory = jobsDir;
        
        try{
            climateTempThreshold = Float.parseFloat(params[0]);
        }catch(Exception e){
            climateTempThreshold = 18.3F;
        }
        
        try{
            minMaxTol = Double.parseDouble(params[1]);
            if(minMaxTol > 1.00D || minMaxTol < 0.00D) minMaxTol = 0.60D;
        }catch(Exception e){
            minMaxTol = 0.60D;
        }
        
        try{
            numOfYears = Integer.parseInt(params[2]);
        }catch(Exception e){
            numOfYears = 200;
        }
    }
    
    /**
     * Inits the Places and Agents for the MASS Tasmax calculation
     */
    public void massInit(){

        int interv = 0;

        int[][] grid = inputClimateModel.getDimensions();
        x = grid[0][0]; // longitude(east / west)
        y = grid[0][1]; // latitude (north / south)
        z = 150;        // time    
        
        x = 5;
        y = 5;
        z = 5;
    
        String msg = " Init TasmaxPlace Places size  x:" + Integer.toString(x) + " y:"  + Integer.toString(y) + " z:"  + Integer.toString(z);
        this.getProvLogger().logProvenance(msg);
        
        // instanciate our places
        places = new Places(jobNumber, "uwca.calculations.toe.places.TasmaxPlace", (Object)interv, x, y, z);  
        
        msg =  " Init TasmaxAgent Agents size  x:" + Integer.toString(x) + " y:"  + Integer.toString(y);
        this.getProvLogger().logProvenance(msg);
        
        agents = new Agents(jobNumber, "uwca.calculations.toe.agents.TasmaxAgent", null, places, x * y); 
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
        /**
         * Old master node read algorithm
         */
        /*
        String msg = "STEP 1 STARTED:  Find days over threshold ";
        this.getProvLogger().logProvenance(msg);
  
        msg = " Setting Climate Threshold: " + Double.toString(this.climateTempThreshold);
        this.getProvLogger().logProvenance(msg);
        // first set the climate threshold
        places.callAll(TasmaxPlace.setClimateTempThreshold, this.climateTempThreshold);
        // set climate model
        // places.callAll(TasmaxPlace.setClimateModel, (Object)inputClimateModel);
      
        msg = " Finding year indexes ";
        this.getProvLogger().logProvenance(msg);
        int[][] yearIndices =   inputClimateModel.findYearReadIndexes(); 
        
        msg = " Finding year indexes ended, starting incremental netcdf data read";
        this.getProvLogger().logProvenance(msg);
        this.readFullYear(x, y, z, yearIndices); // method 1 -- read from master node
        */        
        
        /**
         * New method for doing place reading
         */
        String msg = "STEP 1 STARTED:  Find days over threshold ";
        this.getProvLogger().logProvenance(msg);
        // first set the climate threshold
        msg = " Setting Climate Threshold: " + Double.toString(this.climateTempThreshold);
        this.getProvLogger().logProvenance(msg);        
        places.callAll(TasmaxPlace.setClimateTempThreshold, this.climateTempThreshold);
        // find the year indices
        msg = " Finding year indexes ";
        this.getProvLogger().logProvenance(msg);
        int[][] yearIndices =   inputClimateModel.findYearReadIndexes(); 
        // perform the places read
        msg = " Finding year indexes ended, starting incremental netcdf data read";
        this.getProvLogger().logProvenance(msg);        
        this.placesRead(yearIndices);
    }
    
    /**
     * Uses the TasmaxPlace method to read in the 365-6 day float arrays from within each Place
     * This method is set up to read in certain slices of the time dimension (z) at a time to improve performance.
     */
    public void placesRead(int[][] yearIndices){
        // the places will need the climate model for this alg, so send it
    //    places.callAll(TasmaxPlace.setClimateModel, (Object)inputClimateModel);
        
        // init the node based places map array
        places.callAll(TasmaxPlace.initMapPlacesStripeOnNode);
        // find the mapping of places to node
        places.callAll(TasmaxPlace.mapPlacesStripeOnNode);        
        // set the year indexes 
        places.callAll(TasmaxPlace.setYearIndexArray, (Object)yearIndices);
        // read in file chunks specific to computing node          
        places.callAll(TasmaxPlace.readNetCdfData);
        // read in the days values into the individual places and find days over threshold
        places.callAll(TasmaxPlace.individualPlaceRead);
    }
    
    /**
     * STEP 2:
     * Find Historical tolerance 1950 - 1999. Find the min / max %'s (PARAM 2 && 3)
     *  Find Historical tolerance 1950 - 1999. Find the min / max %'s (PARAM 2 && 3)
     *  OUTPUT: 2 2 dim array's (x * y) of min and max values
     *      - this output is used in the final step for finding the ToE
     */
    private void findHistoricalTolerance(){
        String msg = "STEP 2 STARTED:  Finding historical Tolerances Parm Tolerance used: " + Double.toString(minMaxTol);
        this.getProvLogger().logProvenance(msg);
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
       /**
         * Old non-async agent migration code
         */
        /*
        // get our historical min / max values
        for(int i = 0; i < 50; i++){
            agents.callAll(TasmaxAgent.gatherHistoricalTolerance);
            agents.manageAll();
        }
        */
        
        /**
         * New agent async migration code
         */
        int[] funcIds = new int[50];
        for(int i = 0; i < 50; i++){
            funcIds[i] = TasmaxAgent.gatherHistoricalTolerance;
        }
        Object[] agentArgs = new Object[x*y];
        try {
          agents.callAllAsync(funcIds, agentArgs);
        }catch(Throwable e) {
            String error = e.toString();            
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
        
        String msg = "STEP 3 STARTED:  Find climatology ";
        this.getProvLogger().logProvenance(msg);

        agents.callAll(TasmaxAgent.setClimatologyInitPosition, 29);
        agents.manageAll();
        
//        // get our historical min / max values
//        for(int i = 0; i < 30; i++){
//            agents.callAll(TasmaxAgent.gatherClimatologyValues);
//            agents.manageAll();
//        }
        int climatologyYears = 30;
        int[] funcIds = new int[climatologyYears];
        for(int i = 0; i < climatologyYears; i++){
            funcIds[i] = TasmaxAgent.gatherClimatologyValues;
        }
        Object[] agentArgs = new Object[x*y];
        try {
          agents.callAllAsync(funcIds, agentArgs);
        }catch(Throwable e) {
            String error = e.toString();            
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
        
        String msg = "STEP 4 STARTED:  LSR - Least Squared Regression ";
        this.getProvLogger().logProvenance(msg);

        slopes = new double[x][y];
        slopePlusConInt = new double[x][y];
        slopeMinusConInt = new double[x][y];
        // set the initial position for step 5
        int lsrStartPosition = 2006 - inputClimateModel.getStartYear();
        int lsrEndPosition = z - 1;
        agents.callAll(TasmaxAgent.setInitialLsrPosition, (Integer)lsrStartPosition);
        // update agent statuses
        agents.manageAll();
//        // move the agents along the z axis and gather values
//        for(int i = 0; i < lsrEndPosition; i++){
//            agents.callAll(TasmaxAgent.gatherLsrValue);
//            agents.callAll(TasmaxAgent.migrateZDimension);
//            agents.manageAll();            
//        }
        
            
        int[] funcIds = new int[lsrEndPosition * 2];
        for(int i = 0; i < lsrEndPosition * 2; i += 2){
            funcIds[i] = TasmaxAgent.gatherLsrValue;
            funcIds[i + 1] = TasmaxAgent.migrateZDimension;
        }
        Object[] agentArgs = new Object[x*y];
        try {
          agents.callAllAsync(funcIds, agentArgs);
        }catch(Throwable e) {
            String error = e.toString();            
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
//            double agentSlope = Math.abs((double)agentSlopes[i]);
//            double agentErrTerm = Math.abs((double)agentErrorTerm[i]);
            double agentSlope = (double)agentSlopes[i];
            double agentErrTerm = (double)agentErrorTerm[i];
            double tval = inputClimateModel.getTvalue();
            slopes[xIndex][yIndex] = agentSlope;   
            slopePlusConInt[xIndex][yIndex] = agentSlope + (agentErrTerm * tval);    
            slopeMinusConInt[xIndex][yIndex] = agentSlope - (agentErrTerm * tval);
        }  
    }
    
    /**
     * STEP 5 Find ToE
     * Take the 3 output arrays from step 4 and expand them into 3d arrays which we will use to find our ToE in step 6
     */
    private void findToe(){
        String msg = "STEP 5 STARTED:  Find ToE, Number of years Param used: " + Integer.toString(numOfYears);
        this.getProvLogger().logProvenance(msg);
        
        climaSlope1 = new double[x][y][numOfYears]; // z[0] = clima; z[1] = z[0] + slopes; z[2] = z[1] + slopes; ... ect
        climaSlope2 = new double[x][y][numOfYears]; // z[0] = clima; z[1] = z[0] + slopePlusConInt; z[2] = z[1] + slopePlusConInt; ... ect
        climaSlope3 = new double[x][y][numOfYears]; // z[0] = clima; z[1] = z[0] + slopeMinusConInt; z[2] = z[1] + slopeMinusConInt; ... ect
        
        for(int i = 0; i < x; i++){
            for(int k = 0; k < y; k++){
                // assigning extra vars here for debugging / readability purposes
                double clima1;
                double clima2;
                double clima3;
                
                double maxHist = maxHistTolVals[i][k];
                double minHist = minHistTolVals[i][k];
                // if min and max tolerances are 0, then set the toe to 0
                if(maxHist == 0 && minHist == 0){
                    toeReg[i][k] = 0;
                    toePls[i][k] = 0;
                    toeMin[i][k] = 0;
                }
                
                for(int j = 0; j < numOfYears; j++){
                    
                    if(j == 0){
                        clima1 = climaSlope1[i][k][j] = climatologies[i][k];
                        clima2 = climaSlope2[i][k][j] = climatologies[i][k];
                        clima3 = climaSlope3[i][k][j] = climatologies[i][k];
                    }else{
                        clima1 = climaSlope1[i][k][j] = climaSlope1[i][k][j-1] + slopes[i][k];
                        clima2 = climaSlope2[i][k][j] = climaSlope2[i][k][j-1] + slopePlusConInt[i][k];
                        clima3 = climaSlope3[i][k][j] = climaSlope3[i][k][j-1] + slopeMinusConInt[i][k];
                    }
          
                    // FIND TOE
                    // first array
                    if(clima1 >  maxHist && toeReg[i][k] == TOE_NULL_VAL){
                        toeReg[i][k] = TOE_START_YEAR + j;
                    }else if(clima1 <  minHist && toeReg[i][k] == TOE_NULL_VAL){
                  //      toeReg[i][k] = (TOE_START_YEAR + j) * -1;
                    }
                    // second array
                    if(clima2 >  maxHist && toePls[i][k] == TOE_NULL_VAL){
                        toePls[i][k] = TOE_START_YEAR + j;
                    }else if(clima2 <  minHist && toePls[i][k] == TOE_NULL_VAL){
                 //       toePls[i][k] = (TOE_START_YEAR + j) * -1;
                    }
                    // third array
                    if(clima3 >  maxHist && toeMin[i][k] == TOE_NULL_VAL){
                        toeMin[i][k] = TOE_START_YEAR + j;
                    }else if(clima3 <  minHist && toeMin[i][k] == TOE_NULL_VAL){
                  //      toeMin[i][k] = (TOE_START_YEAR + j) * -1;
                    }
                }
            }        
        }
    }
    
    /**
     * Inits the ToE arrays with 0 values
     */
    private void initToeArrays(){
        
        toeReg = new int[x][y];
        toePls = new int[x][y];
        toeMin = new int[x][y];
    
        for(int i = 0; i < x; i++){
            for(int k = 0; k < y; k++){
                toeReg[i][k] = TOE_NULL_VAL;
                toePls[i][k] = TOE_NULL_VAL;
                toeMin[i][k] = TOE_NULL_VAL;
            }
        }    
    }
    
    /**
     * The main method which drives our calculations
     */
    public void executeCalculations(){
        // init MASS
        massInit();   
//        initToeArrays();
//        // step 1 read data
 //       readDataIntoPlaces();
// //       places.callAll(TasmaxPlace.falsifyDaysOverThreshold); // temp method for testing (speeds up performance)
//        // step 2
//        findHistoricalTolerance();
//        // step 3
//        findClimatology();
//        // step 4
//        leastSquaredRegression();
//        // step 5
//        findToe();  
 
        //this.placesTest();
        
//        places.callAll(TasmaxPlace.setNumberOfNodes, (Object)1);
//        
//         int[][] yearIndices =   inputClimateModel.findYearReadIndexes(); 
//        // set the year indexes 
//        places.callAll(TasmaxPlace.setYearIndexArray, (Object)yearIndices);
//        Object[] objs = (Object[])places.callAll(TasmaxPlace.netCdfReadTest, new Object[x*y*z]);
       
        places = null;
        agents = null;
        
        
 
    }
    
    public void writeNetCdfFiles(String toeRegFile, String toeMinFile, String toeMaxFile){
        NetCdf fileWriter = new NetCdf();
        fileWriter.writeToeFile(toeRegFile, x, y, toeReg);
        fileWriter.writeToeFile(toeMaxFile, x, y, toePls);
        fileWriter.writeToeFile(toeMinFile, x, y, toeMin);
        
        String msg =  " Writting files, toeReg, toePls, and toeMin to jobs folder " ;
        this.getProvLogger().logProvenance(msg);
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
            int year = i + startYear;
            
            String msg = " Reading Year: " + Integer.toString(year);
            this.getProvLogger().logProvenance(msg);
            
            Object obj = inputClimateModel.readFullYear(x, y, i, yearIndices);
            
  
            float[][][] tempVals = (float[][][])obj;
            float[] yearData = tempVals[0][0];
            
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
            tempVals = null;
            placesArgs = null;
        }
    }    
    /******************************************************************************************************************
     * Test methods to get place and agent information to verify mass is working across cluster
     *****************************************************************************************************************/
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
        
        Object[] agentIndexes = (Object[])agents.callAll(TasmaxAgent.getPlaceIndex, new Object[x*y]);
        
        String s = "";
    }
}
