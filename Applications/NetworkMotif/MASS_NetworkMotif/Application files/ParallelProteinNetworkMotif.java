import MASS.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author amala
 */
public class ParallelProteinNetworkMotif {

    /**
     * @param args the command line arguments
     */
    
    public static void main( String[] args ) throws Exception {
        // TODO code application logic here
        // verify arguments
        System.out.println("Total arguments = " + args.length);
	if ( args.length != 5 ) {
	    System.out.println( "usage: java -cp MASS.jar:jsch-0.1.44.jar:. ParallelProteinNetworkMotif userid port nProcs nThrs size" );
	    System.exit( -1 );
	}
	String[] massArgs = new String[4];
	massArgs[0] = args[0];          // user name
	massArgs[1] = "arpi15ankit1";   // password
	massArgs[2] = "machinefile.txt"; // machine file
	massArgs[3] = args[1];           // port

	int nProcesses = Integer.parseInt( args[2] );
	int nThreads = Integer.parseInt( args[3] );
	int nVertices = Integer.parseInt( args[4] ); //should be number of vertices + 1 (file prefix +1), because there may be a node '0'

	// start MASS
	MASS.init( massArgs, nProcesses, nThreads );
        
        //create the adjacency matrix
        Places adjMatrix = new Places( 1, "AdjMatrix", ( Object )null, nVertices, nVertices );
        
        //initialize the all matrix elements to false
        adjMatrix.callAll( AdjMatrix.init_, null );
	
	// find the edges
	File file = new File("9vertices.txt");
        try
	    {
		Scanner scanner = new Scanner( file );
		String tok1_Vertex, tok2_Edge;
           
		while ( scanner.hasNextLine( ) )
		    {
			String line = scanner.nextLine();
			// Each line in input file has V and E, separated by a tab.
			StringTokenizer tokenizer=new StringTokenizer(line,"\t");
			tok1_Vertex = tokenizer.nextToken();			
			tok2_Edge = tokenizer.nextToken();               
			int V = Integer.parseInt(tok1_Vertex);
			int E = Integer.parseInt(tok2_Edge);
			int[] index = {V , E};
			//update the Place V,E to true
			//System.out.print( index[0] + " - " + index[1] + "  " );
			adjMatrix.callSome( AdjMatrix.update_, ( Object )null, index[0], index[1] );
			adjMatrix.callSome( AdjMatrix.update_, ( Object )null, index[1], index[0] );
			
		    }
	    }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	
	// distribute agents over each row of adjMatrix
	// Remeber that the agents fill first row then second , and so on. 
	Agents agents = new Agents( 2, "MotifFinder", null, adjMatrix, nVertices * nVertices );
	
	// Start the time
	Date startTime = new Date( );
	
	/* In case of initiating nVertices agents , move them to 
	   start of each row ( may be used in future) */
	//agents.callAll( MotifFinder.goToMyRow_ );
	//agents.manageAll( );
	
	/*In case of initiating nVertices * nVertices agents
	  kill the agents who dont have 1 in their place */
	agents.callAll( MotifFinder.initialKill_ );
	agents.manageAll( );

	//Loop until all the agents are killed
	while ( agents.nAgents( ) > 0 )
	{
	    agents.callAll( MotifFinder.find_ ); // examine if this place has an edge
	    agents.manageAll( );     // move to a next place
	    //System.out.println( "Number of agents is:" + agents.totalAgents() );
	}
	
	//End time
	Date endTime = new Date( );

	//Total miliseconds in finding the motif
        System.out.println( "\tTime (ms): " + ( endTime.getTime( ) - startTime.getTime( ) ) );
	
	//Finish MASS
	MASS.finish();
	
   }
}