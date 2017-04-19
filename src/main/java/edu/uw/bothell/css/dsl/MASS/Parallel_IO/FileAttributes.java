package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
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
    protected static final Log4J2Logger logger = Log4J2Logger.getInstance();

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

    protected int getPlaceReadOffset(int sizeOfBufferToReadFrom) {
        int placeOffset = sizeOfBufferToReadFrom / totalPlaces;

        if (placeOffset < 1) {
            throw new InvalidNumberOfPlacesException(String.format(
                    "Too many places attempting to read a %s file. Number of places: %d, file size: %d.",
                    fileType,
                    totalPlaces,
                    sizeOfBufferToReadFrom
            ));
        }

        return placeOffset;
    }

    protected  int getCurrentPlaceReadLength(int sizeOfBufferToReadFrom, int placeReadOffset, int placeOrder) {
        int remainingLength = placeReadOffset;

        // Last place reads remainder
        if (placeOrder == totalPlaces - 1) {    // TODO: 4/19/17 should be total places on this node
            remainingLength += sizeOfBufferToReadFrom % totalPlaces;
        }

        // logger.debug(String.format("Place %d will read %d starting from %d", placeOrder, remainingLength, placeOrder * placeReadOffset));
        return remainingLength;
    }

    protected int getNodeReadOffset(int sizeOfBufferToReadFrom) {
        int nodeOffset = sizeOfBufferToReadFrom / totalNodes;

        if (nodeOffset < 1) {
           throw new InvalidNumberOfNodesException(String.format("Too many nodes attempting to read a %s file. Number of nodes: %d, file size: %d.",
                   fileType,
                   totalNodes,
                   sizeOfBufferToReadFrom));
        }

        return nodeOffset;
    }

    protected  int getCurrentNodeReadLength(int sizeOfBufferToReadFrom, int nodeReadOffset) {
        int remainingLength = nodeReadOffset;

        // Last place reads remainder
        if (myNodeId == totalNodes - 1) {
            remainingLength += sizeOfBufferToReadFrom % totalNodes;
        }

        return remainingLength;
    }


}
