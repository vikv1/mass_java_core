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

import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

	private Vector<int[]> neighbours = null;

	/**
	 * stores all files that have been opened
	 * String array stores file name at index 0
	 * boolean has been read at index 1
	 * number of places (for write at index 2)
	 */

	// stores each file and its attributes
	protected static Hashtable<Integer, FileAttributes> fileTable = new Hashtable<>();

	// open options, 0 for READ, 1 for WRITE
	private static final OpenOption[] OpenOperations = new OpenOption[]{READ, WRITE};

	// counts the number of files open
	private static int count = 0;

	// current file descriptor (for open method)
	// TODO: Check where used
	private static int fileDescriptor;

	public static final int READ_ = 0;

	public static final int WRITE_ = 1;


	// private class that stores all of a file's attributes
	private class FileAttributes {

		// name of opened file
		private String fileName;

		// number of places being used
		private int numberOfPlaces;

		// number of read operations remaining (one per place)
		private int remainingReads;

		// number of write operations remaining (one per place)
		private int remainingWrites;

		// this files count number (unique for each file)
		private int count;

		// length for reading from the buffer
		private int readLength;

		// buffer text files are read to
		private ByteBuffer buffer;

		// file descriptor
		private Object file;

		// variable to read or write (Netcdf)
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

		FileAttributes(String fileName, Object file, int numberOfPlaces, int count, Hashtable<String, Variable> variables) {
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

		// decrements the number of remaining writes by 1
		public synchronized void decrementWrites() {
			this.remainingWrites -= 1;
		}

		// getter methods

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

		// setter methods//

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

		public void setBuffer(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		public void setFile(Object file) {
			this.file = file;
		}

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
	 * An IOException is thrown if the given filePath does not exist.
	 *
	 * @param filePath
	 * @param ioType
	 * @return fileDescriptor
	 */
	protected int open(String filePath, int ioType) throws IOException {
		if (ioType != 0 && ioType != 1) {
			throw new IllegalArgumentException("ioType must be either 0 (for read) or 1 (for write)");
		}
		// create a path object from the given file path string
		Path path = Paths.get(filePath);

		// isolate the file name
		String fileName = path.getFileName().toString();

		synchronized (fileTable) {
			// only the first place opens the file
			if (!fileTable.containsKey(count) && index[0] == 0 && index[1] == 0 && index[2] == 0) {

				// open the file if the file type is supported, return -1 if not supported
				if (fileName.toLowerCase().endsWith(".nc")) {
					fileDescriptor = openNetcdfFile(fileName, ioType);
				} else if (fileName.toLowerCase().endsWith(".txt")) {
					fileDescriptor = openTextFile(fileName, ioType, path);
				} else {
					System.err.println("File type not supported by MASS parallel I/O");
					return -1;
				}
				System.out.println(fileTable.get(fileDescriptor).getFileName() + " opened");
			}
		}

		// return the file's unique file descriptor
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

		// read entire file into memory for reading
		if (ioType == 0) {
			try {
				netcdfFile = NetcdfFile.openInMemory(ncFileName);
			} catch (IOException e) {
				System.err.println("Exception opening netcdf file in memory: " + e);
				return -1;
			}
		}

		// open file in disk for writing
		else {
			try {
				netcdfFile = NetcdfFile.open(ncFileName);
			} catch (IOException e) {
				System.err.print("Exception opening netcdf file on disk: " + e);
				return -1;
			}
		}

		List<Variable> varList = netcdfFile.getVariables();
		if (varList.isEmpty()) {
			System.err.println("No Netcdf variables to read");
		}

		Hashtable<String, Variable> variables = new Hashtable<String, Variable>();

		for (int i = 0; i < varList.size(); i++) {
			Variable currVar = varList.get(i);
			variables.put(currVar.getShortName(), currVar);
		}

		// set file attributes and add them to the file table
		FileAttributes fileAttributes = new FileAttributes(ncFileName, netcdfFile, MASS.getCurrentPlaces().getPlacesSize(), count, variables);

		fileTable.put(count, fileAttributes);


		// increment the file count since a file has been added to the file table
		count++;


		// return the file's count (which is the file's unique descriptor)
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
	private int openTextFile(String txtFileName, int ioType, Path path) throws IOException {
		FileChannel fileChannel;

		// opens a file, returning a FileChannel to access the supplied file
		// file is opened with the specified OpenOption of either READ or WRITE
		try {
			fileChannel = FileChannel.open(path, OpenOperations[ioType]);
		} catch (IOException e) {
			System.err.println("Exception opening text file: " + e);
			return -1;
		}

		// set file attributes and add them to the file table
		FileAttributes fileAttributes = new FileAttributes(txtFileName, fileChannel, MASS_base.getCurrentPlaces().getPlacesSize(), count);

		// set file attributes for a read operation
		if (ioType == 0) {

			// create a buffer that has the same space as the file being read
			fileAttributes.setBuffer(ByteBuffer.allocate((int) fileChannel.size()));

			ByteBuffer buffer = fileAttributes.getBuffer();

			// read the file contents to the buffer
			fileChannel.read(buffer);

			buffer.flip();

			fileAttributes.setReadLength(buffer.capacity() / fileAttributes.getNumberOfPlaces());
		}

		fileTable.put(count, fileAttributes);

		// increment the file count since a file has been added to the file table
		count++;

		// return the file's count (which is the file's unique descriptor)
		return fileAttributes.getCount();
	}

	// read function used for text files
	protected boolean read(int fd, byte[] txtData) {
		if (fileTable.containsKey(fd)) {
			FileAttributes fileAttributes = fileTable.get(fd);
			if (fileAttributes.getFileName().toLowerCase().endsWith(".txt")) {
				return readTextFile(fileAttributes, txtData);
			}
		}
		return false;
	}

	// read function used for netcdf files
	protected boolean read(int fd, Hashtable<String, Array> ncData) {
		synchronized (fileTable) {
			if (fileTable.containsKey(fd)) {

				FileAttributes fileAttributes = fileTable.get(fd);
				if (fileAttributes.getFileName().toLowerCase().endsWith(".nc")) {
					return readNetcdfFile(fileAttributes, ncData);
				} else {
					System.err.println("Given fd to read is not supported by MASS parallel I/O");
				}
			} else {
				System.err.println("Given fd to read does not exist in the file table (has not been opened)");
			}
		}
		return false;
	}

	// assume number of places match the number of netcdf indexes
	private boolean readNetcdfFile(FileAttributes fileAttributes, Hashtable<String, Array> varsData) {

		// get all variable names
		Enumeration<String> varNames = varsData.keys();

		// read one index of each variable
		while (varNames.hasMoreElements()) {

			String varName = varNames.nextElement();
			Variable var = fileAttributes.getVariable(varName);
			if (var == null) {
				System.err.println("Given varaible: \"" + varName + "\" does not exist in: \"" + fileAttributes.getFileName() + "\"");
				return false;
			}

			// read data and add to place storage
			try {

				Array userDataset = varsData.get(varName);
				ArrayFloat.D3 varData;

				// read for 3D float
				if (userDataset instanceof ArrayFloat.D3) {
					// read one element starting at this places index
					varData = (ArrayFloat.D3) var.read(index, new int[]{1, 1, 1});
					((ArrayFloat.D3) userDataset).set(index[0], index[1], index[2], varData.get(0, 0, 0));
				}

			} catch (InvalidRangeException err) {
				System.err.println("Invalid range: " + err);
				return false;
			} catch (IOException err) {
				System.err.println("Invalid range: " + err);
				return false;
			}
		}
		return true;
	}


	private boolean readTextFile(FileAttributes fileAttributes, byte[] data) {

		try {

			ByteBuffer buffer = fileAttributes.getBuffer();

			byte[] txtData;

			// used for determining which part of the file to read
			int placeOrder = (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];

			int length = fileAttributes.getReadLength();

			if ( placeOrder != fileAttributes.getNumberOfPlaces() - 1 ) {

				txtData = new byte[ length ];

				//TODO: Ask
				synchronized (buffer) {

					buffer.position(placeOrder * length);

					buffer.get(data, 0, length);
				}
			}

			// perform final read
			else {
				buffer.position(length * placeOrder);

				txtData = new byte[ buffer.remaining( ) ];

				int pos = 0;
				while(buffer.hasRemaining()) {
					data[pos] = buffer.get();
					pos++;
				}
			}

			// copies the Place's data to the correct position in the entire array
			// of data that is passed by the user
			System.arraycopy(txtData, 0, data, placeOrder, txtData.length);
		}
		catch ( BufferUnderflowException err ) {
			System.err.println( err );
			return false;
		}
		return true;
	}

	protected boolean close( int fd ) {
		synchronized (fileTable) {
			if (fileTable.containsKey(fd) && index[0] == 0 && index[1] == 0 && index[2] == 0) {
				String fileName = fileTable.get(fd).getFileName();
				Object file = fileTable.get(fd).getFile();
				if (file instanceof NetcdfFile) {
					try {
						((NetcdfFile) file).close();
						System.out.println(fileName + " closed");

						return true;
					} catch (IOException ioe) {
						System.err.println(ioe);
					}
				} else if (file instanceof FileChannel) {
					try {
						((FileChannel) file).close();
						System.out.println(fileName + " closed");

						return true;
					} catch (IOException ioe) {
						System.err.println(ioe);
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

		// compute the global linear index from offset[]
		Places_base places = MASS_base.getPlacesMap().get( new Integer( handle ) );
		int[] neighborCoord = new int[places.getSize().length];
		places.getGlobalNeighborArrayIndex( index, offset, places.getSize(),
				neighborCoord );
		int globalLinearIndex
		= places.getGlobalLinearIndexFromGlobalArrayIndex( neighborCoord,
				places.getSize() );

		if ( globalLinearIndex == Integer.MIN_VALUE )
			return null;

		// identify the destination place  
		int destinationLocalLinearIndex
		= globalLinearIndex - places.getLowerBoundary();

		Place dstPlace = null;
		int shadow_index;
		if ( destinationLocalLinearIndex >= 0 &&
				destinationLocalLinearIndex < places.getPlacesSize() )
			dstPlace = places.getPlaces()[ destinationLocalLinearIndex ];
		else if ( destinationLocalLinearIndex < 0 &&
				( shadow_index = destinationLocalLinearIndex + 
				places.getShadowSize() ) >= 0 )
			dstPlace = places.getLeftShadow()[ shadow_index ];
		else if ( (shadow_index = 
				destinationLocalLinearIndex - places.getPlacesSize()) >= 0
				&& shadow_index < places.getShadowSize() )
			dstPlace = places.getRightShadow()[ shadow_index ];

		return dstPlace;
	
	}

	public synchronized Set<Agent> getAgents() {
		return agents;
	}

	public Number getDebugData()
	{
		return null;
	}

	public int[] getIndex() {
		return index;
	}

	public Object[] getInMessages() {
		return inMessages;
	}

	public Vector<int[]> getNeighbours() {
		return neighbours;
	}

	public void setNeighbours(Vector<int[]> neighbours)
	{
		this.neighbours = neighbours;
	}

	public Object getOutMessage() {
		return outMessage;
	}

	protected Object getOutMessage( int handle, int[] offset_index ) {

		Place dstPlace = findDstPlace( handle, offset_index );

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

	protected void putInMessage( int handle, int[] offset_index, int position, Object value ) {

		Place dstPlace = findDstPlace( handle, offset_index );

		// write to the destination inMessage[position]
		if ( dstPlace != null && position < dstPlace.inMessages.length )
			dstPlace.inMessages[position] = value;
	
	}

	public void setIndex(int[] index) {
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

	public void setSize(int[] size) {
		this.size = size.clone();
	}
	
}