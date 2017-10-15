package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

//import com.sun.javaws.exceptions.InvalidArgumentException;
import org.omg.CORBA.DynAnyPackage.Invalid;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;


/**
 * Created by Michael on 4/14/17
 * Edited by Jas on 9/1/17
 */
public class NetcdfFile extends File {
    
    // Variable to read or write (NetCDF)
    private Hashtable<String, Object> variables;
    private ucar.nc2.NetcdfFile netcdfFile;
    private NetcdfFileWriter netcdfFileWriter;
    private ArrayList<Object> writeDataHolder;
    private int[] shape;
    Variable dataVariable;
    long fileSize;
    private static final long MAX_FILE_SIZE_FOR_OPEN_IN_MEMORY = 2000001000;
    private static Array dataOut = null;
    private static int numberOfPreparedPlace = 0;

    public NetcdfFile(Path filepath) {
        super(filepath, FileType.NETCDF);
    }

    public void open(int ioType) throws IOException, InvalidRangeException, InvalidNumberOfNodesException, RuntimeException {
        if(netcdfFile != null || netcdfFileWriter != null) {
            throw new RuntimeException("Multiple calls to open");
        }
        switch (ioType) {
            case OPEN_FOR_READ:
                openForRead();
                break;
            case OPEN_FOR_WRITE:
               // openForWrite();
                break;
        }

    }

    public void open(String variableName, int[] shape) throws IOException, InvalidRangeException, InvalidNumberOfNodesException, RuntimeException {
        if(netcdfFile != null || netcdfFileWriter != null) {
            throw new RuntimeException("Multiple calls to open");
        }
        openForWrite(variableName, shape);
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

    private void openForWrite(String variableName, int[] shape) throws IOException, InvalidRangeException {
        String tempFilePath = filepath.toString();
        tempFilePath = tempFilePath.replace(".nc", "xx.nc");
        writeDataHolder = new ArrayList<>();
        netcdfFileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, tempFilePath,null);

        prepareAndCreateNetCDFWriteFile(variableName, shape);
        dataOut = new ArrayFloat(shape);
    }

    // With Trinh
//    public void writeFloat(String variableName, float[] input) throws InvalidArgumentException {
//        if(input.length > 0) {
//            String[] e = {"Empty Input"};
//            throw new InvalidArgumentException(e);
//        }
//        float[] data = input;
//        Dimension dim = netcdfFileWriter.addDimension(null, variableName, input.length);
//        Variable variable = netcdfFileWriter.addVariable(null, variableName, DataType.FLOAT, variableName);
//
//        ArrayList<String> stringData = new ArrayList<>(input.length);
//        for(int i = 0; i < data.length; i++) {
//            stringData.add(Float.toString(data[i]));
//        }
//        variable.setValues(stringData);
//    }


    private void prepareAndCreateNetCDFWriteFile(String variableName, int[] shape) throws IOException, InvalidRangeException {
        this.shape = shape;
        fileSize = getFileSizeFromShape(shape);

        List<Dimension> dimensions = new ArrayList<>();
        for (int dim = 0; dim < shape.length; dim++) {
            dimensions.add(netcdfFileWriter.addDimension(null, "Dim" + dim, shape[dim]));
        }
        dataVariable = netcdfFileWriter.addVariable(null, variableName, DataType.FLOAT, dimensions);
        netcdfFileWriter.create();
    }


    public boolean write(float[] dataToWrite, String variableName, int[] shape, int placeOrder)
            throws IOException, InvalidRangeException, InvalidNumberOfPlacesException {

            synchronized (dataOut) {
                fillArrayWithFloatData(dataToWrite, placeOrder);
                numberOfPreparedPlace++;

                if(numberOfPreparedPlace == myTotalPlaces) {
                    netcdfFileWriter.write(dataVariable, dataOut);
                    return true;
                }
            }

        return false;
    }

    private void fillArrayWithFloatData(float[] dataToWrite, int placeOrder) throws InvalidNumberOfPlacesException {
        Index index = Index.factory(shape);

        int placeOffset = getPlaceReadOffset(dataToWrite.length);
        int placeReadLength = getCurrentPlaceReadLength(dataToWrite.length, placeOffset, placeOrder);
        int offset = placeOffset * placeOrder;

        for(int i = 0; i < (offset + placeReadLength); i++) {
            if(i >= offset) {
                dataOut.setFloat(index, dataToWrite[i]);
            }
            index.incr();
        }

    }


    private int getFileSizeFromShape(int[] shape) {
        int size = 1;
        for (int i : shape) {
            size *= i;
        }
        return size;
    }


    public void closeFileWrite() throws IOException {
        if(netcdfFileWriter != null) {
            netcdfFileWriter.close();
        }
    }

    public void close() throws IOException {
        if(netcdfFile != null) {
            netcdfFile.close();
        }
        if(netcdfFileWriter != null) {
            netcdfFileWriter.close();
        }
    }

    /*
        Opens the NetCDF file to a NetCDF object for access
        - get the variables and the corresponding data from this opened NetCDF file
        - variables is a key/value pair table that stores variable as key and its corresponding data as value
     */
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

        variables = retrieveNetcdfVariablesAndData(netcdfFile);
    }


    /*
        Retrieves variable name and data from the NetCDF file to table
     */
    private Hashtable<String, Object> retrieveNetcdfVariablesAndData(ucar.nc2.NetcdfFile netcdfFile)
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

    /*
        Called by read() - all data already in the buffer. Only reading the correct portion based on placeOrder
     */
    private float[] readIntoFloatBuffer(float[] bufferToReadFrom, int placeOrder) throws InvalidNumberOfPlacesException {
        int placeOffset = getPlaceReadOffset(bufferToReadFrom.length);
        int placeReadLength = getCurrentPlaceReadLength(bufferToReadFrom.length, placeOffset, placeOrder);
        int offset = placeOffset * placeOrder;
        return Arrays.copyOfRange(bufferToReadFrom, offset, offset + placeReadLength);
    }

    /*
     *  Get the data for the requesting variable from the variables table
     *  @param variableName requesting variable
     */
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
