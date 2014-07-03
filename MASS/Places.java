package MASS;



import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;

import main.java.com.dlb.dlbhelper.DynamicLoadBalancer;
import main.java.com.dlb.utils.DLBParams;

/**
 * Instantiates and manipulates a multi-dimensional array of MASS.Place objects
 * over a cluster of multi-core computing nodes.
 *
 * @author  Tim Chuang, John Spiger, and Munehiro Fukuda (CSS, UW Bothell)
 * @since   6/18/10
 * @version 0.1
 */
public class Places {
  
  private int 		handle;		//the handle for this MASS.Places object
  private Place[] 	holder;		//the array of MASS.Place objects
  private int[] 	size;
  private Class<?> 	klas;
  private Constructor<?> ctor;
  private int 		length;
  private int 		totalLength;
  static int[] 		placeInitIndex;
  static int[] 		placeInitSize;

  // New Exchange Boundary Variables 
  private int           boundaryWidth;
  private int           totalBoundaryLength;
  private boolean       wrapEdges;
  private Place[]       lBoundary;
  private Place[]       rBoundary;

  private static String arrayType;
  private static String placeType;
  private static int 	offSet;
  public static int 	chunkSize;
  public static int 	remainder;
  
  public static int dlbCount = 0;
  public static int methodCounter = 0;
  
  public static Long[][] threadTime = null;
  public static ThreadMXBean bean   = ManagementFactory.getThreadMXBean();
  
  static URLClassLoader loader;



  /**
   * Is the constructor that instantiates a size-dimensional array of
   * "className" objects.
   * @throws Exception if an element of the size is less than 1 or if the handle is already in use
   * @param handle    a user-given non-negative number to uniquely identify
   *                  this distributed array over the system.
   * @param className the name of the class from which each array element
   *                  is instantiated.
   * @param argument  an argument passed to each array element.
   * @param size      the size of each dimension
   */
  public Places( int handle, String className, Object argument, int... size ) throws Exception
  {
                this( handle, className, argument, 0, false, size );
  }

