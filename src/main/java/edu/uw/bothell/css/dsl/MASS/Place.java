/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.Parallel_IO.FileAttributes;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.NetcdfFileAttributes;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.TxtFileAttributes;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import sun.nio.ch.Net;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 *	Place represents a single element from a collection of places distributed
 *	among all cluster nodes. A Place may contain a collection of Agents that
 *	perform operations on objects contained within the Place. 
 *
 */
public class Place {


	/**
	 * Defines the size of the matrix that consists of application-specific
	 * places. Intuitively, size[0], size[1], and size[2] correspond to the size
	 * of x, y, and z, or that of i, j, and k.
	 */
	private int[] size;

	/**
	 * Is an array that maintains each place’s coordinates. Intuitively,
	 * index[0], index[1], and index[2] correspond to coordinates of x, y, and
	 * z, or those of i, j, and k.
	 */
	private int[] index;

	/**
	 * Stores a set arguments to be passed to a set of remote-cell functions
	 * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	 * nearest future. The argument size must be specified with
	 * outMessage_size.
	 */
	private Object outMessage = null;

	/**
	 * Receives a return value in inMessages[i] from a function call made to
	 * the i-th remote cell through exchangeAll( ) and exchangeSome( ).
	 * Each element size must be specified with inMessage_size.
	 */
	private Object[] inMessages = null;

	/**
	 * Includes all the agents residing locally on this place.
	 */
	private Set<Agent> agents = Collections.synchronizedSet(new HashSet<Agent>());

	private Vector<int[]> neighbors = null;

	private transient Log4J2Logger logger = Log4J2Logger.getInstance();

	//
	// Parallel I/O Fields
	//

	// Stores each file and its attributes
	protected static final Hashtable<Integer, FileAttributes> fileTable = new Hashtable<>();

	// Open options, 0 for READ, 1 for WRITE (used for opening file channels)
	private static final OpenOption[] OpenOperations = new OpenOption[]{READ, WRITE};

	// Counts the number of files open
	private static int fileDescriptorIndex = 0;

	// File descriptor value that each place has access too
	private static int fileDescriptor;

	private static final Hashtable<Integer, Boolean> filesAttemptedToClose = new Hashtable<Integer, Boolean>();

	private static long totalReadTime = 0;

	/**
	 * The first Place opens a file specified by the given filePath and ioType. If ioType is 0 then the file
	 * is opened for reading, if the ioType is 1 then the file is opened for writing (it is
	 * expected that the ioType is either 0 or 1; otherwise, -1 is returned). A file that is opened for reading
	 * will be opened in memory and added to the fileTable so that the file can be accessed by all Places. A
	 * file that is opened for writing will be opened on the disk and added to the fileTable so that the file
	 * can be accessed by all Places. A successfully opened file is given a unique file descriptor (integer)
	 * and the file descriptor is returned. An unsuccessfully opened file returns a file descriptor of -1.
	 *
	 * @param filepath
	 * @param ioType
	 * @return unique file descriptor for the newly opened file; otherwise returns -1
	 */
	protected int open(String filepath, int ioType) {
		synchronized (fileTable) {
			if (!fileTable.containsKey(fileDescriptorIndex - 1)) {	// Only one place should open the file
				try {

					if (ioType != 0 && ioType != 1) {
						throw new IllegalArgumentException("ioType must be either 0 (for read) or 1 (for write)");
					}
					Path path = Paths.get(filepath);

					if (!Files.exists(path)) {
						throw new IllegalArgumentException("The given file to open does not exist: " + path);
					}

					if (ioType == 0) {
						fileDescriptor = openFileForRead(path);
					} else {
						fileDescriptor = openFileForWrite(path);
					}
				} catch (Exception e) {
					logger.debug(String.format("An exception occurred while opening the file: %s, exception: %s", filepath, e.getMessage()));
					return -1;
				}
				logger.debug(filepath + " opened on Node " + MASSBase.getMyPid());
			}
		}
		return fileDescriptor;
	}


