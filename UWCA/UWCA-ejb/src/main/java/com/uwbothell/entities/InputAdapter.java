package com.uwbothell.entities;

import java.io.*;
import java.util.*;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;     // for netCDF reader
import ucar.ma2.ArrayInt;
import ucar.ma2.IndexIterator;  // for netCDF reader
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;     // for netCDF reader
import ucar.nc2.Variable;       // for netCDF reader

/**
 * Adapter to read variables and values from NetCDF files.
 *
 * @author Niko Simonson (Primary)
 * @author Brett Yasutake (Contributing)
 */
public class InputAdapter {

    /**
     *
     * @param firstDay
     * @param secondDay
     * @return
     * @throws Exception
     */
    public static boolean isSameDay(String firstDay, String secondDay) throws Exception {
        final char DELIMITER = '_';
        String subFirst, subSecond;

        subFirst = firstDay.substring(0, firstDay.indexOf(DELIMITER));
        subSecond = secondDay.substring(0, secondDay.indexOf(DELIMITER));

        return subFirst.equals(subSecond);
    }

    /**
     *
     * @param fileName
     * @return
     * @throws Exception
     */
    public static int getDay(String fileName) throws Exception {
        int result = 0;
        String component;
        String day = readTime(fileName);
//        System.err.println("getDay, readTime is " + day);
        // get the year
        component = day.substring(0, 4);
        result = Integer.parseInt(component) * 10000;

        // get the month
        component = day.substring(5, 7);
        result += Integer.parseInt(component) * 100;

        // get the day
        component = day.substring(8, 10);
        result += Integer.parseInt(component);

        return result;
    }