  /**
   * Constructor - instantiates a size-dimensional array of "className" objects.
   *
   * @throws Exception          if an element of the size is less than 1 or if the handle is already in use
   * @param handle              a user-given non-negative number to uniquely identify
   *                                    this distributed array over the system.
   * @param className           the name of the class from which each array element is instantiated.
   * @param argument            an argument passed to each array element.
   * @param boundaryWidth       the width of each of the shadow boundaries (1st dimension)
   * @param wrapEdge            if true, wrap the array by space shadowing each of the edges 
   * @param size                the size of each dimension
   */
  public Places( int handle, String className, Object argument, int boundaryWidth, boolean wrapEdge, int... size ) throws Exception
  {
	  Places.bean.setThreadCpuTimeEnabled(true);
	  
	  /**
	   * Read the dynamic load balancing property file and
	   * get the config values.
	   */
	  if (MASS.myPid == 0) {
		  readDLBPropertyFile();
	  }
	  
	  
        this.handle = handle;
        if (MASS.addPlaces(this))
        { 	//set the handle in the MASS.MASS object //TODO addPlaces should be later

          //Setup Variables
          chunkSize = 1;
          remainder = 1;
          totalLength = 1;
          this.boundaryWidth = boundaryWidth;
          this.totalBoundaryLength = boundaryWidth;           // set 1st dimension    
          this.wrapEdges = wrapEdge;
          lBoundary = null;
          rBoundary = null;

		  // Calculate totalLength of array (flattened), the chunksize, 
          // the remainder,  and the totalBoundaryLength (flattened)
          for (int elem = 0; elem < size.length; elem++)
          {
            	if (size[elem] < 1) { throw new Exception("size value less than 1");}
            	totalLength *= size[elem]; //multiply by each element to eventually get the total length

                // Calculate chunksize and remainder. Dividing the first dimension for each machine
                if( elem == 0 ) {
                        chunkSize   *= size[elem] / MASS.systemSize;
                        remainder   *= size[elem] % MASS.systemSize;
                }
                else {
                        chunkSize   *= size[elem];
                        remainder   *= size[elem];
                }

                // Multiply by each element after the 1st dimension (boundaryWidth) 
                // to get total boundary size
                if( boundaryWidth > 0 && elem > 0 ) {
                        totalBoundaryLength *= size[elem];
                }
          }

		  // Set the array size and offset
          this.size = size; //assign the size
          this.offSet = MASS.myPid * chunkSize;
         
		  // Determine command class and constructor 
          File curDir 	= new File(MASS.CUR_DIR);
          loader 		= URLClassLoader.newInstance(new URL[] { curDir.toURI().toURL() });
          klas 			= Class.forName(className, true, loader); 	//get class
          ctor 			= klas.getConstructor(Object.class); 		//get constructor

          // now register offset for index calculation later
          arrayType = Constants.OBJECT_ARRAY;
          placeType = Constants.PLACE;
          
          
          MASS.log("\n------------------Beginning Initialization sequence for place handle " + this.handle  
				   + "------------------"  );  
          // Run on Master Rank Only, and only when Multiple Machines are used
          // ========================================================================
          if(MASS.myPid == 0  && MASS.systemSize > 1) // master rank
          {
              	MASS.log("Initialization parameters");
                MASS.log("-----------------------------------");
                MASS.log("Total Length       : " + totalLength );
                MASS.log("Chunksize          : " + chunkSize);
                MASS.log("Remainder          : " + remainder);
	
   			  	// Print Shadow Boundary information
        	  	if( totalBoundaryLength > 0 ) { 
            		MASS.log("Boundary Shadowing : ON");
                    MASS.log("  * Boundary Width     : " + boundaryWidth);
                    MASS.log("  * Total Bndry Length : " + totalBoundaryLength);
                	if( wrapEdges ) MASS.log("  * Boundary Wrapping  : ON \n" );
            		else 			MASS.log("  * Boundary Wrapping  : OFF \n" );
        		}   
        		else {
            		MASS.log("Boundary Shadowing : OFF !\n");
        		}   
 
              // DEBUG
              //System.err.println(" My Rank: " + MASS.myPid + " offset:  " + this.offSet );
              
              // commence initialization communication with other nodes, this task is only handled by master rank

			  // Get local host name
              String masterRankHostName = null;
              try 
              {
					InetAddress addr 	= InetAddress.getLocalHost();
                    byte[] ipAddr 		= addr.getAddress();			// get IP address
                    masterRankHostName 	= addr.getHostName();			// get hostname
              } 
              catch (UnknownHostException e) { MASS.log("Unable to obtain the master rank host name"); }

             
			  // Setup Global Linear Index 
              int globalLinearIndex = 0;             

              String hostName = null;
              MASS.nodePidMap.put(masterRankHostName, 0);

              // please note that # of mNode is systemSize - 1, since the master is not a part of mNodes.
              for(int mNodeId = 0; mNodeId < MASS.systemSize - 1; mNodeId++)
              {            
                  hostName = MASS.mNodes[mNodeId].getHostName();
                  MASS.nodePidMap.put(hostName, mNodeId + 1);
              }
             
              // Create initialization Message and send to all other nodes. Recieve Ack.
              Message m = new Message();
              m.createInitializationMessage(size, arrayType, placeType, handle, className, argument, MASS.nodePidMap, 
				boundaryWidth, wrapEdges, dlbCount, DLBParams.HISTORY_BASED, DLBParams.WINDOW_BASED, DLBParams.SLOPE_BASED);

              for(MNode node : MASS.mNodes) 	node.sendMessage(m);
              System.err.println("Information sent! Awaiting Acknowledgement... ");
              
	      /* Move to the end in order to let the master work on its places generation: Fukuda 3-26-14
              for(MNode node: MASS.mNodes) 		node.receiveMessage();
              System.err.println("Received all Acknowledgement... ");
	      */

          }	// END Master Rank Only


          // last rank handles the remainder
          length =  (MASS.myPid == MASS.systemSize - 1) ? (chunkSize + remainder) : chunkSize;
          holder = new Place[length];
          placeInitIndex = new int[1];

          for (int i = 0; i < length; i++)
          {
            synchronized (placeInitIndex)
            {
              placeInitIndex = getGlobalArrayIndex( i, size );
              placeInitSize = size.clone();
              Place plc = (Place)ctor.newInstance(argument); //make the MASS.Place subclass element
              //plc = MASS.Place.class.cast(plc); //cast it as a MASS.Place
              holder[i] = plc; //Array.set(holder, i, plc);
            }
          }
        
 
      	  // Setup and Init the Left Shadow Boundary
      	  int tmpIdx, k;
      	  if( (MASS.myPid == 0 && wrapEdges) || MASS.myPid > 0 ) {
          	lBoundary = new Place[ totalBoundaryLength ];

        	for( int i = 0; i < totalBoundaryLength; i++ ) {
               	synchronized( placeInitIndex )
               	{
               		// Note: getGlobalArrayIndex() adds offset of current machine to k when calculating

                    if( MASS.myPid == 0 )   // wrapped edge 
                    	k = (MASS.systemSize * chunkSize) + remainder  - totalBoundaryLength;
                    else
                       	k = i - totalBoundaryLength;

                    placeInitIndex = getGlobalArrayIndex( k, size );
                    placeInitSize = size.clone();
                    Place plc = (Place)ctor.newInstance( argument );
                	lBoundary[i] = plc;
               	}

            	if( i == 0 )
                	MASS.log("Left Boundary - First Location: " + (k + offSet) );
            	if( i == (totalBoundaryLength - 1) )
                	MASS.log("Left Boundary - Last Location: " + (k + offSet) );
			}
		  }

		  // Setup and Init the Right Shadow Boundary
      	  if( (MASS.myPid == MASS.systemSize-1 && wrapEdges) || MASS.myPid < MASS.systemSize-1 ) {
        	rBoundary = new Place[ totalBoundaryLength ];

        	for( int i = 0; i < totalBoundaryLength; i++ ) {
           		synchronized( placeInitIndex )
               	{
                	// Note: getGlobalArrayIndex() adds offset of current machine to k when calculating

                	if( MASS.myPid == MASS.systemSize-1 ) // wrapped edge
                    	k = i - offSet;
                	else
                    	k = i + chunkSize;

                    placeInitIndex = getGlobalArrayIndex( k, size );
                    placeInitSize = size.clone();
                    Place plc = (Place)ctor.newInstance( argument );
                	rBoundary[i] = plc;
				}

            	if( i == 0 )
                	MASS.log("Right Boundary - First Location: " + (k + offSet) );
            	if( i == (totalBoundaryLength - 1) )
                	MASS.log("Right Boundary - Last Location: " + (k + offSet) );
        	}

		  }


		// Start ExchangeHelper 
	  // MASS.startExchangeHelper(); Fukuda 3-25-14
	  MASS.log( "Places: will create MASS.exchangeHelper[0]" );
	  MASS.exchangeHelper[0] = new ExchangeHelper( );
	  MASS.exchangeHelper[0].establishConnection( );

          if(MASS.myPid == 0  && MASS.systemSize > 1) { // master rank
              for(MNode node: MASS.mNodes) 		node.receiveMessage();
              System.err.println("Received all Acknowledgement... ");
	  }
          
        MASS.log("--------------Complete Initialization for " + MASS.myPid + " with place handle " 
					+ this.handle  + "-----------------");
        MASS.log("---Initialization variables for " + MASS.myPid + " Places(holder) length: " + holder.length 
				+ " chunksize: " + chunkSize + " total length: " + totalLength + " remainder: " + remainder );                   
        }
        else
        {
            throw new Exception ("handle already in use");
        }
  }

  
  /**
   * Returns Hostname from a global index location.
   * @param gIdx    the global index of the location to look up
   * @return        a string that represents the hostname of the global index loc provided
   */
   public String getHostname( int gIdx ) {

		// Verify the provided info is valid
    	if( gIdx < 0  ||  gIdx >= totalLength )
        	return null;

    	// Loop through each entry of the nodePidMap (node PID map)
    	for( Map.Entry< String, Integer > entry : MASS.nodePidMap.entrySet( ) )
    	{

        	// Calculate the entry :  PID, first place location, and last place location
        	int pid     = entry.getValue( );            // pid
        	int fLoc    = pid * chunkSize;          	// fist place location
        	int lLoc    = ((pid+1) * chunkSize ) - 1;   // last place location

        	// Add remainder to last location
        	if( pid + 1 == MASS.systemSize )
            	lLoc = totalLength - 1;

        	if( fLoc <= gIdx  &&  gIdx  <=  lLoc )
            	return entry.getKey();
    	}
    	return null;
  }

