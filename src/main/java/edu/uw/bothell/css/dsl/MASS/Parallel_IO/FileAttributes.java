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

    // Name of opened file
    protected final Path filepath;

    protected final String fileName;

    protected final FileType fileType;

    protected final int totalPlaces;

    // The file
    protected Object file;

    public enum FileType {
        NETCDF,
        TXT;
    }

    public FileAttributes(Path filepath, FileType fileType) {
        this.filepath = filepath;
        this.fileName = filepath.getFileName().toString();
        this.fileType = fileType;
        totalPlaces = MASSBase.getCurrentPlacesBase().getTotalPlaces();
    }


    public String getFileName() {
        return fileName;
    }

    public Object getFile() {
        return file;
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
            throw new UnsupportedFileTypeException(String.format("The file type of %s is not supported by MASS Parallel IO.", fileName));
        }
    }

    public abstract void openForRead() throws Exception;

    public abstract void openForWrite() throws Exception;

}
