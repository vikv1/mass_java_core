package uwca.climatemodels;


import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author jwoodrin
 */
public class Tasmax_1 implements ClimateModelInterface{
        
    /*
       float longitude(longitude=462);
     :_Netcdf4Dimid = 0; // int
     :standard_name = "longitude";
     :long_name = "Longitude";
     :units = "degrees_east";
     :axis = "X";
     :bounds = "longitude_bnds";

   float longitude_bnds(longitude=462, nb2=2);
     :_Netcdf4Dimid = 0; // int

   float latitude(latitude=222);
     :_Netcdf4Dimid = 2; // int
     :standard_name = "latitude";
     :long_name = "Latitude";
     :units = "degrees_north";
     :axis = "Y";
     :bounds = "latitude_bnds";

   float latitude_bnds(latitude=222, nb2=2);
     :_Netcdf4Dimid = 2; // int

   double time(time=20820);
     :standard_name = "time";
     :long_name = "Time axis";
     :units = "days since 1950-01-01 00:00:00";
     :calendar = "standard";
     :_Netcdf4Dimid = 3; // int

   float tasmax(time=20820, latitude=222, longitude=462);
     :missing_value = 1.0E20f; // float
     :units = "C";
     :_FillValue = 1.0E20f; // float
    */
 //   private String file1 = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_hist_r1i1p1.daily.tasmax.1950-2005.nc";
    private String file1 = "C:\\UWCA\\model1\\conus_c5.noresm1-m_hist_r1i1p1.daily.tasmax.1950-2005.nc";

    /*
       float longitude(longitude=462);
     :_Netcdf4Dimid = 0; // int
     :standard_name = "longitude";
     :long_name = "Longitude";
     :units = "degrees_east";
     :axis = "X";
     :bounds = "longitude_bnds";

   float longitude_bnds(longitude=462, nb2=2);
     :_Netcdf4Dimid = 0; // int

   float latitude(latitude=222);
     :_Netcdf4Dimid = 2; // int
     :standard_name = "latitude";
     :long_name = "Latitude";
     :units = "degrees_north";
     :axis = "Y";
     :bounds = "latitude_bnds";

   float latitude_bnds(latitude=222, nb2=2);
     :_Netcdf4Dimid = 2; // int

   double time(time=7670);
     :standard_name = "time";
     :long_name = "Time axis";
     :units = "days since 1950-01-01 00:00:00";
     :calendar = "standard";
     :_Netcdf4Dimid = 3; // int

   float tasmax(time=7670, latitude=222, longitude=462);
     :missing_value = 1.0E20f; // float
     :units = "C";
     :_FillValue = 1.0E20f; // float    
    */
  //  private String file2 = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2006-2026.nc";
    private String file2 = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2006-2026.nc";
    /*
       float longitude(longitude=462);
     :_Netcdf4Dimid = 0; // int
     :standard_name = "longitude";
     :long_name = "Longitude";
     :units = "degrees_east";
     :axis = "X";
     :bounds = "longitude_bnds";

   float longitude_bnds(longitude=462, nb2=2);
     :_Netcdf4Dimid = 0; // int

   float latitude(latitude=222);
     :_Netcdf4Dimid = 2; // int
     :standard_name = "latitude";
     :long_name = "Latitude";
     :units = "degrees_north";
     :axis = "Y";
     :bounds = "latitude_bnds";

   float latitude_bnds(latitude=222, nb2=2);
     :_Netcdf4Dimid = 2; // int

   double time(time=8766);
     :standard_name = "time";
     :long_name = "Time axis";
     :units = "days since 1950-01-01 00:00:00";
     :calendar = "standard";
     :_Netcdf4Dimid = 3; // int

   float tasmax(time=8766, latitude=222, longitude=462);
     :missing_value = 1.0E20f; // float
     :units = "C";
     :_FillValue = 1.0E20f; // float
    */
 //   private String file3 = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2027-2050.nc";
    private String file3 = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2027-2050.nc";
      /*
       float longitude(longitude=462);
     :_Netcdf4Dimid = 0; // int
     :standard_name = "longitude";
     :long_name = "Longitude";
     :units = "degrees_east";
     :axis = "X";
     :bounds = "longitude_bnds";

   float longitude_bnds(longitude=462, nb2=2);
     :_Netcdf4Dimid = 0; // int

   float latitude(latitude=222);
     :_Netcdf4Dimid = 2; // int
     :standard_name = "latitude";
     :long_name = "Latitude";
     :units = "degrees_north";
     :axis = "Y";
     :bounds = "latitude_bnds";

   float latitude_bnds(latitude=222, nb2=2);
     :_Netcdf4Dimid = 2; // int

   double time(time=8766);
     :standard_name = "time";
     :long_name = "Time axis";
     :units = "days since 1950-01-01 00:00:00";
     :calendar = "standard";
     :_Netcdf4Dimid = 3; // int

   float tasmax(time=8766, latitude=222, longitude=462);
     :missing_value = 1.0E20f; // float
     :units = "C";
     :_FillValue = 1.0E20f; // float
    */
 //   private String file4 = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2051-2074.nc";
    private String file4 = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2051-2074.nc";
      /*
       float longitude(longitude=462);
     :_Netcdf4Dimid = 0; // int
     :standard_name = "longitude";
     :long_name = "Longitude";
     :units = "degrees_east";
     :axis = "X";
     :bounds = "longitude_bnds";

   float longitude_bnds(longitude=462, nb2=2);
     :_Netcdf4Dimid = 0; // int

   float latitude(latitude=222);
     :_Netcdf4Dimid = 2; // int
     :standard_name = "latitude";
     :long_name = "Latitude";
     :units = "degrees_north";
     :axis = "Y";
     :bounds = "latitude_bnds";

   float latitude_bnds(latitude=222, nb2=2);
     :_Netcdf4Dimid = 2; // int

   double time(time=9131);
     :standard_name = "time";
     :long_name = "Time axis";
     :units = "days since 1950-01-01 00:00:00";
     :calendar = "standard";
     :_Netcdf4Dimid = 3; // int

   float tasmax(time=9131, latitude=222, longitude=462);
     :missing_value = 1.0E20f; // float
     :units = "C";
     :_FillValue = 1.0E20f; // float

    */
    
