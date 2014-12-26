/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package analytics;

import edu.uw.bothell.css.dsl.MASS.*;
import static ucar.grib.grib1.Grib1SplitByGridID.fileName;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author jwoodrin
 * Tmax calculation
 */
public class Tmax extends Place implements ManagementVar{
    
    // File paths for mgmt vars
    // replace this with configuration settings from a file of some sort, probably xml
    private static final String file1 = "D:\\KrakenResources\\files\\conus_c5.noresm1-m_hist_r1i1p1.daily.tasmax.1950-2005.nc";
    private static final String file2 = "D:\\KrakenResources\\files\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2006-2026.nc";
    private static final String file3 = "D:\\KrakenResources\\files\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2027-2050.nc";
    private static final String file4 = "D:\\KrakenResources\\files\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2051-2074.nc";
    private static final String file5 = "D:\\KrakenResources\\files\\conus_c5.noresm1-m_rcp45_r1i1p1.daily.tasmax.2075-2099.nc";
    // the set of files to process for the mgmt var
    private static final String[] files = {file1, file2, file3, file4, file5};
    // Functions
    private static final int calculate_mgmt = 0;         // calculate the mgmt var
    
    // our functions array, which determines the order of execution
    private static int[] functions = {calculate_mgmt};

    /**
     * Public constructor required by MASS
     * @param interval 
     */
    public Tmax (Object interval) {
     //   this.interval = ( ( Integer)interval ).intValue();
    }
    /**
     * The standard implementation of the callMethod function required by classes
     * extending the MASS Place class
     * @param functionId The function to be executed
     * @param argument Any function argument necessary
     * @return 
     */
    @Override
    public Object callMethod(int functionId, Object argument) {
        switch (functionId) {
            case calculate_mgmt:
                calculateMgmtVar((int)argument);
                break;
            default:
                break;
        }
        return null;
    }

    /**
     * 
     * @param fullRead 0 means read in incrementally, 1 means read full file set into heap
     */
    public void calculateMgmtVar(int fullRead) {
        // if we're doing a full read just read the content of all the files onto the heap
        // otherwise use our incremental read algorithm
        if(fullRead == 1){
        
        }
        // incremental read algorithm (suspected performance hit)
        else{
            // for each data set the place is responsible for read in values one by one and process
            try{
               //   NetcdfFile nc = new NetcdfFile(file1, true);
                  
            }catch(Exception e){
                // TODO: error handler here
            }
        
        }
        // calling out cdo greater than 18, input file , output file; 
        // yearly sum counting # of days over 18.3 per year 
        //(every file is a 1 or a 0, no other information other than lat && long)
        
        // adds up the daily files, so instead of 365 files, there's now one
        
        // lets just clean up our files (the 365 left over files)
    }
    /**
     * 
     * @return 
     */
     public float[][][] readFile(){
     
         return null;
     }
     /**
      * 
      */
     public void initialize() {
        try {
            // probably not needed

        } catch (Exception ex) {         
        }
    }

    /**
     * @return the functions 
     * JobRunner uses this to understand the order of execution
     */
    public static int[] getFunctions() {
        return functions;
    }    

    /**
     * @return the files
     */
    public static String[] getFiles() {
        return files;
    }
}
