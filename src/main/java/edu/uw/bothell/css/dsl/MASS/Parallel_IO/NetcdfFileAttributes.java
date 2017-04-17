package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

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

    public NetcdfFileAttributes(Path filepath) {
        super(filepath, FileType.NETCDF);
    }
    
    public Hashtable<String, Object> getVariables() { return variables; }

    // TODO: 4/14/17 error check? 
    public Object getVariable(String variableName) {
        return variables.get(variableName);
    }

    public void openForRead() throws IOException, InvalidRangeException {
            NetcdfFile netcdfFile;
            netcdfFile = NetcdfFile.openInMemory(filepath.toString());
            variables = readNetcdfVariables(netcdfFile);
            file = netcdfFile;
    }

    public void openForWrite() {

    }

    /**
     * Private helper method that opens the given ncFileName (throws an IOException if the file does
     * not exist) based on the given ioType, and add the file and its attributes to the fileTable.
     * Returns the file's unique file descriptor if opened successfully; otherwise, returns -1.
     *
     */
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
}