  /**
   *
  */
  private static void readDLBPropertyFile() {
    String propertyFilePath = MASS.CUR_DIR + "/" + DLBParams.DLB_PROPERTY_FILE_NAME;
  	File propertyFile = new File(propertyFilePath);
  	if (!propertyFile.exists()) {
  		System.out.println("WARNING : Property file does not exists, load balancing is disabled !");
  	} else {
			Properties properties = new Properties();
			try {
				FileInputStream stream = new FileInputStream(propertyFile);
				properties.load(stream);
				DLBParams.HISTORY_BASED = Boolean.parseBoolean(properties.getProperty("HISTORY_BASED"));
				DLBParams.WINDOW_BASED = Boolean.parseBoolean(properties.getProperty("WINDOW_BASED"));
				DLBParams.SLOPE_BASED = Boolean.parseBoolean(properties.getProperty("SLOPE_BASED"));
				dlbCount = Integer.parseInt(properties.getProperty("dlbCount"));
				
				/**
				 * Validate the load balancing flags
				 */
				if (!validatePropertyFile(DLBParams.HISTORY_BASED, DLBParams.WINDOW_BASED, DLBParams.SLOPE_BASED, dlbCount)) {
					System.out.println("ERROR : Please check DLB.properties file, any one algorithm should be set to true and" +
							"dlbCount value should be greater than zero");
					System.exit(1);
				}
				
				System.out.println("DLB.properties: values are :");
				System.out.println("History Based Algo: "+DLBParams.HISTORY_BASED);
				System.out.println("Window Based Algo: "+DLBParams.WINDOW_BASED);
				System.out.println("Slope Based Algo: "+DLBParams.SLOPE_BASED);
				System.out.println("DLB step count : "+dlbCount);
				
				
			} catch (IOException ex) {
				System.out.println("WARNING : there is no DLB.properties file, which disables  DLB");
			}
  	}
  }

