// Land.java
/////////////////////////////////////////////////////////////////////////////////////////
//
//	SugarScape Land 
//	---------------
//
/////////////////////////////////////////////////////////////////////////////////////////
import MASS.*;             
import java.util.*;  
import java.awt.*;       
import java.awt.event.*;

// Land Array
public class Land extends Place {

    	// Function identifiers
    	public static final int exchange_ 	 = 0;
    	public static final int update_ 	 = 1;
    	public static final int startGraphics_ 	 = 2;
    	public static final int finishGraphics_  = 3;
    	public static final int init_ 		 = 4;
    	public static final int collectLocData_	 = 5;
    	public static final int writeToGraphics_ = 6;

   	// Graphics 
  	private static final int defaultN = 100; 	// the default system size
  	private static final int defaultCellWidth = 8;
  	private static Color 	 bgColor;            	//white background
  	private static Frame 	 gWin;               	// a graphics window
  	private static int 	 cellWidth;            	// each cell's width in the window
  	private static Insets 	 theInsets;         	// the insets of the window 
  	private static Color 	 agentColor[];         	// agent color
  	private static Color 	 sugarColor[];         	// sugar color
  	private static int 	 N = 0;                	// array size

   	// Setup the array size and index location (x,y), and sugar inventory
  	private int sizeX, sizeY;
  	private int myX, myY;
	public int sugar;
  	int[] nbrNumAgents = null;
	int[] nbrNumSugar  = null;
	int vDist;

    	// Construct(rs
    	public Land( ) {
        	super(  );
    	}

    	public Land( Object object ) {
        	super(  );
    	}


  	/**
     	* @param funcId the function Id to call
     	* @param args argumenets passed to this funcId.
     	*/
  	// --------------------------------------------------------------------------
    	public Object callMethod( int funcId, Object args )
    	{
            switch ( funcId )
            {
            	case init_ : 		return init(args);
            	case exchange_: 	return exchange( args );
            	case update_: 		return update( args );
            	case collectLocData_ : 	return ( Object )collectLocationData(args);

            	case startGraphics_: 	return startGraphics(args);
            	case finishGraphics_ : 	return finishGraphics(args);
            	case writeToGraphics_ : return writeToGraphics(args);
            }
            return null;
    	}

	// SugarScape Functions
	// ==============================================================================

	// Initialize the place
	// -----------------------
  	public Object init( Object args ) {

        	sizeX = size[0]; sizeY = size[1]; // size  is the base data members
        	myX = index[0];  myY = index[1];  // index is the base data members

		// Setup Visual Distance and the arrays for 
		// storing my neighbors agent and sugar count
		vDist = 8;
        int arrySize =  4 * ((vDist * vDist) + vDist);
  		nbrNumAgents = new int[ arrySize ]; 
		nbrNumSugar  = new int[ arrySize ]; 
        for( int i = 0; i < arrySize; i++ ) {
  			nbrNumAgents[i] = 0;
			nbrNumSugar[i]  = 0;
        }

		// Place Sugar Randomly all over the grid
		int numSugarValues = 5;
		Random gen = new Random();
		int tmpValue = gen.nextInt( numSugarValues * 2 );
		if( tmpValue >= numSugarValues ) sugar = 0;
		else	sugar = tmpValue;

        	return null;
  	}

    	/**
     	  * Is called from exchangeAll( ) to exchange #agents with my neighbors
     	  * @param args formally requested but actuall not used.
      	*/ // ----------------------------------------------------
    	public Object exchange( Object args ) {

		int[] unitData = { (int)agents.size(), sugar };
		return (Object)unitData;
    	}


    	/**
     	  * Is called from callAll( ) to update my neighbors' #agents and #sugar
     	  * @param args formally requested but actuall not used.
     	*/ // ----------------------------------------------------
    	public Object update( Object args ) {

		int arrySize = inMessages.length;

		for( int i = 0; i < arrySize; i++ ) {
		
			if( inMessages[i] == null ) {
				nbrNumAgents[i] = 0;
				nbrNumSugar[i]  = 0;
			} else {			
				nbrNumAgents[i] = ((int[])inMessages[i])[0];
				nbrNumSugar[i]  = ((int[])inMessages[i])[1];
			}
		}
        	return null;
    	}

	/** Used by Unit (agent) to get the index of array locations 
	  * that contain the neighbors for the unit with a different 
	  * (smaller) visual distance than what is stored by the land
	  * @param unitVDist 	Unit's visual distance 
   	*/ // ----------------------------------------------------
	public int[] getNbrLocations( int unitVDist ) {


	    if( unitVDist > vDist ) { 
		MASS.log( "getMbrLocations: nitVDist = " + unitVDist + ", vDist = " + vDist );
		return null;
	    }

		int arrySize  =  4 * ((unitVDist * unitVDist) + unitVDist);
		int[] nbrList = new int[ arrySize ];

		int maxIndex = 0;
		int tmpIndex = 0;

                for( int x = 0 - vDist; x <= vDist; x++ ) {
                    for( int y = 0 - vDist; y <= vDist; y++ ) {

                        if( !(x == 0 && y == 0) && 
			     ( x >= 0 - unitVDist ) && ( x <= unitVDist ) &&
			     ( y >= 0 - unitVDist ) && ( y <= unitVDist ) ) {

				nbrList[ tmpIndex ] = maxIndex;
				tmpIndex++;
			}
                        if( !(x == 0 && y == 0) ) maxIndex++;
		   }
		}

		return nbrList;
	}


