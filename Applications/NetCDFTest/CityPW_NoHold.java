import java.io.IOException;

import ucar.ma2.ArrayInt;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import MASS.Place;


public class CityPW_NoHold extends Place {
		// function identifiers
		public static final int exchange_ = 0;
		public static final int update_ = 1;
		public static final int saveState_ = 2;
		public static final int agentAges_ = 3;
		
		private static final int NeighborX = 2;
		private static final int NeighborY = 2;
		public static final int MAX_POP = 5;
		
		public static String fileName = null;

		// Keep track of neighbor cells
		public int[][] neighborCells = new int[NeighborX][NeighborY];;
		
		public CityPW_NoHold() { }
		
		public CityPW_NoHold(Object args) {
			fileName = (String) args;
		}

		public Object callMethod( int funcId, Object args ) {
			switch ( funcId ) {
				case exchange_: return exchange( args );
				case update_: return update( args );
				case saveState_: return saveState( args );
				case agentAges_: return getAgentAges( args );
			}
			return null;
		}

		private Object saveState(Object args) {
			NetcdfFileWriteable fileOut = null;

			try {
				// Open file 
				fileOut = NetcdfFileWriteable.openExisting(fileName, false);
				
				// Variable name for the city is its x & y indices
				String varName = (index[0] + "-" + index[1]);	
				Variable cityVar = fileOut.findVariable(varName);
				
				// Check that the variable exists
				if (cityVar == null) {
					System.out.println("Cant find requested Variable");
					return null;
				}

				// Use the shape and origin to read all values currently stored
				int[] shape = cityVar.getShape();
				int[] origin = new int[2];
				ArrayInt.D1 agentAges = (ArrayInt.D1) cityVar.read(origin, shape);
				
				// Write the ages from each citizen in the city
				for (int i = 0; i < agents.size(); i++) {
					CitizenPW_NoHold curr = (CitizenPW_NoHold) agents.get(i);
					agentAges.set(i, curr.getAge());
				}
				
				// Fill in the rest of the array with zeros
				// Erases any old values if there are currently less agents in the city 
				// than the last time a snapshot was taken 
				for (int i = agents.size(); i < shape[0]; i++) {
					agentAges.set(i, 0);
				}
				
				// Write the new data 
				fileOut.write(varName, agentAges);
						
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidRangeException e) {
				e.printStackTrace();
			} finally {		// Close out the file no matter what happens
				if (null != fileOut)
					try {
						fileOut.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
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
				CitizenPW_NoHold curr = (CitizenPW_NoHold) agents.get(i);
				agentAges[i] = curr.getAge();
			}
			
			return agentAges;
		}
	}