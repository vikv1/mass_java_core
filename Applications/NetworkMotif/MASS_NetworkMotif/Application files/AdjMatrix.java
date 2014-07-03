/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import MASS.*;
import java.util.Vector;

/**
 *
 * @author amala
 */
public class AdjMatrix extends Place
{
    public boolean isEdge;
    Vector myMotif = new Vector(5); // change the size later on

    public AdjMatrix()
    {
	super();
    }

    public AdjMatrix( Object args )
    {
	super();
    }
              
    public static final int init_ = 0;
    public static final int update_ = 1;
    public static final int collect_ = 2;
    
    public Object callMethod( int funcId, Object args ) 
    {
	switch( funcId ) 
	{
	    case init_: return init( args );
            case update_: return update ( args );
            case collect_ : return collect ( args );
	}
	return null;
    }
       
    
    //Initialize all the places to false
    public Object init( Object args )
    {
	//System.err.print( "init" );

        isEdge = false;       
        return null;
    }


    // Update the place of index[0],index[1] to true, to indicate there is an edge    
    public Object update( Object args )
    {
        isEdge = true;

        return null;
    }


    //checkEdge is used by agent to know if the there is an edge at this place
    public boolean checkEdge()
    {
	boolean thisEdge = isEdge;
	return thisEdge;
    }

    
    public Object collect(Object args)
    {
        return (Object) myMotif;
    }
}

