/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import MASS.*;
import java.util.Vector;

public class MotifFinder extends Agent
{
    int motifSize = 3;  // the size of motifs to find
    int nEdges = 0; // # edges to traverse
    private Vector motif ;

    public MotifFinder()
    {
	super();
    }

    public MotifFinder( Object args )
    {
	super();

	//motifSize = ( ( Integer )arg ).intValue( );
	this.motif = new Vector( 3 ); // Initialize the motif
    }

    //function id
    public static final int find_ = 0;
    public static final int goToMyRow_ = 1;
    public static final int initialKill_ = 2;

    public Object callMethod( int funcId, Object args )
    {
	switch( funcId )
	    {
	       case find_: return find( args );
	       case goToMyRow_: return goToMyRow( args );
	       case initialKill_ : return initialKill( args );
	    }

	return null;
    }

    public Object initialKill ( Object args )
    {
	/* This place does not have edge, so,
	   kill the agent on that place */
	if ( !( ( AdjMatrix )place ).checkEdge() )
	{
	     kill();
	}

	motif.add ( place.index[0] );
	return null;
    }

    public Object goToMyRow( Object args)
    {
	int x = place.index[0];
	int y = place.index[1];
	
	migrate( y , x );
	return null;
    }


    //Find the motifs
    public Object find( Object args)
    {
	// This place has an edge
	if ( ( ( AdjMatrix )place ).checkEdge() )
        {   	        
	    /* To eliminate duplicates, only if the 
	       column > row then add column to motif */
	     motif.add( place.index[1] ); 
	     
	     /* If it reached motif size, remove the last
		vertex in motif to create different combinations 
		with motif.size() - 1, like 1-2-3, 1-2-4, 1-2-5  */
	     if (  motif.size() ==  motifSize  )
	     {
		     //find a motif with motfiSize, not done currently
		     // ( ( AdjMatrix )place ).putMotif( motif );      // register it

		     for ( int i = 0 ; i < motif.size() ; i++ )
		     {
			     System.err.print( motif.get(i) + "-" );
		     }
		     System.err.println();
		     motif.remove( 2 );
		     
	      }

	     migrate( place.index[0] , place.index[1] + 1 );
	     if ( place.index[1] == place.size[1] - 1 )
	     {
		     kill( );
	     }

        }

	/* Agent bumped into the rightmost column place 
	   on the current row, so termminate it */
       else if ( place.index[1] == place.size[1] - 1 ) 
       {
	       kill( );
       }

	/* Agent himself move to a next column place. */
       else
       {	      
	      migrate( place.index[0] , place.index[1] + 1 );
       }

      return null;
    }

}