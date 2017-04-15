package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;

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
    private final Path filepath;

    private final String fileName;

    private final FileType fileType;

    // File descriptor
    private final int fileDescriptor;

    // The file
    private final Object file;

    enum FileType {
        NETCDF,
        TXT;
    }

    FileAttributes(int fileDescriptor, Path filepath, Object file, FileType fileType) {
        this.filepath = filepath;
        this.fileName = filepath.getFileName().toString();
        this.file = file;
        this.fileDescriptor = fileDescriptor;
        this.fileType = fileType;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileDescriptor() {
        return fileDescriptor;
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

}
