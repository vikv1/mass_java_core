package MASS;



import java.util.Vector;

/**
 * Instantiates and manipulates each element of a multi-dimensional distributed
 * MASS.Places array.
 * 
 * @author  Tim Chuang, John Spiger, and Munehiro Fukuda (CSS, UW Bothell)
 * @since   6/18/10
 * @version 0.1
 */
public class Place {
  /**
   * The size of the matrix that consists of application-specific
   * places. Intuitively, size[0], size[1], and size[2] correspond
   * to the size of x, y, and z or that of i, j, and k.
   */
  public final int[] size;
  
  /**
   * used by agents to register wakeUpAllCalls for this MASS.Place,
   * true values indicate registered wakeupall events
   */
  boolean[] eventsToFire;
  
  /**
   * An array that maintains each place's coordinates. Intuitively,
   * index[0], index[1], and index[2] correspond to coordinates of
   * x, y, and z, or those of i, j, and k.
   */
  public final int[] index; 
  
  /**
   * A vector of all the agents residing locally on this place.
   */
  protected Vector<Agent> agents = new Vector<Agent>( );
  
  /*
   * Vectors used to organize agents during manageAll.
   */
  Vector<Agent> firingEvent1 = new Vector<Agent>();
    Vector<Agent> firingEvent2 = new Vector<Agent>();
    Vector<Agent> firingEvent3 = new Vector<Agent>();
    Vector<Agent> firingEvent4 = new Vector<Agent>();
    Vector<Agent> firingEvent5 = new Vector<Agent>();
    Vector<Agent> firingEvent6 = new Vector<Agent>();
    Vector<Agent> firingEvent7 = new Vector<Agent>();
    Vector<Agent> firingEvent8 = new Vector<Agent>();
    Vector<Agent> firingEvent9 = new Vector<Agent>();
    Vector<Agent> firingEvent10 = new Vector<Agent>();
  

  /**
   * A package scope vector to hold immigrants and new spawns created
   * during MASS.Agents ManageAll
   */
  Vector<Agent> immigrants = new Vector<Agent>();

  /**
   * A set of arguments to be passed to a set of remote-call
   * functions that will be invoked by exchangeAll( ) or
   * exchangeSome( ) in the nearest future
   */
  protected Object outMessages = null;
  
  /**
   * A set of return values: receives a return value in
   * inMessages[i] from a function call made to the i-th remote cell
   * through exchangeAll( ) and exchangeSome( ).
   */
  protected Object[] inMessages = null; 
  
  /* public MASS.Place(){
   size = null;
   index = null;
   } */
  
  /**
   * Is the constructor. No primitive data type can be passed to the
   * methods, since they are not derivable from the "Object" class.
   *
   * @param args an argument passed to each MASS.Place object.
   */ 
  protected Place() 
  {
	index = Places.placeInitIndex.clone();
	size = Places.placeInitSize.clone();
	eventsToFire = new boolean[10];
	for (int i = 0; i < eventsToFire.length; i++)
        {            
            eventsToFire[i] = false;
	}
	
  }
  
  /*
   * Gets a copy of the eventsToFire array, resets the eventsToFire array
   * @return a copy of the eventsToFire array
   */
  boolean[] getAndResetEventsToFire()
  {
	boolean[] retVal = null;
	synchronized (eventsToFire) 
        {
	  retVal = eventsToFire.clone();
	  for (int i = 0; i < eventsToFire.length; i++)
          {
		eventsToFire[i] = false;
	  }
	}
	return retVal;
  }
  
  /**
   * Is called from MASS.Places.callAll( ), callSome( ),
   * exchangeAll( ), and exchangeSome( ); and invokes mass_0,
   * mass_1, mass_2, mass_3, or mass_4 whose postfix number
   * corresponds to functionId. An application may override
   * callMethod( ) so as to direct MASS.Places to invoke an
   * application-specific method
   *
   * @param functionId the identifier of a method to call.
   * @param argument an argument passed to a method to call.
   * @return a return value from functionId.
   */
  public Object callMethod( int functionId, Object argument ) {
	System.out.println("Error: MASS.Place callMethod for Object called.");
	return null; // default, i.e., an error
  }
}
