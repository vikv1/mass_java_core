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

/**
 * AgentList is a container for a collection of Agents, with a simple iterator and automatic
 * resize capability 
 */
public class AgentList {

	private final int CAPACITY_X = 1000; // max agent population = 1 million
	private final int CAPACITY_Y = 1000;
	
	private int capacityY = 0;
	private Agent[][] array = null;
	private boolean reduceDone = true;

	private int currentX = -1;
	private int nextY = 0;
	private int iterator = 0;
	private int population = 0; // 2/20/20 by Fukuda

	/**
	 * Instantiate an AgentList with default storage size
	 */
	public AgentList( ) {
		init( CAPACITY_Y );
	}

	/**
	 * Instantiate an AgentList with an initial capacity
	 * @param init_capacity The number of Agents to store in this collection
	 */
	public AgentList( int init_capacity ) {
		init( init_capacity );
	}

	/**
	 * Add an Agent to the collection
	 * @param item The Agent to add
	 */
	public synchronized void add( Agent item ) {
		
		if ( nextY == capacityY ) {
			increaseX( );
			nextY = 0;
		}
		
		array[currentX][nextY++] = item;
		population++;
	}
		
	/**
	 * Add an Agent to the collection, at a specified index position
	 * @param item The Agent to add
	 * @param index The position at which to add the Agent
	 */
	public synchronized void add(Agent item, int index) {
		int xindex = index / CAPACITY_X;
		int yindex = index % CAPACITY_X;
		if(array[xindex] == null) {
			array[xindex] = new Agent[CAPACITY_Y];
		}
		array[xindex][yindex] = item;
		population++;
	}

	/**
	 * Send contents of the AgentList to the logger for debugging
	 */
	@Deprecated
	public void checkInternal( ) {
		
		for ( int x = 0; x < currentX * capacityY + nextY; x++ )
			MASS.getLogger().debug( "AgentList[{}]", get( x ) );
	
	}

	/**
	 * Remove all Agents from the collection
	 */
	public void clear( ) {
		
		for ( int i = 0; i < size_unreduced( ); i++ )
			remove( i );
		reduceHelper( );
		synchronized( this ) {
			population = 0;
		}
	}

	/**
	 * Get an Agent at a specified index position
	 * @param linear_index The position for retrieving the Agent
	 * @return The Agent at the specified position, or NULL if the position contains no Agent or the index is invalid
	 */
	public synchronized Agent get( int linear_index ) {
		
		if ( linear_index <= size_unreduced( ) ) {
			int x = linear_index / capacityY;
			int y = linear_index % capacityY;
			return array[x][y];
		}
		
		return null;
	
	}

	/**
	 * Determine if the next index position pointed to by the iterator has an Agent
	 * @return TRUE if the next position contains an Agent
	 */
	public synchronized boolean hasNext( ) {
		return ( iterator < size_unreduced( ) );
	}

	private void increaseX( ) {
		
		currentX++;
		array[currentX] = new Agent[capacityY];
		
		for ( int i = 0; i < capacityY; i++ )
			array[currentX][i] = null;
	
	}

	/**
	 * Get the index position of a specified Agent
	 * @param item The Agent to search for
	 * @return The index position containing the Agent, or -1 if the Agent was not found within the collection
	 */
	public synchronized int indexOf( Agent item ) {
		
		for ( int i = 0; i < array.length && array[i] != null; i++ ) {
			
			int max_j = ( array[i + 1] == null ) ? nextY : capacityY;
			
			for ( int j = 0; j < max_j; j++ ) {
				
				if ( array[i][j] == item ) {
					return i * capacityY + j;
				}
			
			}
		
		}
		
		return -1;
	
	}

	private synchronized void init( int init_capacity ) {
		
		// create array[capacity_x][]
		array = new Agent[CAPACITY_X][];
		
		// create only array[0][capacity_y]
		capacityY = ( init_capacity > CAPACITY_Y ) ? 
				init_capacity : CAPACITY_Y;
		
		increaseX( );
		
	}

	/**
	 * Get the Agent at the next iterator position
	 * @return The Agent at the index position pointed to after the iterator is incremented
	 */
	public synchronized Agent next( ) {
		return get( iterator++ );
	}

	/**
	 * Adjust the collection capacity to match the current population of Agents
	 */
	public synchronized void reduce( ) {
		reduceHelper( );
	}

	private void reduceHelper( ) {
		
		if ( reduceDone )
			return;
		
		int max = size_unreduced( );
		int currentNull = 0;
		int cur_full = max - 1;
		int xNull, yNull, x_full, y_full;

		while (true ) {
			
			for ( ; currentNull < max && get( currentNull ) != null; currentNull++ );
			for ( ; cur_full >= 0 && get( cur_full ) == null; cur_full-- );
			if ( currentNull >= cur_full )
				break;

			// swapping
			xNull = currentNull / capacityY;
			yNull = currentNull % capacityY;
			x_full = cur_full / capacityY;
			y_full = cur_full % capacityY;
			array[xNull][yNull] = array[x_full][y_full];
			array[x_full][y_full] = null;

		}

		// reduce
		xNull = currentNull / capacityY;
		yNull = currentNull % capacityY;	
		
		for ( int i = xNull + 1; i < array.length && array[i] != null; i++ )
			array[i] = null;
		
		currentX = xNull;
		nextY = yNull;
		reduceDone = true;
		MASS.getLogger().debug( "Reduce done to {}", size_unreduced( ) );
	
	}

	/**
	 * Remove an Agent from the collection
	 * @param item The Agent to remove
	 */
	public synchronized void remove( Agent item ) {
		
		for ( int i = 0; i < array.length && array[i] != null; i++ ) {
			
			int max_j = ( array[i + 1] == null ) ? nextY : capacityY;
			
			for ( int j = 0; j < max_j; j++ ) {
				
				if ( array[i][j] == item ) {
					
					array[i][j] = null;
					reduceDone = false;
					population--;
					return;
				
				}
			
			}
		
		}
		
		return;
	
	}

	/**
	 * Remove an Agent by index position
	 * @param linear_index The index position at which the Agent will be removed
	 */
	public synchronized void remove( int linear_index ) {
		
		if ( linear_index <= size_unreduced( ) ) {
			
			int x = linear_index / capacityY;
			int y = linear_index % capacityY;
			array[x][y] = null;
			reduceDone = false;
			population--;
		}
	}

	/**
	 * Reset the iterator to the start of the collection (index position zero)
	 */
	public synchronized void setIterator( ) {
		reduceHelper( );
		iterator = 0;
	}

	/**
	 * Get the current number of Agents in the collection
	 * @return The number of Agents in the collection
	 */
	public synchronized int size_reduced( ) {
		reduceHelper( );
		return currentX * capacityY + nextY;
	}

	public int size( ) {
		return population;
	}

	/**
	 * Get the current capacity of the collection
	 * @return The current capacity
	 */
	public int size_unreduced( ) {
		return currentX * capacityY + nextY;
	}

}
