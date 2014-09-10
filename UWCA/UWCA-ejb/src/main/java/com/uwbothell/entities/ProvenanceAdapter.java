// git test

package com.uwbothell.entities;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;

/**
 * The provenance adapter provides two functions. The provenance adapter
 * instantiated using the empty constructor provides an exception logging object.
 * A provenance provenance adapter instantiated with a source and target file
 * name creates a metadata file which transfers all metadata from a source climate
 * model to a metadata file, also containing all experimental parameters.
 * 
 * @author Brett Yasuake
 * @author Niko Simonson
 */
public class ProvenanceAdapter {

    // Constants
    public static final int DOUBLE = 0; // data type code for double
    public static final int STRING = 1; // data type code for string
    
    // Variables
    private int type;                   // every variable must be the same type
    private static String name;         // provenance file name
    private static NetcdfFileWriteable currentProvenanceFile;

    private static final Logger logger = Logger.getLogger(ProvenanceAdapter.class.getName());
    private static String source;
    
    private static String LOGFILENAME = "Exceptions_";
    
    public static ArrayList<String> provenanceMessages;
    
    /**
     * Stores provenance information in a central provenance output file.
     * Provenance information is stored as NetCDF global variables.
     * 
     * @param fileName      the name of the provenance file
     * @param sourceName    the name of the source NetCDF file
     */
//TODO add log file creation to this constructor
    public ProvenanceAdapter(String fileName, String sourceName) {
        name = fileName;
        source = sourceName;
        try {
            FileHandler handler = new FileHandler(LOGFILENAME, true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
        } catch (IOException | SecurityException ex) {
            logger.log(Level.SEVERE, "exception caught in provenance adapter", ex);
        }
    }
    
    /**
     * Creates a provenance adapter for logging exceptions to a central log file.
     */
    public ProvenanceAdapter() {
        // Create unique log file name
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String now = sdf.format(cal.getTime());
        LOGFILENAME += now + ".log";
        
        try {
            FileHandler handler = new FileHandler(LOGFILENAME, true);
            logger.addHandler(handler);
            handler.setFormatter(new SimpleFormatter());
        } catch (IOException | SecurityException ex) {
            logger.log(Level.SEVERE, "exception caught in provenance adapter", ex);
        }
    }

    /**
     * Creates a single NetCDF file containing global attributes for each
     * element in the SimpleEntry array.
     * 
     * @param args  array of SimpleEntry key value pairs for each piece of 
     *              provenance captured
     */
    public void create(SimpleEntry<String, String>[] args) {
        try {
            // Create provenance NetCDF file
            currentProvenanceFile = NetcdfFileWriteable.createNew(name);
            
            // Pull global attributes from source file and write to provenance file
            NetcdfFile sourceFile = NetcdfFile.open(source, null);
            
            List<Attribute> globals = sourceFile.getGlobalAttributes();
            Iterator<Attribute> giter = globals.iterator();
            Attribute gatt;
            
            while(giter.hasNext()) {
                gatt = giter.next();
                currentProvenanceFile.addGlobalAttribute(gatt);
            }
            
            List<Dimension> dims = sourceFile.getDimensions();
            Iterator<Dimension> diter = dims.iterator();
            Dimension currentDim;
            while(diter.hasNext()) {
                currentDim = diter.next();
                currentProvenanceFile.addDimension(currentDim.getName(), currentDim.getLength());
            }
            // Store each key value pair as a global attribute in the NetCDF file
            for (int i = 0; i < args.length; ++i) {
                currentProvenanceFile.addGlobalAttribute(args[i].getKey(), args[i].getValue());
            }
            Dimension[] analyticsProvenance = new Dimension[2];
            analyticsProvenance[0] = currentProvenanceFile.addUnlimitedDimension("Records");
//            analyticsProvenance[1] = currentProvenanceFile.addDimension("StringLength", Integer.MAX_VALUE/2);
            analyticsProvenance[1] = currentProvenanceFile.addDimension("StringLength", Constants.MAXPROVENANCESIZE);
            currentProvenanceFile.addVariable("AnalyticsProvenance", DataType.CHAR, analyticsProvenance);
            
            currentProvenanceFile.create();
            currentProvenanceFile.close();

        } catch (IOException ex) {
             logger.log(Level.SEVERE, null, ex);
        }
    }
    

    /**
     * Returns this provenance adapter's data type
     * 
     * @return  integer that represents this provenance adapter data type
     */
    private DataType getType() {
        switch (type) {
            case DOUBLE:
                return DataType.DOUBLE;
            case STRING:
                return DataType.STRING;
            default:
                return DataType.DOUBLE;
        }
    }

/**
     * Writes a string variable to the output file.
     *
     * @param targetName target variable to write values to
     * @param values string values to write to target variable
     * 
     * @throws Exception  
     */
    public void logProvenance() throws Exception {

        // Retreive target variable's range for each dimension
        int range = provenanceMessages.size();
//        for (int i = 0; i < vars.length; ++i) {
//            if (vars[i].getKey() == null ? targetName == null : vars[i].getKey().equals(targetName)) {
//                ranges = vars[i].getValue();
//            }
//        }
        int maxLength = 0;
        for(int i = 0; i < range; i++) {
            if( provenanceMessages.get(i).length() > maxLength) {
                maxLength = provenanceMessages.get(i).length();
            }
        }

        // NetCDF takes the ArrayDouble.D2 data structure to write values
        ArrayChar.D2 inValues = new ArrayChar.D2(provenanceMessages.size(), maxLength);
        for (int i = 0; i < range; ++i) {
            inValues.setString(i, provenanceMessages.get(i));
        }
        // Write values to file in target variable
        try {
            currentProvenanceFile = NetcdfFileWriteable.openExisting(name);
            currentProvenanceFile.write("AnalyticsProvenance", inValues);
            currentProvenanceFile.close();
        } catch (IOException | InvalidRangeException e) {
            throw e;
        }
    }
    
    /**
     * Receives exceptions and logs them to the log file.
     * 
     * @param lvl   the level of the exception to be logged
     * @param msg   an optional log message
     * @param ex    the exception
     */
    public void log(Level lvl, String msg, Exception ex) {
        logger.log(lvl, msg, ex);
    }
}