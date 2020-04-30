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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;     // implementation for Agent bag
import java.util.Hashtable;
import java.util.Set;         // local Agent bag
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.Parallel_IO.File;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.InvalidNumberOfNodesException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.InvalidNumberOfPlacesException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.NetcdfFile;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.TxtFile;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.UnsupportedBufferTypeException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.UnsupportedFileTypeException;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;
import ucar.ma2.InvalidRangeException;

/**
 *	Place represents a single element from a collection of places distributed
 *	among all cluster nodes. A Place may contain a collection of Agents that
 *	perform operations on objects contained within the Place. 
 *
 */
@SuppressWarnings("serial")
public class Place implements Serializable {

	private boolean visited;

	public static final String HDFS_USERFOLDER = "/user/dslab/input/";
	public static final String MYSCRIPT_DIRCTORY = "/tmp/myscript";
	public static final String WORKING_DIRECTORY = "/tmp";
	public static final int FOR_WRITE = 1;


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

	protected Vector<int[]> neighbors = null;

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

	public Place() {
		
		// TODO - hack! "pulling" index is bad practice - should be supplied during init
		if ( MASS.getCurrentPlacesBase() != null ) index = MASS.getCurrentPlacesBase().getNextIndex();
		
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
		if (!fileTable.containsKey(thisPlaceFileDescriptor)) {
			openFile(filepath, ioType);
		} else {

			thisPlaceFileDescriptor++;
		}

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

		logFormattedDebug("size: %d, index: %d, lowerBoundary: %d",
				getSize()[0],
				getIndex()[0],
				MASSBase.getCurrentPlacesBase().getLowerBoundary());

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
		
		return MatrixUtilities.getLinearIndex( getSize( ), getIndex( ) )
				- MASSBase.getCurrentPlacesBase().getLowerBoundary( );
	
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
	 * Is called from Places.callAll( ), callSome( ), exchangeAll( ), and
	 * exchangeSome( ), and invoke the function specified with functionId as
	 * passing arguments to this function. A user-derived Place class must
	 * implement this method.
	 * @param functionId The ID number of the function to invoke
	 * @param argument An argument that will be passed to the invoked function
	 * @return Always returns NULL
	 */
	public Object callMethod( int functionId, Object argument ) {
		return null;
	}

	private Place findDstPlace( int handle, int offset[] ) {

		// Compute the global linear index from offset[]
		PlacesBase places = MASSBase.getPlacesMap().get( handle );
		int[] neighborCoord = new int[places.getSize().length];
		places.getGlobalNeighborArrayIndex( index, offset, places.getSize(),
				neighborCoord );
		int globalLinearIndex = MatrixUtilities.getLinearIndex( places.getSize(), neighborCoord );
		if ( globalLinearIndex == Integer.MIN_VALUE ) return null;

		// Identify the destination place
		int destinationLocalLinearIndex
		= globalLinearIndex - places.getLowerBoundary();

		Place destintationPlace = null;
		int shadowIndex;
		
		// TODO - commented-out portions are from Jas' and Michael's implementation
		//if ( destinationLocalLinearIndex >= 0 && destinationLocalLinearIndex < places.getNumberOfPlacesOnCurrentNode() )
		if ( destinationLocalLinearIndex >= 0 && destinationLocalLinearIndex < places.getPlacesSize() )
			destintationPlace = places.getPlaces()[ destinationLocalLinearIndex ];
		else if ( destinationLocalLinearIndex < 0 &&
				( shadowIndex = destinationLocalLinearIndex + 
				  places.getShadowSize() ) >= 0 ) {
		    /////
		    logger.debug( "shadowIndex = " + shadowIndex );
		    logger.debug( "places.getLeftShaow = " + places.getLeftShadow( ) );
		    logger.debug( "places.getLeftShaow = " + places.getLeftShadow( )[ shadowIndex ] );
			destintationPlace = places.getLeftShadow()[ shadowIndex ];
		//else if ( (shadowIndex = destinationLocalLinearIndex - places.getNumberOfPlacesOnCurrentNode()) >= 0
		}
		else if ( (shadowIndex = destinationLocalLinearIndex - places.getPlacesSize() ) >= 0 && shadowIndex < places.getShadowSize() )
			destintationPlace = places.getRightShadow()[ shadowIndex ];

		return destintationPlace;
	
	}

	/**
	 * Get the collection of Agents residing locally on this place
	 * <p>
	 * Important: Synchronized set is NOT serializable! Therefore when agent is de-serialized
	 * the place field of agent must be re-assigned. Otherwise you will get an exception when you
	 * call [agent instance].getPlace().getAgents()
	 *  
	 * @return The Agents residing in this instance of Place
	 */
	public synchronized Set<Agent> getAgents() {
		return agents;
	}
	
	/**
	 * Get the number of Agents residing in this Place
	 * @return The number of Agents associated with this Place
	 */
	public int getNumAgents() {
		return agents.size();
	}

	/**
	 * To be overridden by a developer - return debug data from this Place
	 * @return Debug data contained by this Place
	 */
	public Number getDebugData()
	{
		return null;
	}

	/**
	 * To be overridden by a developer - set debug data for this Place
	 * @param argument Debug data for this Place
	 */
	public void setDebugData(Number argument) {
	}

	/**  
	 * Get the array that maintains each place’s coordinates. Intuitively,
	 * index[0], index[1], and index[2] correspond to coordinates of x, y, and
	 * z, or those of i, j, and k.
	 * @return The coordinates of this Place as an array of indices 
	 */
	public int[] getIndex() {
		return index;
	}

	/** 
	 * Get incoming Messages received by this Place
	 * @return Received incoming messages 
	 */
	public Object[] getInMessages() {
		return inMessages;
	}

	/**
	 * Get a collection of indexes representing the location of neighboring Places
	 * @return Index arrays representing neighboring Places
	 */
	public Vector<int[]> getNeighbours() {
		return neighbors;
	}

	/**
	 * Set the collection of indexes representing the location of neighboring Places
	 * @param neighbors Index arrays representing neighboring Places
	 */
	public void setNeighbors(Vector<int[]> neighbors)
	{
		this.neighbors = neighbors;
	}

	/** 
	 * Get the arguments to be passed to a set of remote-cell functions
	 * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	 * nearest future.
	 * @return The message to be passed during exchange 
	 */
	protected Object getOutMessage() {
		return outMessage;
	}

	/**
	 * Get the out Message destined for a specific Place, given by the offsetIndex
	 * @param handle The handle ID of the Place
	 * @param offsetIndex The offset index
	 * @return The message intended for the specified Place/Index
	 */
	public Object getOutMessage( int handle, int[] offsetIndex ) {

		Place dstPlace = findDstPlace( handle, offsetIndex );

		// return the destination outMessage
		return ( dstPlace != null ) ? dstPlace.outMessage : null;
	
	}

	/**
	 * Returns the size of the matrix that consists of application-specific
	 * places. Intuitively, size[0], size[1], and size[2] correspond to the size
	 * of x, y, and z, or that of i, j, and k.
	 * @return Matrix size
	 */
	public int[] getSize() {
		return MASSBase.getCurrentPlacesBase().getSize();
	}

	/**
	 * Get the visit status - i.e. if this Place has been visited by an Agent
	 * @return TRUE if visited by an Agent
	 */
	public boolean getVisited() {
		return visited;
	}

	protected void putInMessage( int handle, int[] offsetIndex, int position, Object value ) {

		Place dstPlace = findDstPlace( handle, offsetIndex );

		// Write to the destination inMessage[position]
		if ( dstPlace == null )                                 // 4-12-18 by Fukuda
		    // out of range
		    return;                                             // 4-12-18 by Fukuda
		logger.debug( "dstPlace.inMessages = " + dstPlace.inMessages );
		if ( dstPlace.inMessages == null )                      // 4-12-18 by Fukuda
		    dstPlace.setInMessages( new Object[position + 1] ); // 4-12-18 by Fukuda
		logger.debug( "dstPlace.inMessages.length = " + dstPlace.inMessages.length );
		if ( dstPlace != null && position < dstPlace.inMessages.length )
			dstPlace.inMessages[position] = value;
	
	}

	 /**  
	  * Set the array that maintains each place’s coordinates. Intuitively,
	  * index[0], index[1], and index[2] correspond to coordinates of x, y, and
	  * z, or those of i, j, and k.
	  * @param index The coordinates of this Place as an array of indices 
	  */
	protected void setIndex(int[] index) {
		this.index = index.clone();
	}

	/**
	 * To be overridden by a developer - set debug data for this Place
	 * @param argument Debug data for this Place
	 */
	public void setDebugData(Object argument) {
	}

	/** 
	 * Set incoming Messages received by this Place
	 * @param inMessages Collection of incoming Messages 
	 */
	public void setInMessages(Object[] inMessages) {
		this.inMessages = inMessages;
	}

	/** 
	 * Stores a set of arguments to be passed to a set of remote-cell functions
	 * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	 * nearest future.
	 * @param outMessage The message to be passed during exchange 
	 */
	public void setOutMessage(Object outMessage) {
		this.outMessage = outMessage;
	}

	/**
	 * Set the size of the matrix that consists of application-specific places.
	 * @param size Matrix size
	 */
	@Deprecated
	protected void setSize(int[] size) {

		// Cannot change matrix size here - PlacesBase is the authority for this
		//this.size = size.clone();

	}

	/**
	 * Set the visit status of this Place by an Agent
	 * @param visited Set TRUE if visited by an Agent
	 */
	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	private void logFormattedDebug(String formattedLog, Object... args) {
		logger.debug(String.format(formattedLog, args));
	}

	@Override
	public String toString() {
		return "Place: " + Arrays.toString(this.getIndex());
	}
	
}
