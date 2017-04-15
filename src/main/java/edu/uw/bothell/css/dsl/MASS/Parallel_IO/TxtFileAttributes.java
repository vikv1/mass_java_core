package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Created by Michael on 4/14/17.
 */
public class TxtFileAttributes extends FileAttributes {

    // Buffer text files are read to
    private final byte[] buffer;

    private final int bytesPerPlace;

    public TxtFileAttributes(int fileDescriptor, Path filepath, FileChannel txtFile, byte[] buffer) {
        super(fileDescriptor, filepath, txtFile, FileType.TXT);
        this.buffer = Arrays.copyOf(buffer, buffer.length);
        this.bytesPerPlace = buffer.length / MASSBase.getCurrentPlacesBase().getTotalPlaces();
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public int getBytesPerPlace() {
        return bytesPerPlace;
    }
}
