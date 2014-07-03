/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CFD;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JFrame;
import java.util.Scanner;

/**
 *
 * @author munehiro
 */
class Mesh {

    static final int defaultN = 100;  // the default system size
    static final Color bgColor = new Color(255, 255, 255);//white background
    private int N = 100;                     // simulation size (default 100 x 100)
    private JFrame gWin;                     // a graphics window
    private int cellWidth = 20;              // each cell's width in the window
    private int offset = 50;
    private int width;
    private int height;
    private double coYi  = 0.75; // 0.9;  // 0.75
    private double coYj  = 0.25;  // 0.25
    private double coXj  = 0.0;   // -0.25;     // 0.0
    private Insets theInsets;                // the insets of the window
    private Color air[];                     // mesh color [0]: while, [1]: blue
    private Color white = new Color( 0xFFFFFF );
    private Color gray  = new Color( 0xCCCCCC );
    private Color black = new Color( 0x000011 );
    private Color red   = new Color( 0xFF0000 );
    private Color green = new Color( 0x00FF00 );

    public Mesh( int size ) {
        this.N = size;
	this.width = ( int )( cellWidth * N * 2 ) + offset;
	this.height = ( int )( cellWidth * N * 1.5 ) + offset * 2;
	startGraphics();
    }

    private void startGraphics() {

        // initialize window and graphics:
        gWin = new JFrame("Fluid Dynamics");
        gWin.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gWin.setLocation(50, 50);  // screen coordinates of top left corner
        gWin.setResizable(false);
        gWin.setVisible(true);     // show it!
        theInsets = gWin.getInsets();
        gWin.setSize(width + theInsets.left + theInsets.right,
		     height + theInsets.top + theInsets.bottom);

        // wait for frame to get initialized
        long resumeTime = System.currentTimeMillis() + 1000;
        do {
        } while (System.currentTimeMillis() < resumeTime);

	// pop out the graphics
        Graphics g = gWin.getGraphics();
        g.setColor( bgColor );
        g.fillRect( theInsets.left,
		    theInsets.top,
		    width,
		    height );

	// set two colors: meshColor[0] = "while", meshColor[1] = "blue"
	air = new Color[61];
	air[0] = new Color( 0x0000FF ); // blue
	air[1] = new Color( 0x0011FF );
	air[2] = new Color( 0x0022FF );
	air[3] = new Color( 0x0033FF );
	air[4] = new Color( 0x0044FF );
	air[5] = new Color( 0x0055FF );
	air[6] = new Color( 0x0066FF );
	air[7] = new Color( 0x0077FF );
	air[8] = new Color( 0x0088FF );
	air[9] = new Color( 0x0099FF );
	air[10] = new Color( 0x00AAFF );
	air[11] = new Color( 0x00BBFF );
	air[12] = new Color( 0x00CCFF );
	air[13] = new Color( 0x00DDFF );
	air[14] = new Color( 0x00EEFF );
	air[15] = new Color( 0x00FFFF );
	air[16] = new Color( 0x00FFEE );
	air[17] = new Color( 0x00FFDD );
	air[18] = new Color( 0x00FFCC );
	air[19] = new Color( 0x00FFBB );
	air[20] = new Color( 0x00FFAA );
	air[21] = new Color( 0x00FF99 );
	air[22] = new Color( 0x00FF88 );
	air[23] = new Color( 0x00FF77 );
	air[24] = new Color( 0x00FF66 );
	air[25] = new Color( 0x00FF55 );
	air[26] = new Color( 0x00FF44 );
	air[27] = new Color( 0x00FF33 );
	air[28] = new Color( 0x00FF22 );
	air[29] = new Color( 0x00FF11 );
	air[30] = new Color( 0x00FF00 ); // green
	air[31] = new Color( 0x11FF00 );
	air[32] = new Color( 0x22FF00 );
	air[33] = new Color( 0x33FF00 );
	air[34] = new Color( 0x44FF00 );
	air[35] = new Color( 0x55FF00 );
	air[36] = new Color( 0x66FF00 );
	air[37] = new Color( 0x77FF00 );
	air[38] = new Color( 0x88FF00 );
	air[39] = new Color( 0x99FF00 );
	air[40] = new Color( 0xAAFF00 );
	air[41] = new Color( 0xBBFF00 );
	air[42] = new Color( 0xCCFF00 );
	air[43] = new Color( 0xDDFF00 );
	air[44] = new Color( 0xEEFF00 );
	air[45] = new Color( 0xFFFF00 );
	air[46] = new Color( 0xFFEE00 );
	air[47] = new Color( 0xFFDD00 );
	air[48] = new Color( 0xFFCC00 );
	air[49] = new Color( 0xFFBB00 );
	air[50] = new Color( 0xFFAA00 );
	air[51] = new Color( 0xFF9900 );
	air[52] = new Color( 0xFF8800 );
	air[53] = new Color( 0xFF7700 );
	air[54] = new Color( 0xFF6600 );
	air[55] = new Color( 0xFF5500 );
	air[56] = new Color( 0xFF4400 );
	air[57] = new Color( 0xFF3300 );
	air[58] = new Color( 0xFF2200 );
	air[59] = new Color( 0xFF1100 );
	air[60] = new Color( 0xFF0000 ); // red
    }

