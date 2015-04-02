package uwca.calculations.toe.places;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import edu.uw.bothell.css.dsl.MASS.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import java.util.Random;
import ucar.ma2.InvalidRangeException;

/**
 *
 * @author jwoodrin
 */
public class TasmaxPlace extends Place {

    // variables for reading netcdf data into places
    private static ArrayList<float[][][]> tempsFileChunks = null;
    private static final Object readLock = new Object();
    private int numNodes;
    private static Boolean[] placeStripeMap = null;
    private static int stripeStartIndex;

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
     * The input climate model This variable is only necessary for when you do
     * reading in from the individual Places
     */
//    private ClimateModelInterface inputClimateModel = null;
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
    public static final int netCdfReadTest = 25;
    public static final int mapPlacesStripeOnNode = 27;
    public static final int initMapPlacesStripeOnNode = 28;
    public static final int individualPlaceRead = 29;

    /**
     * public constructor
     *
     * @param interval
     */
    public TasmaxPlace(Object interval) {
        this.interval = ((Integer) interval).intValue();
    }

    /**
     *
     * @param i the method to be called
     * @param o the method parameters
     * @return
     */
    public Object callMethod(int method, Object o) {
        switch (method) {
            case setClimateModel:
                return setInputClimateModel(o);
            case getDaysOverThreshold:
                return getDaysOverThreshold(o);
            case falsifyDaysOverThreshold: // test / debug related method
                return falsifyDaysOverThreshold(o);
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
            case readNetCdfData:
                return readNetCdfData(o);
            case netCdfReadTest:
                return netCdfReadTest(o);
            case mapPlacesStripeOnNode:
                return mapPlacesStripeOnNode(o);
            case initMapPlacesStripeOnNode:
                return initMapPlacesStripeOnNode(o);
            case individualPlaceRead:
                return individualPlaceRead(o);
            default:
                return null;
        }
    }    

    /**
     * ****************************************************************************************************************
     * Read NetCDF data into Places
     * ***************************************************************************************************************
     */
    
    /**
     * Maps the width of the x dimension stripe on the computing node
     * This helps to set up for reading in all the climate data at the place level
     * @param o
     * @return 
     */
    public Object initMapPlacesStripeOnNode(Object o) {
        synchronized (readLock) {
            if (placeStripeMap == null) {
                int xDimSize = this.getSize()[0];
                placeStripeMap = new Boolean[xDimSize];
                // init the array to false
                for (int i = 0; i < placeStripeMap.length; i++) {
                    placeStripeMap[i] = false;
                }
            }
        }
        return null;
    }
    
    /**
     * Marks the x index of the place in the stripe map with true, to signify that this node is
     * responsible for this place.
     * @param o
     * @return 
     */
    public Object mapPlacesStripeOnNode(Object o) {
        int xIndex = this.getIndex()[0];
        int yIndex = this.getIndex()[1];
        int zIndex = this.getIndex()[2];
        if(yIndex == 0 && zIndex == 0)
            placeStripeMap[xIndex] = true;        
        return null;
    }    

    /**
     * Sets the climate model at the place level.
     *
     * @param o The climate model variable we will be using
     * @return
     */
    public Object setInputClimateModel(Object o) {
//        inputClimateModel = (ClimateModelInterface)o;

        //   inputClimateModel = o;
        //     inputClimateModel = new Tasmax_1();
        return null;
    }
    
    /**
     * 
     * @param o
     * @return 
     */
    private Object setYearIndexArray(Object o) {
        yearIndices = (int[][]) o;
        return null;
    }
    