	private int openFileForRead(Path filepath) throws IOException, InvalidRangeException {
		String fileName = filepath.getFileName().toString();

		if (fileName.toLowerCase().endsWith(".nc")) {
			fileDescriptor = openNetcdfFileInMemory(fileName, filepath);
		} else if (fileName.toLowerCase().endsWith(".txt")) {
			fileDescriptor = openTextFileInMemory(fileName, filepath);
		} else {
			logger.error("File type to open is not supported by MASS parallel I/O: " + fileName);
			return -1;
		}
		return fileDescriptor;
	}

	private int openFileForWrite(Path filepath) {
		return -1;
	}

	/**
	 * Private helper method that opens the given ncFileName (throws an IOException if the file does
	 * not exist) based on the given ioType, and add the file and its attributes to the fileTable.
	 * Returns the file's unique file descriptor if opened successfully; otherwise, returns -1.
	 *
	 * @param ncFileName
	 * @return fileDescriptor
	 */
	private int openNetcdfFileInMemory(String ncFileName, Path path) throws IOException, InvalidRangeException {
		NetcdfFile netcdfFile;
		netcdfFile = NetcdfFile.openInMemory(path.toString());
		Hashtable<String, Object> netcdfVariables = readNetcdfVariables(netcdfFile);
		FileAttributes fileAttributes = new NetcdfFileAttributes(fileDescriptorIndex, path, netcdfFile, netcdfVariables);
		fileTable.put(fileDescriptorIndex, fileAttributes);
		return fileDescriptorIndex++;
	}

	private Hashtable<String, Object> readNetcdfVariables(NetcdfFile netcdfFile) throws InvalidRangeException, IOException {
		List<Variable> unReadVariables = netcdfFile.getVariables();

		if (unReadVariables.isEmpty()) {
			logger.debug("No NetCDF variables to read in: " + netcdfFile.getCacheName());
		}

		Hashtable<String, Object> readVariables = new Hashtable<String, Object>();

		for (int i = 0; i < unReadVariables.size(); i++) {
			Variable currentUnreadVariable = unReadVariables.get(i);
			// TODO: 3/31/17 read only what is needed for this node
			Array varData = currentUnreadVariable.read(new int[currentUnreadVariable.getShape().length], currentUnreadVariable.getShape());
			readVariables.put(currentUnreadVariable.getShortName(), varData.copyTo1DJavaArray());
		}
		return readVariables;
	}

	/**
	 * Private helper method that opens the given txtFileName (throws an IOException if the file does
	 * not exist) based on the given ioType, and add the file and its attributes to the fileTable.
	 * Returns the file's unique file descriptor if opened successfully; otherwise, returns -1.
	 *
	 * @param txtFileName
	 * @param
	 * @return fileDescriptor
	 */
	private int openTextFileInMemory(String txtFileName, Path path) throws IOException {
		FileChannel fileChannel;

		fileChannel = FileChannel.open(path, OpenOperations[0]);

		byte[] txtFile = readTextFileInMemory(fileChannel);

		// Set file attributes and add them to the file table
		FileAttributes fileAttributes = new TxtFileAttributes(fileDescriptorIndex, path, fileChannel, txtFile);

		fileTable.put(fileDescriptorIndex, fileAttributes);

		return fileDescriptorIndex++;
	}

	private byte[] readTextFileInMemory(FileChannel fileChannel) throws IOException{
		ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
		fileChannel.read(buffer);
		return buffer.array();
	}

	/**
	 * The read function used for Netcdf files
	 * <p>
	 * Reads from the specified file descriptor into the given Hashtable of buffers
	 *
	 * @param fd             specifies the file to read from
	 * @param variableToRead should contain the variable names to read, and their corresponding
	 *                       array buffers (this is a UCAR array, the dimensions and data type must
	 *                       match those of the Netcdf file being read)
	 * @return true on a successful read; otherwise false
	 */
	protected boolean read(int fd, String variableToRead, Object variableBuffer) {
		//logger.debug("PARALLEL IO: Read started.");
		//synchronized (fileTable) {
		if (fileTable.containsKey(fd)) {
			FileAttributes fileAttributes = fileTable.get(fd);
			if (fileAttributes instanceof NetcdfFileAttributes) {
				return readNetcdfFile((NetcdfFileAttributes) fileAttributes, variableToRead, variableBuffer);
			} else {
				logger.debug("Given fd to read is not supported by MASS parallel I/O");
			}
		} else {
			logger.debug("Given fd to read does not exist in the file table (has not been opened)");
		}
		//}
		return false;
	}