    /**
     *
     */
	private static boolean validatePropertyFile(boolean historyBased,
			boolean windowBased, boolean slopeBased, int dlbCnt) {
		
		boolean res = true;
		
		if (historyBased && windowBased) {
			res = false;
		} else if (windowBased && slopeBased) {
			res = false;
		} else if (slopeBased && historyBased) {
			res = false;
		}
		
		if (dlbCnt <= 0) {
			res = false;
		}
		
		return res;
	}
  
  
  /**
   * Returns the handle associated with this distributed array.
   *
   * @return a non-negative integer as this array's handle. -1 is
   * returned upon an error.
   */
  public int getHandle( ) {
	return handle;
  }
  /**
   * Returns the size of each dimension.
   *
   * @return the size of each dimension, stored in the corresponding array
   *         element. A null value is returned upon an error.
   */
  public int[] size() {
	return size.clone();
	//return null; // default, i.e., an error
  }
  /**
   * Calls the method specified with functionId of all array
   * elements. Done in parallel among multi-processes/threads.
   *
   * @param functionId the identifier of a method to call.
   */
  public void callAll( int functionId )
  {
	callAll( functionId, (Object)null );
  }
  /**
   * Calls the method specified with functionId of all array
   * elements as passing an Object argument to the method. Done in
   * parallel among multi-processes/threads.
   *
   * @param functionId the identifier of a method to call.
   * @param argument an argument passed to each method.
   */
  public void callAll( int functionId, Object argument ) 
  {
	
	MASS.ca_setup_1arg(this, functionId, argument);
	MASS.ca_callAll();
	if (DLBParams.WINDOW_BASED || DLBParams.HISTORY_BASED || DLBParams.SLOPE_BASED) {
		methodCounter++;
		doLoadBalancing();
	}

  }
  /**
   * Calls the method specified with functionId of all array
   * elements as passing arguments[i] to element[i]'s method, and
   * recives a return value from it into Object[i]. Done in parallel
   * among mulit-processes/threads. In case of a multi-dimensional
   * array, "i" is considered as the index when the array is
   * flattened to a single dimension.
   *
   * @param functionId the identifier of a method to call.
   * @param arguments arguments[i] passed to element[i]'s method.
   * @return Object[i] used to store a return value from element[i].
   */
  public Object[] callAll( int functionId, Object[] arguments )
  {
	  if(arguments==null){
			MASS.ca_setup_1arg(this, functionId, null);
			MASS.ca_callAll();
			if (DLBParams.WINDOW_BASED || DLBParams.HISTORY_BASED || DLBParams.SLOPE_BASED) {
				methodCounter++;
				doLoadBalancing();
			}			
			return null;
		  }else {	  
		MASS.ca_setup_args(this, functionId, arguments);
        Object[] objArr = MASS.ca_callAll();
        if (DLBParams.WINDOW_BASED || DLBParams.HISTORY_BASED || DLBParams.SLOPE_BASED) {
        	methodCounter++;
        	doLoadBalancing();
        }
        return objArr;
		  }
  }
  /**
   * Calls the method specified with functionId of one or more
   * selected array elements as passing. If index[i] is a
   * non-negative number, it indexes a particular element, a row, or
   * a column. If index[i] is a negative number, say -x, it indexes
   * every x element. Done in parallel among
   * multi-processes/threads.
   *
   * @param functionId the idnetifier of a method to call.
   * @param index indexing particular array elements.
   */
  public void callSome( int functionId, int... index ) {
	callSome(functionId, (Object) null, index);
  }
  /**
   * Calls the method specified with functionId of one or more
   * selected array elements as passing an Object argument to the
   * method. The format of index[] is the same as the above
   * callSome( ). Done in parallel among multi-processes/threads.
   *
   * @param functionId the idnetifier of a method to call.
   * @param argument an argument passed to each method.
   * @param index indexing particular array elements.
   */
  public void callSome( int functionId, Object argument, int... index ) 
  {
      try
      {
          //if(argument == null) MASS.log("argument is null");
          int globalLinearIndex = getGlobalLinearIndexFromGlobalArrayIndex(index, size);
          int localLinearIndex = getLocalLinearIndexFromGlobalLinearIndex(globalLinearIndex);
          // if local
          if(localLinearIndex >= 0 && localLinearIndex < length())
            get( getLocalIndexFromGlobalArrayIndex(index, size ) ).callMethod( functionId, argument );
          else // remote
          {
              Message m = new Message();
              m.setHandle(this.handle);
              m.createActionMessage(Constants.CALL_SOME_VOID_OBJECT, functionId, argument, index);
              MASS.mNodes[ MASS.nodePidMap.get( getHostname(globalLinearIndex) ) - 1 ].sendMessage(m);
	      MASS.mNodes[ MASS.nodePidMap.get( getHostname(globalLinearIndex) ) - 1 ].receiveMessage(); // ack
          }
      }
      catch (Exception e) 
      { 
          MASS.log(" Error in call some: " + e.getMessage()); 
          MASS.logException(e);
          MASS.finish();
      }
  }
  /**
   * Calls the method specified with functionId of one or more
   * selected array elements as passing argument[i] to element[i]'s
   * method, and receives a return value from it into Object[i]. The
   * format of index[ ] is the same as the other callSome( ). Done
   * in parallel among multi-processes. In case of multi-dimensional
   * array, "i" is considered as the index when the array is
   * flattened to a single dimension.
   *
   * @param functionId the identifier of a method to call.
   * @param arguments arguments[i] passed to element[i]'s method.
   * @param index indexing particular array elements.
   * @return Object[i] used to store a return value from element[i].
   */
  public Object[] callSome( int functionId, Object[] arguments, int... index ) {
	return null;
  }
  
