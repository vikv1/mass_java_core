/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/* Approach 2: This is a different approach to protein network. It is based on ESU Algorithm which is used by the Tool "FaNMOD". */
/* This approach is much faster sequentially */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Date;
/**
 * Test Application to search network motif
 * @author amala
 */
public class SequentialNetworkMotif {

    
    public static int filePrefix = 16 ;
    public static int requiredMotifSize = 3;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int totalVertices = 1000; // should be in 1000's in pratical
        
        boolean[][] adjMatrix = new boolean[totalVertices][totalVertices]; //adjacency matrix
                
        //Initialize the adjacency matrix to false
        for (int i = 0 ; i < totalVertices ; i++ ){
            for (int j = 0; j < totalVertices; j++){
                adjMatrix [i][j] = false;
            }
        }
        
        // Add the Edge to the Adjacency Matrix. Get V and E from input file.
        File file = new File("16vertices.txt");
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
		    }
	    }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // The ESU Algorithm
        Date startTime = new Date( );
        ArrayList<Integer> VSubgraph = new ArrayList(); // store the subgraph
        ArrayList<Integer> VExtension = new ArrayList(); // adjacent edges for one vertex              
        ArrayList<Integer> VdashExtension = new ArrayList();      
        int sugraphsize = 0; // sub grpah size
        
        for ( int graphSize = 0 ; graphSize < 1 ; graphSize++)
	    {
		//Iterate for all the vertices
		for (int currentvertex = 1; currentvertex <= filePrefix ; currentvertex++)
		    {
			//Add vertices to VExtension
			//To satisfy condition, u > v , start with currentvertex + 1
			for (int j = currentvertex + 1; j <= filePrefix; j++)
			    {
				if (adjMatrix[currentvertex][j])
				    {
					VExtension.add(j);
				    }
			    }
			VSubgraph.add(currentvertex);  
			//Call ExtendSubgraph method to find the subgraph with current vertex and size
			SequentialNetworkMotif pf = new SequentialNetworkMotif();
           
			if(!VExtension.isEmpty())
			    {
                
				pf.ExtendSubGraph(VSubgraph, VExtension, VdashExtension, currentvertex, totalVertices, adjMatrix);
			    }
			VSubgraph.clear();
			VExtension.clear();
                                 
		    }
	    }
        Date endTime = new Date( );
        System.out.println( "\tTime (ms): " + ( endTime.getTime( ) - startTime.getTime( ) ) );

    }

    //The recursive method to find the subgrapah 
    void ExtendSubGraph(ArrayList<Integer> VSubgraph, ArrayList<Integer> VExtension, ArrayList<Integer> VdashExtension, int currentvertex, int totalVertices, boolean[][] adjMatrix)
    {
     
	// if the subgraph is of the required size - 1, then output the subgraphs
	if(VSubgraph.size() == ( requiredMotifSize - 1 ))// or the size required
	    {
		System.out.println("\nOutput Subgraphs");
             
		//pop out all the remaining vertices in VExtension
		while (!VdashExtension.isEmpty())
		    {
			for(int j = 0 ; j <  requiredMotifSize - 1 ; j++)
			    {
				System.out.print(VSubgraph.get(j));
				System.out.print("-");
			    }
			System.out.println(VdashExtension.get(0));
			VdashExtension.remove(0);
		    }
		VSubgraph.clear();
		VSubgraph.add(currentvertex);
          
	    }
     
	while (!VExtension.isEmpty())
	    {             
		// Choose the next vertex in VExtension, Remove it from VExt and add it to subgraph
		int w = VExtension.get(0);
		VExtension.remove(0);
		VSubgraph.add(w);
                                 
		//copy the VExtension to VdashExtension
		for (int i = 0 ; i < VExtension.size(); i++)
		    {
			VdashExtension.add(VExtension.get(i));
		    }
              
		// add the exclusive neigbour of w in VExtension                              
		for (int vertex = w+1 ; vertex <= filePrefix; vertex++)
		    {
			//check if vertex is the neighbour of w
			if (adjMatrix[w][vertex]) 
			    {
                            
				boolean flag = false;
				//Check if vertex is already present in VdashExtension, if not add it
				for (int i = 0 ; i < VdashExtension.size() ; i++)
				    {
					if (VdashExtension.get(i) == vertex )
					    {
						flag = true;
						break;
					    }

				    }
				if (flag == false)
				    {
					VdashExtension.add(vertex);
				    }

			    }

		    }
                // Recursion over ExtendSubgraph till required subgraph size is reached
                ExtendSubGraph(VSubgraph, VExtension, VdashExtension, currentvertex, totalVertices, adjMatrix);                        
	    }
      
    }

}
