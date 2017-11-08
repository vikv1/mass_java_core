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

import edu.uw.bothell.css.dsl.MASS.Parallel_IO.*;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.File;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;
import ucar.ma2.InvalidRangeException;

import java.io.FileNotFoundException;
import java.io.IOException;
//import java.nio.file.Files;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
//import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



/**
 *	Place represents a single element from a collection of places distributed
 *	among all cluster nodes. A Place may contain a collection of Agents that
 *	perform operations on objects contained within the Place. 
 *
 */
public class Place {
	public static final String HDFS_USERFOLDER = "/user/dslab/input/";
	public static final String MYSCRIPT_DIRCTORY = "/tmp/myscript";
	public static final String WORKING_DIRECTORY = "/tmp";
	public static final int FOR_WRITE = 1;


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
	protected static final Hashtable<Integer, edu.uw.bothell.css.dsl.MASS.Parallel_IO.File> fileTable = new Hashtable<>();

	// File descriptor value that each place has access too
	private static int allPlaceFileDescriptor = -1;

	private int thisPlaceFileDescriptor = 0;

	private static final Hashtable<Integer, Boolean> filesAttemptedToClose = new Hashtable<Integer, Boolean>();

	private static edu.uw.bothell.css.dsl.MASS.Parallel_IO.File writeFile = null;
	private static final Object WRITE_FILE_LOCK = new Object();


	/**
	 * A single Place opens a file specified by the given filePath and ioType. If ioType is 0 then the file
	 * is opened for reading, if the ioType is 1 then the file is opened for writing (it is
	 * expected that the ioType is either 0 or 1; otherwise, -1 is returned). A file that is opened for reading
	 * will be opened in memory and added to the fileTable so that the file can be accessed by all Places. A
	 * file that is opened for writing will be opened on the disk and a temporary buffer to write to is added to the
	 * fileTable so that the temp buffer can be accessed by all Places. A successfully opened file is given a unique
	 * file descriptor (integer) and the file descriptor is returned. An unsuccessfully opened file will result in
	 * an exception being thrown
	 *
	 * @param filepath the filepath of the file to be opened
	 * @param ioType either 0 for read or 1 for write
	 * @return unique file descriptor for the newly opened file
	 */
	protected int open(String filepath, int ioType)
			throws InvalidNumberOfNodesException, InvalidRangeException, IOException, UnsupportedFileTypeException, InterruptedException {
		synchronized (fileTable) {
			openFileUsingOnePlace(filepath, ioType);
		}
		return allPlaceFileDescriptor;
	}

	/**
	 * Opens the file only if the file has not been opened and added to the fileTable
	 * @param filepath the filepath of the file to be opened
	 * @param ioType either 0 for read or 1 for write
     */
	private void openFileUsingOnePlace(String filepath, int ioType)
			throws InvalidNumberOfNodesException, InvalidRangeException, IOException, UnsupportedFileTypeException, InterruptedException {
		logFormattedDebug("Before My place order = " + getPlaceOrderPerNode() + " allPlaceFileDescriptor = " + allPlaceFileDescriptor);
		if (!fileTable.containsKey(thisPlaceFileDescriptor)) {
			openFile(filepath, ioType);
		} else {

			thisPlaceFileDescriptor++;
		}
		logFormattedDebug("After My place order = " + getPlaceOrderPerNode() + " allPlaceFileDescriptor = " + allPlaceFileDescriptor);

	}