    /**
     * 
     * @param fileName
     * @return
     * @throws Exception 
     */
    public static String readTime(String fileName) throws Exception {
        final String VARNAME = "Times";
        String result;

        NetcdfFile inputFile = null;    // target netCDF file
        Variable ncdfVar;               // NetCDF Variable
        ArrayFloat.D3 d3Var;            // 3D NetCDF float array
        ArrayChar time;

        List<Variable> inputVariables;

        try {
            // Open target netCDF file
            inputFile = NetcdfFile.open(fileName, null);    // Open the file

            // Find target variable
            ncdfVar = inputFile.findVariable(VARNAME);

            // Get list of file's variables
            inputVariables = inputFile.getVariables();
            ncdfVar = inputVariables.get(inputVariables.indexOf(ncdfVar));

            // Read values from variable
            time = (ArrayChar) ncdfVar.read();

            result = time.toString();

            return result;
        } catch (Exception e) {
            throw e;
        } finally {                 // Close out the file no matter what occurs
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (Exception e) {
                    throw e;
                }
            }
        }
    }

    /**
     * Returns a NetCDF 2-dimensional variable's values.
     * 
     * @param fileName  file name
     * @param varName   variable name
     * @param xRng      X axis range
     * @param yRng      Y axis range
     * 
     * @return          [xRng] by [yRng] double array that holds all the values
     *                  retrieved from the NetCDF file
     * @throws Exception  
     */
    public static double[][] read2DFloat(String fileName, String varName, int xRng, int yRng) throws Exception {
        NetcdfFile inputFile = null;    // target netCDF file
        Variable ncdfVar;               // NetCDF Variable
        ArrayFloat.D3 d3Var;            // 3D NetCDF float array
        List<Variable> inputVariables;
        double[][] results;             // holds retreived values

        // Initialize results
        results = new double[xRng][yRng];

        try {
            // Open target netCDF file
            inputFile = NetcdfFile.open(fileName, null);    // Open the file

            // Find target variable
            ncdfVar = inputFile.findVariable(varName);

            // Get list of file's variables
            inputVariables = inputFile.getVariables();
            ncdfVar = inputVariables.get(inputVariables.indexOf(ncdfVar));

            // Read values from variable
            d3Var = (ArrayFloat.D3) ncdfVar.read();

            // Iterators
            IndexIterator iter;
            iter = d3Var.getIndexIterator();

            int x = 0;
            int y = 0;


            // Fill arrays with values from file
            while (iter.hasNext()) {
                iter.next();

                results[x][y] = (double) iter.getFloatCurrent();

                // Iterating through 2 dimensions
                // The outermost index must be filled first
                if (y == yRng - 1) {
                    ++x;
                    y = 0;
                } else {
                    ++y;
                }
            }
        } catch (Exception e) {
            throw e;
//            e.printStackTrace();
        } finally {                 // Close out the file no matter what occurs
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException ioe) {
                    throw ioe;
                }
            }
        }
        //find bugs - redundent check for null
        if (results == null) {
            System.err.println("In input adapter: null results");
        }
        return results;
    }

    /**
     * Returns a NetCDF 2-dimensional variable's values.
     * 
     * @param fileName  file name
     * @param varName   variable name
     * @param xRng      X axis range
     * @param yRng      Y axis range
     * @return          [xRng] by [yRng] double array that holds all the values
     *                  retrieved from the NetCDF file
     * @throws Exception  
     */
    public static double[][] read2DDouble(String fileName, String varName, int xRng, int yRng) throws Exception {
        NetcdfFile inputFile = null;    // target netCDF file
        Variable ncdfVar;               // NetCDF Variable
        ArrayDouble.D2 d2Var;            // 3D NetCDF float array
        List<Variable> inputVariables;
        double[][] results;             // holds retreived values
        

        // Initialize results
        results = new double[xRng][yRng];

        try {
            // Open target netCDF file
            inputFile = NetcdfFile.open(fileName, null);    // Open the file

            // Find target variable
            ncdfVar = inputFile.findVariable(varName);

            // Get list of file's variables
            inputVariables = inputFile.getVariables();
            ncdfVar = inputVariables.get(inputVariables.indexOf(ncdfVar));

            // Read values from variable
            d2Var = (ArrayDouble.D2) ncdfVar.read();

            // Iterators
            IndexIterator iter;
            iter = d2Var.getIndexIterator();

            int x = 0;
            int y = 0;


            // Fill arrays with values from file
            while (iter.hasNext()) {
                iter.next();

                results[x][y] = (double) iter.getDoubleCurrent();

                // Iterating through 2 dimensions
                // The outermost index must be filled first
                if (y == yRng - 1) {
                    ++x;
                    y = 0;
                } else {
                    ++y;
                }
            }
        } catch (Exception e) {
            throw e;
//            e.printStackTrace();
        } finally {                 // Close out the file no matter what occurs
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException ioe) {
                    throw ioe;
                }
            }
        }
        //find bugs - redundent check for null
        if (results == null) {
            System.err.println("In input adapter: null results");
        }
        return results;
    }

    /**
     * Returns a NetCDF 2-dimensional variable's values.
     * 
     * @param fileName  file name
     * @param varName   variable name
     * @param xRng      X axis range
     * @param yRng      Y axis range
     * @return          [xRng] by [yRng] double array that holds all the values
     *                  retrieved from the NetCDF file
     * @throws Exception  
     */
    public static int[][] read2DInt(String fileName, String varName, int xRng, int yRng) throws Exception {
        NetcdfFile inputFile = null;    // target netCDF file
        Variable ncdfVar;               // NetCDF Variable
        ArrayInt.D3 d3Var;            // 3D NetCDF float array
        List<Variable> inputVariables;
        int[][] results;             // holds retreived values

        // Initialize results
        results = new int[xRng][yRng];

        try {
            // Open target netCDF file
            inputFile = NetcdfFile.open(fileName, null);    // Open the file

            // Find target variable
            ncdfVar = inputFile.findVariable(varName);

            // Get list of file's variables
            inputVariables = inputFile.getVariables();
            ncdfVar = inputVariables.get(inputVariables.indexOf(ncdfVar));

            // Read values from variable
            d3Var = (ArrayInt.D3) ncdfVar.read();

            // Iterators
            IndexIterator iter;
            iter = d3Var.getIndexIterator();

            int x = 0;
            int y = 0;


            // Fill arrays with values from file
            while (iter.hasNext()) {
                iter.next();

                results[x][y] = (int) iter.getIntCurrent();

                // Iterating through 2 dimensions
                // The outermost index must be filled first
                if (y == yRng - 1) {
                    ++x;
                    y = 0;
                } else {
                    ++y;
                }
            }
        } catch (Exception e) {
            throw e;
//            e.printStackTrace();
        } finally {                 // Close out the file no matter what occurs
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException ioe) {
                    throw ioe;
                }
            }
        }
        //find bugs - redundent check for null
        if (results == null) {
            System.err.println("In input adapter: null results");
        }
        return results;
    }

    /**
     * Returns multiple NetCDF 2-dimensional variable's values.
     * 
     * @param fileName  file name
     * @param varNames  variable names
     * @param xRng      X axis range
     * @param yRng      Y axis range
     * @return          [xRng] by [yRng] by [# of variables] double array that
     *                  holds all values retrieved from the NetCDF file
     * @throws Exception  
     */
    public static double[][][] read2DFloats(String fileName, String[] varNames, int xRng, int yRng) throws Exception {
        double[][][] results;
        double[][] tempResults;

        // Initialize results
        int lng = varNames.length;
        results = new double[xRng][yRng][lng];
        tempResults = new double[xRng][yRng];

        // Iterate through each requested variable and place values in results
        for (int i = 0; i < lng; ++i) {
            tempResults = read2DFloat(fileName, varNames[i], xRng, yRng);
            results[i] = tempResults;
        }
        return results;
    }

    /**
     * Returns a NetCDF 3-dimensional variable's values.
     * 
     * @param fileName  file name
     * @param varName   variable name
     * @param xRng      X axis range
     * @param yRng      Y axis range
     * @param zRng      Z axis range
     * @return          [xRng] by [yRng] by [zRng] double array that holds all
     *                  the values retrieved from the NetCDF file
     * @throws Exception  
     */
    public static double[][][] read3DFloat(String fileName, String varName, int xRng, int yRng, int zRng) throws Exception {
        NetcdfFile inputFile = null;    // Target netCDF file
        Variable ncdfVar;               // NetCDF variable
        ArrayFloat.D3 d3Var;            // 3D NetCDF float array
        List<Variable> inputVariables;
        double[][][] results;           // Return array

        // Initialize results
        results = new double[xRng][yRng][zRng];

        try {
            // Open target netCDF file
            inputFile = NetcdfFile.open(fileName, null);    // Open the file

            // Find target variable
            ncdfVar = inputFile.findVariable(varName);

            // Get list of file's variables
            inputVariables = inputFile.getVariables();
            ncdfVar = inputVariables.get(inputVariables.indexOf(ncdfVar));

            // Read values from variable
            d3Var = (ArrayFloat.D3) ncdfVar.read();

            // Iterators
            IndexIterator iter;
            iter = d3Var.getIndexIterator();

            int x = 0;
            int y = 0;
            int z = 0;

            // Fill arrays with values from file
            while (iter.hasNext()) {
                iter.next();

                results[x][y][z] = (double) iter.getFloatCurrent();

                // Iterating through 3 dimensions
                if (x == xRng - 1) {
                    x = 0;
                    if (y == yRng - 1) {
                        y = 0;
                        ++z;
                    } else {
                        ++y;
                    }
                } else {
                    ++x;
                }
            }
        } catch (Exception e) {
            throw e;

        } finally {                 // Close out the file no matter what occurs
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException ioe) {
                    throw ioe;
                }
            }
        }
        return results;
    }

    /**
     * Returns multiple NetCDF 3-dimensional variable's values.
     * 
     * @param fileName  file name
     * @param varNames  variable names
     * @param xRng      X axis range
     * @param yRng      Y axis range
     * @param zRng      Z axis range
     * @return          [xRng] by [yRng] by [zRng] by [# of variables] double
     *                  array that holds all values retrieved from the NetCDF file
     * @throws Exception  
     */
    public static double[][][][] read3DFloats(String fileName, String[] varNames, int xRng, int yRng, int zRng) throws Exception {
        double[][][][] results;
        double[][][] tempResults;

        // Initialize results
        int lng = varNames.length;
        results = new double[xRng][yRng][zRng][lng];
        tempResults = new double[xRng][yRng][zRng];

        // Iterate through each requested variable and place values in results
        for (int i = 0; i < lng; ++i) {
            tempResults = read3DFloat(fileName, varNames[i], xRng, yRng, zRng);
            results[i] = tempResults;
        }
        return results;
    }

    /**
     * Retrieve ranges for a NetCDF variable.
     * 
     * @param fileName  file name
     * @param varName   variable name
     * @return          Range for each dimension of the target variable
     * @throws Exception  
     * 
     */
    public static int[] getRanges(String fileName, String varName) throws Exception {
        NetcdfFile inputFile = null;    // target netCDF file
        Variable ncdfVar;               // NetCDF variable

        try {
            // Open the file
            inputFile = NetcdfFile.open(fileName, null);

            // Find target variable
            ncdfVar = inputFile.findVariable(varName);

            List<Dimension> dimensions = ncdfVar.getDimensions();
            ListIterator<Dimension> dIter = dimensions.listIterator();

            // Get the range of each dimension
            int[] ranges = new int[dimensions.size()];
            int counter = 0;
            while (dIter.hasNext()) {
                ranges[counter] = dIter.next().getLength();
                ++counter;
            }
            return ranges;

        } catch (Exception e) {
            throw e;
        } finally {
            if (inputFile != null) {
                inputFile.close();
            }
            // find bugs -- return zero length array instead?
            return null;
        }
    }

    /**
     * Retrieve the dimensions of a NetCDF variable.
     * @param fileName  file name
     * @param varName   variable name
     * @return          number of dimensions in the target variable
     * @throws Exception  
     */
    public static int getDimensions(String fileName, String varName) throws Exception {
        NetcdfFile inputFile = null;    // Target netCDF file
        Variable ncdfVar;               // NetCDF variable

        try {
            // Open the file
            inputFile = NetcdfFile.open(fileName, null);

            // Find target variable
            ncdfVar = inputFile.findVariable(varName);

            // Return its number of dimensions
            return ncdfVar.getDimensions().size();
        } catch (Exception e) {
            throw e;
        } finally {
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
            return -1;
        }
    }
}