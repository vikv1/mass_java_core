package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import ucar.nc2.util.IO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;

/**
 * Created by Michael on 4/14/17.
 */
//
// Class that stores the file attributes needed for parallel I/O
//

public abstract class FileAttributes {

    public static final int OPEN_FOR_READ = 0;
    public static final int OPEN_FOR_WRITE = 1;

    // Name of opened file
    protected final Path filepath;

    protected final String fileName;

    protected final FileType fileType;

    protected final int totalPlaces;    // TODO: 4/17/17 sometime we may want only the places available to one node

    protected final int totalNodes;

    protected final int myNodeId;

    public enum FileType {
        NETCDF,
        TXT;
    }

    public FileAttributes(Path filepath, FileType fileType) {
        this.filepath = filepath;
        this.fileName = filepath.getFileName().toString();
        this.fileType = fileType;
        totalPlaces = MASSBase.getCurrentPlacesBase().getTotalPlaces();
        totalNodes = MASSBase.getAllNodes().size();
        myNodeId = MASSBase.getMyPid();
    }


    public String getFileName() {
        return fileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public Path getFilepath() {
        return filepath;
    }

    public static FileAttributes factory(Path filepath) {
        String fileName = filepath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".nc")) {
            return new NetcdfFileAttributes(filepath);
        } else if (fileName.endsWith(".txt")){
            return new TxtFileAttributes(filepath);
        } else {
            throw new UnsupportedFileTypeException(String.format(
                    "The file type of %s is not supported by MASS Parallel IO.",
                    fileName
            ));
        }
    }

    public abstract void open(int ioType) throws Exception;

    public abstract void close() throws IOException;

    protected int getPlaceReadLength(int sizeOfBufferToReadFrom, int placeOrder) {
        int placeReadLength = sizeOfBufferToReadFrom / totalPlaces;

        if (placeReadLength < 1) {
            throw new InvalidNumberOfPlacesException(String.format(
                    "Too many places attempting to read a %s file. Number of places: %d, NetCDF file indexes: %d.",
                    fileType,
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


}
