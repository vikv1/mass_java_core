package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Created by Michael on 4/14/17.
 */
public class TxtFileAttributes extends FileAttributes {

    // Buffer text files are read to
    private byte[] buffer;

    // Open options, 0 for READ, 1 for WRITE (used for opening file channels)
    private static final OpenOption[] OpenOperations = new OpenOption[]{READ, WRITE};

    private int bytesPerPlace;

    public TxtFileAttributes(Path filepath) {
        super(filepath, FileType.TXT);
    }

/*    public TxtFileAttributes(int fileDescriptor, Path filepath, FileChannel txtFile, byte[] buffer) {
        super(fileDescriptor, filepath, txtFile, FileType.TXT);
        this.buffer = Arrays.copyOf(buffer, buffer.length);
        this.bytesPerPlace = buffer.length / MASSBase.getCurrentPlacesBase().getTotalPlaces();
    }*/

    public byte[] getBuffer() {
        return buffer;
    }

    public int getBytesPerPlace() {
        return bytesPerPlace;
    }

    public void openForRead() throws Exception {
        FileChannel fileChannel;
        fileChannel = FileChannel.open(filepath, OpenOperations[0]);
        buffer = readTextFileInMemory(fileChannel);
        file = fileChannel;
    }

    public void openForWrite() {

    }

    /**
     * Private helper method that opens the given txtFileName (throws an IOException if the file does
     * not exist) based on the given ioType, and add the file and its attributes to the fileTable.
     * Returns the file's unique file descriptor if opened successfully; otherwise, returns -1.
     *
     * @param
     * @return fileDescriptor
     */
    private void openTextFileInMemory(Path path) throws IOException {

    }

    private byte[] readTextFileInMemory(FileChannel fileChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
        fileChannel.read(buffer);
        return buffer.array();
    }
}
