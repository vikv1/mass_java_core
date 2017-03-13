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

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.IO;

import java.io.IOException;
import java.nio.BufferUnderflowException;
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

    /** Includes all the agents residing locally on this place. */
	private Set<Agent> agents = Collections.synchronizedSet( new HashSet<Agent>( ) );

	private Vector< int[] > neighbors = null;

	private transient Log4J2Logger logger = Log4J2Logger.getInstance();

	//
	// Parallel I/O Fields
	//

	// Stores each file and its attributes
	protected static final Hashtable<Integer, FileAttributes> fileTable = new Hashtable<>();

	// Open options, 0 for READ, 1 for WRITE (used for opening file channels)
	private static final OpenOption[] OpenOperations = new OpenOption[] {READ, WRITE};

	// Counts the number of files open
	private static int count = 0;

	// Current file descriptor - used for giving each file a unique descriptor
	private static int fileDescriptor;

	private static long totalReadTime = 0;

	//
	// Private class that stores the file attributes needed for parallel I/O
	//

	private class FileAttributes {

		// Name of opened file
		private String fileName;

		// Number of places being used
		private int numberOfPlaces;

		// Number of read operations remaining (one per place)
		private int remainingReads;

		// Number of write operations remaining (one per place)
		private int remainingWrites;

		// File descriptor
		private int count;

		// Length for reading from the buffer
		private int readLength;

		// Buffer text files are read to
		private ByteBuffer buffer;

		// The file
		private Object file;

		// Variable to read or write (NetCDF)
		private Hashtable<String, Variable> variables;

		FileAttributes() {
			this.count = -1;
		}

		FileAttributes(String fileName, Object file, int numberOfPlaces, int count) {

			this.fileName = fileName;
			this.numberOfPlaces = numberOfPlaces;
			remainingReads = numberOfPlaces;
			remainingWrites = numberOfPlaces;
			this.file = file;
			this.count = count;
			readLength = 0;
		}

		FileAttributes(String fileName, Object file, int numberOfPlaces,
					   int count, Hashtable<String, Variable> variables) {
			this.fileName = fileName;
			this.numberOfPlaces = numberOfPlaces;
			remainingReads = numberOfPlaces;
			remainingWrites = numberOfPlaces;
			this.file = file;
			this.count = count;
			readLength = 0;
			this.variables = variables;
		}

		public synchronized int testAndDecrementReads() {
			int returnValue = remainingReads--;
			return returnValue;
		}

		// Decrements the number of remaining writes by 1
		public synchronized void decrementWrites() {
			this.remainingWrites -= 1;
		}

		//
		// Getter methods
		//

		public String getFileName() {
			return fileName;
		}

		public int getNumberOfPlaces() {
			return numberOfPlaces;
		}

		public int getRemainingWrites() {
			return remainingWrites;
		}

		public int getRemainingReads() {
			return remainingReads;
		}

		public int getCount() {
			return count;
		}

		public int getReadLength() {
			return readLength;
		}

		public ByteBuffer getBuffer() {
			return buffer;
		}

		public Object getFile() {
			return file;
		}

		public Hashtable<String, Variable> getVariables() {
			return variables;
		}

		public Variable getVariable(String varName) {
			return variables.get(varName);
		}

		//
		// Setter methods
		//

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public void setNumberOfPlaces(int numberOfPlaces) {
			this.numberOfPlaces = numberOfPlaces;
		}

		public void setRemainingReads(int remainingReads) {
			this.remainingReads = remainingReads;
		}

		public void setRemainingWrites(int remainingWrites) {
			this.remainingWrites = remainingWrites;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public void setReadLength(int readLength) {
			this.readLength = readLength;
		}

		//TODO check for privacy leaks
		public void setBuffer(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		//TODO check for privacy leaks
		public void setFile(Object file) {
			this.file = file;
		}

		//TODO check for privacy leaks
		public void setVariables(Hashtable<String, Variable> variables) {
			this.variables = variables;
		}
	}

	/**
	 * The first Place opens a file specified by the given filePath and ioType. If ioType is 0 then the file
	 * is opened for reading, if the ioType is 1 then the file is opened for writing (it is
	 * expected that the ioType is either 0 or 1; otherwise, -1 is returned). A file that is opened for reading
	 * will be opened in memory and added to the fileTable so that the file can be accessed by all Places. A
	 * file that is opened for writing will be opened on the disk and added to the fileTable so that the file
	 * can be accessed by all Places. A successfully opened file is given a unique file descriptor (integer)
	 * and the file descriptor is returned. An unsuccessfully opened file returns a file descriptor of -1.
	 *
	 * @param filePath
	 * @param ioType
	 * @return unique file descriptor for the newly opened file; otherwise returns -1
	 */
	protected int open(String filePath, int ioType) {

		if (ioType != 0 && ioType != 1) {
			throw new IllegalArgumentException("ioType must be either 0 (for read) or 1 (for write)");
		}

		// Create a path object from the given file path string
		Path path = Paths.get(filePath);

		// Ensure the file exists at the specified path
		if (!Files.exists(path)) {
			logger.error("The given file path does not exist");
			return -1;
		}

		// Isolate the file name
		String fileName = path.getFileName().toString();
		synchronized (fileTable) {

			// Only the first place opens the file
			if (!fileTable.containsKey(count - 1) ) {

				// Open the file if the file type is supported, return -1 if not supported
				if (fileName.toLowerCase().endsWith(".nc")) {
					fileDescriptor = openNetcdfFile(fileName, ioType);
				} else if (fileName.toLowerCase().endsWith(".txt")) {
					fileDescriptor = openTextFile(fileName, ioType, path);
				} else {
					logger.debug("File type not supported by MASS parallel I/O");
					return -1;
				}
				logger.debug(fileTable.get(fileDescriptor).getFileName() + " opened");
			}
		}

		// Return the file's unique file descriptor
		return fileDescriptor;
	}

	/**
	 * Private helper method that opens the given ncFileName (throws an IOException if the file does
	 * not exist) based on the given ioType, and add the file and its attributes to the fileTable.
	 * Returns the file's unique file descriptor if opened successfully; otherwise, returns -1.
	 *
	 * @param ncFileName
	 * @param ioType
	 * @return fileDescriptor
	 */
	private int openNetcdfFile(String ncFileName, int ioType) {
		NetcdfFile netcdfFile;

		// Read entire file into memory for reading
		if (ioType == 0) {
			try {
				netcdfFile = NetcdfFile.openInMemory(ncFileName);
			} catch (IOException e) {
				logger.debug("Exception opening netcdf file in memory: " + e);
				return -1;
			}
		}

		// Open file in disk for writing
		else {
			try {
				netcdfFile = NetcdfFile.open(ncFileName);
			} catch (IOException e) {
				logger.debug("Exception opening netcdf file on disk: " + e);
				return -1;
			}
		}

		List<Variable> varList = netcdfFile.getVariables();
		if (varList.isEmpty()) {
			logger.debug("No NetCDF variables to read");
		}

		Hashtable<String, Variable> variables = new Hashtable<String, Variable>();

		for (int i = 0; i < varList.size(); i++) {
			Variable currVar = varList.get(i);
			variables.put(currVar.getShortName(), currVar);
		}

		// Set file attributes and add them to the file table
		FileAttributes fileAttributes = new FileAttributes(ncFileName, netcdfFile, MASS.getCurrentPlacesBase().getPlacesSize(), count, variables);

		fileTable.put(count, fileAttributes);


		// Increment the file count since a file has been added to the file table
		count++;


		// Return the file's count (which is the file's unique descriptor)
		return fileAttributes.getCount();
	}

	/**
	 * Private helper method that opens the given txtFileName (throws an IOException if the file does
	 * not exist) based on the given ioType, and add the file and its attributes to the fileTable.
	 * Returns the file's unique file descriptor if opened successfully; otherwise, returns -1.
	 *
	 * @param txtFileName
	 * @param ioType
	 * @return fileDescriptor
	 */
	private int openTextFile(String txtFileName, int ioType, Path path) {
		FileChannel fileChannel;

		// opens a file, returning a FileChannel to access the supplied file
		// file is opened with the specified OpenOption of either READ or WRITE
		try {
			fileChannel = FileChannel.open(path, OpenOperations[ioType]);
		} catch (IOException e) {
			logger.debug("Exception opening text file: " + e);
			return -1;
		}

		// Set file attributes and add them to the file table
		FileAttributes fileAttributes = new FileAttributes(txtFileName, fileChannel, MASSBase.getCurrentPlacesBase().getPlacesSize(), count);

		// Set file attributes for a read operation
		if (ioType == 0) {

			// create a buffer that has the same space as the file being read
			try {
				fileAttributes.setBuffer(ByteBuffer.allocate((int) fileChannel.size()));
			} catch (IOException ioe) {
				logger.error("Could not create a buffer for the given text file: " + ioe);
				return -1;
			}

			ByteBuffer buffer = fileAttributes.getBuffer();

			// read the file contents to the buffer
			try {
				fileChannel.read(buffer);
			} catch (IOException ioe) {
				logger.error("Could not read the text file into the buffer: " + ioe);
				return -1;
			}

			buffer.flip();

			fileAttributes.setReadLength(buffer.capacity() / fileAttributes.getNumberOfPlaces());
		}

		fileTable.put(count, fileAttributes);

		// Increment the file count since a file has been added to the file table
		count++;

		// Return the file's count (which is the file's unique descriptor)
		return fileAttributes.getCount();
	}

	/**
	 * The read function used for Netcdf files
	 *
	 * Reads from the specified file descriptor into the given Hashtable of buffers
	 * @param fd specifies the file to read from
	 * @param ncData should contain the variable names to read, and their corresponding
	 *               array buffers (this is a UCAR array, the dimensions and data type must
	 *               match those of the Netcdf file being read)
     * @return true on a successful read; otherwise false
     */
	protected boolean read(int fd, Hashtable<String, Array> ncData) {
		synchronized (fileTable) {
			if (fileTable.containsKey(fd)) {
				FileAttributes fileAttributes = fileTable.get(fd);
				if (fileAttributes.getFileName().toLowerCase().endsWith(".nc")) {
					return readNetcdfFile(fileAttributes, ncData);
				} else {
					logger.debug("Given fd to read is not supported by MASS parallel I/O");
				}
			} else {
				logger.debug("Given fd to read does not exist in the file table (has not been opened)");
			}
		}
		return false;
	}

/*	private synchronized void addToReadTime(long amount) {
		totalReadTime += amount;
	}*/

	/**
	 * Private method that implements reading for Netcdf files
	 * @param fileAttributes the file attributes of the file to be read
	 * @param varsData the buffers to read into - variable name (key), data array (value)
     * @return true on success; otherwise false
     */
	// TODO currently each place reads a single index, each place should determine how much to read
	// based on the number of places, also assumes that the given data arrays are of the correct dimensions
	// (matches the dimensions of places)
	private boolean readNetcdfFile(FileAttributes fileAttributes, Hashtable<String, Array> varsData) {
		long start = System.currentTimeMillis();

		// Get all variable names
		Enumeration<String> varNames = varsData.keys();

		// Read one index of each variable
		while (varNames.hasMoreElements()) {

			String varName = varNames.nextElement();
			Variable var = fileAttributes.getVariable(varName);
			if (var == null) {
				logger.debug("Given variable: \"" + varName + "\" does not exist in: \""
						+ fileAttributes.getFileName() + "\"");
				return false;
			}

			// TODO: 1/30/17 test bellow
			List<Dimension> dimensions = var.getDimensions();

			int[] readDim = new int[dimensions.size()];

			for (int i = 0; i < dimensions.size(); i++) {
				readDim[i] = dimensions.get(i).getLength();
				//logger.debug(i + "dimension size: " + readDim[i]);
			}

			// Split along x axis
			readDim[0] = readDim[0] / fileAttributes.getNumberOfPlaces();

			int placeOrder = (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];
			//logger.debug("x dimension: " + readDim[0] + ", for place: " + placeOrder);

			// Read data and add to place storage
			try {

				Array userDataset = varsData.get(varName);
				ArrayFloat.D3 currVarData;

				// Read for 3D float
				// TODO: 1/13/17 there is probably a better way to do this so that each data type can be read
				// without having to write a separate implementation for each data type
				// (You will have to have separate implementations for each dimension if we want to support that)
				if (userDataset instanceof ArrayFloat.D3) {
					// read one element starting at this places index
					currVarData = (ArrayFloat.D3) var.read(new int[] { placeOrder, 0, 0 }, readDim);

					// logger.debug("Place Number: " + placeOrder + ", and read data: " + currVarData.toString());

					//((ArrayFloat.D3) userDataset).set(placeOrder, 0, 0, ??);

					for (int x = 0; x < readDim[0]; x++) {
						for (int y = 0; y < readDim[1]; y++) {
							for (int z = 0; z < readDim[2]; z++) {
								((ArrayFloat.D3) userDataset).set(x + placeOrder, y, z, currVarData.get(x, y, z));
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
		}

		long end = System.currentTimeMillis();
		//addToReadTime(end - start);
		return true;
	}

	/**
	 * The read function used for text files
	 *
	 * Reads from the specified file descriptor into the given byte buffer
	 * @param fd specifies the file to read from
	 * @param txtData the byte buffer to read into
	 * @return true on a successful read; otherwise false
	 */
	// TODO: 1/13/17 I don't believe the size of the given byte array is checked -
	// currently the implementation reads the whole specified text file and assumes the byte array is large
	// enough to store the data, this must be changed.
	protected boolean read(int fd, byte[] txtData) {
		if (fileTable.containsKey(fd)) {
			FileAttributes fileAttributes = fileTable.get(fd);
			if (fileAttributes.getFileName().toLowerCase().endsWith(".txt")) {
				return readTextFile(fileAttributes, txtData);
			}
		}
		return false;
	}

	private boolean readTextFile(FileAttributes fileAttributes, byte[] data) {

		try {

			// Get the buffer to read from
			ByteBuffer buffer = fileAttributes.getBuffer();

			// Temporary storage
			byte[] txtData;

			// Used for determining which part of the file to read
			int placeOrder = (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];

			int length = fileAttributes.getReadLength();

			// Determine if this place should read to the end of the file
			if (placeOrder != fileAttributes.getNumberOfPlaces() - 1) {		// No, read predetermined amount
																			// (currently 1 index)

				txtData = new byte[ length ];

				// Read from the file into the temp buffer
				synchronized (buffer) {        // TODO: test

					buffer.position(placeOrder * length);

					buffer.get(data, 0, length);
				}
			}

			// Perform final read
			// Read the remaining bytes of the file (this should be done by only the last Place)
			else {
				buffer.position(length * placeOrder);

				txtData = new byte[buffer.remaining()];

				int pos = 0;
				while(buffer.hasRemaining()) {
					data[pos] = buffer.get();
					pos++;
				}
			}

			// Copies the Place's temp buffer to the correct position in the entire array
			// of data that is passed by the user
			System.arraycopy(txtData, 0, data, placeOrder, txtData.length);
		}
		catch (BufferUnderflowException bue) {
			logger.debug(bue.toString());
			return false;
		}
		return true;
	}


	// TODO: 1/13/17 Once finished implementing and testing read() (including on mutliple nodes) add write() functionality


	/**
	 * Closes the specified file descriptor and removes it from the file table
	 * @param fd the file descriptor to close
	 * @return true if the file is successfully found in the file table, closed, and removed; otherwise false
     */
	protected boolean close( int fd ) {

		synchronized (fileTable) {

			// Check if the file exists in the file table
			if (fileTable.containsKey(fd)) {

				// Get the file
				String fileName = fileTable.get(fd).getFileName();
				Object file = fileTable.get(fd).getFile();

				// Closes Netcdf files
				if (file instanceof NetcdfFile) {
					try {
						((NetcdfFile) file).close();
						System.out.println(fileName + " closed.");
						fileTable.remove(fd);

						return true;
					} catch (IOException ioe) {
						logger.debug(ioe.toString());
					}
				}

				// Closes text files
				else if (file instanceof FileChannel) {
					try {
						((FileChannel) file).close();
						System.out.println(fileName + " closed");
						fileTable.remove(fd);

						return true;
					} catch (IOException ioe) {
						logger.debug(ioe.toString());
					}
				}
			}
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
	
}