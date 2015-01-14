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



/**
 *
 * @author jwoodrin
 */
public class TasmaxPlace extends Place{
    
    // our days over threshold value which each place is responsible for
    // STEP 2
    public int daysOverThreshold = 0;
        
    private int interval;
    private int x;
    
    // set for step 1 && 2
    private float climateTempThreshold = 0;
    
    public static final int readNetCdfData = 1;
    public static final int setClimateModel = 2;
    public static final int getDaysOverThreshold = 4;
    public static final int readNetCdfDataFullYear = 5;
    public static final int falsifyDaysOverThreshold = 10;
    public static final int findHostName = 20;
    public static final int setDaysArray = 11;
     public static final int returnInt = 55;
    public static final int calculateDaysOverThreshold = 12;
    public static final int setMinMaxThresholdUserValues = 13;
    public static final int setClimateTempThreshold = 21;
    
    public int[] myPlace;
    
    private float[] daysTemps;
    
    
    // historicalThresholds[0] = min value
    // historicalThresholds[1] = max value
    private double[] historicalThresholds;
    
    private Tasmax_1 inputClimateModel = null;
    

   //   Variable ncdfVar;               // NetCDF Variable
 //     ArrayFloat.D3 d3Var;            // 3D NetCDF float array        
 //     List<Variable> inputVariables;

      String longitude = "longitude";
      String latitude = "latitude";
      String time = "time";
      String varname = "tasmax";
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
            case returnInt:
                return getInt();
            case calculateDaysOverThreshold:
                return calculateDaysOverThreshold(o);     
            case setMinMaxThresholdUserValues:
                return setMinMaxThresholdUserValues(o);
            case setClimateTempThreshold:
                return setClimateTempThreshold(o);
            default:
                return null;              
        }
    }
    
    /**
     * Sets the climate temp threshold for step 2
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
     * Sets the user defined thresholds for historical min max calculations
     * @param o
     * @return 
     */
    public Object setMinMaxThresholdUserValues(Object o){
        
        historicalThresholds = (double[])o;        
        return null;    
    }
    
    /**
     * sets the 365-6 days array of values to be processed into management variable
     * @param o
     * @return 
     */
    public Object setDaysArray(Object o){
        if(o != null){
            daysTemps = (float[])o;
            calculateDaysOverThreshold(new Object());
            daysTemps = null;
        }        
        return null;
    }
    
    /**
     * Determines the number of days over the threshold
     * This is STEP 2
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
    
    public Object findHostName(Object o){
        String s = "error";
        try{
             s =  InetAddress.getLocalHost().getHostName() +" " + Integer.toString(this.getIndex()[0]) + ":" + Integer.toString(this.getIndex()[1]) + ":" + Integer.toString(this.getIndex()[2]);
        }catch(Exception e){}
        return s;
    }
    /**
     * 
     * @param o
     * @return 
     */
    public Object falsifyDaysOverThreshold(Object o){
        Random rn = new Random();
        daysOverThreshold =  rn.nextInt(50);
        return null;
    }
    
    /**
     * 
     * @param o
     * @return 
     */
    public Object getDaysOverThreshold(Object o){
        return daysOverThreshold; 
    }
    
    /**
     * Sets the climate model at the place level. 
     * @param o The climate model variable we will be using
     * @return 
     */
    public Object setInputClimateModel(Object o){
     //   inputClimateModel = (Tasmax_1)o
        inputClimateModel = new Tasmax_1();
        return null;
    }    
    
    /**
     * This method reads in the entire year chunk of data at a time
     * @param o
     * @return 
     */
    public Object readNetCdfDataFullYear(Object o){
        // we read in one z slice at a time, if this isnt the right slice, return
        int element = (Integer)o;
        if(this.getIndex()[2] != element) return null;
        
        inputClimateModel= new Tasmax_1();
        daysTemps = inputClimateModel.readLocalizedYear(this.getIndex()[0], this.getIndex()[1], this.getIndex()[2]);

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
        public double getInt(){
        return x;
    }
}
