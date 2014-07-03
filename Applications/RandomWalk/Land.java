/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import MASS.*;             // Library for Multi-Agent Spatial Simulation
import java.util.Vector;   // for Vector
import java.awt.*;         // uses the abstract windowing toolkit
import java.awt.event.*;   // also uses key events so we need this

// Land Array
public class Land extends Place {
    // function identifiers
    public static final int exchange_ = 0;
    public static final int update_ = 1;
    public static final int startGraphics_ = 2;
    public static final int finishGraphics_ = 3;
    public static final int init_ = 4;
    public static final int collectAgents_ = 5;
    public static final int writeToGraphics_ = 6;
    
   // Graphics --------------------------------------------------------------------------
  private static final int defaultN = 100; // the default system size
  private static final int defaultCellWidth = 8;
  private static Color bgColor;            //white background
  private static Frame gWin;               // a graphics window
  private static int cellWidth;            // each cell's width in the window
  private static Insets theInsets;         // the insets of the window 
  private static Color wvColor[];          // wave color
  private static int N = 0;                // array size
  
   // the array size and my index in (x, y) coordinates
  private int sizeX, sizeY;
  private int myX, myY;
  // wave height from four neighbors: north, east, south, and west
  private final int north = 0, east = 1, south = 2, west = 3;
  
    // constructor
    public Land( ) {
        super(  );
    }
    
    // constructor
    public Land( Object object ) {
        super(  );
    }  
    
  /** 
   * Since size[] and index[] are not yet set by
   * the system when the constructor is called, this init( ) method must
   * be called "after" rather than "during" the constructor call
   * @param args formally declared but actually not used
   */
  public Object init( Object args ) {
	sizeX = size[0]; sizeY = size[1]; // size  is the base data members
	myX = index[0];  myY = index[1];  // index is the base data members
        
	return null;
  }
  
  // start a graphics window ------------------------------------------------------
  public Object startGraphics( Object args ) {
	// define the array size
	N = size[0];
	
	// Graphics must be handled by a single thread
	bgColor = new Color( 255, 255, 255 );//white background
	
	// the cell width in a window
	cellWidth = (int)((double) defaultCellWidth / ((double) N / (double) defaultN )); //mod by John
	if ( cellWidth == 0 )
	  cellWidth = 1;
	
	// initialize window and graphics:
	gWin = new Frame( "Random Walk Simulation" );
	gWin.setLocation( 50, 50 );  // screen coordinates of top left corner
	
	gWin.setResizable( false );
	gWin.setVisible( true );     // show it!
	theInsets = gWin.getInsets();
	Dimension frameDim = new Dimension (N * cellWidth + theInsets.left + theInsets.right,
										N * cellWidth + theInsets.top + theInsets.bottom);
	gWin.setSize(frameDim);
	
	// wait for frame to get initialized
	long resumeTime = System.currentTimeMillis() + 1000;
	do {} while (System.currentTimeMillis() < resumeTime);
	
	// paint the back ground
	Graphics g = gWin.getGraphics( );
	g.setColor( bgColor );
	g.fillRect( theInsets.left,
			   theInsets.top,
			   N * cellWidth,
			   N * cellWidth );
	
	// prepare cell colors
	wvColor = new Color[21];
	wvColor[0] = new Color( 0x0000FF );   // blue
	wvColor[1] = new Color( 0x0033FF );
	wvColor[2] = new Color( 0x0066FF );
	wvColor[3] = new Color( 0x0099FF );
	wvColor[4] = new Color( 0x00CCFF );
	wvColor[5] = new Color( 0x00FFFF );
	wvColor[6] = new Color( 0x00FFCC );
	wvColor[7] = new Color( 0x00FF99 );
	wvColor[8] = new Color( 0x00FF66 );
	wvColor[9] = new Color( 0x00FF33 );
	wvColor[10] = new Color( 0x00FF00 );  // green
	wvColor[11] = new Color( 0x33FF00 );
	wvColor[12] = new Color( 0x66FF00 );
	wvColor[13] = new Color( 0x99FF00 );
	wvColor[14] = new Color( 0xCCFF00 );
	wvColor[15] = new Color( 0xFFFF00 );
	wvColor[16] = new Color( 0xFFCC00 );
	wvColor[17] = new Color( 0xFF9900 );
	wvColor[18] = new Color( 0xFF6600 );
	wvColor[19] = new Color( 0xFF3300 );
	wvColor[20] = new Color( 0xFF0000 );  // red
	
	gWin.setLocation( 0, 0 ); //mod by John, debug line
	return null;
  }
  
