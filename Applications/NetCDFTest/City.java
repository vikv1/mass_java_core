
import MASS.Place;


public class City extends Place {
		// function identifiers
		public static final int exchange_ = 0;
		public static final int update_ = 1;
		public static final int agentAges_ = 3;
		
		private static final int NeighborX = 2;
		private static final int NeighborY = 2;
		public static final int MAX_POP = 5;
		
		// Keep track of neighbor cells
		public int[][] neighborCells = new int[NeighborX][NeighborY];;
		
		public City() { }
		
		public City(Object args) { }

		public Object callMethod( int funcId, Object args ) {
			switch ( funcId ) {
				case exchange_: return exchange( args );
				case update_: return update( args );
				case agentAges_: return getAgentAges( args );
			}
			return null;
		}

		// Return the number of citizens in the city
		private Object exchange(Object args) {
			return new Integer( agents.size( ) );
		}

		// Update the neighbor array 
		public Object update(Object args) {
			int index = 0;
			for ( int x = 0; x < NeighborX; x++ ) {
				for ( int y = 0; y < NeighborY; y++ ) {
					neighborCells[x][y] = (inMessages[index] == null) ?
							Integer.MAX_VALUE : 
								((Integer)inMessages[index]).intValue( );

					index++;	// Update index for next pass --- Added; not in doc
				}
			}
			return null;
		}

		private Object getAgentAges(Object args) {
			int[] agentAges = new int[agents.size()];
			
			for (int i = 0; i < agents.size(); i++) {
				Citizen curr = (Citizen) agents.get(i);
				agentAges[i] = curr.getAge();
			}
			
			return agentAges;
		}
	}