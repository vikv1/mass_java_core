package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Michael on 4/14/17.
 */
public class NetcdfFileAttributes extends FileAttributes {
    
    // Variable to read or write (NetCDF)
    private Hashtable<String, Object> variables;
    private NetcdfFile netcdfFile;

    public NetcdfFileAttributes(Path filepath) {
        super(filepath, FileType.NETCDF);
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
    public void read(String variableToRead, Object userVariableBuffer, int placeOrder) {
        readIntoProperVariableBuffer(variableToRead, userVariableBuffer, placeOrder);
    }

    public void close() throws IOException {
        netcdfFile.close();
    }

    private Hashtable<String, Object> readNetcdfVariables(NetcdfFile netcdfFile) throws InvalidRangeException, IOException {
        List<Variable> unReadVariables = netcdfFile.getVariables();

        Hashtable<String, Object> readVariables = new Hashtable<String, Object>();

        for (int i = 0; i < unReadVariables.size(); i++) {
            Variable currentUnreadVariable = unReadVariables.get(i);
            // TODO: 3/31/17 read only what is needed for this node
            Array varData = currentUnreadVariable.read(new int[currentUnreadVariable.getShape().length], currentUnreadVariable.getShape());
            readVariables.put(currentUnreadVariable.getShortName(), varData.copyTo1DJavaArray());
        }
        return readVariables;
    }

    private void readIntoProperVariableBuffer(String variableToRead, Object userVariableBuffer, int placeOrder) {
        if (userVariableBuffer instanceof float[]) {
            readIntoFloatBuffer(variableToRead, (float[]) userVariableBuffer, placeOrder);
        } else {
            throw new UnsupportedBufferTypeException("The NetCDF variable buffer type to read to is not supported.");
        }
    }

    private void readIntoFloatBuffer(String variableToRead, float[] userFloatBuffer, int placeOrder) {

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
    }

    private Object getVariable(String variableName) {
        Object variableBuffer = variables.get(variableName);
        if (variableBuffer == null) {
            throw new NullPointerException(String.format("The NetCDF file %s does not contain the variable %s", fileName, variableName));
        }
        return variableBuffer;
    }
}
