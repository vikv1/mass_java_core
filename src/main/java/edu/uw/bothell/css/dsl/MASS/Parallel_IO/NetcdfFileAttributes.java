package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import ucar.nc2.NetcdfFile;

import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Created by Michael on 4/14/17.
 */
public class NetcdfFileAttributes extends FileAttributes {
    
    // Variable to read or write (NetCDF)
    private final Hashtable<String, Object> variables;
    
    public NetcdfFileAttributes(int fileDescriptor, Path filepath, NetcdfFile netcdfFile, Hashtable<String, Object> variables) {
        super(fileDescriptor, filepath, netcdfFile, FileType.NETCDF);
        this.variables = variables;
    }
    
    public Hashtable<String, Object> getVariables() { return variables; }

    // TODO: 4/14/17 error check? 
    public Object getVariable(String variableName) {
        return variables.get(variableName);
    }
}