    // total time dimension across files 55153
    // lat and long are the same for all files 
    // lat Y dim
    // long X dim
    // time Z?
 //   private String file5 = "/net/cssfs01p/opt/mfukuda-data/UWCA/data_models/model1/conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2075-2099.nc";
    private String file5 = "C:\\UWCA\\model1\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2075-2099.nc";
    
    // our file list
    private String[] files = new String[]{file1, file2, file3, file4, file5};
    
    // Latitude = 222
    // Longitude = 462
    // Time, 3rd var
    private int[][] dimensions = new int[][]{{462, 222, 20820},{ 462, 222, 7670},{ 462, 222, 8766},{ 462, 222, 8766},{ 462, 222, 9131}};
    
    private int[][] readIndexes;
    
    // tvalue for the climate model
    private double tvalue = 1.986;
    
    String longitude = "longitude";
    String latitude = "latitude";
    String time = "time";
    String varname = "tasmax";
    private int startLeapYear = 1948;
    private int startYear = 1950;
    private int endYear = 2099;

    /**
     * @return the files
     */
    public String[] getFiles() {
        return files;
    }

    /**
     * @return the dimensions
     */
    public int[][] getDimensions() {
        return dimensions;
    }
    
    /**
     * Reads the entire 365-6 days over the entire lat long coordinates for that time frame.
     * @param readIndex
     * @return 
     */
    public Object readFullYear(int z, int[][] yearIndices){
        readIndexes = yearIndices;

        NetcdfFile inputFile = null;    // target netCDF file   
        int readIndex;
        int yr;
        int readAmount;
        int[] orgin;
        int[] shape;
        int latitude;
        int longitude;
        int time;
        Variable ncdfTasMaxVar;
        Array dataSection;
        String varname = "tasmax";
        yr = z + startYear;      
        
        readIndex = readIndexes[z][0];
        readAmount = readIndexes[z][1] - readIndexes[z][0];
 
        String fileToRead = "";
        
        if(yr > 2074){
            fileToRead = file5;
        }
        else if (yr > 2050){
            fileToRead = file4;
        }
         else if (yr > 2026){
            fileToRead = file3;
         }
         else if (yr > 2005){
            fileToRead = file2;
         }
         else{
            fileToRead = file1;
         }

        try {  
            inputFile = NetcdfFile.open(fileToRead);
        } catch (IOException ex) {
        }
        // get the netcdf variable
        ncdfTasMaxVar = inputFile.findVariable(varname);          

        latitude = 222;
        longitude = 462;
        
        orgin = new int[]{readIndex, 0, 0};
        shape = new int[]{readAmount, latitude, longitude};   
        Object yearData = null;
        try{
            dataSection = ncdfTasMaxVar.read(orgin, shape);  

            dataSection =   dataSection.transpose(0, 2);
            yearData = dataSection.copyToNDJavaArray();

        }catch(Exception e){
            String s = e.toString();
        }     
        try{
            inputFile.close();
        }catch(Exception e){
           String s = e.toString();
        }
        return yearData;     
    }
    