    /**
     * First place to reach this method will lock it, and read in the complete data for the computing node. 
     * @param o
     * @return 
     */
    public Object readNetCdfData(Object o) {

        synchronized (readLock) {
            // if daysTemps is null, then we need to read in the netcdf data
            if (tempsFileChunks == null) {
                tempsFileChunks = new ArrayList<>();

                // get place index
                int x = this.getIndex()[0];
                int y = this.getIndex()[1];
                int z = this.getIndex()[2];      

                int[][] dims = new int[][]{{462, 222, 20820}, {462, 222, 7670}, {462, 222, 8766}, {462, 222, 8766}, {462, 222, 9131}};

                String[] files = new String[5];
                files[0] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_hist_r1i1p1.daily.tasmax.1950-2005.nc";
                files[1] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2006-2026.nc";
                files[2] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2027-2050.nc";
                files[3] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2051-2074.nc";
                files[4] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2075-2099.nc";
         
                // find stripe size
                int stripeSize = 0;
                int startReadPosition = 0;
                Boolean startFound = false;
                for (int i = 0; i < placeStripeMap.length; i++) {
                    if (placeStripeMap[i] == true) {
                        if (!startFound) {
                            startReadPosition = i;
                            stripeStartIndex = startReadPosition;
                            startFound = true;
                        }
                        stripeSize++;
                    }       
                }
                
                // FOR EACH FILE, READ IN A CHUNK OF DATA
                for(int i = 0; i < files.length; i++){
                    // have the node read in each chunk of data one at a time
//                  private static ArrayList<float[][][]> tempsFileChunks = null;
                    NetcdfFile inputFile = null;    // target netCDF file
                    int zIndex;
                    int yr;
                    int readIndex;
                    int readAmount;
                    int inputFileIndex;// the index 0-4 of which file to start with
                    int[] orgin;
                    int[] shape;
                    int latitude;
                    int longitude;
                    int time;
                    ucar.unidata.util.Format format = new ucar.unidata.util.Format();
                    Variable ncdfTasMaxVar;
                    Array dataSection;
                    String varname = "tasmax";                  
  

                    // open the file
                    try {
                        inputFile = NetcdfFile.open(files[i]);
                    } catch (Exception e) {
                        String s = e.toString();
                        return null;
                    }
                    ncdfTasMaxVar = inputFile.findVariable(varname);

                 
                    int longReadAmount = stripeSize;

                    longitude = startReadPosition;
                    latitude = 0;

                    int latReadAmount = 222 - 1;

                    time = 0;
                    readAmount = dims[i][2] - 1;
                    orgin = new int[]{time, latitude, longitude};
                    shape = new int[]{readAmount, latReadAmount, longReadAmount};
                    daysTemps = null;
                    try {
                        // read and set the array as a class variable
                        dataSection = ncdfTasMaxVar.read(orgin, shape);
                        // add the chunk to our node array list
                        float[][][] dataChunk = (float[][][])dataSection.copyToNDJavaArray();
                        tempsFileChunks.add(dataChunk);

                    } catch (IOException | InvalidRangeException e) {
                        String s = e.toString();
                        return null;
                    }
                    try {
                        inputFile.close();
                    } catch (Exception e) {
                    }
                }
                String s = "";
            }
        }
        return null;
    }    
    
    /**
     * 
     * @param o
     * @return 
     */
    public Object individualPlaceRead(Object o){
    
        int xIndex = this.getIndex()[0];
        int yIndex = this.getIndex()[1];
        int zIndex = this.getIndex()[2];
        
        // figure out the x read index
        int xReadIndex = xIndex - stripeStartIndex;
        
        // figure out what file chunk we need to read from && and the z read index
        int sum = 0;
        int readChunkIndex = 0;
        int[] zLengths = new int[tempsFileChunks.size()];
        for(int i = 0; i < zLengths.length; i++){
            zLengths[i] = tempsFileChunks.get(i)[0][0].length; // z dimension length for the file chunk
            sum += zLengths[i];
            if(zIndex <= sum -1){
                readChunkIndex = i;
                break;
            }
        }
        int startYearIndice = yearIndices[zIndex][0];
        int endYearIndice = yearIndices[zIndex][1];

        // figure out the number of elements to read
        int readAmount = 365;
        float[][][] fileChunk = tempsFileChunks.get(readChunkIndex);
        for (int i = 0; i < readAmount; i++) {
            float dayTempVal = fileChunk[xReadIndex][yIndex][startYearIndice + i];
            if (climateTempThreshold <= dayTempVal && dayTempVal != 1.0E20f) {
                daysOverThreshold++;
            }
        }        
        return null;
    }

    /**
     * ****************************************************************************************************************
     * STEP 1: Find days over temperature threshold
     * ***************************************************************************************************************
     */
    /**
     * Part of STEP 1: sets the 365-6 days array of values to be processed into
     * management variable
     *
     * @param o
     * @return
     */
    public Object setDaysArray(Object o) {
        int x = this.getIndex()[0];
        int y = this.getIndex()[1];
        int z = this.getIndex()[2];

        if (o != null) {
            try {
                daysTemps = (float[]) o;
                calculateDaysOverThreshold(new Object());
                daysTemps = null;
            } catch (Exception e) {
                String s = "";
            }
        }
        return null;
    }

