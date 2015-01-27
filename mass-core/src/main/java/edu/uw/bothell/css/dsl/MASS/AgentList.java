package edu.uw.bothell.css.dsl.MASS;

import java.util.LinkedList;

public class AgentList {

	private final int CAPACITY_X = 1000; // max agent population = 1 million
	private final int CAPACITY_Y = 1000;
	private int capacity_y = 0;
	private Agent[][] array = null;
	private boolean reduceDone = true;

	private int curr_x = -1;
	private int next_y = 0;
	private int iterator = 0;

	public AgentList( ) {
		init( CAPACITY_Y );
	}

	public AgentList( int init_capacity ) {
		init( init_capacity );
	}

	public synchronized void add( Agent item ) {
		
		if ( next_y == capacity_y ) {
			increaseX( );
			next_y = 0;
		}
		
		array[curr_x][next_y++] = item;
	
	}
	
	/**
	 * Use when Agents.callAllAsync()
	 * @param item
	 */
	public synchronized void addForAsyncProcess(Agent item) {
	  item.setMyAsyncIndex(size_unreduced());
	  add(item);
	}

	public void check_internal( ) {
		
		for ( int x = 0; x < curr_x * capacity_y + next_y; x++ )
			MASS_base.log( "AgentList[" + get( x ) + "]" );
	
	}

	public void clear( ) {
		
		for ( int i = 0; i < size_unreduced( ); i++ )
			remove( i );
		reduce_helper( );

	}

	public synchronized Agent get( int linear_index ) {
		
		if ( linear_index <= size_unreduced( ) ) {
			int x = linear_index / capacity_y;
			int y = linear_index % capacity_y;
			return array[x][y];
		}
		
		return null;
	
	}

	public synchronized boolean hasNext( ) {
		return ( iterator < size_unreduced( ) );
	}

	private void increaseX( ) {
		
		curr_x++;
		array[curr_x] = new Agent[capacity_y];
		
		for ( int i = 0; i < capacity_y; i++ )
			array[curr_x][i] = null;
	
	}

	public synchronized int indexOf( Agent item ) {
		
		for ( int i = 0; i < array.length && array[i] != null; i++ ) {
			
			int max_j = ( array[i + 1] == null ) ? next_y : capacity_y;
			
			for ( int j = 0; j < max_j; j++ ) {
				
				if ( array[i][j] == item ) {
					return i * capacity_y + j;
				}
			
			}
		
		}
		
		return -1;
	
	}

	private synchronized void init( int init_capacity ) {
		
		// create array[capacity_x][]
		array = new Agent[CAPACITY_X][];
		
		for ( int i = 1; i < array.length; i++ )
			array[i] = null;

		// create only array[0][capacity_y]
		capacity_y = ( init_capacity > CAPACITY_Y ) ? 
				init_capacity : CAPACITY_Y;
		
		increaseX( ); 
	
	}

	public synchronized Agent next( ) {
		return get( iterator++ );
	}

	public synchronized void reduce( ) {
		reduce_helper( );
	}

	private void reduce_helper( ) {
		
		if ( reduceDone )
			return;
		
		int max = size_unreduced( );
		int cur_null = 0;
		int cur_full = max - 1;
		int x_null, y_null, x_full, y_full;

		while (true ) {
			
			for ( ; cur_null < max && get( cur_null ) != null; cur_null++ );
			for ( ; cur_full >= 0 && get( cur_full ) == null; cur_full-- );
			if ( cur_null >= cur_full )
				break;

			// swapping
			x_null = cur_null / capacity_y;
			y_null = cur_null % capacity_y;
			x_full = cur_full / capacity_y;
			y_full = cur_full % capacity_y;
			array[x_null][y_null] = array[x_full][y_full];
			array[x_full][y_full] = null;

			/*
	    	System.out.println( "swaped[" + x_null + "][" + y_null + 
				"] and [" + x_full + "][" + y_full + "] = " +
				array[x_null][y_null] );
			 */
		
		}

		// reduce
		x_null = cur_null / capacity_y;
		y_null = cur_null % capacity_y;	
		
		for ( int i = x_null + 1; i < array.length && array[i] != null; i++ )
			array[i] = null;
		
		curr_x = x_null;
		next_y = y_null;
		reduceDone = true;
		// System.out.println( "reduce done to " + size_unreduced( ) );
	
	}

	public synchronized void remove( Agent item ) {
		
		for ( int i = 0; i < array.length && array[i] != null; i++ ) {
			
			int max_j = ( array[i + 1] == null ) ? next_y : capacity_y;
			
			for ( int j = 0; j < max_j; j++ ) {
				
				if ( array[i][j] == item ) {
					
					array[i][j] = null;
					reduceDone = false;
					
					return;
				
				}
			
			}
		
		}
		
		return;
	
	}

	public synchronized void remove( int linear_index ) {
		
		if ( linear_index <= size_unreduced( ) ) {
			
			int x = linear_index / capacity_y;
			int y = linear_index % capacity_y;
			array[x][y] = null;
			reduceDone = false;
			
			/*
	    	System.out.println( "AgentList.remove: " +
				"linear_index = " + linear_index +
				" array[" + x + "][" + y + "] = " +
				array[x][y] );
			 */
		
		}
	
	}

	public synchronized void setIterator( ) {
		reduce_helper( );
		iterator = 0;
	}

	public synchronized int size( ) {
		reduce_helper( );
		return curr_x * capacity_y + next_y;
	}

	public int size_unreduced( ) {
		return curr_x * capacity_y + next_y;
	}
	
	public synchronized LinkedList<Agent> getAll() {
	  reduce_helper();
	  LinkedList<Agent> result = new LinkedList<Agent>();
	  int x = 0, y = 0;
	  for(int i = 0; i < size_unreduced(); i++)
	  {
	    result.add(array[x][y]);
	    ++y;
	    if(y == capacity_y)
	    {
	      y = 0;
	      ++x;
	    }
	  }
	  return result;
	}
}
