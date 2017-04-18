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

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
	 * file that is opened for writing will be opened on the disk and a temporary buffer to write to is added to the
	 * fileTable so that the temp buffer can be accessed by all Places. A successfully opened file is given a unique
	 * file descriptor (integer) and the file descriptor is returned. An unsuccessfully opened file returns a
	 * file descriptor of -1.
	 *
	 * @param filepath the filepath of the file to be opened
	 * @param ioType either 0 for read or 1 for write
	 * @return unique file descriptor for the newly opened file; otherwise returns -1
	 */
	protected int open(String filepath, int ioType) {
		try {
			synchronized (fileTable) {
				openFileUsingOnePlace(filepath, ioType);
				return fileDescriptor;
			}
		} catch (Exception e) {
			logFormattedError("An exception occurred while opening the file: %s, exception: %s", filepath, e.getMessage());
			return -1;
		}
	}

	/**
	 * Opens the file only if the file has not been opened and added to the fileTable
	 * @param filepath the filepath of the file to be opened
	 * @param ioType either 0 for read or 1 for write
	 * @throws Exception any exception that may occur during the opening process
     */
	private void openFileUsingOnePlace(String filepath, int ioType) throws Exception {
		if (!fileTable.containsKey(fileDescriptorIndex - 1)) {
			openFile(filepath, ioType);
		}
		fileDescriptor = fileDescriptorIndex;
	}

	/**
	 * Ensures valid arguments (file exists, and ioType is either 0 or 1), and if the arguments are valid, the
	 * specified file is opened accordingly
	 * @param filepath file to open
	 * @param ioType either 0 for read or 1 for write
	 * @throws Exception any exception that may occur during the opening process
     */
	private void openFile(String filepath, int ioType) throws Exception {
		if (ioType != 0 && ioType != 1) {
			throw new IllegalArgumentException("ioType must be either 0 (for read) or 1 (for write)");
		}

		Path path = Paths.get(filepath);
		if (!Files.exists(path)) {
			throw new FileNotFoundException("The given file to open does not exist: " + path);
		}

		FileAttributes fileAttributes = FileAttributes.factory(path);
		fileAttributes.open(ioType);
		fileTable.put(fileDescriptorIndex++, fileAttributes);
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
	protected Object read(int fileDescriptor, String variableToRead) {
		try {
			FileAttributes fileAttributes = getFileAttribute(fileDescriptor);
			NetcdfFileAttributes netcdfFileAttributes = convertFileAttributesToNetcdFileAttributes(fileAttributes);
			return netcdfFileAttributes.read(variableToRead, getPlaceOrder());
		} catch (Exception e) {
			logFormattedError(
					"An exception occurred while reading the NetCDF file with file descriptor %d, exception: %s",
					fileDescriptor,
					e.getMessage()
			);
			return null;
		}
	}

	/**
	 * Gets the file attribute from the file table.
	 * @param fileDescriptor unique identifier for the file attribute to return
	 * @return the file attribute corresponding to the given file descriptor
     */
	private FileAttributes getFileAttribute(int fileDescriptor) {
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
	 * @param fileAttributes the file attributes to convert
	 * @return the file attributes converted to NetCDF file attributes
     */
	private NetcdfFileAttributes convertFileAttributesToNetcdFileAttributes(FileAttributes fileAttributes) {
		if (fileAttributes instanceof NetcdfFileAttributes) {
			return (NetcdfFileAttributes) fileAttributes;
		} else {
			throw new ClassCastException(String.format(
					"The given file is not a valid NetCDF file: %s",
					fileAttributes.getFilepath()
			));
		}
	}

	/**
	 * Reads a specific portion of the a TXT file based on this place's order and returns the results
	 * as a byte array. Each place reads only a portion of the TXT file, but if each place calls this method, then the
	 * entire file will be read and parts of the data will be contained on each place involved in the computation.
	 *
	 * @param fileDescriptor unique identifier for the file to read
	 * @return the portion of the file read by this place - if an error occurs then null is returned
     */
	protected byte[] read(int fileDescriptor) {
		try {
			FileAttributes fileAttributes = getFileAttribute(fileDescriptor);
			TxtFileAttributes txtFileAttributes = convertFileAttributesToTxtFileAttributes(fileAttributes);
			return txtFileAttributes.read(getPlaceOrder());
		} catch (Exception e) {
			logFormattedError("An exception occurred while reading the TXT file with file descriptor %d, exception: %s", fileDescriptor, e.getMessage());
			return null;
			// TODO: 4/18/17 throw exception rather than returning null?
		}
	}

	/**
	 * Converts the given file attributes to TXT file attributes
	 * @param fileAttributes the file attributes to convert
	 * @return the file attributes converted to TXT file attributes
	 */
	private TxtFileAttributes convertFileAttributesToTxtFileAttributes(FileAttributes fileAttributes) {
		if (fileAttributes instanceof TxtFileAttributes) {
			return (TxtFileAttributes) fileAttributes;
		} else {
			throw new ClassCastException(String.format("The given file is not a valid TXT file: %s", fileAttributes.getFilepath()));
		}
	}

	/**
	 * @return this place's order number determined its index
     */
	private int getPlaceOrder() {
		return (size[0] * size[1] * index[2]) + (size[0] * index[1]) + index[0];

	}

	// TODO: 1/13/17 Once finished implementing and testing read() (including on mutliple nodes) add write() functionality

	/**
	 * Closes the specified file descriptor and removes it from the file table
	 *
	 * @param fileDescriptor the file descriptor to close
	 * @return true if the file is successfully found in the file table, closed, and removed; otherwise false
	 */
	protected synchronized boolean close(int fileDescriptor) {
		try {
			return attemptToCloseFile(fileDescriptor);
		} catch (Exception e) {
			logFormattedError("An exception occurred while closing the file with the file descriptor %d, exception: %s", fileDescriptor, e.getMessage());
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
	private boolean attemptToCloseFile(int fileDescriptor) throws Exception {
		if (fileTable.containsKey(fileDescriptor)) {
			FileAttributes fileAttributes = fileTable.remove(fileDescriptor);
			filesAttemptedToClose.put(fileDescriptor, false);
			fileAttributes.close();
			filesAttemptedToClose.put(fileDescriptor, true);
			return true;
		} else if (filesAttemptedToClose.containsKey(fileDescriptor)) {
			return filesAttemptedToClose.get(fileDescriptor);
		} else {
			return false;
		}
	}s

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

	private void logFormattedDebug(String formattedLog, Object... args) {
		logger.debug(String.format(formattedLog, args));
	}

	private void logFormattedError(String formattedLog, Object... args) {
		logger.error(String.format(formattedLog, args));
	}
}