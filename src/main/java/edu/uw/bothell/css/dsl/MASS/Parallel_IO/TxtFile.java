package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

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
public class TxtFile extends File {

    // Buffer text files are read to
    private byte[] entireTxtFileBuffer;

    private FileChannel fileChannel;

    // Open options, 0 for READ, 1 for WRITE (used for opening file channels)
    private static final OpenOption[] OpenOperations = new OpenOption[]{READ, WRITE};

    public TxtFile(Path filepath) {
        super(filepath, FileType.TXT);
    }

    public void open(int ioType) throws IOException, InvalidNumberOfNodesException {
        switch(ioType) {
            case OPEN_FOR_READ:
                openForRead();
                break;
            case OPEN_FOR_WRITE:
                break;
        }
    }

    private void openForRead() throws IOException, InvalidNumberOfNodesException {
        fileChannel = FileChannel.open(filepath, OpenOperations[0]);
        entireTxtFileBuffer = readTextFileInMemory(fileChannel);
    }

    private byte[] readTextFileInMemory(FileChannel fileChannel) throws IOException, InvalidNumberOfNodesException {
        // TODO: 4/19/17 issues with long to int and vice versa? (possibly when file size is large)
        int nodeOffset = getNodeReadOffset((int) fileChannel.size());
        int nodeReadLength = getCurrentNodeReadLength((int) fileChannel.size(), nodeOffset);
        int offset = myNodeId * nodeOffset;

        ByteBuffer buffer = ByteBuffer.allocate(nodeReadLength);
        fileChannel.read(buffer, (long) offset);
        return buffer.array();
    }

    public byte[] read(int placeOrder) throws InvalidNumberOfPlacesException {
        int placeOffset = getPlaceReadOffset(entireTxtFileBuffer.length);
        int placeReadLength = getCurrentPlaceReadLength(entireTxtFileBuffer.length, placeOffset, placeOrder);
        int offset = placeOffset * placeOrder;
        return Arrays.copyOfRange(entireTxtFileBuffer, offset, offset + placeReadLength);
    }

    public void close() throws IOException {
        fileChannel.close();
    }

    public byte[] getEntireTxtFileBuffer() {
        return entireTxtFileBuffer;
    }


}
