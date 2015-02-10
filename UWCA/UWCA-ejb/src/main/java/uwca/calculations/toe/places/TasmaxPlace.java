package uwca.calculations.toe.places;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import uwca.climatemodels.Tasmax_1;
import edu.uw.bothell.css.dsl.MASS.*;
import java.net.InetAddress;
//import java.util.Calendar;
//import java.util.Date;
//import ucar.ma2.Array;
//import ucar.ma2.Index;
//import ucar.nc2.Attribute;
//import ucar.nc2.NetcdfFile;
//import ucar.nc2.Variable;
//import ucar.nc2.units.DateUnit;
import java.util.Random;
import uwca.climatemodels.ClimateModelInterface;



/**
 *
 * @author jwoodrin
 */
public class TasmaxPlace extends Place{
    
    /**
     * Step 1 && 2 variables
     */
    // our days over threshold value which each place is responsible for 
    public int daysOverThreshold = 0;
    private float climateTempThreshold = 0;    
    private float[] daysTemps;  
    // used to mark the location of year beginnings and endings in files
    private int[][] yearIndices;

    
    /**
     * Step 3 variables
     * historicalThresholds[0] = min value
     * historicalThresholds[1] = max value
     */
    private double[] historicalThresholds;
    
    /**
     * The input climate model
     * This variable is only necessary for when you do reading in from the individual Places
     */
    private ClimateModelInterface inputClimateModel = null;
  //   private Tasmax_1 inputClimateModel = null;
  
    private int interval;      
    /**
     * Methods that are callable from callAll
     */
    public static final int readNetCdfData = 1;
    public static final int setClimateModel = 2;
    public static final int getDaysOverThreshold = 4;
    public static final int readNetCdfDataFullYear = 5;
    public static final int falsifyDaysOverThreshold = 10;
    public static final int findHostName = 20;
    public static final int setDaysArray = 11;
    public static final int calculateDaysOverThreshold = 12;
    public static final int setClimateTempThreshold = 21;
    public static final int setYearIndexArray = 22;
      
    /**
     * public constructor
     * @param interval 
     */
    public TasmaxPlace(Object interval){
        this.interval = ( ( Integer)interval ).intValue();
        
    }
    
    /**
     * 
     * @param i the method to be called
     * @param o the method parameters
     * @return 
     */
    public Object callMethod(int method, Object o){
        switch(method){
            case setClimateModel:
                return setInputClimateModel(o);       
            case getDaysOverThreshold:
                return getDaysOverThreshold(o);
            case falsifyDaysOverThreshold:
                return falsifyDaysOverThreshold(o);
            case readNetCdfDataFullYear:
                return readNetCdfDataFullYear(o);
            case findHostName:
                return findHostName(o);
            case setDaysArray:
                return setDaysArray(o);
            case calculateDaysOverThreshold:
                return calculateDaysOverThreshold(o);     

            case setClimateTempThreshold:
                return setClimateTempThreshold(o);
            case setYearIndexArray:
                return setYearIndexArray(o);
            default:
                return null;              
        }
    }
    
    /**
     * Sets the climate model at the place level. 
     * @param o The climate model variable we will be using
     * @return 
     */
    public Object setInputClimateModel(Object o){
        inputClimateModel = (ClimateModelInterface)o;
        
     //   inputClimateModel = o;
   //     inputClimateModel = new Tasmax_1();
        return null;
    }   
    
    /******************************************************************************************************************
     * STEP 1: Find days over temperature threshold
     *****************************************************************************************************************/
    /**
     * Part of STEP 1: sets the 365-6 days array of values to be processed into management variable
     * @param o
     * @return 
     */
    public Object setDaysArray(Object o){
        if(o != null){
            daysTemps = (float[])o;
            calculateDaysOverThreshold(new Object());
            daysTemps = null;
            /**
             * Next section for de-bugging
             */
            String ind = Integer.toString(this.getIndex()[0]) + " " 
                    + Integer.toString(this.getIndex()[1])+ " " 
                    + Integer.toString(this.getIndex()[2]);
            int i = 0;
        }        
        return null;
    }
    
    /**
     * Part of STEP 1: Set the climate threshold (user defined)
     * @param o
     * @return 
     */
    public Object setClimateTempThreshold(Object o){
        try{
            climateTempThreshold = (float)o;
        }catch(Exception e){}
        return null;
    }
    
    /**
     * Part of STEP 1: Determines the number of days over the threshold
     * @param o
     * @return 
     */
    public Object calculateDaysOverThreshold(Object o){
        if(daysTemps == null) return null;      
        // step through the array stored and see how many days are over threshold
        for(float f: daysTemps){
            if(climateTempThreshold <= f && f != 1.0E20f){
                daysOverThreshold++;
            }
        }
        return null;
    }   
    
   /**
     * Simply returns the daysOverThreshold value -- primarily used by Agents in further steps 
     * @param o
     * @return 
     */
    public Object getDaysOverThreshold(Object o){
        return daysOverThreshold; 
    }    
    
