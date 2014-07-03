/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/* Sequential Version of Epidemic Simulation "FluTE"*/

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
/**
 *
 * @author amala_000
 */


public class EpidemicInfluenzaSimulation {

    /**
     * @param args the command line arguments
     */
    
    // Input Parameters 
    public static int InitialSeeds = 10; // number of infected people at the begining
    public static int NewSeeds = 10;
    public static int Communities = 7; // total communities
    public static int PopulationPerCommunity = 1000; // population per community
    public static int MigrantWorkers = 110; // migrant workers       
    public static int SimulationDays = 180; // number of days
    public static double SpreadingRate = 0.06; // influenza spreading rate
    public static double ImmunityRate = 0.25; // Immunize people at this rate
    
    // InfectedPeople represents the whole Population 
    public static boolean[][] InfectedPeople = new boolean[Communities][PopulationPerCommunity];
   
        
    public static void main(String[] args) 
    {
        // TODO code application logic here
	
        EpidemicInfluenzaSimulation fl = new EpidemicInfluenzaSimulation();
        Random rn = new Random();
        
        //Initialze all InfectedPerson to false, since no body is infected
        for ( int i = 0 ; i < Communities; i++ )
	{
		for ( int j = 0 ; j < PopulationPerCommunity ; j++)
		    {
			InfectedPeople[i][j] = false;
		    }
	}
        

        // Infect the initial seeds
        for( int i = 0 ; i < InitialSeeds ; i++ )
	{
		int randomCommunity = rn.nextInt(Communities);
		int randomPerson = rn.nextInt(PopulationPerCommunity);
		InfectedPeople[randomCommunity][randomPerson] = true;
		System.out.println("Infected Neighbour " + randomCommunity + "," + randomPerson);
	}

        //Number of infected people now changes
        NewSeeds = fl.totalSeeds();
        System.out.println("Number of People infected initially  " + NewSeeds );
        
        //Start the timer
	Date startTime = new Date( );
        
        // Run the simulation for given number of days, to infect people at given rate
        for (int day = 1 ; day <= SimulationDays ; day++ )
	{
		//Infect people at day time
		fl.day();

		//Infect people at night time
		fl.night();

		//Exchange the immigrants after one simulation cycle
		//fl.sync();

		//Vaccinate the people at given rate
		fl.response();

		//calculate the total seeds after each simulation cycle
		NewSeeds = fl.totalSeeds();
		System.out.println("Number of People infected after day " + day + "are " + NewSeeds );
	}
        
        Date endTime = new Date( );
        System.out.println( "\tTime (ms): " + ( endTime.getTime( ) - startTime.getTime( ) ) );
        
    }
    
    //Infect a person whose neighbour is infected
    void InfectMyNeighbour( boolean isDay, int row , int col )
    {
        boolean isNeighbourInfected = false;
        
        int rowStart  = Math.max( row - 1, 0   );
        int rowFinish = Math.min( row + 1, Communities - 1 );
        int colStart  = Math.max( col - 1, 0   );
        int colFinish = Math.min( col + 1, PopulationPerCommunity - 1 );
            
        /*Day time, so check all four neighbours because people may 
          interact with different communities */
        if ( isDay )
	    {
		// Find the non-infected neigbour and infect him
		for ( int curRow = rowStart; curRow <= rowFinish; curRow++ ) 
		    {
			for ( int curCol = colStart; curCol <= colFinish; curCol++ ) 
			    {
				if ( !InfectedPeople[curRow][curCol])
				    {
					InfectedPeople[curRow][curCol] = true;
					isNeighbourInfected = true;
					//System.out.println("Infected Neighbour " + curRow + "," + curCol);
					break;
				    }
			    }
                
			//Only wanted to infect one neighbour, so break
			if ( isNeighbourInfected )
			    {
				break;
			    }
		    }
	    }
                
        /* Night time, so only check adjacent neighbours in same row,
           because all the people are in same commnity at night  */
        else
	    {
		if ( (!InfectedPeople[row][colStart]) )
		    {
			InfectedPeople[row][colStart] = true;
		    }
		else if ( (!InfectedPeople[row][colFinish]) )
		    {
			InfectedPeople[row][colFinish] = true;
		    }
	    }
            
    }
    
    /* Total number of people infected after every simulation run*/
    int totalSeeds()
    {
	int count = 0 ;
	for ( int i = 0 ; i < Communities; i++ )
	    {
		for ( int j = 0 ; j < PopulationPerCommunity ; j++)
		    {
			if(InfectedPeople[i][j])
			    {
				count++;
			    }
		    }
	    }
	return count;
    }
    
    //DayTime Simulation
    void day()
    {
        // check every person in all communities
        for ( int i = 0 ; i < Communities ; i++)
	    {
		for( int j = 0 ; j < PopulationPerCommunity; j++ )
		    {
			if (InfectedPeople[i][j])
			    {
				/*check if my neghbour is infected
				  if not, then infect him. day = true, night = false*/
				InfectMyNeighbour(true , i , j);
				j++; // jump over a neighbour
			    }
                
		    }
	    }
        
    }
    
    //Night Time simulation
    void night()
    {
        for ( int i = 0 ; i < Communities ; i++)
	    {
		for( int j = 0 ; j < PopulationPerCommunity; j++ )
		    {
			if (InfectedPeople[i][j])
			    {
				/*check if my neghbour ( of my row ) is infected
				  if not, then infect him. day = true, night = false*/
				InfectMyNeighbour(false , i , j);
				break;
			    }
                
		    }
	    }
    }
    
    //Exchange migrant workers
    void sync()
    {
        // temporary array to store the migrant workers
        boolean Exchange[][] = new boolean[Communities][MigrantWorkers];
        
        // Copy the first 100 elements of all Communities        
        for ( int i = 0; i < Communities ; i++ )
	    {
		for ( int j = 0 ; j < MigrantWorkers ; j++ )
		    {
			Exchange[i][j] = InfectedPeople[i][j];                
		    }
	    }
                      
        // Exchange it in subsequent rows (1 to 2, 2 to 3 etc.)
        for ( int i = 0; i < Communities ; i++ )
	    {
		for ( int j = 0 ; j < MigrantWorkers ; j++ )
		    {
			if ( i == Communities - 1)
			    {
				InfectedPeople[i][j] = Exchange[0][j]; 
			    }
			else
			    {
				InfectedPeople[i][j] = Exchange[i+1][j]; 
			    }               
		    }
	    }
    }
    
    // Give vaccine to people to immune them
    void response()
    {        
        int totalPersonImmune = 0 ;
        Random rn = new Random();
        
        // Minimum of "NewSeeds/4" people should be vaccinated
        while (totalPersonImmune < (NewSeeds/8))
	    {
		int randomComunity = rn.nextInt(Communities);
		int randomPerson = rn.nextInt(PopulationPerCommunity);
            
		//If person is infected, vaccinate him
		if( InfectedPeople[randomComunity][randomPerson] )
		    {
			InfectedPeople[randomComunity][randomPerson] = false;
			totalPersonImmune++;
		    }
	    }
    }    
}

