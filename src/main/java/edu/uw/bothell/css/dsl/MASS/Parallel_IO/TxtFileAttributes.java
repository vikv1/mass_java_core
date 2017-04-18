package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASS;

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
    private byte[] entireTxtFileBuffer;

    private FileChannel fileChannel;

    // Open options, 0 for READ, 1 for WRITE (used for opening file channels)
    private static final OpenOption[] OpenOperations = new OpenOption[]{READ, WRITE};

    private int bytesPerPlace;

    public TxtFileAttributes(Path filepath) {
        super(filepath, FileType.TXT);
    }

    public void open(int ioType) throws Exception {
        switch(ioType) {
            case OPEN_FOR_READ:
                openForRead();
                break;
            case OPEN_FOR_WRITE:
                break;
        }
    }

    public byte[] read(int placeOrder) {
        int placeReadLength = getPlaceReadLength(entireTxtFileBuffer.length, placeOrder);
        return Arrays.copyOfRange(entireTxtFileBuffer, placeOrder * placeReadLength, (placeOrder + 1) * placeReadLength);
    }

    public void close() throws IOException {
        fileChannel.close();
    }

    public byte[] getEntireTxtFileBuffer() {
        return entireTxtFileBuffer;
    }

    public int getBytesPerPlace() {
        return bytesPerPlace;
    }

    private void openForRead() throws Exception {
        fileChannel = FileChannel.open(filepath, OpenOperations[0]);
        entireTxtFileBuffer = readTextFileInMemory(fileChannel);
        bytesPerPlace = entireTxtFileBuffer.length / totalPlaces;

        if (bytesPerPlace < 1) {
            throw new InvalidNumberOfPlacesException(
                    "Txt file opened with too many Places (each place would have to read or write less than 1 byte)."
            );
        }
    }

    private byte[] readTextFileInMemory(FileChannel fileChannel) throws IOException {
        int bytesPerNode = (int) fileChannel.size() / totalNodes;

        if (myNodeId == totalNodes - 1) {
            int remainingBytes = (int) fileChannel.size() % totalNodes;
            bytesPerNode = remainingBytes > 0 ? remainingBytes : bytesPerNode;
        }

        ByteBuffer buffer = ByteBuffer.allocate(bytesPerNode);
        fileChannel.read(buffer);
        return buffer.array();
    }
}