	/**
	 * Private method that implements reading for Netcdf files
	 *
	 * @param fileAttributes the file attributes of the file to be read
	 * @param variableToRead the buffers to read into - variable name (key), data array (value)
	 * @return true on success; otherwise false
	 */
	// TODO currently each place reads a single index, each place should determine how much to read
	// based on the number of places, also assumes that the given data arrays are of the correct dimensions
	// (matches the dimensions of places)
	private boolean readNetcdfFile(NetcdfFileAttributes fileAttributes, String variableToRead, Object userVariableBuffer) {

		Object allVariableData = fileAttributes.getVariable(variableToRead);
		if (allVariableData == null) {
			logger.error("Given variable: \"" + variableToRead + "\" does not exist in: \""
					+ fileAttributes.getFileName() + "\"");
			return false;
		}

		int placeOrder = (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];

		if (userVariableBuffer instanceof float[]) {

			try {
				float[] userFloatBuffer = (float[]) userVariableBuffer;
				float[] allVariableFloatData = (float[]) allVariableData;

				int placeReadLength = allVariableFloatData.length / MASSBase.getCurrentPlacesBase().getTotalPlaces();

				if (placeReadLength < 1) {
					logger.debug("Too many places attempting to read a NetCDF file.");
					return false;
				}

				if (placeOrder <  MASSBase.getCurrentPlacesBase().getTotalPlaces() - 1) {
					for (int i = placeOrder * placeReadLength, j = 0; i < placeReadLength * (placeOrder + 1); i++, j++) {
						userFloatBuffer[j] = allVariableFloatData[i];
					}
					/*logger.debug("Place: " + placeOrder + ", read: " + placeOrder * placeReadLength + " to " +
							placeReadLength * (placeOrder + 1));*/
				} else {
					for (int i = placeOrder * placeReadLength, j = 0; i < allVariableFloatData.length; i++, j++) {
						userFloatBuffer[j] = allVariableFloatData[i];
					}
					/*logger.debug("Place: " + placeOrder + ", read: " + placeOrder * placeReadLength + " to " +
							allVariableFloatData.length);*/
				}

				/*logger.debug("PARALLEL IO: NetCDF Read finished successfully for Place: " + placeOrder + ", " +
						"running on Machine: " + MASS.getMyPid());*/


			} catch (ClassCastException cce) {
				logger.error("Given buffer to read into does not match the NetCDF file data to read.");
				return false;
			} catch (ArrayIndexOutOfBoundsException oob) {
				logger.error("Given buffer to read into is not large enough to hold the NetCDF file data to read.");
				return false;
			}
		} else {
			logger.error("Given buffer to read into is not a supported data type.");
			return false;
		}
		return true;
	}

	/**
	 * The read function used for text files
	 * <p>
	 * Reads from the specified file descriptor into the given byte buffer
	 *
	 * @param fd      specifies the file to read from
	 * @param txtData the byte buffer to read into
	 * @return true on a successful read; otherwise false
	 */
	// TODO: 1/13/17 I don't believe the size of the given byte array is checked -
	// currently the implementation reads the whole specified text file and assumes the byte array is large
	// enough to store the data, this must be changed.
	protected boolean read(int fd, byte[] txtData) {
		if (fileTable.containsKey(fd)) {
			FileAttributes fileAttributes = fileTable.get(fd);
			if (fileAttributes instanceof TxtFileAttributes) {
				return readTextFile((TxtFileAttributes) fileAttributes, txtData);
			}
		}
		return false;
	}