  /**
   * Calls from each of all cells to the method specified with
   * functionId of all destination cells, each indexed with a
   * different Vector element. Each vector element, say
   * destination[] is an array of integers where destination[i]
   * includes a relative index (or a distance) on the coordinate i
   * from the current caller to the callee cell. The caller cell's
   * outMessage, (i.e., an Object) is a set of arguments passed to
   * the callee's method. The caller's inMessage[], (i.e., an array
   * of Objects) stores values returned from all callees. More
   * specifically, inMessages[i] maintains a set of return values
   * from the ith callee.
   *
   * @param handle the handles associated wtih a destination array
   * @param functionId the identifier of a method to call
   * @param destinations each destination element's relative index
   *                     (or relative distance) from the caller 
   *                     element.
   */
  public void exchangeAll( int handle, int functionId,  Vector<int[]> destinations ) 
  {
        
        MASS.ea_setup( handle, functionId, destinations);
        MASS.ea_exchangeAll();
        if (DLBParams.WINDOW_BASED || DLBParams.HISTORY_BASED || DLBParams.SLOPE_BASED) {
        	methodCounter++;
        	doLoadBalancing();
        }
  }
 

  /** 
  * New ExchangeBoundary Funcation
  * @param handle       the handles associated wtih a destination array
  * @param functionId       the identifier of a method to call
  */
  public void exchangeBoundary( int handle, int functionId,  Vector<int[]> destinations )
  {
        MASS.eb_setup( this, functionId, destinations, lBoundary, rBoundary );
        MASS.eb_exchangeBoundary();
        MASS.eb_update();
        if (DLBParams.WINDOW_BASED || DLBParams.HISTORY_BASED || DLBParams.SLOPE_BASED) {
        	methodCounter++;
        	doLoadBalancing();
        }
  }