	/**
	  *
	*/ //---------------------------------------------------
	public boolean consumeSugar() {
		if( sugar > 0 ) { 
			System.err.print( "Sugar consumed.... Level: " + sugar );
			sugar--;
			System.err.println( " -> " + sugar );
			return true;
		}
		return false;
	}


	/**
	  *
	*/ //---------------------------------------------------
	public int getNumberOfAgents(){
		return (int)agents.size();
	}


  	/** Used for collecting the local data to diplay
   	  * @param args formally declared but actually not used.
   	*/ // ----------------------------------------------------
  	public Object collectLocationData( Object args ) {

		int[] unitData = { (int)agents.size(), sugar };
      		return (Object)unitData;
  	}


	// Graphics Functions
	// ==============================================================================

	// Start a graphics window
	// -----------------------
	public Object startGraphics( Object args ) {

        	// define the array size
        	N = size[0];

        	// Graphics must be handled by a single thread
        	bgColor = new Color( 255, 255, 255 );	//white background

        	// Calculate the cell width in a window
        	cellWidth = (int)((double) defaultCellWidth / ((double) N / (double) defaultN ));
        	if ( cellWidth == 0 ) cellWidth = 1;

        	// Initialize window and graphics:
        	gWin = new Frame( "SugarScape Simulation" );
        	gWin.setLocation( 50, 50 );  		// screen coordinates of top left corner

        	gWin.setResizable( false );
        	gWin.setVisible( true );     
        	theInsets = gWin.getInsets();
        	Dimension frameDim = new Dimension (N * cellWidth + theInsets.left + theInsets.right,
               		                            N * cellWidth + theInsets.top + theInsets.bottom);
        	gWin.setSize(frameDim);

        	// Wait for frame to get initialized
        	long resumeTime = System.currentTimeMillis() + 1000;
        	do {} while (System.currentTimeMillis() < resumeTime);

        	// Paint the background
        	Graphics g = gWin.getGraphics( );
        	g.setColor( bgColor );
        	g.fillRect( theInsets.left, theInsets.top, N * cellWidth, N * cellWidth );

        	// Agent Colors
		agentColor = new Color[10];
        	agentColor[0] = new Color( 0x0066FF );
        	agentColor[1] = new Color( 0x0033FF );
        	agentColor[2] = new Color( 0x0000FF );   // blue
        	agentColor[3] = new Color( 0x0000CC );
        	agentColor[4] = new Color( 0x000099 );
        	agentColor[5] = new Color( 0x330099 );
        	agentColor[6] = new Color( 0x660099 );
        	agentColor[7] = new Color( 0x660066 );
        	agentColor[8] = new Color( 0x660033 );
        	agentColor[9] = new Color( 0x660000 );

		// Sugar Colors
		sugarColor = new Color[10];
		sugarColor[0] = new Color( 0xFFFFFF ); 	// white
		sugarColor[1] = new Color( 0x00FF99 );	// Lt Green
        	sugarColor[2] = new Color( 0x00FF66 );
        	sugarColor[3] = new Color( 0x00FF33 );
        	sugarColor[4] = new Color( 0x00CC33 );
        	sugarColor[5] = new Color( 0x00FF00 );
        	sugarColor[6] = new Color( 0x00CC00 );
        	sugarColor[7] = new Color( 0x009900 );
        	sugarColor[8] = new Color( 0x006600 );
        	sugarColor[9] = new Color( 0x003300 );

        	return null;
  	}

  	// Update graphics window with new cell information 
	// ------------------------------------------------
  	public Object writeToGraphics( Object data ) {

		Object[] locData = (Object[]) data;
        	Graphics g = gWin.getGraphics( );

		int maxAgentColor = agentColor.length;
		int maxSugarColor = sugarColor.length;

        	for ( int i = 0; i < sizeX; i++  ) {
          	   for ( int j = 0; j < sizeY; j++ ) {

                	Color cellColor = null;
                	int numAgents = ((int[])( (Object)locData[ i * sizeY + j ] ))[0];
                	int numSugar  = ((int[])( (Object)locData[ i * sizeY + j ] ))[1];

			// **********************************************************************************
			if( numAgents > 0 ) { 		// Has agents
				cellColor = ( numAgents >= maxAgentColor ) ? agentColor[ maxAgentColor - 1 ] 
									  : agentColor[ numAgents ];
			} else if( numSugar > 0 ) {	// Has sugar
				cellColor = ( numSugar >= maxSugarColor ) ? sugarColor[ maxSugarColor - 1 ]
									 : sugarColor[ numSugar ];
			} else {
				cellColor = bgColor;
			}

                	// show a cell
                	g.setColor( cellColor );
                	g.fill3DRect( theInsets.left + i * cellWidth, theInsets.top  + j * cellWidth,
                                         cellWidth, cellWidth, true ); //mod by John
          	    }
		}
        	return null;
  	}

  	// Finish graphics window 
	// ----------------------
  	public Object finishGraphics( Object args ) {

        Graphics g = gWin.getGraphics( );

		if( g != null ) {
        	g.dispose( );
        	gWin.removeNotify( );
        	gWin = null;
		}
        return null;
  	}

}
