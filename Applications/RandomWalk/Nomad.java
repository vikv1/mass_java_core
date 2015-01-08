/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import edu.uw.bothell.css.dsl.MASS.*;             // Library for Multi-Agent Spatial Simulation
import java.util.Random;
import java.util.Vector;   // for Vector

// Nomad Agents
public class Nomad extends Agent {
    public Nomad( ) {
        super(  );
    }
    // define the four neighbors of each cell
    private Vector<int[]> neighbors = new Vector<int[]>( );
    public Nomad( Object object ) {
        super(  );

	int[] north = { 0, -1 }; 
        neighbors.add( north );
	int[] east  = { 1,  0 }; 
        neighbors.add( east );
	int[] south = { 0,  1 }; 
        neighbors.add( south );
	int[] west  = { -1, 0 }; 
        neighbors.add( west );  
    }
    
    /**
     * Instantiate an agent at each of the cells that form a square
     * in the middle of the matrix
     */
         public int map( int maxAgents, int[] size, int[] coordinates ) 
         {
             
         int sizeX = size[0], sizeY = size[1];
         int populationPerCell 
             = (int)Math.ceil( maxAgents / ( sizeX * sizeY * 0.6 ) );
         int currX = coordinates[0], currY = coordinates[1];
         if ( sizeX * 0.4 < currX && currX < sizeX * 0.6 &&
              sizeY * 0.4 < currY && currY < sizeY * 0.6 )
         {
             System.err.println("mapping max agents " + maxAgents + " size: " + size[0] + " population per cell: " + populationPerCell);
             return populationPerCell;
         }
         else
             return 0;
     }

    // function identifiers
    public static final int decideNewPosition_ = 0;

    /**
     * Is called from callAll( ) and forwards this call to 
     * decideNewePosition( )
     * @param funcId the function Id to call
     * @param args argumenets passed to this funcId.
     */
    public Object callMethod( int funcId, Object args ) {
        switch ( funcId ) {
        case decideNewPosition_: return decideNewPosition( args );
        }
        return null;
    }
  
    /**
     * Computes the index of a next cell to migrate to.
     * @param args formally requested but actually not used
     */
    public Object decideNewPosition( Object args ) {
        int newX = 0;                 // a new destination's X-coordinate
        int newY = 0;                 // a new destination's Y-coordinate
        int min = 1;  // a new destination's # agents

        int currX = place.index[0], currY = place.index[1]; // curr index
        int sizeX = place.size[0], sizeY = place.size[1];   // land size

        /*for ( int x = 0; x < 2; x++ )
            for ( int y = 0; y < 2; y++ ) {
                if ( currY < 0 )
                    continue; // no north
                if ( currX >= sizeX )
                    continue; // no east
                if ( currY >= sizeY )
                    continue; // no south
                if ( currX < 0 )
                    continue; // no west
                if ( ( ( Land )place ).neighbors[x][y] < min ) 
                { 
                    // found a candidate cell to go.
                    newX = currX + x - 1;
                    newY = currY + y - 1;
                    min = ( ( Land )place ).neighbors[x][y];
                }
                
            }*/
        Random generator = new Random();
        boolean candidatePicked = false;
        int next = 0;
        while(!candidatePicked)
        {
           next = generator.nextInt(4);
           int[] neighbor = neighbors.get(next); 
           newX = currX + neighbor[0];
           newY = currY + neighbor[1]; 
           if ( newY < 0 )
               continue; // no north
           if ( newX >= sizeX )
               continue; // no east
           if ( newY >= sizeY )
               continue; // no south
           if ( newX < 0 )
               continue; // no west 
            
           candidatePicked = true; 
        }
        // let's migrate
        migrate( newX, newY );
        //System.err.println("Migrating to X:" + newX + " Y:" +newY );


        return null;
    }
}
