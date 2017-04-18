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

    public byte[] getEntireTxtFileBuffer() {
        return entireTxtFileBuffer;
    }

    public int getBytesPerPlace() {
        return bytesPerPlace;
    }

    public void openForRead() throws Exception {
        fileChannel = FileChannel.open(filepath, OpenOperations[0]);
        entireTxtFileBuffer = readTextFileInMemory(fileChannel);
        bytesPerPlace = entireTxtFileBuffer.length / totalPlaces;

        if (bytesPerPlace < 1) {
            throw new InvalidNumberOfPlacesException(
                    "Txt file opened with too many Places (each place would have to read or write less than 1 byte)."
            );
        }
    }

    public void openForWrite() {

    }

    public byte[] read(int placeOrder) {
        int placeReadLength = getPlaceReadLength(entireTxtFileBuffer.length, placeOrder);
        return Arrays.copyOfRange(entireTxtFileBuffer, placeOrder * placeReadLength, (placeOrder + 1) * placeReadLength);
    }

    public void close() throws IOException {
        fileChannel.close();
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

// Determine if this place should read to the end of the file
        /*if (placeOrder != totalPlaces - 1) {        // No, read predetermined amount
            for (int allIndex = placeOrder * bytesPerPlace, userIndex = 0; allIndex < bytesPerPlace * (placeOrder + 1); allIndex++, userIndex++) {
                userTxtBuffer[userIndex] = entireTxtFileBuffer[allIndex];
            }
        }

        // Perform final read
        // Read the remaining bytes of the file (this should be done by only the last Place)
        else {
            for (int allIndex = placeOrder * bytesPerPlace, userIndex = 0; allIndex < entireTxtFileBuffer.length; allIndex++, userIndex++) {
                userTxtBuffer[userIndex] = entireTxtFileBuffer[allIndex];
            }
        }*/
