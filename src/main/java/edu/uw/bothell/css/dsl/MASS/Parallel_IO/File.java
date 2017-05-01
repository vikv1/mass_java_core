package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Michael G. O'Keefe
 *
 */
public abstract class File {

    public static final int OPEN_FOR_READ = 0;
    public static final int OPEN_FOR_WRITE = 1;
    protected static final Log4J2Logger logger = Log4J2Logger.getInstance();

    // Name of opened file
    protected final Path filepath;

    protected final String fileName;

    protected final FileType fileType;

    protected final int totalPlaces;    // Total places in computation

    protected final int myTotalPlaces;

    protected final int totalNodes;     // Currently the slave node does not have access

    protected final int myNodeId;

    public enum FileType {
        NETCDF,
        TXT;
    }

    public File(Path filepath, FileType fileType) {
        logger.setLogLevel(LogLevel.DEBUG);
        this.filepath = filepath;
        this.fileName = filepath.getFileName().toString();
        this.fileType = fileType;
        totalPlaces = MASSBase.getCurrentPlacesBase().getTotalPlaces(); // TODO: 4/19/17 this must be total places for one node
        totalNodes = MASSBase.getSystemSize();
        myTotalPlaces = MASSBase.getCurrentPlacesBase().getNumberOfPlacesOnCurrentNode();
        myNodeId = MASSBase.getMyPid();
        logger.debug(String.format(
                "This is node %d, there are %d nodes total, %d places total, and %d places on this node",
                myNodeId,
                totalNodes,
                totalPlaces,
                myTotalPlaces
        ));
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

    public static File factory(Path filepath) {
        logger.debug("File factory called.");
        String fileName = filepath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".nc")) {
            return new NetcdfFile(filepath);
        } else if (fileName.endsWith(".txt")){
            return new TxtFile(filepath);
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
        logger.debug("Get Place Read Offset");

        int placeOffset = sizeOfBufferToReadFrom / myTotalPlaces;

        if (placeOffset < 1) {
            throw new InvalidNumberOfPlacesException(String.format(
                    "Too many places attempting to read a %s file. Number of places: %d, file size: %d.",
                    fileType,
                    myTotalPlaces,
                    sizeOfBufferToReadFrom
            ));
        }

        return placeOffset;
    }

    protected  int getCurrentPlaceReadLength(int sizeOfBufferToReadFrom, int placeReadOffset, int placeOrder) {
        logger.debug("Get Place Read Length");

        int remainingLength = placeReadOffset;

        // Last place reads remainder
        if (placeOrder == myTotalPlaces - 1) {    // TODO: 4/19/17 should be total places on this node
            remainingLength += sizeOfBufferToReadFrom % myTotalPlaces;
        }

        // logger.debug(String.format("Place %d will read %d starting from %d", placeOrder,
        // remainingLength, placeOrder * placeReadOffset));
        return remainingLength;
    }

    protected int getNodeReadOffset(int sizeOfBufferToReadFrom) {
        logger.debug("Get Node Read Offset");
        int nodeOffset = sizeOfBufferToReadFrom / totalNodes;

        if (nodeOffset < 1) {
           throw new InvalidNumberOfNodesException(String.format(
                   "Too many nodes attempting to read a %s file. Number of nodes: %d, file size: %d.",
                   fileType,
                   totalNodes,
                   sizeOfBufferToReadFrom));
        }

        return nodeOffset;
    }

    protected  int getCurrentNodeReadLength(int sizeOfBufferToReadFrom, int nodeReadOffset) {
        logger.debug("Get Node Read Length");

        int remainingLength = nodeReadOffset;

        // Last place reads remainder
        if (myNodeId == totalNodes - 1) {
            remainingLength += sizeOfBufferToReadFrom % totalNodes;
        }

        return remainingLength;
    }


}