  /**
  * Get Boundary Width
  * @return int the boundary width (first dimension)
  */
  public int getBoundaryWidth()
  {
    	return boundaryWidth;
  }

 
  /**
   * do the load balancing depending on the 
   * algorithm set. This method kicks in when the
   * load balancing counter is set.
   */
  private void doLoadBalancing() {
	if (dlbCount == methodCounter) {
		populateTimeSpentByAllThreads();
		
		//if (DLBParams.DEBUG) {
			MASS.log("*********************** doLoadBalancing - Start ************************************");
			for (int i = 0; i < Places.threadTime.length; i++) {
				MASS.log("threadTime["+Places.threadTime[i][0]+"]["+Places.threadTime[i][1]+"]");
			}
		//}
		
		DynamicLoadBalancer.setThreadTimeNew(Places.threadTime);
		
		//if (DLBParams.DEBUG) {
			MASS.log("*********************** doLoadBalancing -   End ************************************");
		//}
		
		methodCounter = 0;
	}
  }
  
  /**
   * populates the time spent by each thread
   * in the thread time array.
   */
  private void populateTimeSpentByAllThreads() {
	  if (Places.threadTime == null) {
		  int size = MASS.threads.size()+1;
		  Places.threadTime = new Long[size][size];
	  }
	  
	  for (int i = 0; i < MASS.threads.size(); i++) {
		  Places.threadTime[i][0] = MASS.threads.get(i).getId();
		  Places.threadTime[i][1] = getThreadCpuTime(MASS.threads.get(i).getId());
	  }
	  
	  /**
	   * Add time for main thread
	   */
	  Places.threadTime[MASS.threads.size()][0] = Thread.currentThread().getId();
	  Places.threadTime[MASS.threads.size()][1] = getThreadCpuTime(Thread.currentThread().getId());

  }
  
  /**
   * Get thread cpu time.
   * @param id
   * @return
   */
  private Long getThreadCpuTime(long id) {
	  
	return (Places.bean.getThreadCpuTime(id)/1000000);
  }
  
  
/**
   * Calls from each of the cells indexed with index[ ] (whose
   * format is the same as the above callSome( )) to the method
   * specified with functionId of all destination cells, each
   * indexed with a different Vector element. Each vector element,
   * say destination[ ] is an array of integers where destination[i]
   * includes a relative index (or a distance) on the coordinate i
   * from the current caller to the callee cell. The caller cell's
   * outMessages[], (i.e., an array of Objects) is a set of
   * arguments passed to the callee's method. The caller's
   * inMessages[], (i.e., an array of Objects) stores values
   * returned from all callees. More specifically, inMessages[i]
   * maintains a set of return values from the ith callee.
   *
   * @param handle the handles associated wtih a destination array
   * @param functionId the identifier of a method to call
   * @param destinations each destination element's relative index
   *                     (or relative distance) from the caller 
   *                     element.
   * @param index indexing particular array elements.
   */
  public void exchangeSome( int handle, int functionId,   Vector<int[]> destinations, int... index ) {
  }
  // ------------------ additional methods --------------------------------
  
