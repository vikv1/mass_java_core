/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/* Approach 1: This approach is used by MASS to find the network motif, currently this approach is considered to measure the performance */


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Date;
/**
 *
 * @author amala_000
 */
public class NewProteinNetworkMotif {
    
    public static int filePrefix = 101 ;// total vertices + 1, because there may be a '0' vertex 
    public static int requiredMotifSize = 3;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int totalVertices = 1000; // should be greater than the highest value of the node in the input.txt file
        
        boolean[][] adjMatrix = new boolean[totalVertices][totalVertices]; //adjacency matrix
                
        //Initialize the adjacency matrix to false
        for (int i = 0 ; i < totalVertices ; i++ ){
            for (int j = 0; j < totalVertices; j++){
                adjMatrix [i][j] = false;
            }
        }
        
        // Add the Edge to the Adjacency Matrix. Get V and E from input file.
        File file = new File("100vertices.txt");
        try
	    {
		//Reading the file line by line by scanner
		Scanner scanner = new Scanner(file);
		String tok1_Vertex, tok2_Edge;
        
		while (scanner.hasNextLine())
		    {
			String line = scanner.nextLine();
			// Each line in input file has V and E, separated by a space.
			StringTokenizer tokenizer=new StringTokenizer(line,"\t");
			tok1_Vertex=tokenizer.nextToken();
			System.out.println(tok1_Vertex + ".. ");
			tok2_Edge=tokenizer.nextToken();
			System.out.print(tok2_Edge + ", ");
            
			//Add edge by setting matrix element to "true"
			int V = Integer.parseInt(tok1_Vertex);
			int E = Integer.parseInt(tok2_Edge);
			adjMatrix [V][E] = true;
			adjMatrix [E][V] = true; 
		    }
	    }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //Start Time
        Date startTime = new Date( );
        ArrayList<Integer> motif = new ArrayList(); // store the subgraph - possible motif 
        ArrayList<Integer> tmpMotif = new ArrayList();
        int motifSize = 3; // required motif size
        
	// Loop for all the rows, Every row is the first node of the motif
	// so add the row to the motif 
        for ( int row = 0 ; row < filePrefix ; row++)
	    {
		motif.add(row);
		tmpMotif = motif;
		
		// Add the column with represents the edge with the motif
		for ( int col = 0 ; col < filePrefix ; col++)
		    {
			//check if there is and edge. 
			if (adjMatrix[row][col])
			    {
				motif.add(col);
				if (  motif.size() ==  motifSize - 1  )
				    {
					for ( int j = col + 1; j < filePrefix; j++)
					    {
						if ( adjMatrix[row][j])
						    {
							motif.add(j);
							// Print the motif
							for ( int i = 0 ; i < motif.size() ; i++ )
							    {
								System.out.print( motif.get(i) + "-" );
							    }
							System.out.println();
							motif.remove( motifSize - 1 );
						    }
					    }
					motif.clear();
					// Now start finding motifs from the next row
					motif.add(row);
				    }
                       
			    }
		    }
		motif.clear();
	    }

	// End Time
        Date endTime = new Date( );
        System.out.println( "\tTime (ms): " + ( endTime.getTime( ) - startTime.getTime( ) ) );
    }
}
