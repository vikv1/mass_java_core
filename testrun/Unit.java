//Unit.java
/////////////////////////////////////////////////////////////////////////////////////////
//
//      SugarScape Unit
//      ---------------
//
/////////////////////////////////////////////////////////////////////////////////////////
import MASS.*;             // Library for Multi-Agent Spatial Simulation
import java.util.Random;
import java.util.Vector;   // for Vector

public class Unit extends Agent {

	// Define the four neighbors of each cell
	public final int maxSugar = 10;
	public final int minSugar = 0;

   	private Vector<int[]> neighbors = new Vector<int[]>( );
	private int sugar;
	private int vDist;


	// Constructors
	// ------------
    public Unit( ) {
        	super(  );
    }

    public Unit( Object object ) {
    	super(  );

		Random gen = new Random();

		int maxVDist = 3;		
		//vDist = 1;
		vDist = gen.nextInt(maxVDist) + 1;

		// Set initial sugar level
		sugar = 5;

    	// Define the neighbors 
        for( int x = 0 - vDist; x <= vDist; x++ ) {
        	for( int y = 0 - vDist; y <= vDist; y++ ) {

           		if( !(x == 0 && y == 0) ) neighbors.add( new int[]{ x, y } );
           	}
        }
	}

	/**
	 *
   	*/ //-----------------------------------------------------------
	public int getSugarLevel(){
		return sugar;
	}

	/**
      * Instantiate agents randomly around the grid
      * 
   	*/ //-----------------------------------------------------------
    public int map( int maxAgents, int[] size, int[] coordinates )
    {
		Random gen = new Random();
		int num = gen.nextInt( size[0] );

		if( num < 5 ) 	return 1;
		else		return 0;
    }


    // function identifiers
    public static final int decideNewPosition_ = 0;

    /**
      * Is called from callAll( ) and forwards this call to 
      * decideNewePosition( )
      * @param funcId the function Id to call
      * @param args argumenets passed to this funcId.
    */ //-----------------------------------------------------------
    public Object callMethod( int funcId, Object args ) 
	{
		switch ( funcId ) {
        	case decideNewPosition_: return decideNewPosition( args );
        }
        return null;
    }


    /**
      * Computes the index of a next cell to migrate to.
      * @param args formally requested but actually not used
    */ //-----------------------------------------------------------
    public Object decideNewPosition( Object args ) 
	{
		if( sugar <= minSugar ) {
			MASS.log("Unit died... :( ");
			kill();
			return null;
		}
	
		// Get Neighbor List
		int[] nbrLandIdx = ((Land)place).getNbrLocations( vDist );

		// ERROR checking
		if( nbrLandIdx == null ){
		    MASS.log("Wrong Lcation... (" +place.index[0]+ "," +place.index[1]+ ")" );
			MASS.log( "*** ERROR: Neighbor Land Index (nbrLandIdx) is NULL !!! ***");
			kill( );
			return null;
		}
		if( nbrLandIdx.length != neighbors.size()){
			MASS.log( "*** ERROR: nbrLandIdx length (" + nbrLandIdx.length  
						+ ")  !=  neighbor length (" + neighbors.size() + ") ***");
			System.exit(-1);
		}

       	int sizeX = place.size[0],  	sizeY = place.size[1];  	// land size
       	int currX = place.index[0], 	currY = place.index[1]; 	// curr index
       	int newX = currX,	    		newY = currY; 				// new destination X and Y

		int localSugar    		= ((Land)place).sugar;				// local land sugar level
		int localAgents 		= ((Land)place).getNumberOfAgents();// local land agent level
		int localMigrateVal		= localSugar - localAgents + 1;		// local land migrate value 
		int largestMigrateVal 	= localSugar - localAgents;			// largest migrate value (migrate->highest)
		boolean migrate			= false;

		// Loop through each of the neighbor locations looking for a 
		// location with the largest migrate value (#sugar - #agents)
		for( int i = 0; i < neighbors.size(); i++ ) { 

			int   idx 			= nbrLandIdx[i];
			int[] neighbor 		= neighbors.get( i );
			int   nbrSugar 		= ((Land)place).nbrNumSugar[idx];
			int   nbrAgents		= ((Land)place).nbrNumAgents[idx];
			int   migrateVal 	= nbrSugar - nbrAgents;

			// Check if neighbor has better migrate value, if so update larget values
			if( migrateVal >  largestMigrateVal ) {

				largestMigrateVal = migrateVal;
				newX = currX + neighbor[0];
				newY = currY + neighbor[1];
			}
		}

		// After checking all neighbors, decide wheather to move or not 
		if( largestMigrateVal > localMigrateVal ) {   
			migrate = true;
		} 
		else {

			boolean consumed = false;

			// Try to consume sugar, set consumed flag to result
			if( sugar < maxSugar ) 
				consumed = ((Land)place).consumeSugar(); 

			// If sugar consumed, add sugar to units inventory
			// If unable (sugar value == 0  or  agents sugar value == max) 
			// move to a random neighbor location
			if( consumed )  {
				sugar++;	
			}
			else {
				Random gen 		= new Random();
				int tmpVal 		= gen.nextInt( neighbors.size() );
				int[] neighbor 	= neighbors.get( tmpVal );
				newX 			= currX + neighbor[0];
				newY 			= currY + neighbor[1];
				migrate 		= true;
			}	
		}

		if( migrate ) {
			migrate( newX, newY );								// Move (migrate) unit
			if( sugar > 0 ) 	sugar--;						// Move penalty: -1 sugar for each move
        	MASS.log("Migrating... (" +currX+ "," +currY+ ") -> (" +newX+ "," +newY+ ")" );
    	}
        return null;
	}
}


