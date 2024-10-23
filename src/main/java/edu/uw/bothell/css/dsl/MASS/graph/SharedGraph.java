/*

 	MASS Java Software License
	© 2012-2024 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.graph;

import java.util.Map;
import java.util.HashMap;
import java.io.File;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.*;
import org.apache.commons.lang3.SerializationUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.VertexPlace;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.infra.MASSSimpleDistributedMap;

// This class is used as a utility for reading/writing to /dev/shm only
public class SharedGraph {
    // for shared file
    private final String sharedDirectory = "/dev/shm/";
    private String sharedGraphName = null;
    private File sharedFile = null;
    private FileChannel sharedFileChannel = null;
    private MappedByteBuffer sharedFileBuffer = null;
    private int sharedFileSize;
    private boolean sharedFilePermissionSet = false;

    // constructor for shared graph
    public SharedGraph(String name) {
        this.sharedGraphName = name;
    }

    // shared file names
    private String getGraphFileName() {
        return sharedDirectory + "/" + sharedGraphName + "-" + "pid=" + MASSBase.getMyPid() + "-graph";
    }

    private String getDistributedMapFileName() {
        return sharedDirectory + "/" + sharedGraphName + "-" + "pid=" + MASSBase.getMyPid() + "-distributed_map";
    }

    private String getIdQueueFileName() {
        return sharedDirectory + "/" + sharedGraphName + "-" + "pid=" + MASSBase.getMyPid() + "-idqueue";
    }

    private String getNextVertexIDFileName() {
        return sharedDirectory + "/" + sharedGraphName + "-" + "pid=" + MASSBase.getMyPid() + "-nextvid";
    }

    // read/write data to shm
    private void writePlacesToShm(Vector<VertexPlace> places) {
        String filename = getGraphFileName();
        // setup shared file buffer
        sharedFile = new File(filename);
        try {
            // serialize
            byte[] byteArray = SerializationUtils.serialize(places);

            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_WRITE, 0, byteArray.length);

            // write
            sharedFileBuffer.put(byteArray);
            sharedFileBuffer.force();
            sharedFileChannel.close();

            // change the shared file permission to be accessed by other users               
            if (sharedFileExists(filename) && !multiUserCanAccessFile(filename)) {
                permitMultiUser(filename);
            }
            
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while writing AdjMap to shared memory!", e);
        }
        
    }

    public Vector<VertexPlace> readPlacesFromShm() {
        // setup shared file buffer
        String filename = getGraphFileName();
        sharedFile = new File(filename);
        Vector<VertexPlace> places = null;
        try {
            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ);
            sharedFileSize = (int)sharedFileChannel.size();
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_ONLY, 0, sharedFileSize);

            // read and deserialize
            byte[] byteArray = new byte[sharedFileBuffer.remaining()];
            sharedFileBuffer.get(byteArray);
            places = SerializationUtils.deserialize(byteArray);
            sharedFileChannel.close();
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while reading places from shared memory!", e);
            return null;
        }
        return places;
    }

    private void writeDistributedMapToShm(MASSSimpleDistributedMap distributed_map) {
        String filename = getDistributedMapFileName();
        // setup shared file buffer
        sharedFile = new File(filename);
        try {
            // serialize
            byte[] byteArray = SerializationUtils.serialize(distributed_map);

            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_WRITE, 0, byteArray.length);

            // write
            sharedFileBuffer.put(byteArray);
            sharedFileBuffer.force();
            sharedFileChannel.close();

            // change the shared file permission to be accessed by other users               
            if (sharedFileExists(filename) && !multiUserCanAccessFile(filename)) {
                permitMultiUser(filename);
            }
            
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while writing distributed map to shared memory!", e);
        }
        
    }

    public MASSSimpleDistributedMap<Object, Integer> readDistributedMapFromShm() {
        // setup shared file buffer
        String filename = getDistributedMapFileName();
        sharedFile = new File(filename);
        MASSSimpleDistributedMap<Object, Integer> distributed_map = null;
        try {
            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ);
            sharedFileSize = (int)sharedFileChannel.size();
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_ONLY, 0, sharedFileSize);

            // read and deserialize
            byte[] byteArray = new byte[sharedFileBuffer.remaining()];
            sharedFileBuffer.get(byteArray);
            distributed_map = SerializationUtils.deserialize(byteArray);
            sharedFileChannel.close();
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while reading distributed map from shared memory!", e);
            return null;
        }
        return distributed_map;
    }

    private void writeIdQueueToShm(ConcurrentLinkedQueue<Integer> idQueue) {
        String filename = getIdQueueFileName();
        // setup shared file buffer
        sharedFile = new File(filename);
        try {
            // serialize
            byte[] byteArray = SerializationUtils.serialize(idQueue);

            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_WRITE, 0, byteArray.length);

            // write
            sharedFileBuffer.put(byteArray);
            sharedFileBuffer.force();
            sharedFileChannel.close();

            // change the shared file permission to be accessed by other users               
            if (sharedFileExists(filename) && !multiUserCanAccessFile(filename)) {
                permitMultiUser(filename);
            }
            
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while writing idQueue to shared memory!", e);
        }
        
    }

    public ConcurrentLinkedQueue<Integer> readIdQueueFromShm() {
        // setup shared file buffer
        String filename = getIdQueueFileName();
        sharedFile = new File(filename);
        ConcurrentLinkedQueue<Integer> idQueue = null;
        try {
            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ);
            sharedFileSize = (int)sharedFileChannel.size();
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_ONLY, 0, sharedFileSize);

            // read and deserialize
            byte[] byteArray = new byte[sharedFileBuffer.remaining()];
            sharedFileBuffer.get(byteArray);
            idQueue = SerializationUtils.deserialize(byteArray);
            sharedFileChannel.close();
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while reading idQueue from shared memory!", e);
            return null;
        }
        return idQueue;
    }

    private void writeNextVertexIDToShm(int nextVertexID) {
        String filename = getNextVertexIDFileName();
        // setup shared file buffer
        sharedFile = new File(filename);
        try {
            // serialize
            byte[] byteArray = SerializationUtils.serialize(new Integer(nextVertexID));

            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_WRITE, 0, byteArray.length);

            // write
            sharedFileBuffer.put(byteArray);
            sharedFileBuffer.force();
            sharedFileChannel.close();

            // change the shared file permission to be accessed by other users               
            if (sharedFileExists(filename) && !multiUserCanAccessFile(filename)) {
                permitMultiUser(filename);
            }
            
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while writing nextVertexID to shared memory!", e);
        }
        
    }

    public int readNextVertexIDFromShm() {
        // setup shared file buffer
        String filename = getNextVertexIDFileName();
        sharedFile = new File(filename);
        Integer nextVertexID = 0;
        try {
            sharedFileChannel = FileChannel.open(sharedFile.toPath(), StandardOpenOption.READ);
            sharedFileSize = (int)sharedFileChannel.size();
            sharedFileBuffer = sharedFileChannel.map(MapMode.READ_ONLY, 0, sharedFileSize);

            // read and deserialize
            byte[] byteArray = new byte[sharedFileBuffer.remaining()];
            sharedFileBuffer.get(byteArray);
            nextVertexID = SerializationUtils.deserialize(byteArray);
            sharedFileChannel.close();
        } catch (Exception e) {
            MASSBase.getLogger().debug("exception while reading nextVertexID from shared memory!", e);
            return -1;
        }
        return nextVertexID.intValue();
    }

    // check shared file existence
    private boolean sharedFileExists(String filename) {
        sharedFile = new File(filename); 
        if (sharedFile.isFile()) {
            return true;
        }
        return false;
    }

    public boolean placesFileExists() {
        return sharedFileExists(getGraphFileName());
    }

    public boolean distributedMapFileExists() {
        return sharedFileExists(getDistributedMapFileName());
    }

    public boolean idQueueFileExists() {
        return sharedFileExists(getIdQueueFileName());
    }

    public boolean nextVertexIDFileExists() {
        return sharedFileExists(getNextVertexIDFileName());
    }

    // function for checking if a file can be accessed by multi-user
    private boolean multiUserCanAccessFile(String filePath) {
        Path file = FileSystems.getDefault().getPath(filePath);

        try {
            // Attempt to read the current permissions
            Set<PosixFilePermission> currentPermissions = Files.getPosixFilePermissions(file);

            // Check if others have read and write permissions
            if (currentPermissions.contains(PosixFilePermission.OTHERS_READ) &&
                currentPermissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            // Unable to access file
            MASSBase.getLogger().debug("Error checking file permissions: " + e.getMessage());
            return false;
        }
    }

    // function for changing permissions of a shared file
    private void permitMultiUser(String filePath) throws IOException {
        Path file = FileSystems.getDefault().getPath(filePath);

        // Set read and write permissions for owner, group, and others
        Set<PosixFilePermission> permissions = EnumSet.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_WRITE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_WRITE
        );

        Files.setPosixFilePermissions(file, permissions);
    }

    public String getSharedPlaceName() {
        return sharedGraphName;
    }

    public void writeDataToShm(Vector<VertexPlace> places, MASSSimpleDistributedMap distributed_map, ConcurrentLinkedQueue<Integer> idQueue, int nextVertexID) {
        writePlacesToShm(places);
        writeDistributedMapToShm(distributed_map);
        writeIdQueueToShm(idQueue);
        writeNextVertexIDToShm(nextVertexID);
    }
}