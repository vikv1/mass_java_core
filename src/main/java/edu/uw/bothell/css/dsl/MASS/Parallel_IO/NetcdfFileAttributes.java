package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Michael on 4/14/17.
 */
public class NetcdfFileAttributes extends FileAttributes {
    
    // Variable to read or write (NetCDF)
    private Hashtable<String, Object> variables;
    private NetcdfFile netcdfFile;
    private Log4J2Logger logger;

    public NetcdfFileAttributes(Path filepath) {
        super(filepath, FileType.NETCDF);
        logger = Log4J2Logger.getInstance();
    }

    public void openForRead() throws IOException, InvalidRangeException {
            netcdfFile = NetcdfFile.openInMemory(filepath.toString());
            variables = readNetcdfVariables(netcdfFile);
    }

    public void openForWrite() {

    }

    /**
     * Private method that implements reading for Netcdf files
     *
     * @param variableToRead the buffers to read into - variable name (key), data array (value)
     * @return true on success; otherwise false
     */
    // TODO currently each place reads a single index, each place should determine how much to read
    // based on the number of places, also assumes that the given data arrays are of the correct dimensions
    // (matches the dimensions of places)
    public Object read(String variableToRead, int placeOrder) {
        Object allVariableData = getVariable(variableToRead);

        if (allVariableData instanceof float[]) {
            return readIntoFloatBuffer((float[]) allVariableData, placeOrder);
        } else {
            throw new UnsupportedBufferTypeException(String.format(
                    "The NetCDF variable %s to read has a data type that is not supported.",
                    variableToRead
            ));
        }
    }

    private float[] readIntoFloatBuffer(float[] bufferToReadFrom, int placeOrder) {
        int placeReadLength = getPlaceReadLength(bufferToReadFrom.length, placeOrder);
        return Arrays.copyOfRange(bufferToReadFrom, placeOrder * placeReadLength, placeReadLength * (placeOrder + 1));
    }

    private int getPlaceReadLength(int sizeOfBufferToReadFrom, int placeOrder) {
        int placeReadLength = sizeOfBufferToReadFrom / totalPlaces;

        if (placeReadLength < 1) {
            throw new InvalidNumberOfPlacesException(String.format(
                    "Too many places attempting to read a NetCDF file. Number of places: %d, NetCDF file indexes: %d.",
                    totalPlaces,
                    sizeOfBufferToReadFrom
            ));
        }

        // Last place reads remainder
        if (placeOrder == totalPlaces - 1) {
            int remainingIndexes = sizeOfBufferToReadFrom % totalPlaces;
            placeReadLength = remainingIndexes > 0 ? remainingIndexes : placeReadLength;
        }

        return placeReadLength;
    }

    public void close() throws IOException {
        netcdfFile.close();
    }

    private Hashtable<String, Object> readNetcdfVariables(NetcdfFile netcdfFile) throws InvalidRangeException, IOException {
        List<Variable> unReadVariables = netcdfFile.getVariables();

        Hashtable<String, Object> readVariables = new Hashtable<String, Object>();

        for (int i = 0; i < unReadVariables.size(); i++) {
            Variable currentUnreadVariable = unReadVariables.get(i);

            // TODO: 4/17/17 Read only the portion of the array needed - difficult because of multi dimensions and limited NetCDF Array API 
            Array variableArray = currentUnreadVariable.read(new int[currentUnreadVariable.getShape().length], currentUnreadVariable.getShape());

            if (float.class == variableArray.getElementType()) {
                float[] allVariableData = (float[]) variableArray.copyTo1DJavaArray();
                float[] thisNodesVariableData = getIndividualNodeVariableData(allVariableData);
                readVariables.put(currentUnreadVariable.getShortName(), thisNodesVariableData);
            }
            // TODO: 4/17/17 add more supported variable data types 
            else {
                logger.debug(String.format(
                        "The variable %s for NetCDF file %s could not be read (data type %s not supported).",
                        currentUnreadVariable.getShortName(),
                        fileName, variableArray.getElementType()
                ));
            }
        }
        return readVariables;
    }

    private float[] getIndividualNodeVariableData(float[] allVariableData) {
        int indexesPerNode = allVariableData.length / totalNodes;

        float[] nodeVariableData;
        if (myNodeId == totalNodes - 1) {
            int remainingIndexes = allVariableData.length % totalNodes;
            remainingIndexes = remainingIndexes > 0 ? remainingIndexes : indexesPerNode;
            nodeVariableData = new float[remainingIndexes];
        } else {
            nodeVariableData = new float[indexesPerNode];
        }
        for (int allIndex = indexesPerNode * myNodeId, nodeIndex = 0; nodeIndex < nodeVariableData.length; allIndex++, nodeIndex++) {
            nodeVariableData[nodeIndex] = allVariableData[allIndex];
        }
        return nodeVariableData;
    }

    private Object getVariable(String variableName) {
        Object variableBuffer = variables.get(variableName);
        if (variableBuffer == null) {
            throw new NullPointerException(String.format("The NetCDF file %s does not contain the variable %s", fileName, variableName));
        }
        return variableBuffer;
    }
}


  /*  private Object readIntoProperVariableBuffer(String variableToRead, int placeOrder) {
        if (userVariableBuffer instanceof float[]) {
            readIntoFloatBuffer(variableToRead, (float[]) userVariableBuffer, placeOrder);
        } else {
            throw new UnsupportedBufferTypeException("The NetCDF variable buffer type to read to is not supported.");
        }
    }*/

   /* private void readIntoFloatBuffers(String variableToRead, float[] userFloatBuffer, int placeOrder) {

        Object allVariableData = getVariable(variableToRead);
        if (!(allVariableData instanceof float[])) {
            throw new UnsupportedBufferTypeException(String.format(
                    "The NetCDF variable buffer type to read to does not match the variable data to read, variable: %s",
                    variableToRead
            ));
        }

        float[] allVariableFloatData = (float[]) allVariableData;

        int placeReadLength = allVariableFloatData.length / totalPlaces;

        if (placeReadLength < 1) {
            throw new InvalidNumberOfPlacesException(String.format(
                    "Too many places attempting to read a NetCDF file. Number of places: %d, NetCDF file indexes: %d.",
                    totalPlaces,
                    allVariableFloatData.length
            ));
        }

        if (placeOrder <  totalPlaces - 1) {
            for (int allIndex = placeOrder * placeReadLength, userIndex = 0; allIndex < placeReadLength * (placeOrder + 1); allIndex++, userIndex++) {
                userFloatBuffer[userIndex] = allVariableFloatData[allIndex];
            }
        } else {
            for (int allIndex = placeOrder * placeReadLength, userIndex = 0; allIndex < allVariableFloatData.length; allIndex++, userIndex++) {
                userFloatBuffer[userIndex] = allVariableFloatData[allIndex];
            }
        }
    }*/
