import MASS.*;

public class Main {
    public static void main( String[] args ) {
	if ( args.length != 6 ) {
	    System.err.println( "usage: java -cp MASS.jar:. Main username " +
				"password machinefile port nProc nThr" );
	    System.exit( -1 );
	}
	String[] arguments = new String[4];
	arguments[0] = args[0]; // username
	arguments[1] = args[1]; // password
	arguments[2] = args[2]; // machinefile
	arguments[3] = args[3]; // port
	int nProc = Integer.parseInt( args[4] );
	int nThr = Integer.parseInt( args[5] );

	MASS.init( arguments, nProc, nThr );
	String msg = "hello";
	Places land = new Places( 1, "Land", 1, (Object)msg, 100, 100 );
 
	MASS.finish( );
    }
}