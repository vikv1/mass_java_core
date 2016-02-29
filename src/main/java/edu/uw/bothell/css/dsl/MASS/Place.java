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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Collections; // for synchronized set
import java.util.HashSet;     // implementation for Agent bag
import java.util.Set;         // local Agent bag
import java.util.Vector;
import java.util.Hashtable;   // for file storage

import sun.nio.ch.Net;
import ucar.nc2.NetcdfFile;	  // for Netcdf files
import java.io.*;			  // for IO

import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.StandardOpenOption.READ;


import ucar.ma2.*;
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
	
	 /** Stores a set arguments to be passed to a set of remote-cell functions
	  * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	  * nearest future. The argument size must be specified with
	  * outMessage_size. 
	  */
	private Object outMessage = null;
	
	 /** Receives a return value in inMessages[i] from a function call made to
	  * the i-th remote cell through exchangeAll( ) and exchangeSome( ).
	  * Each element size must be specified with inMessage_size. 
	  */
	private Object[] inMessages = null;
	
	/** Includes all the agents residing locally on this place. */
	private Set<Agent> agents = Collections.synchronizedSet( new HashSet<Agent>( ) );
	
	private Vector< int[] > neighbours = null;

	/** stores all files that have been opened
	 *  String array stores file name at index 0
	 *  boolean has been read at index 1
	 *  number of places (for write at index 2)
	 */

	protected static Hashtable<Object, FileAttributes> fileTable = new Hashtable<>();

	private static final OpenOption[] OpenOperations = new OpenOption[] { READ, WRITE};

	private byte[] data;

	private static ByteBuffer buffer;


	private class FileAttributes {
		private String fileName;
		private int numberOfPlaces;
		private boolean isRead;
		private int remainingReads;
		private int remainingWrites;

		FileAttributes( String fileName, int numberOfPlaces, boolean isRead ) {
			this.fileName = fileName;
			this.numberOfPlaces = numberOfPlaces;
			this.isRead = isRead;
			remainingReads = numberOfPlaces;
			remainingWrites = numberOfPlaces;
		}

		// decrements the number of remaining reads by 1
		public void decrementReads( ) {
			this.remainingReads -= 1;
		}

		// decrements the number of remaining writes by 1
		public void decrementWrites( ) {
			this.remainingWrites -= 1;
		}

		// getter methods

		public String getFileName( ) {
			return fileName;
		}

		public int getNumberOfPlaces( ) { return numberOfPlaces; }

		public boolean isRead( ) { return isRead; }

		public int getRemainingWrites( ) { return remainingWrites; }

		public int getRemainingReads( ) { return remainingReads; }

		// setter methods

		public void setFileName( String fileName ) {
			this.fileName = fileName;
		}

		public void setNumberOfPlaces( int numberOfPlaces ) {
			this.numberOfPlaces = numberOfPlaces;
		}

		public void setRead( boolean isRead ) {
			this.isRead = isRead;
		}

		public void setRemainingReads( int remainingReads ) { this.remainingReads = remainingReads; }

		public void setRemainingWrites( int remainingWrites ) { this.remainingWrites = remainingWrites; }


	}

	/**
	 * Open method
	 * Takes the given fileName, opens the specified file and stores it in the fileTable, returns the file descriptor
	 */

	/**
	 *
	 * Questions:
	 * - should I store the fileName
	 * - should read and write really be done within the open method? What if you want to write
	 * - and read? Wouldn't you open the file twice then
     */
	protected synchronized Object open( String filePath, int ioType ) throws IOException {

		// set file descriptor to null
		Object descriptor = null;

		// set file attributes to null
		FileAttributes fileAttributes = null;

		// create a path object from the given file path string
		Path path = Paths.get( filePath );

		// isolate the file name
		String fileName = path.getFileName( ).toString( );

		// check if the file is type nc
		if ( fileName.toLowerCase( ).endsWith( ".nc" ) ) {

			NetcdfFile netcdfFile = NetcdfFile.open( fileName );

			descriptor = netcdfFile;

			if ( !fileTable.containsKey( descriptor ) ) {

				// set the file attributes - string file name, int number of places, boolean has been read
				fileAttributes = new FileAttributes( fileName, 1 , false );

				// add the file descriptor and the corresponding file attributes to the file table
				fileTable.put( descriptor, fileAttributes );



			} else {
				fileAttributes = fileTable.get( descriptor );
			}
			if ( ioType == 0 && !fileAttributes.isRead ( ) ) {
				descriptor = NetcdfFile.openInMemory( fileName );
			}

		}

		// check if the file is type txt
		else if ( fileName.toLowerCase( ).endsWith( ".txt" ) ) {

			// opens a file, returning a FileChannel to access the supplied file
			// file is opened with the specified OpenOption of either READ or WRITE
			FileChannel fileChannel = FileChannel.open( path, OpenOperations[ ioType ] );

			descriptor = fileChannel;

			// file descriptor has not been added to file table
			if ( !fileTable.containsKey( descriptor ) ) {

				// set the file attributes
				// note that the second parameter should be: MASS_base.getCurrentPlaces().getPlacesSize()
				// but 0 is being used now for testing purposes
				fileAttributes = new FileAttributes( fileName, 2 , false );

				// add file to the file table
				fileTable.put( descriptor, fileAttributes );

			} else {

				// retrieve the file attributes from the file table
				fileAttributes = fileTable.get( descriptor );

			}

			// the user wants to preform a read operation and the the
			// first read has not yet been preformed
			if ( ioType == 0 && !fileAttributes.isRead( ) ) {

				// create a buffer that has the same space as the file being read
				buffer = ByteBuffer.allocate( ( int) fileChannel.size( ) );

				// read the file contents to the buffer
				fileChannel.read( buffer );

				buffer.position(0);

				// update the file table
				fileAttributes.setRead( true );

			}

			if ( ioType == 0 && fileAttributes.isRead( ) ) {

				// divide the buffer size by the number of places
				int length = buffer.capacity( ) / fileAttributes.getNumberOfPlaces( );

				read( descriptor, length, fileAttributes );
			}
		}
		return descriptor;
	}

	private void read( Object descriptor, int length, FileAttributes fileAttributes ) {
		if ( descriptor instanceof FileChannel ) {
			try {
				// perform all place reads but the last one
				if ( fileAttributes.getRemainingReads( ) > 1 ) {

					data = new byte[ length ];

					buffer.get( data, buffer.position( ), length);

					String byteData = new String( data );

					fileAttributes.decrementReads( );

					System.out.println( byteData );
				}

				// perform final read
				else {

					data = new byte[ buffer.remaining( ) ];

					buffer.get( data, buffer.position( ), buffer.remaining( ) );

					String byteData = new String( data );

					fileAttributes.decrementReads( );

					System.out.println( byteData );

				}
			}
			catch ( BufferUnderflowException err ) {
				System.err.println( err );
			}
		}
	}

	protected synchronized boolean close( Object descriptor ) {
		if ( fileTable.containsKey( descriptor ) ) {

			if ( descriptor instanceof NetcdfFile ) {
				try {
					( ( NetcdfFile ) descriptor ).close( );
				} catch ( IOException ioe ) {
					System.err.println( ioe );
				}
			}

			else if ( descriptor instanceof BufferedReader ) {
				try {
					( ( BufferedReader ) descriptor ).close( );
				} catch ( IOException ioe ){
					System.err.println( ioe );
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