    /**
     * Tester method which bypasses the read operations (not used by sequence normally)
     * @param o
     * @return 
     */
    public Object falsifyDaysOverThreshold(Object o){
        Random rn = new Random();
        daysOverThreshold =  rn.nextInt(50);
        return null;
    }    
    
    private Object setYearIndexArray(Object o){
        yearIndices = (int[][])o;
        return null;
    }


    /**
     * This method reads in the entire year chunk of data at a time specific to the place
     * @param o
     * @return 
     */
    public Object readNetCdfDataFullYear(Object o){       
        
   //   Variable ncdfVar;               // NetCDF Variable
 //     ArrayFloat.D3 d3Var;            // 3D NetCDF float array        
 //     List<Variable> inputVariables;
        String longitude = "longitude";
        String latitude = "latitude";
        String time = "time";
        String varname = "tasmax";
        // we read in one z slice at a time, if this isnt the right slice, return
    //    int element = (Integer)o;
    //    if(this.getIndex()[2] != element) return null;        
     
        daysTemps = inputClimateModel.readLocalizedYear(this.getIndex()[0], this.getIndex()[1], this.getIndex()[2]);
        
        // find the days over threshold
        calculateDaysOverThreshold(new Object());

        return null;
//        int element = (Integer)o;
// //       if(index[2] != element && index[2] != element + 1 && index[2] != element +2) return null;
//        if(index[2] != element) return null;
//
//        NetcdfFile inputFile = null;    // target netCDF file
//        int zIndex;
//        int yr;
//        int readIndex;
//        int readAmount;
//        int inputFileIndex;// the index 0-4 of which file to start with
//        int[] orgin;
//        int[] shape;
//        int latitude;
//        int longitude;
//        int time;
//        Variable ncdfTasMaxVar;
//        Array dataSection;
//        // our climate model file set
//        inputClimateModel= new Tasmax_1();
//        int[][] dims = inputClimateModel.getDimensions();
//        String[] files = inputClimateModel.getFiles();
//        int leapYearModifier;
//        int readIndexFileMod;
//        
//            /*
//            *need to find starting read position
//            */
////            Random rn = new Random();
////            int mod =  rn.nextInt(150);
////            zIndex = this.index[2] + mod;
//            zIndex = this.index[2];
//            yr = 1950 + zIndex; // 2099 is the last year, zIndex will be 0-149
//            // find the starting read index value (leap year modifier included)
//            leapYearModifier = (yr - 1948) / 4;
//            readIndex = (zIndex * 365) + leapYearModifier;
//            // figure out leap year modifier
//            readAmount = 365; // by default we read in 365 days. 
//            // figure out if we need to add one for leap year
//            if((yr - 1948) % 4 == 0){
//                readAmount++;
//            }
//            // 5th input file
//            /*
//            Figure out which z element (time) of what file we are starting with
//            */
//            int file0ind = 0;
//            int file1ind = dims[0][2];
//            int file2ind = dims[0][2] + dims[1][2];
//            int file3ind = dims[0][2] + dims[1][2] + dims[2][2];
//            int file4ind = dims[0][2] + dims[1][2] + dims[2][2] + dims[3][2];
//            
//            if(readIndex >= file4ind - 1){
//                inputFileIndex = 4;
//                readIndex = readIndex - file4ind + 1;
//            }
//            // 4th input file
//            else if(readIndex >= file3ind - 1){
//                inputFileIndex = 3;
//                readIndex = readIndex - file3ind + 1;
//            }
//            // 3rd input file
//            else if(readIndex >= file2ind - 1){
//                inputFileIndex = 2;
//                readIndex = readIndex - file2ind + 1;
//            } 
//            // 2nd input file
//            else if(readIndex >= file1ind - 1){
//                inputFileIndex = 1;
//                readIndex = readIndex - file1ind + 1;
//            }      
//            // 1st input file
//            else{
//                inputFileIndex = 0;
//            } 
//            // open the file
//            try{
//                inputFile = NetcdfFile.open(files[inputFileIndex]);  
//            }catch(Exception e){
//                return null;
//            }
//            ncdfTasMaxVar = inputFile.findVariable(varname); 
//            
////            latitude = this.index[0] + rn.nextInt(200);
////            longitude = this.index[1] + rn.nextInt(400);
//            latitude = this.index[0];
//            longitude = this.index[1];
//            time = readIndex;
//            orgin = new int[]{time, latitude, longitude};
//            shape = new int[]{readAmount, 1, 1};   
//            try{
//                // read and set the array as a class variable
//                dataSection = ncdfTasMaxVar.read(orgin, shape);  
//                daysTemps = (float[])dataSection.copyTo1DJavaArray();
//
//            }catch(Exception e){
//            }     
//            try{
//                inputFile.close();
//            }catch(Exception e){}
//        return null;
    }
    
    /**
     * Returns host name and place index
     * @param o
     * @return 
     */
    public Object findHostName(Object o){
        String s = "error";
        try{
             s =  InetAddress.getLocalHost().getHostName() +" " + Integer.toString(this.getIndex()[0]) + ":" + Integer.toString(this.getIndex()[1]) + ":" + Integer.toString(this.getIndex()[2]);
        }catch(Exception e){}
        return s;
    }
}