	private boolean readTextFile(TxtFileAttributes fileAttributes, byte[] data) {

		try {

			// Get the buffer to read from
			byte[] buffer = fileAttributes.getBuffer();

			// Used for determining which part of the file to read
			int placeOrder = (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];

			int length = fileAttributes.getBytesPerPlace();

			// Determine if this place should read to the end of the file
			if (placeOrder != MASSBase.getCurrentPlacesBase().getTotalPlaces() - 1) {        // No, read predetermined amount
				// (currently 1 index)

				// Read from the file into the temp buffer

				for (int i = placeOrder * length; i < length * (placeOrder + 1); i++) {
					data[i - (length * MASS.getMyPid())] = buffer[i];
				}
			}

			// Perform final read
			// Read the remaining bytes of the file (this should be done by only the last Place)
			else {
				for (int i = placeOrder * length; i < buffer.length; i++) {
					data[i - (length * MASS.getMyPid())] = buffer[i];
				}
			}
		} catch (ArrayIndexOutOfBoundsException oob) {
			logger.error("Given buffer to read into is not large enough to hold the TXT file data to read.");
			return false;
		}
		return true;
	}


	// TODO: 1/13/17 Once finished implementing and testing read() (including on mutliple nodes) add write() functionality


	/**
	 * Closes the specified file descriptor and removes it from the file table
	 *
	 * @param fd the file descriptor to close
	 * @return true if the file is successfully found in the file table, closed, and removed; otherwise false
	 */
	protected synchronized boolean close(int fd) {

		// Check if the file exists in the file table
		if (fileTable.containsKey(fd)) {

			FileAttributes fileAttributes = fileTable.get(fd);

			// Get the file
			Object file = fileAttributes.getFile();

			// Closes Netcdf files
			if (file instanceof NetcdfFile) {
				try {
					((NetcdfFile) file).close();
					fileTable.remove(fd);
					filesAttemptedToClose.put(fd, true);
					logger.debug("CLOSE SUCCESS");
					return true;
				} catch (IOException ioe) {
					logger.error("An IO Exception occurred while attempting to close a NetCDF file: "
							+ ioe.toString());
					filesAttemptedToClose.put(fd, false);
					return false;
				}
			}

			// Closes text files
			else if (file instanceof FileChannel) {
				try {
					((FileChannel) file).close();
					fileTable.remove(fd);
					filesAttemptedToClose.put(fd, true);
					return true;
				} catch (IOException ioe) {
					logger.error("An IO Exception occurred while attempting to close a TXT file: "
							+ ioe.toString());
					filesAttemptedToClose.put(fd, false);
					return false;
				}
			}
		} else if (filesAttemptedToClose.containsKey(fd)) {
			return filesAttemptedToClose.get(fd);
		}

		return false;
	}


	/**
	 * Is called from Places.callAll( ), callSome( ), exchangeAll( ), and
	 * exchangeSome( ), and invoke the function specified with functionId as
	 * passing arguments to this function. A user-derived Place class must
	 * implement this method.
	 * @param functionId
	 * @param argument
	 * @return 
	 */
	public Object callMethod( int functionId, Object argument ) {
		return null;
	}

	private Place findDstPlace( int handle, int offset[] ) {

		// Compute the global linear index from offset[]
		PlacesBase places = MASSBase.getPlacesMap().get( new Integer( handle ) );
		int[] neighborCoord = new int[places.getSize().length];
		places.getGlobalNeighborArrayIndex( index, offset, places.getSize(),
				neighborCoord );
		int globalLinearIndex
		= places.getGlobalLinearIndexFromGlobalArrayIndex( neighborCoord,
				places.getSize() );

		if ( globalLinearIndex == Integer.MIN_VALUE )
			return null;

		// Identify the destination place
		int destinationLocalLinearIndex
		= globalLinearIndex - places.getLowerBoundary();

		Place destintationPlace = null;
		int shadowIndex;
		if ( destinationLocalLinearIndex >= 0 &&
				destinationLocalLinearIndex < places.getPlacesSize() )
			destintationPlace = places.getPlaces()[ destinationLocalLinearIndex ];
		else if ( destinationLocalLinearIndex < 0 &&
				( shadowIndex = destinationLocalLinearIndex + 
				places.getShadowSize() ) >= 0 )
			destintationPlace = places.getLeftShadow()[ shadowIndex ];
		else if ( (shadowIndex = 
				destinationLocalLinearIndex - places.getPlacesSize()) >= 0
				&& shadowIndex < places.getShadowSize() )
			destintationPlace = places.getRightShadow()[ shadowIndex ];

		return destintationPlace;
	
	}

