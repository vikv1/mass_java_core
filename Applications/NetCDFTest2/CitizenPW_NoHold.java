import java.util.Random;

import MASS.Agent;


public class CitizenPW_NoHold extends Agent {
		// function identifiers
		public static final int decideNewPosition_ = 0;
		
		public static final int NORTH = 0;
		public static final int EAST = 1;
		public static final int SOUTH = 2;
		public static final int WEST = 3;

		private int age;

		public CitizenPW_NoHold() { 
			age = 0; 
		}
		
		public CitizenPW_NoHold(Object args) { 
			this();
		}
		

		// Default is used for uniform distribution of citizens
		//	public static int map( int maxAgents, int[] size, int[] coordinates ) {
		//		return 0;
		//	}

		//	public static Object callMethod( int funcId, Object args ) {
		public Object callMethod( int funcId, Object args ) {
			switch ( funcId ) {
			case decideNewPosition_: return decideNewPosition( args );
			}

			return null;
		}

		public Object decideNewPosition(Object args) {
			age++;	// Update age

			// Current location
			int currX = place.index[0];
			int currY = place.index[1]; 

			// Size of the grid of cities
			int sizeX = place.size[0]; 
			int sizeY = place.size[1];   

			if (shouldMigrate()) {
				int counter = 0;
				for ( int x = 0; x < 2; x++ ) {
					for ( int y = 0; y < 2; y++ ) {
						if ( currY < 0 )
							continue; // no north
						if ( currX >= sizeX )
							continue; // no east
						if ( currY >= sizeY )
							continue; // no south
						if ( currX < 0 )
							continue; // no west
						if ( ((CityPW_NoHold)place).neighborCells[x][y] < CityPW_NoHold.MAX_POP ) {  // found new cell
							switchMigrate(counter, currX, currY);	// Migrate to the first available city
							return null;
						}
						counter++;
					}
				}
			}
			return null;
		}

		private boolean shouldMigrate() {
			Random randi = new Random();
			float chance = randi.nextFloat();

			boolean childAndAdult = (chance >= .75);
			boolean youngAdult = (chance >= .5);

			if (age > 18) {
				if (age > 30) {
					if (age > 60) {		// If over 60, citizen does no move
						return false;
					}

					return childAndAdult;
				}
				return youngAdult;
			}
			return childAndAdult;
		}
		
		private void switchMigrate(int caseNum, int currX, int currY) {
			switch ( caseNum ) {
				case NORTH: migrate( currX, (currY - 1) );
				case SOUTH: migrate( currX, (currY + 1) );
				case EAST: migrate( (currX + 1), currY );
				case WEST: migrate( (currX - 1), currY );
			}
		}
		
		public int getAge() {
			return age;
		}
	}