package edu.uw.bothell.css.dsl.MASS.Parallel_IO;

import edu.uw.bothell.css.dsl.MASS.MASSBase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private static ByteBuffer dataOut = null;
    private static int numberOfPreparedPlace = 0;

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

        MASSBase.getLogger().trace(String.format("TxtFile::read { placeOffset: %d, placeReadLength: %d, placeOrder: %d, offset: %d }",
                placeOffset, placeReadLength, placeOrder, offset));

        MASSBase.getLogger().trace(String.format("TxtFile::read { entireTxtFileBuffer.length: %d }",
                entireTxtFileBuffer.length));

        return Arrays.copyOfRange(entireTxtFileBuffer, offset, offset + placeReadLength);
    }

    public void open(int ioType, int size) throws IOException {
        if(fileChannel != null) {
            throw new RuntimeException("Multiple calls to open");
        }
        openForWrite(size);
    }

    private void openForWrite(int size) throws IOException {
        String tempFileName = filepath.toString();
        tempFileName = tempFileName.replace(".txt", "_.txt");
        java.io.File newFile = new java.io.File(tempFileName);
        if(newFile.createNewFile()) {
            Path tempFilePath = Paths.get(tempFileName);
            fileChannel = FileChannel.open(tempFilePath, OpenOperations[1]);
            //dataOut = ByteBuffer.allocateDirect(size);
            dataOut = ByteBuffer.allocate(size);
        }
    }

    public boolean write(byte[] dataToWrite, int placeOrder)
            throws IOException, InvalidNumberOfPlacesException {

        synchronized (dataOut) {
            fillBufferWithByteData(dataToWrite, placeOrder);
            numberOfPreparedPlace++;

            if(numberOfPreparedPlace == myTotalPlaces) {
//                byte[] b = dataOut.array();
                dataOut.rewind();
                fileChannel.write(dataOut);
                return true;
            }
        }

        return false;
    }

    private void fillBufferWithByteData(byte[] dataToWrite, int placeOrder) throws InvalidNumberOfPlacesException {
        int placeOffset = getPlaceReadOffset(dataToWrite.length);
        int placeReadLength = getCurrentPlaceReadLength(dataToWrite.length, placeOffset, placeOrder);
        int offset = placeOffset * placeOrder;
        for(int i = offset; i < (offset+placeReadLength); i++) {
            dataOut.put(i, dataToWrite[i]);
        }

    }

    public void close() throws IOException {
        if(fileChannel != null) {
            fileChannel.close();
        }
    }

    public byte[] getEntireTxtFileBuffer() {
        return entireTxtFileBuffer;
    }
}