	public synchronized Set<Agent> getAgents() {
		return agents;
	}
	
	public int getNumAgents() {
		return agents.size();
	}

	public Number getDebugData()
	{
		return null;
	}
	
	// To be overridden by developer - for debugging
	public void setDebugData(Number argument) {
	}

	public int[] getIndex() {
		return index;
	}

	public Object[] getInMessages() {
		return inMessages;
	}

	public Vector<int[]> getNeighbours() {
		return neighbors;
	}

	public void setNeighbors(Vector<int[]> neighbors)
	{
		this.neighbors = neighbors;
	}

	protected Object getOutMessage() {
		return outMessage;
	}

	public Object getOutMessage( int handle, int[] offsetIndex ) {

		Place dstPlace = findDstPlace( handle, offsetIndex );

		// return the destination outMessage
		return ( dstPlace != null ) ? dstPlace.outMessage : null;
	
	}

	/**
	 * Returns the size of the matrix that consists of application-specific
	 * places. Intuitively, size[0], size[1], and size[2] correspond to the size
	 * of x, y, and z, or that of i, j, and k.
	 * @return 
	 */
	public int[] getSize() {
		return size;
	}

	protected void putInMessage( int handle, int[] offsetIndex, int position, Object value ) {

		Place dstPlace = findDstPlace( handle, offsetIndex );

		// Write to the destination inMessage[position]
		if ( dstPlace != null && position < dstPlace.inMessages.length )
			dstPlace.inMessages[position] = value;
	
	}

	protected void setIndex(int[] index) {
		this.index = index.clone();
	}

	// To be overridden by developer - for debugging
	public void setDebugData(Object argument) {
	}

	public void setInMessages(Object[] inMessages) {
		this.inMessages = inMessages;
	}

	public void setOutMessage(Object outMessage) {
		this.outMessage = outMessage;
	}

	protected void setSize(int[] size) {
		this.size = size.clone();
	}

	// TODO: 3/31/17  Old netcdf read - remove when no longer needed
	/*List<Dimension> dimensions = var.getDimensions();

			int[] readDim = new int[dimensions.size()];

			for (int i = 0; i < dimensions.size(); i++) {
				readDim[i] = dimensions.get(i).getLength();
			}

			// Split along x axis
			readDim[0] = readDim[0] / fileAttributes.getNumberOfPlaces();

			int placeOrder = (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];

			logger.debug("PLACE: " + placeOrder);
			logger.debug("READ DIM: " + Arrays.toString(readDim));

			// Read data and add to place storage
			try {

				Number[][][] userDataset = varsData.get(varName);
				ArrayFloat.D3 currVarData;

				// Read for 3D float
				// TODO: 1/13/17 there is probably a better way to do this so that each data type can be read
				// without having to write a separate implementation for each data type
				// (You will have to have separate implementations for each dimension if we want to support that)
				if (userDataset instanceof Float[][][]) {

					// Netcdf is not thread-safe
					// synchronized (var) {
						// read section of file
						currVarData = (ArrayFloat.D3) var.read(new int[]{ placeOrder * readDim[0], 0, 0}, readDim);
					// }

					synchronized (userDataset) {
						// Write the read data to the user's buffer
						for (int x = 0; x < readDim[0]; x++) {
							for (int y = 0; y < readDim[1]; y++) {
								for (int z = 0; z < readDim[2]; z++) {
									((ArrayFloat.D3) userDataset).set(x + (placeOrder * readDim[0]), y, z, currVarData.get(x, y, z));
								}
							}
						}
					}
				}

			} catch (InvalidRangeException ire) {
				logger.debug("Invalid range: " + ire);
				return false;
			} catch (IOException ioe) {
				logger.debug("Invalid range: " + ioe);
				return false;
			}
		}*/
	
}