  /**
   * Gets the length of the MASS.Places object
   * @return int the length
   */
  int length(){
	return length;
  }

  public int totalLength()
  {
      return totalLength;
  }
  /**
   * Calculates the coordinates of a new MASS.Place based on the coordinates of a starting
   * MASS.Place and a set of offsets.
   *
   * @param start the starting coordinates
   * @param offsets The set of values to be used as offsets from the starting coordinates
   * @param size the size of the MASS.Places object in which the coordinates are located
   * @param destination the array to be filled with the coordinates of the MASS.Place located at the
   * offset location, filled with values of -1 if the offsets lead outside the size of the MASS.Places object
   */
  public static int[] getGlobalNeighborArrayIndex( int[] start, int[] offsets, int[] size) 
  {
      int[] destination = new int[size.length];
      try 
      {
        for (int i = 0; i < offsets.length; i++)
        {
              destination[i] = start[i] + offsets[i];
              if ( destination[i] < 0 || destination[i] >= size[i])
              {
                throw new Exception();
              }
        }
      } 
      catch (Exception e) 
      {
        for (int i = 0; i < destination.length ; i++)
        {
            destination[i] = -1;
        }
      }
      return destination;
  }
  
  /**
   * Provides the seqential number of an element in a MASS.Places object
   * of a given size.
   *
   * @param index the position of the element in the size
   * @param size array containing an integer (always > 0) for each dimension
   * @return an integer representing the sequential index of an element
   * @return -1 if a parameter is invalid
   */
  public static int indexArr2Num (int[] index, int[] size) throws ArrayIndexOutOfBoundsException { 
	int retVal = 0;
	if (index != null && size != null && index.length == size.length){
	  for (int i = 0; i < index.length ; i++){
		if (index[i] >= 0 && size [i] > 0 && index[i] < size[i] ){
		  retVal = retVal * size[i]; //retVal zero to start
		  retVal += index[i];
		} else {
		  throw new ArrayIndexOutOfBoundsException("illegal parameter value");
		}
	  }
	} else {
	  throw new ArrayIndexOutOfBoundsException("null or mismatched arrays");
	}
	return retVal;
  }
  
  public static int getGlobalLinearIndexFromGlobalArrayIndex (int[] index, int[] size) throws ArrayIndexOutOfBoundsException { 
	int retVal = 0;
	if (index != null && size != null && index.length == size.length)
        {
	  for (int i = 0; i < index.length ; i++){
		if (index[i] >= 0 && size [i] > 0 && index[i] < size[i] ){
		  retVal = retVal * size[i]; //retVal zero to start
		  retVal += index[i];
		} else {
		  throw new ArrayIndexOutOfBoundsException("illegal parameter value");
		}
	  }
	} else {
	  throw new ArrayIndexOutOfBoundsException("null or mismatched arrays");
	}
	return retVal;
  }
  
  /**
   * Provides the seqential number of an element in a MASS.Places object
   * of a given size.
   *
   * @param index the position of the element in the size
   * @param size array containing an integer (always > 0) for each dimension
   * @return an integer representing the sequential index of an element
   * @return -1 if a parameter is invalid
   */
  public static int getLocalIndexFromGlobalArrayIndex (int[] index, int[] size) throws ArrayIndexOutOfBoundsException 
  { 
	int retVal = 0;
	if (index != null && size != null && index.length == size.length)
        {
	  for (int i = 0; i < index.length ; i++)
          {
		if (index[i] >= 0 && size [i] > 0 && index[i] < size[i] )
                {
		  retVal = retVal * size[i]; //retVal zero to start
		  retVal += index[i];
		} 
                else 
                {
		  throw new ArrayIndexOutOfBoundsException("illegal parameter value: size[ " + i + " ] is " + size[i] + " index[ " + i + " ] is " + index[i]);
		}
	  }
	} 
        else 
        {
	  throw new ArrayIndexOutOfBoundsException("null or mismatched arrays");
	}
	return retVal - offSet;
  }

  
  /**
   * Converts a numerical index to an array index.
   *
   * @param index the numerical index
   * @param size the size of the array, with each element being the length of a dimension
   */
  public static int[] indexNum2Arr( int index, int[] size ) throws ArrayIndexOutOfBoundsException
  {
	//make sure no zero or negative values in size
	for ( int x = 0; x < size.length; x++ ){
	  if (size[x] < 1) {
		throw new ArrayIndexOutOfBoundsException("illegal size value");
	  }
	}
	int[] retVal = new int[ size.length ];
	for ( int i = size.length - 1; i >= 0; i--){
	  retVal[i] = index % size[i];
	  index /= size[i];
	}
	return retVal;
  } //end of method  

