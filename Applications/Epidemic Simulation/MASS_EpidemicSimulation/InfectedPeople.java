/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import MASS.*;

/**
 *
 * @author amala
 */

public class InfectedPeople extends Place
{
    int isInfected;
    int community = size[0];
    int populationPerCommunity = size[1];
    int data ;

    // checking all four neighbors if infected: north, east, south, and west
    //private final int north = 0, east = 1, south = 2, west = 3;
    int[] neighbour = new int[4];
    private int sizeX, sizeY;
    private int myX, myY;

    public InfectedPeople()
    {
	super();
    }

    public InfectedPeople( Object args )
    {
	super();
    }

    public static final int init_ = 0;
    public static final int initialInfection_ = 1;
    public static final int day_ = 2;
    public static final int night_ = 3;
    public static final int sync_ = 4;
    public static final int response_ = 5;
    //public static final int totalSeeds_ = 6;
    public static final int collectInfected_ = 7;

    public Object callMethod( int funcId, Object args )
    {
	switch( funcId )
	{
	    case init_: return init( args );
	    case initialInfection_ : return initialInfection ( args );
	    case collectInfected_ : return ( Object )collectInfected ( args );
	    case day_: return day ( args );
	    case night_ : return night ( args );
	    case sync_ : return sync ( args );
	    case response_ : return response ( args );
		// case totalSeeds_ : return totalSeeds ( args );		
	}
	return null;
    }


    /*Initialize all the places to false,
      to indicate that no one is infected */
    public Object init( Object args )
    {
	isInfected = 0;
	data = 0 ;
	sizeX = size[0]; sizeY = size[1]; // size  is the base data members
	myX = index[0];  myY = index[1];  // index is the base data members
	return null;
    }


    /* Update the place to true, to indicate that
    these people are initially infected */
    public Object initialInfection( Object args )
    {
	isInfected = 1;
	data = 1;
	return null;
    }


    public Object day( Object args_TotalInfected )
    {
	Object[] seedData = ( Object[] ) args_TotalInfected;

	//int value = ((int)( (Object)seedData[0] ));
	//System.err.print( "**" + value );
	
	int myIndex = myX * sizeY + myY;

        int west  = Math.max( myX - 1, 0   );
        int east = Math.min( myX + 1, sizeX - 1 );
        int south  = Math.max( myY - 1, 0   );
        int north = Math.min( myY + 1, sizeY - 1 );
	
	neighbour[0] = west * sizeY + myY;
	neighbour[1] = east * sizeY + myY; 
	neighbour[2] = myX * sizeY + south;
	neighbour[3] = myX * sizeY + north;

	//System.err.println( myIndex + " are " +  neighbour[0] + ", " +  neighbour[1] + ", "  +  neighbour[2] + ", " +  neighbour[3] );

	// If i am not infected then check all 4 neighbour
	// even if they are in different communities
	if ( isInfected != 1)
	{
	    for ( int i = 0 ; i < 4 ; i++ )
	    {
		// If neighbour is infected, then infect me
		if ( neighbour[i] == 1 )
		{
		    isInfected = 1;
		   
		}
	    }
	}

	return null;
    }

    public Object night( Object args )
    {
	int west  = Math.max( myX - 1, 0   );
        int east = Math.min( myX + 1, sizeX - 1 );

	neighbour[0] = west * sizeY + myY;
        neighbour[1] = east * sizeY + myY;

	// If i am not infected then check neighbour in my community
	//because at night i am with people in my community only
        if ( isInfected != 1)
	{
		for ( int i = 0 ; i < 2 ; i++ )
		    {
			// If neighbour is infected, then infect me
			if ( neighbour[i] == 1 )
			    {
				isInfected = 1;
				break;
			    }
		    }
	}	
        return null;
    }


    public Object sync( Object args )
    {
        //isInfected = 1;
        return null;
    }

    public Object response( Object args )
    {
	boolean infectMe;
      	if ( isInfected == 1 )
	    infectMe = false;
        else
	{
	    isInfected = 1;
	    infectMe = true;
	}
	//return infectMe;
	return null;
    }

   
    public Object collectInfected( Object args )
    {    
	return ( Object ) isInfected;
    }
}

