import MASS.*;
import java.util.Date;
import java.util.Random;
import java.util.Vector;

/**
 *
 * @author amala
 */
public class FluteSimulation {

    /**
     * @param args the command line arguments
     */

    public static void main( String[] args ) throws Exception {
        // TODO code application logic here
        // verify arguments
        System.out.println("Total arguments = " + args.length);
        if ( args.length != 5 ) {
            System.out.println( "usage: java -cp MASS.jar:jsch-0.1.44.jar:. FluteSimulation userid port nProcs nThrs size" );
            System.exit( -1 );
        }
        String[] massArgs = new String[4];
        massArgs[0] = args[0];          // user name
        massArgs[1] = "arpi15ankit1";   // password
        massArgs[2] = "machinefile.txt"; // machine file
        massArgs[3] = args[1];           // port

        int nProcesses = Integer.parseInt( args[2] );
        int nThreads = Integer.parseInt( args[3] );
        int PopulationPerCommunity = Integer.parseInt ( args[4] );

	// Inputs for the Simulation
	int Communities = 7;
	int InitialSeeds = 10;
	int SimulationDays = 180;
	Random rn = new Random();
	
        // start MASS
        MASS.init( massArgs, nProcesses, nThreads );

        //create the adjacency matrix
        Places infectedPeople = new Places( 1, "InfectedPeople", ( Object )null, Communities, PopulationPerCommunity );
	
	//initialize the infected people to false
        infectedPeople.callAll( InfectedPeople.init_, ( Object[] )null );

	// Infect the initial seeds
        for( int i = 0 ; i < InitialSeeds ; i++ )
	{
	    int randomCommunity = rn.nextInt( Communities );
	    int randomPerson = rn.nextInt( PopulationPerCommunity );
	    int[] index = { randomCommunity , randomPerson };

	    //update the Place to true to indicate its infected	    
	    infectedPeople.callSome( InfectedPeople.initialInfection_, ( Object )null, index[0], index[1] );	    
	}

	// Collect back the place to find the infected seeds 	
	Object[] temp = new Object[ Communities * PopulationPerCommunity ];
	Object[] TotalInfected = infectedPeople.callAll( InfectedPeople.collectInfected_, temp );
				    
	/*for ( int i = 0 ; i < TotalInfected.length ; i++ )
	{
	    System.out.print ( "\t" + ( int )( Object) TotalInfected[i] );
	    }*/
	
	//Start the timer
	Date startTime = new Date( );
	
	//Run the simulation for given number of days
	for ( int day = 1 ; day < SimulationDays + 1 ; day++ )
	{
	    //Infect People at Day time
	    TotalInfected = infectedPeople.callAll( InfectedPeople.collectInfected_, temp );
	    infectedPeople.callAll( InfectedPeople.day_, ( Object ) TotalInfected );
	    
	    //Infect People at Night time
	    TotalInfected = infectedPeople.callAll( InfectedPeople.collectInfected_, temp );
	    infectedPeople.callAll( InfectedPeople.night_, ( Object ) TotalInfected );

	    //Exchange the migrants
	    /*int migrated = 0;
	    while ( migrated < MigrantWorkers )
	    {
		int randomCommunity = rn.nextInt( Communities );
                int randomPerson = rn.nextInt( PopulationPerCommunity );
                int[] index = { randomCommunity , randomPerson };
		infectedPeople.exchangeSome( InfectedPeople.response_, ( Object )check, index[0], index[1] );

	     }*/
	    

	    //Find total infected people
	    TotalInfected = infectedPeople.callAll( InfectedPeople.collectInfected_, temp );
	   
	    int totalInfections = 0 ;
	    for ( int i = 0 ; i < TotalInfected.length ; i++ )
	    {
		//System.out.print ( "\t" + ( int )( Object ) TotalInfected[i] );
		if ( ( int )( Object ) TotalInfected[i]  == 1)
	        {
		    totalInfections++;
		}
		    
	    }
	    //   System.out.println( "After Day" + day + " total infected are: " + totalInfections);

	    //Vaccinate people - Response
            int vaccinated = 0;
            while ( vaccinated < ( int )( totalInfections/8 ))
	    {
		    int randomCommunity = rn.nextInt( Communities );
		    int randomPerson = rn.nextInt( PopulationPerCommunity );
		    int[] index = { randomCommunity , randomPerson };

		    //update the Place to true to indicate its infected
		    //Object check = new Object();
		    //Object isVaccinated =
		    infectedPeople.callSome( InfectedPeople.response_, ( Object )null, index[0], index[1] );
		    vaccinated++ ;
	     }

	    
	}
	
	Date endTime = new Date( );
        System.out.println( "\tTime (ms): " + ( endTime.getTime( ) - startTime.getTime( ) ) );
	
	//Finish MASS
        MASS.finish();
	
    }
}