package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Michael on 4/14/17.
 */
public class NetcdfFile extends File {
    
    // Variable to read or write (NetCDF)
    private Hashtable<String, Object> variables;
    private ucar.nc2.NetcdfFile netcdfFile;
    private static final long MAX_FILE_SIZE_FOR_OPEN_IN_MEMORY = 2000001000;

    public NetcdfFile(Path filepath) {
        super(filepath, FileType.NETCDF);
    }

    public void open(int ioType) throws IOException, InvalidRangeException, InvalidNumberOfNodesException {
        switch (ioType) {
            case OPEN_FOR_READ:
                openForRead();
                break;
            case OPEN_FOR_WRITE:
                break;
        }
    }

    public Object read(String variableToRead, int placeOrder) throws InvalidNumberOfPlacesException, UnsupportedBufferTypeException {
        Object allVariableData = getVariable(variableToRead);

        // TODO: 4/18/17 add more supported variable data types
        if (allVariableData instanceof float[]) {
            return readIntoFloatBuffer((float[]) allVariableData, placeOrder);
        } else {
            throw new UnsupportedBufferTypeException(String.format(
                    "The NetCDF variable %s to read has a data type that is not supported.",
                    variableToRead
            ));
        }
    }

    public void close() throws IOException {
        netcdfFile.close();
    }

    private void openForRead() throws IOException, InvalidRangeException, InvalidNumberOfNodesException {
        java.io.File ncFileForSizeCheck = new java.io.File(filepath.toUri());
        if (ncFileForSizeCheck.length() > MAX_FILE_SIZE_FOR_OPEN_IN_MEMORY) {
            netcdfFile = ucar.nc2.NetcdfFile.open(filepath.toString());
            logger.debug(String.format(
                    "Opening NetCDF file %s on disk (exceeds max size to open in memory)",
                    fileName
            ));
        } else {
            netcdfFile = ucar.nc2.NetcdfFile.openInMemory(filepath.toString());
            logger.debug(String.format(
                    "Opening NetCDF file %s in memory",
                    fileName
            ));
        }

        variables = readNetcdfVariables(netcdfFile);
    }

    private Hashtable<String, Object> readNetcdfVariables(ucar.nc2.NetcdfFile netcdfFile)
            throws InvalidRangeException, IOException, InvalidNumberOfNodesException {

        List<Variable> unReadVariables = netcdfFile.getVariables();

        Hashtable<String, Object> readVariables = new Hashtable<String, Object>();

        for (int i = 0; i < unReadVariables.size(); i++) {
            Variable currentUnreadVariable = unReadVariables.get(i);

            // TODO: 4/17/17 Read only the portion of the array needed
            // - difficult because of multi dimensions and limited NetCDF Array API

            Array variableArray = currentUnreadVariable.read(
                    new int[currentUnreadVariable.getShape().length],
                    currentUnreadVariable.getShape()
            );

            if (float.class == variableArray.getElementType()) {
                float[] allVariableData = (float[]) variableArray.copyTo1DJavaArray();
                float[] thisNodesVariableData = getIndividualNodeVariableData(allVariableData);
                readVariables.put(currentUnreadVariable.getShortName(), thisNodesVariableData);
            }

            // TODO: 4/17/17 add more supported variable data types 
            else {
                logger.debug(String.format(
                        "NOTE: The variable %s for NetCDF file %s could not be read (data type %s not supported).",
                        currentUnreadVariable.getShortName(),
                        fileName, variableArray.getElementType()
                ));
            }
        }
        return readVariables;
    }

    private float[] getIndividualNodeVariableData(float[] allVariableData) throws InvalidNumberOfNodesException {
        int nodeOffset = getNodeReadOffset(allVariableData.length);
        int nodeReadLength = getCurrentNodeReadLength(allVariableData.length, nodeOffset);
        int offset = nodeOffset * myNodeId;
        return Arrays.copyOfRange(allVariableData, offset, offset + nodeReadLength);
    }

    private float[] readIntoFloatBuffer(float[] bufferToReadFrom, int placeOrder) throws InvalidNumberOfPlacesException {
        int placeOffset = getPlaceReadOffset(bufferToReadFrom.length);
        int placeReadLength = getCurrentPlaceReadLength(bufferToReadFrom.length, placeOffset, placeOrder);
        int offset = placeOffset * placeOrder;
        return Arrays.copyOfRange(bufferToReadFrom, offset, offset + placeReadLength);
    }

    private Object getVariable(String variableName) {
        Object variableBuffer = variables.get(variableName);
        if (variableBuffer == null) {
            throw new NullPointerException(String.format(
                    "The NetCDF file %s does not contain the variable %s",
                    fileName,
                    variableName
            ));
        }
        return variableBuffer;
    }
}
