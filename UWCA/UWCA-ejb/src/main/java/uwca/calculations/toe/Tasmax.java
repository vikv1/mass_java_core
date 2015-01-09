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
    
    private String[] massArgs;
    private int numProcesses;
    private int numThreads;
    
    int x;
    int y;
    int z;
    
    Places places; // our data grid
    Agents agents;
    
 //   ClimateModelInterface inputClimateModel = new Tasmax_1();
    ClimateModelInterface inputClimateModel;
    // step 5 vars
    double[][] slopes;
    double[][] confidenceInterval;
    
    // step 6 vars
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
    float climateThreshold;
    double minMaxTol;
    int numOfYears;
    int toeThreshold;
    
    /**
     * Main constructor
     * Accepts params arguments from web page
     * @param params 
     */
    public Tasmax(String[] params){
        
//        climateThreshold = Float.parseFloat(params[0]);
//        minMaxTol = Double.parseDouble(params[1]);
//        numOfYears = Integer.parseInt(params[2]);
//        toeThreshold = Integer.parseInt(params[3]);
        
        climateThreshold = 30.0f;
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
    public void setArgs(String[] args, int numProc, int numThr, ClimateModelInterface inputModel, int jobNum){
       
        jobNumber =  jobNum;
    
        massArgs = args;
        numProcesses = numProc;
        numThreads = numThr;    
        numProcesses = 3;
        numThreads = 1;   
        
        inputClimateModel = inputModel;
        
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
    
    private void massInit(){
    //    String massLib = "apachemath-3.3.3.jar";
     //   String filePath = "C:\\Users\\jwoodrin\\Documents\\NetBeansProjects\\MASS\\UWCA\\temp\\nodes.xml";
  //      MASS.addLibrary(massLib);
    //    MASS.setNodeFilePath(filePath);
        MASS.init();
       
        
   //    MASS.init(massArgs, numProcesses, numThreads);
        int interv = 0;

        int[][] grid = inputClimateModel.getDimensions();
        x = grid[0][0]; // lat
        y = grid[0][1]; // long
        z = 150;        // time     
        
        x = 10; // longitude(east / west)
        y = 5; // latitude (north / south)
//        z = 150; // time      
        
        // instanciate our places
        places = new Places(1, "uwca.calculations.toe.places.TasmaxPlace", (Object)interv, x, y, z);  
        
        agents = new Agents(2, "uwca.calculations.toe.agents.TasmaxAgent", null, places, x * y); 
    }
    
    /**
     * STEP 1: Read NetCDF year into TasmaxPlaces
     *     Read in each year into a place by lat && long coordinates
     *    - each place in the time dimension will have 365 - 366 days
     */
    private void readDataIntoPlaces(){
  
        // set climate model
        // places.callAll(TasmaxPlace.setClimateModel, (Object)inputClimateModel);
        int[][] yearIndices =   inputClimateModel.findYearReadIndexes();        
        this.readFullYear(x, y, z, yearIndices);
         //      this.readLocalizedYear();
    
    }
    
    /**
      * STEP 2: Each place iterates through the 365-6 days to find days over threshold
      *  Each place iterates through the 365-6 days to find days over threshold (PARAM 1). The result will be     
      *    the number of days over threshold
      *    - one int # kept by place class output
      */
    private void calculateDaysOverThreshold(){ 
        places.callAll(TasmaxPlace.calculateDaysOverThreshold);
    }
    
        /**
     * STEP 3:
     * Find Historical tolerance 1950 - 1999. Find the min / max %'s (PARAM 2 && 3)
     *  Find Historical tolerance 1950 - 1999. Find the min / max %'s (PARAM 2 && 3)
     *    - 2 double min / max number output
     */
    private void findHistoricalTolerance(){

        // AGENTS        
        // set the initial position for the agents to begin step 3 calculations
        agents.callAll(TasmaxAgent.decideInitialPosition, new int[]{x, y});
        // update agent statuses
        agents.manageAll();            
        
        // get our historical min / max values
        for(int i = 0; i < 56; i++){
            agents.callAll(TasmaxAgent.gatherHistoricalTolerance);
            agents.manageAll();
        }
        agents.callAll(TasmaxAgent.calculateHistoricalTolerance, minMaxTol);
        agents.manageAll();  
    }
    
    /**
     * STEP 4:
     * Find Climatology - 1980 - 2010 average 
     * - 2 dim array output
     */
    private void findClimatology(){

        agents.callAll(TasmaxAgent.setClimatologyInitPosition);
        agents.manageAll();
        
        // get our historical min / max values
        for(int i = 0; i < 30; i++){
            agents.callAll(TasmaxAgent.gatherClimatologyValues);
            agents.manageAll();
        }
        // calculate our historical tolerance
        agents.callAll(TasmaxAgent.calculateClimatology);
        agents.manageAll();    
    }    
    
    /**
    * STEP 5:
    * LSR - Least Squared Regression
    *    For 2006-2099 range produce 2 2d arrays (full lat and long coordinates)
    *      5a. 2d array of slopes (use the 150 years and apache package)
    *      5b. 2d array of Confidence Interval (rstdx * tvalue)
    */
    private void leastSquaredRegression(){

        slopes = new double[x][y];
        confidenceInterval = new double[x][y];
        // set the initial position for step 5
        agents.callAll(TasmaxAgent.decideInitialPosition, new int[]{x, y});
        // update agent statuses
        agents.manageAll();  
        // move the agents along the z axis and gather values
        for(int i = 0; i < 150; i++){
            agents.callAll(TasmaxAgent.gatherLsrValue);
            agents.callAll(TasmaxAgent.migrateZDimension);
            agents.manageAll();
            
        }        
        // now that the agents have found the values, lets have the agents return the values
      //  Object[] lsrValues = (Object[]) agents.callAll(TasmaxAgent.getLsrValues, new Object[x*y]);
        
        // calculate the lsr values
        agents.callAll(TasmaxAgent.calculateLsrValues);
        
        // get slope values
        Object[] agentSlopes = (Object[]) agents.callAll(TasmaxAgent.getSlopes, new Object[x*y]);
        // get confidence
        Object[] agentConInterval = (Object[]) agents.callAll(TasmaxAgent.getConInterval, new Object[x*y]);
         
        int cnt = 0;
        
        for(int i = 0; i < agentSlopes.length; i++){
            
            int xIndex = i % x;
            int yIndex = i / x;
            slopes[xIndex][yIndex] = (double)agentSlopes[i];
            confidenceInterval[xIndex][yIndex] = (double)agentConInterval[i];
            
        }  
    }
    
    /**
     * STEP 6
     */
    private void climatologyManipulations(){

        /**
         * STEP 6:
         * 6. Confidence / Slope arrays / add climatology
         *      6a. 2d array of just regular slopes (output from 5a)
         *      6b. 2d array of slope + confidence interval
         *      6c. 2d array of slope - confidence interval
         */
        // get and format the climatologies
        climatologies = new double[x][y];
        Object[] agentClimatologies = (Object[]) agents.callAll(TasmaxAgent.getClimatologyValue, new Object[x*y]);
        for(int i = 0; i < agentClimatologies.length; i++){
            int xIndex = i % x;
            int yIndex = i / x;
            climatologies[xIndex][yIndex] = (double)agentClimatologies[i];
        }
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
     * STEPS 7 && 8
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
        // step 1 read data
//        readDataIntoPlaces();
//        // step 2 find days over threshold
//        calculateDaysOverThreshold();
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
        fileWriter.writeToeFile("jobs/"+jobNumber+"/toeReg.nc", x, y, toeReg);
        fileWriter.writeToeFile("jobs/"+jobNumber+"/toePls.nc", x, y, toePls);
        fileWriter.writeToeFile("jobs/"+jobNumber+"/toeMin.nc", x, y, toeMin);


        // end mass
        MASS.finish();
    }
    
    public void placesTest(){
        
        Object[] placesIndexes = (Object[])places.callAll(TasmaxPlace.findHostName, new Object[x * y * z]);
    
        String s = "";
    
    }
    
    public void agentTest(){
        
        Object[] agentIndexes = (Object[])agents.callAll(TasmaxAgent.getPlaceIndex, new Object[x*y]);
        
        String s = "";
    }
    
    /**
     * reads in an entire lat * long * 365-6 data points and distributes the array to the places
     * @param x - longitude dimension
     * @param y - latitude dimension
     * @param z - time dimension
     */
    public void readFullYear(int x, int y, int z, int[][] yearIndices){
        Tasmax_1 reader = new Tasmax_1();        
        for(int i = 0; i < 150; i++){
            
            Object obj = reader.readFullYear(i, yearIndices);
  
            float[][][] tempVals = (float[][][])obj;
            float[] yearData = tempVals[0][0];
            int year = i + 1950;
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
                    int m = (a * y) + (b % y);
                    int ind = m * z;
                    placesArgs[ind] = tempVals[a][b];
                    
                }
            }
            places.callAll(TasmaxPlace.setDaysArray, placesArgs);

            System.out.println("Year " + year + " Array size: " + tempVals.length);
   
        }
    }
    
    /**
     * Uses the TasmaxPlace method to read in the 365-6 day float arrays from within each Place
     * This method is set up to read in certain slices of the time dimension (z) at a time to improve performance.
     */
    public void readLocalizedYear(){
        for(int i = 0 ; i < 150; i++){   
            places.callAll(TasmaxPlace.readNetCdfDataFullYear, i);
        }
    }
}