	/**
	 * Ensures valid arguments (file exists, and ioType is either 0 or 1), and if the arguments are valid, the
	 * specified file is opened accordingly
	 * @param filepath file to open
	 * @param ioType either 0 for read or 1 for write
     */
	private void openFile(String filepath, int ioType)
			throws InvalidNumberOfNodesException, InvalidRangeException, IOException, UnsupportedFileTypeException, InterruptedException {
		if (ioType != 0 && ioType != 1) {
			throw new IllegalArgumentException("ioType must be either 0 (for read) or 1 (for write)");
		}
		Path path = Paths.get(filepath);
		if (!Files.exists(path)) {
			String filename = filepath.substring(filepath.lastIndexOf('/') + 1, filepath.length());
			getFileFromHDFS(filename);
			if (!Files.exists(path)) {
				// check exists again.. throw exception if doesn't exist
				throw new FileNotFoundException("The given file to open does not exist: " + path);
			} else {
				logFormattedDebug(String.format("**************************************************"));
				logFormattedDebug(String.format("SUCCESS retrieving test file: %s", filepath));
				logFormattedDebug(String.format("**************************************************"));
			}
		}
		edu.uw.bothell.css.dsl.MASS.Parallel_IO.File file = edu.uw.bothell.css.dsl.MASS.Parallel_IO.File.factory(path);
		file.open(ioType); // either NetCDFFile open or TxtFile open
		incrementFileDescriptors();
		fileTable.put(allPlaceFileDescriptor, file);

		logFormattedDebug(
				this + " or Place %d on node %d opened the file %s with the fd %d",
				getPlaceOrderPerNode(),
				MASSBase.getMyPid(),
				filepath,
				allPlaceFileDescriptor
		);

	}


	/**
	 * Calls another program to retrieve the requested file from HDFS
	 *
	 * @param filename name of the file to retrieve
	 */
	private void getFileFromHDFS(String filename) throws IOException, InterruptedException {

		String[] command = { MYSCRIPT_DIRCTORY, "read ", HDFS_USERFOLDER + filename};
		Process process = Runtime.getRuntime().exec(command);

		logFormattedDebug("Retrieving " + filename + " from HDFS ...");
		process.waitFor();
	}

	private void incrementFileDescriptors() {
		allPlaceFileDescriptor = thisPlaceFileDescriptor;
		thisPlaceFileDescriptor++;
	}

	/**
	 * Reads a specific portion of the a NetCDF file's variable based on this place's order and returns the results
	 * as a 1 dimensional primitive java array (i.e. a float[]). Each place reads only a portion of the file, but if
	 * each place calls this method, then the entire file will be read and parts of the data will be contained on
	 * each place involved in the computation.
	 *
	 * @param fileDescriptor specifies the NetCDF file to read from (must be in the fileTable)
	 * @param variableToRead specifies the NetCDF variable to read (must be in the NetCDF file)
     * @return a 1 dimensional primitive java array representing a portion of the NetCDF variable data read by this
	 * place - if an error occurs during the read process, then null is returned
     */
	protected Object read(int fileDescriptor, String variableToRead)
			throws InvalidNumberOfPlacesException, UnsupportedBufferTypeException {

		File file = getFileFromFileTable(fileDescriptor);
		NetcdfFile netcdfFile = convertFileToNetcdfFile(file);
		return netcdfFile.read(variableToRead, getPlaceOrderPerNode());
	}

	/**
	 * Gets the file attribute from the file table
	 * @param fileDescriptor unique identifier for the file attribute to return
	 * @return the file attribute corresponding to the given file descriptor
     */
	private edu.uw.bothell.css.dsl.MASS.Parallel_IO.File getFileFromFileTable(int fileDescriptor) {
		if (fileTable.containsKey(fileDescriptor)) {
			return fileTable.get(fileDescriptor);
		} else {
			throw new IllegalArgumentException(String.format(
					"File descriptor does not exist in the file table: %d",
					fileDescriptor
			));
		}
	}

	/**
	 * Converts the given file attributes to NetCDF file attributes
	 * @param file the file attributes to convert
	 * @return the file attributes converted to NetCDF file attributes
     */
	private NetcdfFile convertFileToNetcdfFile(File file) {
		if (file instanceof NetcdfFile) {
			return (NetcdfFile) file;
		} else {
			throw new ClassCastException(String.format(
					"The given file is not a valid NetCDF file: %s",
					file.getFilepath()
			));
		}
	}