    private int setI( int x, int y, int z ) {
	return theInsets.left + offset + ( int )( ( x + coYi * y ) * cellWidth);
    }

    private int setJ( int x, int y, int z ) {
	return theInsets.top + height - offset + ( int )( N * cellWidth * coXj ) - ( int )( ( coXj * x + coYj * y + z ) * cellWidth );
    }

    private void writeMesh() {
	Graphics g = gWin.getGraphics( );

	g.setColor( green );
	for ( int x = 0; x < N; x++ ) {
	    for ( int y = 0; y < N; y++ ) {
		for ( int z = 0; z < N; z++ ) {
		    g.fillOval( setI( x, y, z ), setJ( x, y, z ), 3, 3 );
		}
	    }
	}

	for ( int x = 0; x < N; x++ ) {
	    for ( int y = 0; y < N; y++ ) {
		for ( int z = 0; z < N; z++ ) {
		    if ( x > 0 ) {
			g.setColor( gray );
			if ( y == 0 )
			    g.setColor( black );
			if ( z == N - 1 )
			    g.setColor( black );
			g.drawLine( setI( x - 1, y, z ), setJ( x - 1, y, z ),
				    setI( x, y, z ), setJ( x, y, z ) );
		    }
		    if ( y > 0 ) {
			g.setColor( gray );
			if ( x == N - 1 )
			    g.setColor( black );
			if ( z == N - 1 )
			    g.setColor( black );
			g.drawLine( setI( x, y - 1, z ), setJ( x, y - 1, z ), 
				    setI( x, y, z ), setJ( x, y, z ) ); 
		    }
		    if ( z > 0 ) {
			g.setColor( gray );
			if ( y == 0 )
			    g.setColor( black );
			if ( x == N - 1 )
			    g.setColor( black );
			g.drawLine( setI( x, y, z - 1 ), setJ( x, y, z - 1 ), 
				    setI( x, y, z ), setJ( x, y, z ) ); 
		    }
		}
	    }
	}
    }

    private int temp2index( double temp ) {
	// int index = ( int )( temp * 60 );
	int index = ( int )( temp * 120 - 30 );
	index = ( index > 60 ) ? 60 : ( ( index < 0 ) ? 0 : index );
	return index;
    }

    private void writeDots( double t[] ) {
	Graphics g = gWin.getGraphics( );

	int i = 0;
	for ( int x = 1; x < N; x++ )
	    for ( int y = 1; y < N; y++ )
		for ( int z = 1; z < N; z++ ) {
		    g.setColor( air[ temp2index( t[i++] ) ] );
		    g.fillOval( setI( x, y, z ), setJ( x, y, z ), 3, 3 );
		}
    }

    public static void main(String args[]) {

        int size = Integer.parseInt( args[0] );
	Mesh mesh = new Mesh( size );
	mesh.writeMesh( );

	double[] t = new double[size * size * size];
	Scanner keyboard = new Scanner( System.in );

	
	while ( keyboard.hasNext( ) ) {
	    int i = 0;
            for ( int x = 1; x < size; x++ )
		for ( int y = 1; y < size; y++ )
		    for ( int z = 1; z < size; z++ ) {
			keyboard.nextInt( ); // x
			keyboard.nextInt( ); // y
			keyboard.nextInt( ); // z
			t[i++] = keyboard.nextDouble( );
		    }
	    mesh.writeDots( t );
        }
        System.out.println( "Done..." );

    }
}