  // update a graphics window with new cell information -------------------------------------
  public Object writeToGraphics( Object agents ) {
	Object[] waves = ( Object[] )agents;
	Graphics g = gWin.getGraphics( );
	for ( int i = 0; i < sizeX; i++  ) 
	  for ( int j = 0; j < sizeY; j++ ) {
		// convert a wave height to a color index ( 0 through to 20 ) 
                int cellValue = (int)((Double)waves[i * sizeY + j ]).intValue();
                Color cellColor = null; 
                
                if(cellValue > 0)
                {
                    int index =  (int)((Double)waves[i * sizeY + j ]).intValue();// + 10;
                    //if((Double)waves[i * sizeY + j ] > 0.0) System.err.println(" index is " + ((Double)waves[i * sizeY + j ]).intValue());
                    index = ( index > 20 ) ? 20 : ( ( index < 0 ) ? 0 : index );
                    cellColor = wvColor[index];
                }
                else
                {
                    cellColor = bgColor;
                }
                
		// show a cell
		g.setColor( cellColor );
		g.fill3DRect( theInsets.left + i * cellWidth,
					 theInsets.top  + j * cellWidth,
					 cellWidth, cellWidth, true ); //mod by John
	  }
	return null;
  }
  
  // finish the graphics window -------------------------------------
  public Object finishGraphics( Object args ) {
	Graphics g = gWin.getGraphics( );
	g.dispose( );
	gWin.removeNotify( );
	gWin = null;
	
	return null;
  }
  // --------------------------------------------------------------------------
  /** 
   * Return the local wave height to the cell[0,0]
   * @param args formally declared but actually not used.
   */
  public double collectAgents( Object args ) {
      //if(agents.size() > 0 ) System.err.println(agents.size());     
      return ( agents.size() ); 
  }
  
  /**
     * Is called from callAll( ) or exchangeAll( ), and forwards this call
     * to update( ) or exchange( ).
     * @param funcId the function Id to call
     * @param args argumenets passed to this funcId.
     */
    public Object callMethod( int funcId, Object args ) 
    {
        switch ( funcId ) 
        {
            case exchange_: return exchange( args );
            case update_: return update( args );
            case startGraphics_: return startGraphics(args);
            case finishGraphics_ : return finishGraphics(args);
            case init_ : return init(args); 
            case collectAgents_ : return ( Object )collectAgents(args);
            case writeToGraphics_ : return writeToGraphics(args);
        }
        return null;
    }

    int[][] neighbors = new int[2][2]; // my four neighbors' #agents

    /**
     * Is called from exchangeAll( ) to exchange #agents with my 
     * four neighbors
     * @param args formally requested but actuall not used.
     */
    public Object exchange( Object args ) {
        return new Integer( agents.size( ) );
    }

    /**
     * Is called from callAll( ) to update my four neighbors' #agents.
     * @param args formally requested but actuall not used.
     */
    public Object update( Object args ) {
        int index = 0;
        for ( int x = 0; x < 2; x++ )
            for ( int y = 0; y < 2; y++ )
                neighbors[x][y] = ( inMessages[index] == null ) ?
                    Integer.MAX_VALUE : 
                    ( ( Integer )inMessages[index] ).intValue( );
        return null;
    }
}