	/**
	 * Write a specific portion of the a NetCDF file's variable based on this place's order into a buffer
	 *
	 * @param dataToWrite data to be written
	 * @param variableName the NetCDF variable name to write
	 * @param shape shape of the netCDF data to be written
	 */
	public void write(int fileDescriptor, float[] dataToWrite, String variableName, int[] shape)
			throws IOException, InvalidRangeException, InvalidNumberOfPlacesException {
		NetcdfFile ncWriteFile = convertFileToNetcdfFile(writeFile);
		boolean doneWriting = ncWriteFile.write(dataToWrite, variableName, shape, getPlaceOrderPerNode());
		if(doneWriting) {
			ncWriteFile.closeFileWrite();
		}
	}


	/**
	 * A single Place opens a file specified by the given filePath and ioType. If ioType is 0 then the file
	 * is opened for reading, if the ioType is 1 then the file is opened for writing (it is
	 * expected that the ioType is either 0 or 1; otherwise, -1 is returned). A file that is opened for reading
	 * will be opened in memory and added to the fileTable so that the file can be accessed by all Places. A
	 * file that is opened for writing will be opened on the disk and a temporary buffer to write to is added to the
	 * fileTable so that the temp buffer can be accessed by all Places. A successfully opened file is given a unique
	 * file descriptor (integer) and the file descriptor is returned. An unsuccessfully opened file will result in
	 * an exception being thrown
	 *
	 * @param filepath the filepath of the file to be opened
	 * @return unique file descriptor for the newly opened file
	 */
	protected boolean openForWrite(String filepath, String variableName, int[] shape) throws InvalidNumberOfNodesException, InvalidRangeException, IOException {
		if(writeFile == null)
		synchronized (WRITE_FILE_LOCK) {
			if(writeFile == null) {
				openFileUsingOnePlaceForWrite(filepath, variableName, shape);
			}
		}
		return writeFile != null;
	}


	/**
	 * Opens the file only if the file has not been opened and added to the fileTable
	 * @param filepath file to open for write
	 */
	private void openFileUsingOnePlaceForWrite(String filepath, String variableName, int[] shape) throws InvalidNumberOfNodesException, InvalidRangeException, IOException {
		Path path = Paths.get(filepath);
		writeFile = new NetcdfFile(path);
		NetcdfFile ncWriteFile = convertFileToNetcdfFile(writeFile);
		ncWriteFile.open(variableName, shape);
	}


	/**
	 * Reads a specific portion of the a TXT file based on this place's order and returns the results
	 * as a byte array. Each place reads only a portion of the TXT file, but if each place calls this method, then the
	 * entire file will be read and parts of the data will be contained on each place involved in the computation.
	 *
	 * @param fileDescriptor unique identifier for the file to read
	 * @return the portion of the file read by this place - if an error occurs then null is returned
     */
	protected byte[] read(int fileDescriptor) throws InvalidNumberOfPlacesException {
		File file = getFileFromFileTable(fileDescriptor);
		TxtFile txtFile = convertFileToTxtFile(file);
		return txtFile.read(getPlaceOrderPerNode());
	}

	/**
	 * A single Place opens a file specified by the given filePath (TxtFile).
	 *
	 * @param filepath the filepath of the file to be opened
	 * @return unique file descriptor for the newly opened file
	 */
	protected boolean openForWrite(String filepath, int size) throws IOException {
		if(writeFile == null)
			synchronized (WRITE_FILE_LOCK) {
				if(writeFile == null) {
					openFileUsingOnePlaceForWrite(filepath, size);
				}
			}
		return writeFile != null;
	}

	/**
	 * Opens the file only if the file has not been opened and added to the fileTable
	 * @param filepath file to open for write
	 */
	private void openFileUsingOnePlaceForWrite(String filepath, int size) throws IOException {
		Path path = Paths.get(filepath);
		writeFile = new TxtFile(path);
		TxtFile txtWriteFile = convertFileToTxtFile(writeFile);
		txtWriteFile.open(FOR_WRITE,size);
	}