    /**
     * Part of STEP 1: Set the climate threshold (user defined)
     *
     * @param o
     * @return
     */
    public Object setClimateTempThreshold(Object o) {
        try {
            climateTempThreshold = (float) o;
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Part of STEP 1: Determines the number of days over the threshold
     *
     * @param o
     * @return
     */
    public Object calculateDaysOverThreshold(Object o) {
        if (daysTemps == null) {
            return null;
        }
        // step through the array stored and see how many days are over threshold
        for (float f : daysTemps) {
            if (climateTempThreshold <= f && f != 1.0E20f) {
                daysOverThreshold++;
            }
        }
        return null;
    }

    /**
     * Simply returns the daysOverThreshold value -- primarily used by Agents in
     * further steps
     *
     * @param o
     * @return
     */
    public Object getDaysOverThreshold(Object o) {
        return daysOverThreshold;
    }

    /**
     * Tester method which bypasses the read operations (not used by sequence
     * normally)
     *
     * @param o
     * @return
     */
    public Object falsifyDaysOverThreshold(Object o) {
        Random rn = new Random();
        daysOverThreshold = rn.nextInt(50);
        return null;
    }
    
    /**
     * Returns host name and place index
     *
     * @param o
     * @return
     */
    public Object findHostName(Object o) {
        String s = "error";
        try {
            s = InetAddress.getLocalHost().getHostName() + " " + Integer.toString(this.getIndex()[0]) + ":" + Integer.toString(this.getIndex()[1]) + ":" + Integer.toString(this.getIndex()[2]);
        } catch (Exception e) {
        }
        return s;
    }

    /**
     * Simple NetCDF read test method
     *
     * @param o
     * @return
     */
    public Object netCdfReadTest(Object o) {
        //    if(this.getIndex()[2] != (int)o) return null;
        try {
            //   Variable ncdfVar;               // NetCDF Variable
            //     ArrayFloat.D3 d3Var;            // 3D NetCDF float array        
            //     List<Variable> inputVariables;
//        String longitude = "longitude";
//        String latitude = "latitude";
//        String time = "time";
            String varname = "tasmax";
            // we read in one z slice at a time, if this isnt the right slice, return 

            int x = this.getIndex()[0];
            int y = this.getIndex()[1];
            int z = this.getIndex()[2];

//        daysTemps = inputClimateModel.readLocalizedYear(this.getIndex()[0], this.getIndex()[1], this.getIndex()[2]);
            NetcdfFile inputFile = null;    // target netCDF file
            int zIndex;
            int yr;
            int readIndex;
            int readAmount;
            int inputFileIndex;// the index 0-4 of which file to start with
            int[] orgin;
            int[] shape;
            int latitude;
            int longitude;
            int time;
            ucar.unidata.util.Format format = new ucar.unidata.util.Format();
            Variable ncdfTasMaxVar;
            Array dataSection;
            // our climate model file set
            //    int[][] dims = this.getDimensions();
            int[][] dims = new int[][]{{462, 222, 20820}, {462, 222, 7670}, {462, 222, 8766}, {462, 222, 8766}, {462, 222, 9131}};

            // set up files locally
            String host = "";
            String[] files = new String[5];
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                host = "";
            }

            if (host.equals("desktop")) {
                files[0] = "C:\\UWCA\\model1\\conus_c5.noresm1-m_hist_r1i1p1.daily.tasmax.1950-2005.nc";
                files[1] = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2006-2026.nc";
                files[2] = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2027-2050.nc";
                files[3] = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2051-2074.nc";
                files[4] = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2075-2099.nc";
            } else {
                files[0] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_hist_r1i1p1.daily.tasmax.1950-2005.nc";
                files[1] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2006-2026.nc";
                files[2] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2027-2050.nc";
                files[3] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2051-2074.nc";
                files[4] = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2075-2099.nc";
            }

            yr = 1950 + z; // 2099 is the last year, zIndex will be 0-149

            readIndex = yearIndices[z][0];
            readAmount = yearIndices[z][1] - yearIndices[z][0];

            String fileToRead = "";

            if (yr > 2074) {
                fileToRead = files[4];
            } else if (yr > 2050) {
                fileToRead = files[3];
            } else if (yr > 2026) {
                fileToRead = files[2];
            } else if (yr > 2005) {
                fileToRead = files[1];
            } else {
                fileToRead = files[0];
            }
            // open the file
            try {
                inputFile = NetcdfFile.open(fileToRead);
            } catch (Exception e) {
                String s = e.toString();
                return null;
            }
            ncdfTasMaxVar = inputFile.findVariable(varname);

            longitude = x;
            latitude = y;
            time = readIndex;
            orgin = new int[]{time, latitude + 100, longitude + 100};
            shape = new int[]{readAmount, 1, 1};
            //   float[] daysTemps = null;
            try {
                // read and set the array as a class variable
                dataSection = ncdfTasMaxVar.read(orgin, shape);
                daysTemps = (float[]) dataSection.copyTo1DJavaArray();

            } catch (Exception e) {
                String s = e.toString();
                inputFile.close();
                return null;
            }
            try {
                inputFile.close();
            } catch (Exception e) {
            }

            // find the days over threshold
            //   calculateDaysOverThreshold(new Object());
        } catch (Exception e) {
            String s = "";
        }
        return daysTemps;
    }
}