  /**
   * Converts a numerical index to an array index while taking into account each rank's offset.
   *
   * @param index the numerical index
   * @param size the size of the array, with each element being the length of a dimension
   */
  public static int[] getGlobalArrayIndex ( int index, int[] size ) throws ArrayIndexOutOfBoundsException
  {
     index = index + offSet;
	//make sure no zero or negative values in size
	for ( int x = 0; x < size.length; x++ )
        {
	  if (size[x] < 1) {
		throw new ArrayIndexOutOfBoundsException("illegal size value");
	  }
	}
	int[] retVal = new int[ size.length ];
	for ( int i = size.length - 1; i >= 0; i--)
        {
	  retVal[i] = index % size[i];
	  index /= size[i];
	}
	return retVal;
  } //end of method

  public static int getLocalLinearIndexFromGlobalLinearIndex(int index)
  {
      return index - offSet;
  }
  /**
   * MASS.Place an object into the MASS.Places object by sequential index.
   * 
   * @param index the sequential index of the location in this MASS.Places object
   * @param place the MASS.Place to be put into this MASS.Places object
   */
  /*
  private void set ( int index, MASS.Place place ) throws ArrayIndexOutOfBoundsException {
	holder[ index ] = place; //Array.set( holder, index, place );
  } 
  */
  /**
   *
   */
  public Place get( int index )
  { // throws ArrayIndexOutOfBoundsException {
	return holder[ index ]; 
	//MASS.Place retVal;
	//try {
	//  retVal = 
	//(MASS.Place)Array.get(holder, index);
	//} catch (Exception e) {
	//  throw new Exception( e.toString() );
	//}
	//return retVal;
  }
  
  //gets an iterator that iterates over all the MASS.Place elements in the MASS.Places object
  public Iterator iterator(){
	int[] range = new int[2];
	range[0] = 0;
	range[1] = length() -1;
	return iterator( range );
  }
  
  /**
   * Returns an Iterator for the MASS.Places object.
   * @param range range[0] is integer index of first MASS.Place, range[1] of last
   * @return an Iterator, or null if there is a problem with the parameters
   */
  public Iterator iterator(int[] range){
	boolean validArgs = false;
	if (range != null 
		&& range.length == 2
		&& range[0] <= range[1]){
	  validArgs = true;
	}
	if (validArgs){
	  return new Iterator( range[0], range[1]);
	} else {
	  return null;
	}
  }
  
  
  /**
   * An Iterator class for MASS.Places.
   */
  /* package */ class Iterator implements java.util.Iterator<Place> {
	
	//private int first;
	private int last;
	private int current;
	
	private Iterator ( int first, int last ){
	  //this.first = first;
	  this.current = first;
	  this.last = last;
	}
	
	/**
	 * used to see if the iterator has a next element
	 */
	public boolean hasNext(){
	  return (current >= 0 && current <= last);
	}
	
	/**
	 * gets the next item in the iterator
	 */
	public Place next() throws NoSuchElementException {
	  Place retVal = null;
	  if (hasNext()){
		  retVal = holder[current];
		  current++;
	  } else {
		throw new NoSuchElementException( "The iterator has passed its end." ); // no next
	  }
	  return retVal;
	}
	
	
	public void remove() throws UnsupportedOperationException {
	  throw new UnsupportedOperationException();
	}
	
  } // end of nested class
  
} // end of MASS.Places class