    /**
     * Finds the indices for the beginning and end of the years in the netcdf data model
     * @return 
     */
    public int[][] findYearReadIndexes(){
        
        int[][] indices = new int[150][2];
        int yearIndexCnt = 0;
        
        NetcdfFile inputFile = null;

        Variable ncdfVar;               // NetCDF Variable
        ArrayFloat.D3 d3Var;            // 3D NetCDF float array        
        List<Variable> inputVariables;
        Array dataSection = null;
        
        String time = "time";
        int[] shape;
        int[] orgin;
        int size;        
        
        // starting index always 0
        indices[0][0] = 0;
        
        int activeYear = startYear;
        
        // loop through each of the five files
        for(String file: files){

            try {
                inputFile = NetcdfFile.open(file);

                ncdfVar = inputFile.findVariable("time"); 

                // lets check out the time var 
                ncdfVar = inputFile.findVariable(time);
                DataType dataType = ncdfVar.getDataType();
                List<Dimension> dims = ncdfVar.getDimensions();


                size =  dims.get(0).getLength();
                shape = new int[] {size};        

                Attribute attr = ncdfVar.findAttribute("units");
                String dateString = attr.toString();
                String[] ss = dateString.split(" ");
                String date = ss[4];
                String[] dateArr = date.split("-");
                String year = dateArr[0];
                String month = dateArr[1];
                String day = dateArr[2];
                DateUnit dateUnit = null;          
                Calendar calDate = Calendar.getInstance();
                calDate.set(Integer.parseInt(year), (Integer.parseInt(month)-1), Integer.parseInt(day));
                Date javaDate = calDate.getTime();
                String[] dates = new String[size];
                
                orgin = new int[]{0};
                shape = new int[] {size};
                try {
                    dataSection = ncdfVar.read(orgin, shape);
                } catch (InvalidRangeException ex) {
                    Logger.getLogger(Tasmax_1.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                // iterate through the time values
                for (int i = 0; i < size; i++){
                    
                    if(i == 0){
                        indices[yearIndexCnt][0] = 0;
                    }
                    Double val = null;
                    try{
                        val = dataSection.getDouble(i);
                    }catch(Exception e){
                        String s = e.toString();
                    }
                 //   Double val = dataSection.getDouble(Index.factory(shape));
                    try {
                        /*
                        value - number of time units
                        timeUnitString - eg "secs"
                        since - date since, eg "secs since 1970-01-01T00:00:00Z"
                        */              
                        dateUnit = new DateUnit(val, "days", javaDate);
                    } catch (Exception ex) {
                        Logger.getLogger(Tasmax_1.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Date curDate = dateUnit.getDate();
                    
                    dates[i] =  curDate.toString();
                   // System.out.println(i + ": " + val + " Date: " + curDate.toString());
                    // figure out what year this value is
                    String[] tempVals = curDate.toString().split(" ");
                    int tempYr = Integer.parseInt(tempVals[tempVals.length-1]);
                    if(tempYr != activeYear && i != 0){
                        indices[yearIndexCnt][1] = i;
                        yearIndexCnt++;
                        indices[yearIndexCnt][0] = i + 1;
                        activeYear = tempYr;
                    }
                    if(i == size -1){
                        indices[yearIndexCnt][1] = size - 1;
                        yearIndexCnt++;
                        activeYear++;
                    }
                }
                    inputFile.close();

                } catch (IOException ex) {
                  Logger.getLogger(Tasmax_1.class.getName()).log(Level.SEVERE, null, ex);
                } 
        }
        readIndexes = indices;
        return indices;
    }
    
    /**
     * Reads in the netcdf 365-6 days for a given lat / long coordinate
     * @param o
     * @return 
     */
     public float[] readLocalizedYear(int x, int y, int z){

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
        Variable ncdfTasMaxVar;
        Array dataSection;
        // our climate model file set
        int[][] dims = this.getDimensions();
        String[] files = this.getFiles();
   
        yr = 1950 + z; // 2099 is the last year, zIndex will be 0-149
  
         readIndex = readIndexes[z][0];
        readAmount = readIndexes[z][1] - readIndexes[z][0];
 
        String fileToRead = "";
        
        if(yr > 2074){
            fileToRead = file5;
        }
        else if (yr > 2050){
            fileToRead = file4;
        }
         else if (yr > 2026){
            fileToRead = file3;
         }
         else if (yr > 2005){
            fileToRead = file2;
         }
         else{
            fileToRead = file1;
         }
            // open the file
            try{
                inputFile = NetcdfFile.open(fileToRead);  
            }catch(Exception e){
                String s = e.toString();
                return null;
            }
            ncdfTasMaxVar = inputFile.findVariable(varname); 

            longitude = x;
            latitude = y;
            time = readIndex;
            orgin = new int[]{time, latitude, longitude};
            shape = new int[]{readAmount, 1, 1};   
            float[] daysTemps = null;
            try{
                // read and set the array as a class variable
                dataSection = ncdfTasMaxVar.read(orgin, shape);  
                daysTemps = (float[])dataSection.copyTo1DJavaArray();

            }catch(Exception e){
                String s = e.toString();
                return null;
            }     
            try{
                inputFile.close();
            }catch(Exception e){}
        return daysTemps;
    }
}