	/**
	 * Write a specific portion of the a Txt file's variable based on this place's order into a buffer
	 *
	 * @param dataToWrite data to be written
	 */
	public void write(int fileDescriptor, byte[] dataToWrite)
			throws IOException, InvalidRangeException, InvalidNumberOfPlacesException {
		TxtFile txtWriteFile = convertFileToTxtFile(writeFile);
		boolean doneWriting = txtWriteFile.write(dataToWrite, getPlaceOrderPerNode());
		if(doneWriting) {
			txtWriteFile.close();
		}
	}

	/**
	 * Converts the given file attributes to TXT file attributes
	 * @param file the file attributes to convert
	 * @return the file attributes converted to TXT file attributes
	 */
	private TxtFile convertFileToTxtFile(File file) {
		if (file instanceof TxtFile) {
			return (TxtFile) file;
		} else {
			throw new ClassCastException(String.format(
					"The given file is not a valid TXT file: %s",
					file.getFilepath()
			));
		}
	}

	/**
	 * @return this place's order number determined by its index
     */
	protected int getPlaceOrderPerNode() {
		int totalNodes = MASSBase.getSystemSize();
		int xDimSize = size[0] / totalNodes;

		if (MASSBase.getMyPid() == totalNodes - 1) {
			xDimSize += size[0] % totalNodes;
		}

		int xIndex = index[0] % xDimSize;

		return (xDimSize * size[1] * index[2]) + (xDimSize * index[1]) + xIndex;
	}

	// TODO: 1/13/17 Once finished implementing and testing read(), add write() functionality

	/**
	 * Closes the specified file descriptor and removes it from the file table
	 *
	 * @param fileDescriptor the file descriptor to close
	 * @return true if the file is successfully found in the file table, closed, and removed; otherwise false
	 */
	protected synchronized boolean close(int fileDescriptor) throws IOException {
		if(writeFile != null) {
			writeFile.close(); // maybe use a separate close function for closing writeFile?!
		}
		if (fileTable.containsKey(fileDescriptor)) {
			File file = fileTable.remove(fileDescriptor);
			filesAttemptedToClose.put(fileDescriptor, false);
			file.close();
			filesAttemptedToClose.put(fileDescriptor, true);
			return true;
		} else if (filesAttemptedToClose.containsKey(fileDescriptor)) {
			return filesAttemptedToClose.get(fileDescriptor);
		} else {
			return false;
		}
	}

	/**
	 * Attempts to close the file that is identified by the given file descriptor only if it does not exist in the
	 * file table; otherwise returns whether or not another place successfully closed the file (ensures only one place
	 * actually closes and removes the file from the file table)
	 *
	 * @param fileDescriptor specifies the file to remove
	 * @return true if the file is successfully found in the file table, closed, and removed (by one place);
	 * otherwise false
	 * @throws Exception thrown if an error occurs during the closing process
     */
	// TODO: 5/4/17  

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
				destinationLocalLinearIndex < places.getNumberOfPlacesOnCurrentNode() )
			destintationPlace = places.getPlaces()[ destinationLocalLinearIndex ];
		else if ( destinationLocalLinearIndex < 0 &&
				( shadowIndex = destinationLocalLinearIndex + 
				places.getShadowSize() ) >= 0 )
			destintationPlace = places.getLeftShadow()[ shadowIndex ];
		else if ( (shadowIndex = 
				destinationLocalLinearIndex - places.getNumberOfPlacesOnCurrentNode()) >= 0
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

	private void logFormattedDebug(String formattedLog, Object... args) {
		logger.debug(String.format(formattedLog, args));
	}

	private void logFormattedError(String formattedLog, Object... args) {
		logger.error(String.format(formattedLog, args));
	}

	@Override
	public String toString() {
		return "Place: " + Arrays.toString(this.getIndex());
	}
}