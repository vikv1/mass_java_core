package MASS;



//import MASS;
//import MNode;
//import Message;
//import Places;

import com.jcraft.jsch.Channel;
import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.Map.Entry;

import main.java.com.dlb.utils.DLBParams;
import main.java.com.dlb.utils.Slice;

/**
 * manages a MASS.MASS environment
 * 
 * @author Tim Chuang, John Spiger
 * @version 2010-06-25
 */
public class MASS 
{
    protected static int[] OPERATION_LOCK = new int[1];
    protected static int[] threadsRunning = new int[1]; 		// number of Mthreads
    public static Vector<Thread> threads; 			// threads of MASS.MASS env - no mods after MASS.MASS.init
    protected static Hashtable<Integer, Places> placesHandles; 	// for  placesHandles & MASS.Places
    protected static Hashtable<Integer, Agents> agentsHandles; 	// for handles for MASS.Agents
    protected volatile static boolean INITIALIZED = false; 		// set true when MASS.MASS.init() is called
    protected volatile static int barrierCounter = 0;

    /** the status of the MASS.MASS environment */
    static int[] STATUS = new int[1];
    /** the MASS.MASS environment has been prepared with MASS.MASS.init */
    static final int STATUS_READY 				= 0;
    /** MASS.MASS.finish() has been called and the MASS.MASS environment is shutting down */
    static final int STATUS_TERMINATE 			= 1;
    /**
     * static callAll variables have been set up by a MASS.Places object calling
     * ca_setup and all threads will do callAll calculations by calling
     * ca_callAll
     */
    static final int STATUS_CALLALL 			= 2;
    /**
     * static exchangeAll variables have been set up by a MASS.Places object calling
     * ea_setup and all threads will do exchangeAll by calling ea_exchangeAll
     */
    static final int STATUS_EXCHANGE_ALL 		= 3;
    static final int STATUS_AGENTS_CALL_ALL 	= 4;
    static final int STATUS_AGENTS_MANAGE_ALL 	= 5;
    static final int STATUS_AGENTS_SORT_ALL 	= 6;
	static final int STATUS_EXCHANGE_BOUNDARY   = 7;


    // /////////////////////////// MASS.Places operations vars //////////////////

    // callAll variables for MASS.Places.callAll
    protected static Places 	ca_places; 			// referred to during callAll
    protected static int 		ca_functionId; 		// referred to during callAll
    protected static Object 	ca_argument; 		// Object parameter to be passed in
    protected static Object[] 	ca_arguments; 		// array parameter
    protected static Object[] 	ca_retVals; 		// to return to the call from MASS.Places object
    protected static Object[] 	ca_finalRetVals;	// final return value of call all after collecting results 

    // exchangeAll variables for MASS.Places.exchangeAll
    protected static Places 	ea_places;
    protected static int 		ea_functionId;		// function to execute
    protected static int[][] 	ea_destinations;	// the neighbor list

    // exchangeBoundary variables
    protected static Places     eb_places; 
    protected static int    	eb_functionId;		// function to execute
    protected static int[][]    eb_destinations;	// the neighbor list
    protected static Place[]    eb_lBoundary;   	// left shadow destinations
    protected static Place[]    eb_rBoundary;   	// right shadow destinations


    // /////////////////////////// MASS.Agents operations vars //////////////////

    protected static Agents 	agentsOpAgents;
    protected static Places 	agentsOpPlaces;
    protected static Object 	agentsOpCallAllArg;
    protected static Object[] 	agentsOpCallAllArgs;
    protected static Object[] 	agentsOpCallAllResults;
    protected static boolean 	agentsOpSaveResults;
    protected static int 		agentsOpFunctionId;
    protected static int 		agentsOpHandle;
    // private static int 		agentsOpForeignHandle;
    protected static boolean 	agentsOpDescending;

    protected static Object 	threadsCatchUpBarrierLockObject = new Object();
    protected static int 		threadsCatchUpBarrierCounter = 0;
    
    
    ///////////////////// Variables for communications among nodes //////////////////////
    protected static MNode[] 	mNodes;         // a list of remote machines
    protected static int 		myPid = 0;		// store process id
    protected static int 		systemSize = 0;

    // a map that stores the global linear index and the hostname of the node that is in charge of the index
    protected static HashMap<String, Integer> nodePidMap = new HashMap<String, Integer>();
    // a map that stores the hostname of the exchange all call destination and the exchange helper associated with it
    protected static ExchangeHelper[] exchangeHelper = new ExchangeHelper[1];

	// the exhangeAllRequestMap variable is used by exchangeAll() and exchangeBoundary()
    protected static HashMap<String,  ArrayList<RemoteExchangeRequest>> exchangeAllRequestMap 
											= new HashMap<String,  ArrayList<RemoteExchangeRequest>>();
    protected static int MASS_PORT = 5000;
    protected static String CUR_DIR;
    protected static HashMap<String,  ArrayList<RemoteAgentRequest>> remoteAgentRequestMap 
											= new HashMap<String,  ArrayList<RemoteAgentRequest>>();     
    //protected static ArrayList<String> RemoteAgentMigrateHostNames = new ArrayList<String>();

    // =========================================================================
     
    /**
     * Involves nProc processes in the same computation and has each process
     * spawn childThrds threads.
     * 
     * @param args
     *            arguments passed to all processes involved in MASS.MASS.
     * @param nProc
     *            the number of processes (including the main process) to be
     *            involved in the same computation.
     * @param childThrds
     *            the number of threads per process to be spawned.
     */
    public static void init(String[] args, int nProc, int childThrds) 
    {
        
        // Variable Declarations
        ArrayList<String> hosts = new ArrayList<String>( );		// A collection of host names
        Utilities util = new Utilities( );				// Used for channel creation
        boolean failed = false;						// Test for node creation
        final int JschPort = 22;					// Port for jsch connection
        

        // Variable Assignment
        String username = args[0];
        String password = args[1];
        String machineFile = args[2];
        
        // try and set optional args
        ArrayList<String> customJarList = null; // storage for custom jar files users may wish to specify
        try 
        {
            MASS_PORT = Integer.parseInt(args[3]);
            MASS.log("set port to " + MASS_PORT);
            
            // load any custom jars
            if(args.length > 4)
            {
                customJarList = new ArrayList<String>();
                String jarList = args[4];
                String next;
                // args list needs to be a semicolon delimited string
                StringTokenizer tokenizer = new StringTokenizer(jarList, ";");
                while(tokenizer.hasMoreTokens())
                {
                    next = tokenizer.nextToken();
                    customJarList.add(next);
                }            
            }
        }
        catch (Exception e) { System.err.println("Error during MASS.init() optional argument parsing " + e.getStackTrace()); }
                  
        
        // A BufferReader object is created for fast reading of the Machine File
        // the BufferReader object is created from the Machine File that is passed in
        // as argument 2 ( args[2] ).
        BufferedReader fileReader = null;

        try {
            fileReader = new BufferedReader( new InputStreamReader
                    ( new BufferedInputStream( new FileInputStream( new File( machineFile ) ) ) ) );

            // Collect all hosts listed in the machine file
            // fileReader.ready() returns false if the file does not have more lines.
            while( fileReader.ready( ) )
                    hosts.add( fileReader.readLine( ) );	// Add host string to vector

            
        } catch( Exception e ) { System.err.println( e ); System.exit(-1); }
        finally
        { 
            try 
            {
                fileReader.close( );				// Close the fileReader 
            } 
            catch (Exception e) 
            { 
                System.err.println(e); 
                System.exit(-1);
            }
        }
        
        // Handle nProc
        if(( nProc < 0 ) || ( nProc > hosts.size( ) )) 
        {
            // Ignore nProc and nThr values and go with defaults
            nProc = hosts.size( ) + 1; // take into account the master
        }  
        
        systemSize = nProc; 

        // get absolute path of the working directory
        CUR_DIR = System.getProperty("user.dir");
        int numOfNodes = nProc - 1;
        mNodes = new MNode[ numOfNodes ];        
        // launch remote processes and set up communication
        int pid = 1; // Process Identification Number
        String currHostName = null;

        for(int i = 0; i < numOfNodes; i++, pid++)
        {
            currHostName = hosts.get(i);
            try
            {
                InetAddress addr = InetAddress.getByName(currHostName);
                // Get hostname
                currHostName = addr.getCanonicalHostName();
            } catch (Exception e) { log("Failure to resolve destination host name"); System.exit(-1); }
                        
            //log("Creating new mNode id:" + (i+ 1) + " name: " + currHostName);
            mNodes[i] = new MNode(currHostName, pid);
            // MASS.MProcess requires pid and system size(nProc)
            String cmd = "java -Xmx1g -cp " + CUR_DIR + "/DLB.jar:" + CUR_DIR + "/MASS.jar:" + CUR_DIR + "/jsch-0.1.44.jar"; 
            
            if(customJarList != null)
            {
                for(String customJar : customJarList)
                    cmd += ":" + CUR_DIR + "/" + customJar;
            }
            
            cmd += " MASS.MProcess " 
            + currHostName + " " + pid + " " + systemSize + " " + childThrds + " " + MASS_PORT + " " + CUR_DIR;
            
            // debug
            System.err.println("MProcess on " + currHostName + " run with cmmand: " + cmd);            
            try 
            {
                // Use MASS.Utilities class to create Channel Objects, args[0] is the username, args[1] is the password
                Channel nodeChannel = util.LaunchRemoteProcessEx( currHostName, JschPort, cmd, username, password );
                mNodes[i].setupMainConnection( nodeChannel );
                
                // debug
                System.err.println("Launched remote process id: " + (i+1) + " name: " + currHostName);
            } 
            catch( Exception e ) 
            {
                System.err.println( e.getMessage( ) );
                mNodes[i] = null;
                failed = true;
                break;
            }
            finally
            {
                // If failed to initalize nodes we cancel all created nodes
                if( failed )
                { 
                    finish( );
                    System.err.println("Mass Init failed!");
                    System.exit( -1 );
                }
            }
        }
        System.err.println("# of mNodes: " + mNodes.length + " SystemSize: " + systemSize);

        // initialize threads
        initializeThreads(childThrds);
        
  
    }
    
    /**
     * Initialize the (thread) boundaries and create Slice objects.
     * The slice objects contain the upper and lower bounds for each (thread) slice.
     * Add the slice objects to the map for respective thread ids.
     * 
     * @param places
     */
    public static void initBoundaries(Places places) {

    	/**
    	 * Do it for the main process.
    	 */
    	if (myPid == 0) {
    		
    		int dd = 0;
	    	MASS.log("Inside initBoundaries!  (thread boundaries)");
	    	
	        /**
	         * set the boundaries for load balancing here
	         */
	        for (int th = 0; th < MASS.threads.size(); th++) {
	        	Long threadId = MASS.threads.get(th).getId();
	        	int position = getThreadPosition(threadId);
	        	int[] range = getLocalRange(places, position);
	        	if (!DLBParams.boundaryMap.containsKey(threadId)) {
		        	DLBParams.boundaryMap.put(threadId, new Slice(range[0], range[1]));
		        }
	        	if (dd < range[1]) {
	        		dd = range[1];
	        	}
	        }//for ends here
	        
	        /**
	         * Also do it for main thread
	         */
	        Long tId = Thread.currentThread().getId();
	        int position = getThreadPosition(tId);
	    	int[] range = getLocalRange(places, position);
	    	if (!DLBParams.boundaryMap.containsKey(tId)) {
	        	DLBParams.boundaryMap.put(tId, new Slice(range[0], range[1]));
	        }
	    	
	    	if (dd < range[1]) {
	    		dd = range[1];
	    	}
	    	
	    	DLBParams.MAX_SIM_SIZE = dd;
	    	MASS.log("initBoundaries DLBParams max size : " + DLBParams.MAX_SIM_SIZE);
	    	MASS.log("Total MASS threads : " + MASS.threads.size() + " for pid : ["+MASS.myPid+"]");
	    	
	    	
    	/**
    	 * Do it for other processes.
    	 */
    	} else {
    		int dd = 0;
    		
    		for (int th = 0; th < MASS.threads.size(); th++) {
	        	Long threadId = MASS.threads.get(th).getId();
	        	int position = getThreadPosition(threadId);
	        	int[] range = getLocalRange(places, position);
	        	if (!DLBParams.boundaryMap.containsKey(threadId)) {
		        	DLBParams.boundaryMap.put(threadId, new Slice(range[0], range[1]));
		        	MASS.log("boundaryMap : threadId ["+threadId+"] lowerRange : ["+range[0]+"] upper["+range[1]+"]");
		        }
	        	if (dd < range[1]) {
	        		dd = range[1];
	        	}
	        }
    		
    		/**
	         * Also do it for main thread
	         */
	        Long tId = Thread.currentThread().getId();
	        int position = getThreadPosition(tId);
	    	int[] range = getLocalRange(places, position);
	    	if (!DLBParams.boundaryMap.containsKey(tId)) {
	        	DLBParams.boundaryMap.put(tId, new Slice(range[0], range[1]));
	        	MASS.log("boundaryMap : threadId ["+tId+"] lowerRange : ["+range[0]+"] upper["+range[1]+"]");
	        }
	    	
	    	if (dd < range[1]) {
	    		dd = range[1];
	    	}
    		
    		DLBParams.MAX_SIM_SIZE = dd;
    		
    		MASS.log("initBoundaries pid != 0 max size : " + DLBParams.MAX_SIM_SIZE);
    		MASS.log("Total MASS threads : " + MASS.threads.size() + " for pid : ["+MASS.myPid+"]");
    	}
    }

	/**
     * Initializes the MASS.MASS implementation.
     * 
     * @param args
     *            These arguments can be used to initialize the MASS.MASS
     *            environment. A "-c" or "-cores" followed by an integer will
     *            cause that number of threads to be started. If no "-c" or
     *            "-cores" is used, the number of threads started will be 1 or a
     *            number equal to the number of cores determined to be available
     *            by looking in "/proc/cpuinfo" on Linux OS or by looking in
     *            "/usr/sbin/system_profiler" on Mac OS X. This argument may be
     *            null.
     */
    public static void init(String[] args) 
    {
        synchronized (STATUS) 
        {
            if (!INITIALIZED) 
            {
                placesHandles = new Hashtable<Integer, Places>();
                agentsHandles = new Hashtable<Integer, Agents>();
                int corePreset = getCorePreset(args); // check for -c or -core
                                                      // flag with parameter
                int cores = (corePreset == 0) ? getCores() : corePreset;
                threadsRunning[0] = 1;
                threads = new Vector<Thread>();
                while (threadsRunning[0] < cores) { // cores - 1 to account for
                                                    // main thread
                        threadsRunning[0]++;
                        threads.add(new Mthread());
                }
                Iterator<Thread> iter = threads.iterator();
                while (iter.hasNext()) { // start the threads
                        try {
                                iter.next().start();
                        } catch (NoSuchElementException e) {
                                System.err.println("Error: MASS.Mthread not found.");
                                e.printStackTrace();
                                System.exit(-1);
                        }
                }
                INITIALIZED = true;
                STATUS[0] = STATUS_READY;
            } 
            else 
            {
                System.err.println("Error: the MASS.MASS environment is already initialized.");
            }
        }
    }

    /**
     * Shuts down the MASS.MASS implementation. STATUS[0] is changed to
     * STATUS_TERMINATE and each thread will call recordThreadExit. When all
     * child threads have called recordThreadExit, the main thread in this
     * function will be notified and will exit.
     */
    public static void finish() 
    {
        try 
        {
            if (INITIALIZED) 
            {
                synchronized (STATUS) 
                {
                    STATUS[0] = STATUS_TERMINATE;
                    STATUS.notifyAll();
                }
                synchronized (threadsRunning) 
                {
                    if (threadsRunning[0] > 1) 
                    {
                        threadsRunning.wait(); // the main thread waits
                    }
                }

                INITIALIZED = false;
            }

            MASS.log("Finishing MASS...");
            closeConnectionsAndFinish();
        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            System.exit(0);
        }            
    }

    static void closeConnectionsAndFinish() throws Exception
    {
        if(myPid == 0)     
        {
            Message msg = new Message();
            msg.createFinishMessage();
            for(MNode node : mNodes )
            {
                // send finish commend
                node.sendMessage(msg);                
            }
            
            // receive confirmation and close connections
            for(MNode node : mNodes )
            {
                node.receiveMessage();
                node.closeMainConnection();
            }
        }

        synchronized(exchangeHelper)
        {
            // kill exchange helpers
            exchangeHelper[0].finish();
        }
    }
    /**
     * Retrieves a "MASS.Places" object that has been created with a user-specified
     * handle.
     * 
     * @param handle
     *            a non-negative integer to uniquely identify a MASS.Places object
     * @return the MASS.Places object corresponding to the handle
     * @return null if no MASS.Places object is found corresponding to the handle
     */
    public static Places getPlaces(int handle) 
    {
        Places returnObj = null;
        if (INITIALIZED) 
        {
            synchronized (placesHandles) 
            {
                if (placesHandles.containsKey(handle)) 
                {
                        returnObj = placesHandles.get(handle);
                }
            }
        } 
        else 
        {
            String msg = "ERROR: MASS.MASS environment not yet initialized";
            System.out.println(msg);
        }
        return returnObj;
    }

	/**
	 * Retrieves an "Agentss" object that has been created by a user-specified
	 * handle and mapped over multiple machines.
	 * 
	 * @param handle
	 *            a non-negative integer to uniquely identify an MASS.Agents object,
	 *            (i.e., a set of multi-agents).
	 * @return an MASS.Agents object identified by handle, null if no MASS.Agents found
	 */
	public static Agents getAgents(int handle) {
		Agents retVal = null;
		if (INITIALIZED) {
			synchronized (agentsHandles) {
				if (agentsHandles.containsKey(handle)) {
					retVal = agentsHandles.get(handle);
				}
			}
		} else {
			String msg = "ERROR: MASS.MASS environment not yet initialized";
			System.out.println(msg);
		}
		return retVal;
	}

    // //////// public methods added to the spec ///////////////////

    /**
     * Finds the first and last index of a thread's range in a MASS.Places object for
     * callAll, i.e., the first and last indices of this thread's portion of the
     * MASS.Places object
     * 
     * @param places
     *            the MASS.Places object from which this thread's range will be
     *            calcualted
     * 
     * @return the first index of this threads range as element 0, the last as
     *         element 1
     * @return null if there not enough MASS.Places elements for the thread in this
     *         position
     */
    private static int[] getLocalRange(Places places) 
    {
        int position = getThreadPosition();
        
        int length = places.length();
        int[] range = new int[2]; // this will be returned
        int numThreads = threads.size() + 1;
        int portion = length / numThreads;
        int remainder = length % numThreads;
        
        if (DLBParams.boundaryMap.containsKey((long)position)) {
			
			range[0] = DLBParams.boundaryMap.get((long)position).getLowerBound();
			range[1] = DLBParams.boundaryMap.get((long)position).getUpperBound();
			
       		return range;
        }
        
        if (portion == 0) 
        { // there are more threads than elements in the
          // MASS.Places object
            if (remainder > position) 
            {
                range[0] = position;
                range[1] = position;
            } 
            else 
            {
                range = null;
            }
        } 
        else 
        { // there are more MASS.Places than threads
            int first = position * portion;
            int last = ((position + 1) * portion) - 1;
            if (position < remainder) 
            { // add in remainders
                first += position;
                last = last + position + 1;
            } 
            else 
            { // remainders have been assigned to previous positions
                first += remainder;
                last += remainder;
            }
            range[0] = first;
            range[1] = last;
        }
        	
        return range;
    }
    
    /**
     * The method is overloaded from the getLocalRange(Places) method.
     * The method takes in the position and returns the upper and lower 
     * range only for that position.
     * 
     * @param places
     * @param pos
     * @return
     */
    private static int[] getLocalRange(Places places, int pos) 
    {
        int position = pos;
        int length = places.length();
        int[] range = new int[2]; // this will be returned
        int numThreads = threads.size() + 1;
        int portion = length / numThreads;
        int remainder = length % numThreads;
        if (portion == 0) 
        { // there are more threads than elements in the
          // MASS.Places object
            if (remainder > position) 
            {
                range[0] = position;
                range[1] = position;
            } 
            else 
            {
                range = null;
            }
        } 
        else 
        { // there are more MASS.Places than threads
            int first = position * portion;
            int last = ((position + 1) * portion) - 1;
            if (position < remainder) 
            { // add in remainders
                first += position;
                last = last + position + 1;
            } 
            else 
            { // remainders have been assigned to previous positions
                first += remainder;
                last += remainder;
            }
            range[0] = first;
            range[1] = last;
        }
        
        return range;
    }


    /**
     * Gets the position of this thread in the threads Vector, with 0 as first.
     * 
     * @return the position
     */
    private static int getThreadPosition() 
    {
        Iterator<Thread> iter = threads.iterator();
        long tid = Thread.currentThread().getId();
        int position = 0; // main thread always zero
        int location = 0;
        while (iter.hasNext()) 
        {
            location++;
            if (tid == iter.next().getId()) 
            { // this is the thread
                    position = location;
            }
        }
        return position;
    }
    
    private static int getThreadPosition(Long threadId) {
    	Iterator<Thread> iter = threads.iterator();
        long tid = threadId;
        int position = 0; // main thread always zero
        int location = 0;
        while (iter.hasNext()) 
        {
            location++;
            if (tid == iter.next().getId()) 
            { // this is the thread
                    position = location;
            }
        }
        return position;
    }

    /**
     * called by a MASS.Places object to set up the static variables for exchangeAll
     * 
     * @param ea_places
     *            the MASS.Places object to be used for the exchangeAll call
     * @param ea_functionId
     *            the function number that will be passed in to callMethod for
     *            each MASS.Place object
     * @param ea_destinations
     *            set of offsets used to identify the MASS.Place objects from which
     *            each MASS.Place will gather return values
     */
    static void ea_setup(Places ea_places, int ea_functionId,
                    Vector<int[]> ea_destinations) 
    {
            if (!INITIALIZED)
                return;
            MASS.ea_places = ea_places;
            // set up the MASS.MASS.ea_destinations as an int[][] from the Vector passed
            // in, to avoid casts later
            Object[] tmp_destinations = ea_destinations.toArray();
            MASS.ea_destinations = new int[tmp_destinations.length][];
            for (int i = 0; i < tmp_destinations.length; i++) 
            {
                MASS.ea_destinations[i] = (int[]) tmp_destinations[i];
            }
            MASS.ea_functionId = ea_functionId;
            
            // send exchange all commands to all nodes
            if(myPid == 0)
            {
                Message exgMsg = new Message();
                exgMsg.setHandle(ea_places.getHandle());
                exgMsg.createExchangeAllMessage(ea_functionId, ea_destinations);
                for(MNode node : mNodes)
                {
                    node.sendMessage(exgMsg);
                }
            }
       
            synchronized (STATUS) 
            {
                STATUS[0] = STATUS_EXCHANGE_ALL;
                STATUS.notifyAll();
            }
            
            //MASS.log("ea_setup is complete for " + myPid);
    }

    /**
     * The function called by each thread to do exchangeAll. Called after
     * ea_setup has been called by a MASS.Places object. Each MASS.Place object's
     * inMessages array is populated with the return values from the MASS.Place
     * objects with which it is exchanging.
     * 
     */
    static void ea_exchangeAll()    
    {
        if (!INITIALIZED) return;
        
        Places.Iterator origin_iter = ea_places.iterator(getLocalRange(ea_places));
        //HashMap<String,  ArrayList<RemoteExchangeRequest>> exchangeAllRequestMap = new HashMap<String,  ArrayList<RemoteExchangeRequest>>();
        //MASS.log("Beginning exchange all - My local thread id =  " + getThreadPosition());
        if (origin_iter != null) 
        { // null when not enough MASS.Place objects for thread
            int[] size = ea_places.size();
            Place origin; // for caller MASS.Place
            // int[] origin_coords;//for caller coordinates
            Place dest; // for callee MASS.Place
            int in_msgs_len = ea_destinations.length;
            
            
            while (origin_iter.hasNext()) 
            { // go through this thread's range
                origin = origin_iter.next();

                if (origin.inMessages == null || origin.inMessages.length != in_msgs_len)
                    origin.inMessages = new Object[in_msgs_len];

                int inMessagesIndex = 0;
                for (int dest_i = 0; dest_i < in_msgs_len; dest_i++)
                {
                    // fill dest_coords
                    int[] neighborCoord = Places.getGlobalNeighborArrayIndex(origin.index, ea_destinations[dest_i], size);
                    if (neighborCoord[0] != -1) 
                    { // destination valid
                        int globalLinearIndex = Places.getGlobalLinearIndexFromGlobalArrayIndex(neighborCoord, size);
                        //MASS.log("Performing exchange all on index  " + globalLinearIndex);
                        int destinationLocalLinearIndex = Places.getLocalLinearIndexFromGlobalLinearIndex(globalLinearIndex);
                        if(destinationLocalLinearIndex >= 0 && destinationLocalLinearIndex < ea_places.length())
                        {                            
                            dest = ea_places.get(Places.getLocalLinearIndexFromGlobalLinearIndex(globalLinearIndex));
                            origin.inMessages[inMessagesIndex] = dest.callMethod(ea_functionId, origin.outMessages);
                        } 
                        else 
                        {                            
                            // this means the destination is not on the local node
                            // look it up and instantiate remote node exchange all call
                            RemoteExchangeRequest request = 
								new RemoteExchangeRequest(globalLinearIndex,
                               							  Places.getGlobalLinearIndexFromGlobalArrayIndex(origin.index, size),
                                                          inMessagesIndex,
                                                          origin.outMessages);

                            // get the host name
                            String destHostName = ea_places.getHostname(globalLinearIndex);

                            //MASS.log("ExchangeAll RemoteCall - index is : " + globalLinearIndex 
							// + " destination: " + destHostName);
                            synchronized(exchangeAllRequestMap)
                            {
                                if(exchangeAllRequestMap.get(destHostName) == null)
                                {
                                    ArrayList<RemoteExchangeRequest> requests = new ArrayList<RemoteExchangeRequest>();
                                    requests.add(request);
                                    exchangeAllRequestMap.put(destHostName, requests);
                                }
                                else
                                {
                                    exchangeAllRequestMap.get(destHostName).add(request);
                                }    
                            }
                            //MASS.log("ExchangeAll exchangeAllRequestMap size : " + exchangeAllRequestMap.size());

                        }
                    } 
                    else 
                    {   // destination invalid, coordinates outside this MASS.Places
                        origin.inMessages[inMessagesIndex] = null;
                    }
                    inMessagesIndex++;
                 } // end of for loop
            }
        }
                       
        barrier(); // exit smoothly 
        //processRemoteExchangeRequest(exchangeAllRequestMap);
        processRemoteExchangeRequest( );
        barrier();

    }
  

    /**
	 * Exchange Boundary - Setup 
     * called by a MASS.Places object to set up the static variables for exchangeBoundary
     * @param eb_places         the MASS.Places object to be used for the exchangeBoundary call
     * @param ea_functionId     the function number that will be passed in to callMethod 
     * @param lBoundary         the Left  Shadow Boundary array
     * @param rBoundary         the Right Shadow Boundary array
     *///---------------------------------------------------------------------------------------
    static void eb_setup( Places places, int functionId, Vector<int[]> destinations,
                                                        Place[] lBoundary, Place[] rBoundary )
    {
		if (!INITIALIZED)   return;

		// Print exchangeBoundary setup message
        MASS.log("\nRunning ExchangeBoundary Setup (eb_setup)");
        if( lBoundary != null )  MASS.log("  * lbSize: " + lBoundary.length );
        else 					 MASS.log("  * lbSize: NULL ");
        if( rBoundary != null )  MASS.log("  * rbSize: " + rBoundary.length );
        else 					 MASS.log("  * rbSize: NULL ");

         // Setup Global variables
         MASS.eb_places      = places;            // the handle for this Places
         MASS.eb_functionId  = functionId;        // callMethod function number

         // Convert destinations from Vector<int[]> to int[][] array, then
         // set the MASS.eb_destinations variable to the new int[][] array
         Object[] tmp_destinations   = destinations.toArray();
         MASS.eb_destinations        = new int[ tmp_destinations.length ][ ];

         for (int i = 0; i < tmp_destinations.length; i++) {
         	MASS.eb_destinations[i] = (int[ ]) tmp_destinations[i];
         }

		// Set the left and right Shadow Boundary variables
        MASS.eb_lBoundary   = lBoundary;            // Left  Shadow Boundary
        MASS.eb_rBoundary   = rBoundary;            // Right Shadow Boudary

        // Master Node Sends exchangeBoundary commands to all nodes
        if(myPid == 0) {
        	Message exgMsg = new Message();
            exgMsg.createExchangeBoundaryMessage( functionId, destinations );

            for( MNode node : mNodes ) {
            	MASS.log("Sending eb_setup (" + exgMsg.getAction() + ") message to " 
							+ node.getHostName() + " (pid=" + node.getPid() + ")" );
				node.sendMessage(exgMsg);
        	}
		}

       	// Wake-up all threads
       	synchronized (STATUS) {
       		STATUS[0] = STATUS_EXCHANGE_BOUNDARY;
           	STATUS.notifyAll();
       	}
    }
 

    /**
     * Exchange Boundary - Main Function
     *///---------------------------------------------------------------------------------------
	static void eb_exchangeBoundary()
    {
   		// The Shadowed Place
        Place shdwPlace;

        // Get thread information
        int numThreads     = threads.size() + 1;
        int threadNumber   = getThreadPosition();

        // Get Boundary information
        int lbSize =  (eb_lBoundary == null) ? 0 : eb_lBoundary.length;
        int rbSize =  (eb_rBoundary == null) ? 0 : eb_rBoundary.length;
        int totalBndrySize = lbSize + rbSize;
        int maxBndrySize   = (lbSize > rbSize) ? lbSize : rbSize;

        // Verify system has been initialized and at least one of the shadow boundaries exists
        if ( !INITIALIZED || totalBndrySize == 0 ) return;

	    // Loop through all of the shandow boundary locations (left and/or right)
        int bndryIdx = threadNumber;
        while( bndryIdx < totalBndrySize )
        {
        		// Get shadow boundary place
                if( bndryIdx < lbSize )	shdwPlace = eb_lBoundary[ bndryIdx ];			// Left Boundary
                else 					shdwPlace = eb_rBoundary[ bndryIdx - lbSize ];	// Right Boundary

				// Get shadow global linear index location and the destination hostname
                int shdwGlobalLinearIdx =
                		Places.getGlobalLinearIndexFromGlobalArrayIndex( shdwPlace.index, eb_places.size() );
				String destHostName = eb_places.getHostname( shdwGlobalLinearIdx );

                // Create a new request
                RemoteExchangeRequest request = new RemoteExchangeRequest( shdwGlobalLinearIdx, bndryIdx, null );

				synchronized(exchangeAllRequestMap)
                {
                	if(exchangeAllRequestMap.get(destHostName) == null) {
						ArrayList<RemoteExchangeRequest> requests = new ArrayList<RemoteExchangeRequest>();
                        requests.add(request);
                        exchangeAllRequestMap.put(destHostName, requests);
                    } else {
                        exchangeAllRequestMap.get(destHostName).add(request);
                    }
                }
                //MASS.log("ExchangeBoundary 'exchangeAllRequestMap' size : " + exchangeAllRequestMap.size());
                bndryIdx += numThreads;
        }

		// Process the Remote exchange requests with the remote nodes
        barrier();
        processRemoteExchangeRequest( );
        barrier();
    }


	/**
	  * Exchange Boundary - Update Function
    *///---------------------------------------------------------------------------------------
    static void eb_update()
    {
		if ( !INITIALIZED ) return;
    	MASS.log("Starting Exchange Boundary Update...");

        // Create Places Iterator for the places assigned to this thread
    	int[] thrdRange 			= getLocalRange( eb_places );
        Places.Iterator origin_iter = eb_places.iterator( thrdRange );

        if ( origin_iter != null )                              // null when not enough MASS.Place objects for thread
        {
            int[ ] size = eb_places.size( );
            Place origin;                                       // for caller MASS.Place
            Place dest;                                         // for callee MASS.Place
            int in_msgs_len = eb_destinations.length;

            while ( origin_iter.hasNext( ) )                    // go through this thread's range
            {
                origin = origin_iter.next( );
                if ( origin.inMessages == null || origin.inMessages.length != in_msgs_len ) {
                     origin.inMessages = new Object[ in_msgs_len ];
                }
                int inMessagesIndex = 0;

				// Loop through all of the destinations (neighbors)
                for ( int dest_i = 0;  dest_i < in_msgs_len;  dest_i++ ) {

                	// fill dest_coords
                    int[] neighborCoord =
						Places.getGlobalNeighborArrayIndex( origin.index, eb_destinations[ dest_i ], size );

                 	// If a Valid Destination, update destination information in inMessages
                    if ( neighborCoord[0] != -1 ) {

                    	int globalLinearIndex = 
									Places.getGlobalLinearIndexFromGlobalArrayIndex( neighborCoord, size );
						int destinationLocalLinearIndex = 
									Places.getLocalLinearIndexFromGlobalLinearIndex( globalLinearIndex );

                        // On Destination Machine
                        if(  ( 0 <= destinationLocalLinearIndex )  &&
                             ( destinationLocalLinearIndex < eb_places.length() ) )
                        {
                        	dest = eb_places.get( destinationLocalLinearIndex );
                            origin.inMessages[ inMessagesIndex ] = dest.callMethod(eb_functionId, origin.outMessages);
                        }

                        // Left Shadow Boundary
                        else if( ( destinationLocalLinearIndex < 0 ) &&
                        		 ( eb_lBoundary.length + destinationLocalLinearIndex  < eb_lBoundary.length ) )
                        {
							dest = eb_lBoundary[ eb_lBoundary.length + destinationLocalLinearIndex ];
                            origin.inMessages[ inMessagesIndex ] = dest.outMessages;
                        }

                        // Right Shadow Boundary
                        else if( ( destinationLocalLinearIndex >= eb_places.length() ) &&
                                 ( destinationLocalLinearIndex - eb_places.length() ) < eb_rBoundary.length )
                        {
                            dest = eb_rBoundary[ destinationLocalLinearIndex - eb_places.length() ];
                            origin.inMessages[ inMessagesIndex ] = dest.outMessages;
                        } 

						// Error - not found
                        else {
                            origin.inMessages[ inMessagesIndex ] = null;
                        }
                    }

                    // Invalid Destination, coordinates outside this MASS.Places
                    else {
                        origin.inMessages[ inMessagesIndex ] = null;
                    }

                    inMessagesIndex++;
                 } // end of destination for loop
            }
        	MASS.log("Exchange Boundary Update Complete!");
        }
    	barrier();
    }


	/**
	  * Process the Remote Exchange Request between nodes
	  * Used by both exchangeAll() and exchangeBoundary()
	*/
    private static void processRemoteExchangeRequest( )
    {   
        java.util.Map.Entry exgRequest = null;
        String destinationHostName = null;
        ExchangeHelper helper = null;
        Message  exchangeP = null;
        ArrayList<RemoteExchangeRequest> requestList = null;        
        
        synchronized( exchangeAllRequestMap )
        {     
            if(exchangeAllRequestMap.isEmpty()) 
                return;           
            else
            {
                Set requestsEntrySet = exchangeAllRequestMap.entrySet();
                Iterator it = requestsEntrySet.iterator();
                if( it.hasNext())
                {
                    exgRequest = (java.util.Map.Entry)it.next();
                    destinationHostName = (String)exgRequest.getKey();
                    requestList = (ArrayList<RemoteExchangeRequest>)exgRequest.getValue(); 
                    exchangeAllRequestMap.remove(destinationHostName);
                    MASS.log("Beginning remote exchange mt version - My local thread id =  " + getThreadPosition() 
						+ " requests remaining: " + exchangeAllRequestMap.size() 
                        +  " exchange destination: " + destinationHostName );
                    //exchangeAllRequestMap.notifyAll();                   
                }               
            }
        }
     
        // perform exchange request processing
        startRemoteExchange(destinationHostName, requestList);
        
        // If there are more requests than threads allocated, one thread needs to pick up the remaining requests
        synchronized( exchangeAllRequestMap ) 
        {                
            while(!exchangeAllRequestMap.isEmpty())
            {
                Set requestsEntrySet = exchangeAllRequestMap.entrySet();
                Iterator it = requestsEntrySet.iterator();
                if( it.hasNext())
                {
                    exgRequest = (java.util.Map.Entry)it.next();
                    destinationHostName = (String)exgRequest.getKey();
                    requestList = (ArrayList<RemoteExchangeRequest>)exgRequest.getValue(); 
                    exchangeAllRequestMap.remove(destinationHostName);
                    MASS.log("Handling remaining remote exchange - My local thread id =  " + getThreadPosition() 
							+ " requests remaining: " + exchangeAllRequestMap.size() 
                            + " exchange destination: " + destinationHostName );
                    startRemoteExchange(destinationHostName, requestList);
                }               
            }
        }
    } 

	/**
	  * Start Remote Exchange Request between nodes
	  * Used by both exchangeAll() and exchangeBoundary()
	*/
    private static void startRemoteExchange(String destinationHostName, ArrayList<RemoteExchangeRequest> requestList)
    {
        exchangeHelper[0].establishConnection(destinationHostName);
        
        //MASS.log("Starting remote exchange with :" + destinationHostName);
        Message  exchangeMsg = new Message();
        exchangeMsg.createExchangeAllRequestMessage(requestList);

        exchangeHelper[0].sendRequest(destinationHostName, exchangeMsg);   
        //MASS.log("Sent Exchange All Request to " + destinationHostName );

        exchangeHelper[0].processRequest(destinationHostName);  
    }
   
	/**
	  * Start ExchangeHelper
	*/ 
    static void startExchangeHelper( )
    {
        synchronized(exchangeHelper)
        {
            if( exchangeHelper[0] == null )
            {
                exchangeHelper[0] = new ExchangeHelper( );
                exchangeHelper[0].start();                      
            }       
        }
    }
    /*public static void processRemoteExchangeRequest( HashMap<String, ArrayList<RemoteExchangeRequest>> exchangeAllRequestMap)
    {        
        if (exchangeAllRequestMap.isEmpty()) return;
        
        MASS.log("Beginning remote exchange mt version - My local thread id =  " + getThreadPosition() + " request size: " + exchangeAllRequestMap.size());
        Set requestsEntrySet = exchangeAllRequestMap.entrySet();
        java.util.Map.Entry exgRequest = null;
        String destinationHostName = null;
        ExchangeHelper helper = null;
        Package  exchangeP = null;
        ArrayList<RemoteExchangeRequest> requestList = null;
 
        synchronized(exchangeHelper)
        {
            if( exchangeHelper[0] == null )
            {
                //MASS.log("Starting Exchange All connection to " + destinationHostName );

                exchangeHelper[0] = new ExchangeHelper( );

                exchangeHelper[0].start();                      
            }
        
           
            for(Iterator it = requestsEntrySet.iterator(); it.hasNext(); )
            {
                exgRequest = (java.util.Map.Entry)it.next();
                destinationHostName = (String)exgRequest.getKey();
                requestList = (ArrayList<RemoteExchangeRequest>)exgRequest.getValue();

                exchangeHelper[0].establishConnection(destinationHostName);

                //MASS.log("Starting remote exchange with :" + destinationHostName);
                exchangeP = new Package();
                exchangeP.createExchangeAllRequestPackage(requestList);

                exchangeHelper[0].sendRequest(destinationHostName, exchangeP);   
                //MASS.log("Sent Exchange All Request to " + destinationHostName );

                exchangeHelper[0].processRequest(destinationHostName);   

            }
        }
        
    } */
  
 
	/**
	  *	Perform Remote Exchange with other nodes
	  * Used by both exchangeAll() and exchangeBoundary()
	*/ 
    static ArrayList<RemoteExchangeRequest> doRemoteExchangeAll(ArrayList<RemoteExchangeRequest> requestList)
    {
        Place destination;
    	RemoteExchangeRequest returnReq;
        ArrayList<RemoteExchangeRequest> retList = new ArrayList<RemoteExchangeRequest>();

        for( RemoteExchangeRequest request : requestList )
        {            
        	int tmpDestGlbLinIdx = request.getDestinationGlobalLinearIndex();
        	int tmpDestLocLinIdx = Places.getLocalLinearIndexFromGlobalLinearIndex( tmpDestGlbLinIdx );

            // If request is a exchangeBoundary request
            // ----------------------------------------
            if( request.isBoundaryRqst() ) {

        		if( eb_places == null ) { MASS.log("***  ERROR: eb_places == null  ***"); System.exit(-1); }
                destination         	= eb_places.get( tmpDestLocLinIdx );
                Object returnMessage    = destination.callMethod( eb_functionId, request.getOutMessage() );

        		// Create a new return request to send back the return values
                returnReq       = new RemoteExchangeRequest( request.getDestinationGlobalLinearIndex(),
                                                             request.getOriginGlobalLinearIndex(),
                                                             returnMessage );
            }

            // If request is an exchangeAll request
            // ----------------------------------------
            else {
            	destination         	= ea_places.get( tmpDestLocLinIdx );
                Object returnMessage    = destination.callMethod( ea_functionId, request.getOutMessage() );

        		// Create a new return request to send back the return values
                returnReq       = new RemoteExchangeRequest( request.getDestinationGlobalLinearIndex(),
                                                             request.getOriginGlobalLinearIndex(),
                                                             request.getInMessageIndex(),
                                                             returnMessage  );
            }
            retList.add(returnReq);
        }
        return retList;  
    }
   
	/** 
	  * Update the inMessages from messages received from other nodes 
	  * Used by both exchangeAll() and exchangeBoundary()
	*/
    static void updateInMessages( ArrayList<RemoteExchangeRequest> requestList )
    {
        Place origin;
        ArrayList<RemoteExchangeRequest> retList = new ArrayList<RemoteExchangeRequest>();

        for(RemoteExchangeRequest request : requestList)
        {           

            // If request is a exchangeBoundary request
            // ----------------------------------------
            if( request.isBoundaryRqst() ) {

                int idx    = request.getBndryIndex();
                int lbSize = (eb_lBoundary == null) ? 0 : eb_lBoundary.length;

                // Determine the shadow boundary location (left or right), then retrieve it
                if( idx < lbSize )  origin = eb_lBoundary[ idx ];
                else                origin = eb_rBoundary[ idx - lbSize ];

                // Update the shadow boundary outMessage variable with the retrieved value
                origin.outMessages = request.getOutMessage();
            }

            // If request is an exchangeAll request
            // ----------------------------------------
            else {
            	origin = ea_places.get(
							Places.getLocalLinearIndexFromGlobalLinearIndex(request.getOriginGlobalLinearIndex()));
            	origin.inMessages[request.getInMessageIndex()] = request.getOutMessage();
			}
        }        
    }


	/**
	 * called by MASS.Places object to set up the static variables for callAll
	 * 
	 * @param plcs
	 *            the MASS.Places object that will be used for the callAll
	 * @param functionId
	 *            the function number that will be passed in to callMethod for
	 *            each MASS.Place
	 * @param argument
	 *            the argument that will be passed in to callMethod for each
	 *            MASS.Place
	 */
    static void ca_setup_1arg(Places plcs, int functionId, Object argument) 
	{
		if (!INITIALIZED)
			return;
		// try {
		synchronized (STATUS) 
		{
			ca_places = plcs;
			ca_functionId = functionId;
			ca_argument = argument;
			ca_arguments = null; // arguments array
			ca_retVals = null;
			ca_finalRetVals = null;
			if(MASS.myPid == 0 )
			{
				Message m = new Message();
				m.setHandle(plcs.getHandle());
				m.createActionMessage(Constants.CALL_ALL_VOID_OBJECT, functionId, argument);
				for(MNode node : mNodes)
				{
					node.sendMessage(m); //use plcs HANDLE HERE
				}                    
			}
			STATUS[0] = STATUS_CALLALL;
			STATUS.notifyAll();
		}

		//MASS.log("ca_setup with NO return value is complete for " + myPid);
	}

	/**
	 * called by MASS.Places object to set up the static variables for callAll, an
	 * array of objects is initialized to be used as the return value
	 * 
	 * @param plcs
	 *            the MASS.Places object that will be used for the callAll
	 * @param functionId
	 *            the function number that will be passed in to callMethod for
	 *            each MASS.Place
	 * @param arguments
	 *            the arguments that will be passed in to callMethod for each
	 *            corresponding MASS.Place
	 */
	static void ca_setup_args(Places plcs, int functionId, Object[] arguments) 
	{
		if (!INITIALIZED)
			return;
		// try{
		synchronized (STATUS)
		{
			ca_places = plcs;
			ca_functionId = functionId;
			ca_argument = null;
			ca_arguments = arguments;
			ca_retVals = new Object[plcs.length()];

			if(myPid == 0)
			{              
				Message m = new Message();
				m.setHandle(plcs.getHandle()); //be sure to set Handle!
				m.createActionMessage(Constants.CALL_ALL_RETURN_OBJECT, functionId, arguments);
				for(MNode node : mNodes)
				{
					node.sendMessage(m);
					
				}
				ca_finalRetVals = new Object[plcs.totalLength()];
			}

			STATUS[0] = STATUS_CALLALL;
			STATUS.notifyAll();
		}
		//MASS.log("ca_setup with return value is complete for " + myPid);
	}

    /**
     * Entry point for individual threads for callAll. Type of callAll, i.e.,
     * whether an array is returned or not, is determined by the available
     * parameters provided by MASS.Places object in callAllSetup method.
     * 
     * @return null if the thread is a child thread or return array not
     *         initialized
     * @return Object[] if thread is main thread and return array initialized
     */
    static Object[] ca_callAll()
    {
        if (!INITIALIZED) return null;
           
        Object[] retVals = null;
        int[] range = getLocalRange(ca_places);
        Places.Iterator iter = ca_places.iterator(range);
        if (iter != null)
        { // null indicates not enough MASS.Places elements for
                                                    // this thread
            Place place;
            if (ca_retVals == null)
            { // no return value, single argument
                
            	int times = 0;
                while (iter.hasNext() && times < 100000) //added to make sure
                {										 //MASS doesn't get stuck
                	place = iter.next();
					if (place != null) 
					{
						place.callMethod(ca_functionId, ca_argument);

					}
					times++;
                }

            }
            else
            { // there is a return value, array argument callAll
                int index = range[0];
                Object param;
                while (iter.hasNext())
                {
                    place = iter.next();
                    if (place == null)
                    {
                        ca_retVals[index] = null; // return val null if MASS.Place is                                                                                    // null
                    }
                    else
                    {
                        param = (ca_arguments != null) ? ca_arguments[index] : null;
                        ca_retVals[index] = place.callMethod(ca_functionId, param);
                    }
                    index++;
                }
                if (getThreadPosition() == 0)
                    retVals = ca_retVals; // only main thread returns array
            }
        }
        barrier();
        if(retVals != null && myPid == 0 && getThreadPosition() == 0) // if a return value is required
        {
            //MASS.log("Master is collecting return values for call all");
            // master collects results from other ranks
            return callAllCollect(retVals);
        }
        
        return retVals; // always null for child threads, may be assigned for
                                                // main thread
    }

    private static Object[] callAllCollect(Object[] retVals)
    {      
        // first set master rank's results onto the array
        System.arraycopy(retVals, 0, ca_finalRetVals, 0, retVals.length);
        int startPos = 0;
        int length = 0;
        for(MNode node : MASS.mNodes)
        {
            Message m = node.receiveMessage();
            Object[] nodeRetVal = (Object[])m.getMessage().get(Constants.CALL_ALL_RETURN_VALUES);
            startPos = node.getPid() * Places.chunkSize;
            length = node.getPid() == MASS.systemSize - 1 ? Places.chunkSize + Places.remainder : Places.chunkSize;
            
            MASS.log("Collection information for rank " + node.getHostName() + " startPos: " + startPos + " length: " + length + " total Length: " + ca_finalRetVals.length);
            System.arraycopy(nodeRetVal, 0, ca_finalRetVals, startPos, length);           
        }
        //MASS.log("Master has finished collecting return values for call all");
        return ca_finalRetVals;
    }
    
    /**
     * Decrements the thread count. Used by MThread objects when STATUS[0] has
     * been set to STATUS_TERMINATE by the main thread in finish(). When all the
     * child threads have passed through this method, the main thread is
     * notified and can exit finish().
     */
    static void recordThreadExit() 
    {
        synchronized (threadsRunning) 
        {
            if (--threadsRunning[0] == 1) 
            { // 1 means only one thread left, main thread in finish()
                threadsRunning.notify();
            }
        }
    }

    /**
     * Adds a MASS.Places object to the map of placesHandles for MASS.Places. Will not add
     * a MASS.Places object with a handle that is already in use.
     * 
     * @param places
     *            the MASS.Places object to add to the map of placesHandles
     * @return true if MASS.Places successfully added
     * @return false if MASS.Places is not successfully added
     */
    public static boolean addPlaces(Places places) 
    {
        boolean result = false;
        if (INITIALIZED) 
        {
            int handle = places.getHandle();
            if (handle != -1) 
            {
                synchronized (placesHandles) 
                {
                    if (!placesHandles.containsKey(handle)) 
                    {
                        placesHandles.put(handle, places);
                        result = true;
                    } 
                    else 
                    {
                        String msg = "ERROR: Handle " + handle
                                        + " already in use.";
                        System.out.println(msg);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Adds a MASS.Agents object to the map of placesHandles for MASS.Agents. Will not add
     * a MASS.Agents object with a handle that is already in use.
     * 
     * @param agents
     *            the MASS.Agents object to add to the map of agentsHandles
     * @return true if MASS.Agents successfully added
     * @return false if MASS.Agents is not successfully added
     */
    static boolean addAgents(Agents agents) 
    {
        boolean result = false;
        if (INITIALIZED) 
        {
            int handle = agents.getHandle();
            if (handle != -1) 
            {
                synchronized (agentsHandles) 
                {
                    if (!agentsHandles.containsKey(handle)) 
                    {
                        agentsHandles.put(handle, agents);
                        result = true;
                    } 
                    else 
                    {
                        String msg = "ERROR: Handle " + handle
                                        + " already in use.";
                        System.out.println(msg);
                    }
                }
            }
        }
        return result;
    }

    // ///////////////// private methods added to the original spec
    // ////////////////

    /**
     * Each thread enters and waits on STATUS until all threads have entered,
     * and then STATUS is set to STATUS_READY and all threads are notified and
     * can exit. This method ensures that operations such as callAll and
     * exchangeAll are completed by all threads before any thread can start
     * doing some other task.
     */
    private static void barrier() 
    {
        synchronized (STATUS) 
        {
            barrierCounter++;
            if (barrierCounter == threads.size() + 1)
            {
                barrierCounter = 0;
                STATUS[0] = STATUS_READY;
                STATUS.notifyAll();
            } else
            {
                try { STATUS.wait(); } 
                catch (InterruptedException e) 
                {
                    e.printStackTrace();
                    System.exit(-1);
                }               
            }
        }
    }

    /**
     * checks an array for a -c or -cores flag in the arguments passed to
     * MASS.MASS.init. If there is a -c or -cores flag, the element following the
     * flag is used as the return value.
     * 
     * @param args
     *            the array to parse
     * @return the number of cores specified by a -c or -cores flag
     * @return 0 if no flag is found or the flag appeared at the end of the
     *         array w/o parameter
     */
    private static int getCorePreset(String[] args) 
    {
        int corePreset = 0;
        boolean errorOccurred = false;
        for (int i = 0; args != null && !errorOccurred && i < args.length; i++) 
        {
            if ((args[i].compareTo("-cores") == 0 || args[i].compareTo("-c") == 0)
                            && i < args.length - 1) 
            {
                try 
                {
                    corePreset = Integer.parseInt(args[i + 1]);
                } 
                catch (Exception e) 
                {
                    corePreset = 0;
                    errorOccurred = true;
                }
            }
        }
        return corePreset;
    }

    /**
     * Determines the number of processor cores available on the machine (Linux
     * or Mac only ).
     * 
     * @return int the number of processor cores available on the current
     *         machine
     */
    private static int getCores() 
    {
        int nCores = 0;
        String str = "";
        try 
        {
            File file = new File("/proc/cpuinfo");// file that stores proc/core info
            if (file.exists()) 
            { // should be Linux machine
                String keyword = "processor"; // the word used in the file to describe each core
                FileReader in = new FileReader(file);
                int c;
                while ((c = in.read()) != -1) 
                {
                    str += (char) c;
                }
                while ((c = str.lastIndexOf(keyword)) != -1) 
                {
                    nCores++;
                    str = str.substring(0, c);
                }
            } 
            else 
            { // try MacOS
                file = new File("/usr/sbin/system_profiler");
                if (file.exists()) 
                { // should be MacOS
                    Process p = Runtime.getRuntime().exec("/usr/sbin/system_profiler");
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    while ((str = in.readLine()) != null) 
                    {
                        str = str.trim();
                        if (str.startsWith("Total Number Of Cores")) 
                        {
                                str = str.substring(str.lastIndexOf(":") + 1).trim();
                            nCores = Integer.parseInt(str);
                        }
                    }
                }
            }
        } 
        catch (Exception e) 
        {
            System.out.println("Error: " + e + ". Defaulting to 1 core.");
            nCores = 1;
        }
        return nCores;
    }
    
    // /////////////////////////// MASS.Agents operations methods //////////////////
    // MASS.MASS class might work better if operations locked on some object while
    // doing any activity, to prevent multi-threaded programs from doing
    // multiple
    // activities

    static void agentsSortAll(int handle, boolean descending) 
    {
        synchronized (OPERATION_LOCK) 
        {
            Agents agents = getAgents(handle);
            if (agents != null && agents.places != null) 
            {
                agentsOpAgents = agents;
                agentsOpPlaces = agents.places;
                agentsOpHandle = handle;
                agentsOpDescending = descending;
                // change the status
                synchronized (STATUS) 
                {
                    STATUS[0] = STATUS_AGENTS_SORT_ALL;
                    STATUS.notifyAll();
                }
                agentsSortAllPerThread();// do op as main thread }
            }
        }
    }

    static void agentsSortAllPerThread() 
    {
        if (agentsSortAllParamsCheckOkay()) 
        {
            Places.Iterator placeIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));
            if (placeIter != null) 
            {
                Place place = null; // reuseable vars
                Agent agent = null;
                Comparator<Agent> agentComparator = agentsOpAgents.new AgentComparator();

                TreeSet<Agent> treeSet = new TreeSet<Agent>(agentComparator);
                while (placeIter.hasNext()) 
                {
                    treeSet.clear();
                    place = placeIter.next();
                    synchronized (place.agents) 
                    {
                        // take out all the agents from the MASS.Agents we are
                        // working with
                        for (int i = 0; i < place.agents.size(); i++) 
                        {
                            agent = place.agents.get(i);
                            if (agent.agentsHandle == agentsOpHandle) 
                            {
                                    treeSet.add(agent); // add them to the treeSet
                                                                            // to sort
                                    place.agents.remove(i--);
                            }
                        }
                        // now get them out of the treeSet and add them to the
                        // end of place.agents

                        java.util.Iterator<Agent> agentIter = treeSet
                                        .iterator();
                        Vector<Agent> sortedAgents = new Vector<Agent>();
                        while (agentIter.hasNext()) 
                        {
                            sortedAgents.add(agentIter.next());
                        }
                        if (agentsOpDescending) 
                        {
                            // reverse the order of the agent, originally a
                            // descending iterator was used to accomplish this,
                            // but Java 1.5 compatibility was desired
                            Vector<Agent> reversedAgents = sortedAgents;
                            int size = reversedAgents.size();
                            sortedAgents = new Vector<Agent>(size);
                            for (int i = 0; i < size; i++) 
                            {
                                int reverseIndex = size - 1 - i;
                                sortedAgents.add(i, reversedAgents.get(reverseIndex));
                            }
                        }
                        place.agents.addAll(sortedAgents);
                    }   
                }
            }
        }
        barrier();                                                      
    }

    private static boolean agentsSortAllParamsCheckOkay() 
    {
        boolean retVal = false;
        if (agentsOpAgents != null && agentsOpPlaces != null) 
        {
            retVal = true;
        }
        return retVal;
    }

    static void agentsCallAll(int handle, Places places, int functionId, Object argument) 
    {
        synchronized (OPERATION_LOCK) 
        {
            // validate args
            boolean validArgs = true;
            Agents agents = getAgents(handle);
            if (agents == null || places == null) 
            {
                validArgs = false;
            }
            if (validArgs && INITIALIZED) 
            { // do it
                cleanAgentsOpVariables();
                // set up variables
                agentsOpHandle = handle;
                agentsOpAgents = agents;
                agentsOpPlaces = places;
                agentsOpFunctionId = functionId;
                agentsOpCallAllArg = argument;
                agentsOpSaveResults = false;
                if(MASS.myPid == 0 )
                {
                    Message m = new Message();
                    m.createActionMessage(Constants.AGENTS_CALL_ALL_VOID, functionId, argument);
                    for(MNode node : mNodes)
                    {
                        node.sendMessage(m);
                    }                    
                }                
                // change the status
                synchronized (STATUS) 
                {
                    STATUS[0] = STATUS_AGENTS_CALL_ALL;
                    STATUS.notifyAll();
                }
                agentsCallAllPerThread();// do op as main thread
            }
        }
    }

    static Object[] agentsCallAll(int handle, Places places, int functionId, Object[] arguments) 
    {
        agentsOpCallAllResults = null;
        synchronized (OPERATION_LOCK) 
        {
            // validate the arguments
            boolean validArgs = true;
            Agents agents = getAgents(handle);
            if (agents == null || places == null) 
            {
                validArgs = false;
            }
            if (validArgs && INITIALIZED) 
            {
                cleanAgentsOpVariables();
                // set up the variables
                agentsOpHandle = handle;
                agentsOpPlaces = places;
                agentsOpFunctionId = functionId;
                agentsOpCallAllArgs = arguments;
                agentsOpSaveResults = true;
                int len = agents.nAgents();
                //agentsOpCallAllResults = new Object[len]; // no harm if no agent and this not used
                if(myPid == 0)
                {              
                    Message m = new Message();
                    m.createAgentActionMessage(Constants.AGENTS_CALL_ALL_RETURN_OBJECT, handle, functionId, arguments);
                    for(MNode node : mNodes)
                    {
                        node.sendMessage(m);
                    }
                    // now other ranks must submit the number of agents they have
                    for(MNode node: mNodes)
                    {
                        len += node.receiveMessage().getNumAgents();
                    }
                }

                if(len > 0)
                    agentsOpCallAllResults = new Object[len];
                           
                synchronized (STATUS) 
                {
                    STATUS[0] = STATUS_AGENTS_CALL_ALL;
                    STATUS.notifyAll();
                }
                agentsCallAllPerThread();// do the operation as main thread
            }
        }
        if(agentsOpCallAllResults != null && myPid == 0 && getThreadPosition() == 0) // if a return value is required
        {
            //MASS.log("Master is collecting return values for call all");
            // master collects results from other ranks
            return agentsCallAllCollect( );
        }       
        return agentsOpCallAllResults;
    }
    
    private static Object[] agentsCallAllCollect( )
    {      
        // first set master rank's results onto the array
        int startPos = MASS.getAgents(agentsOpHandle).nAgents();
        for(MNode node : MASS.mNodes)
        {
            Message m = node.receiveMessage();
            Object[] nodeRetVal = (Object[])m.getMessage().get(Constants.CALL_ALL_RETURN_VALUES);
            MASS.log("Agent collection information for rank " + node.getHostName() + " startPos: " + startPos + " length: " + (nodeRetVal == null ? 0 : nodeRetVal.length) + " total Length: " + (agentsOpCallAllResults == null ? 0 : agentsOpCallAllResults.length));
            if(nodeRetVal != null)
            {
                System.arraycopy(nodeRetVal, 0, agentsOpCallAllResults, startPos, nodeRetVal.length); 
                startPos += nodeRetVal.length;
            }
        }
        MASS.log("Master has finished collecting agents return values for call all");
        //for(int i = 0; i < agentsOpCallAllResults.length; i++)
        //    MASS.log("Agent Result content [" + i + "] is " + agentsOpCallAllResults[i]);
        return agentsOpCallAllResults;
    }
    
    static void agentsCallAllPerThread() 
    {
        Places.Iterator placesIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));
        if (placesIter != null) 
        {
            Place place = null; // reusable variables
            Object result = null;
            Object arg = null;
            Agent localAgent = null;
            while (placesIter.hasNext()) 
            { // go through each place
                place = placesIter.next();
                // select each agent with the current agents handle
                // Iterator<MASS.Agent> allAgentIter = place.agents.iterator();
                Vector<Agent> localAgents = getAllAgentWithHandle(place.agents, agentsOpHandle);
                localAgent = null;
                // now do the call all operation on the selected
                Iterator<Agent> localAgentsIter = localAgents.iterator();
                while (localAgentsIter.hasNext()) 
                { // go through each agent in place
                    localAgent = localAgentsIter.next();
                    //MASS.log("Agent Call All on index [" + place.index[0] + "][" + place.index[1] + "]" );
                    if (agentsOpSaveResults) 
                    { // multi argument w/ return val
                        //int agentId = localAgent.agentId;
                        // since agents might have migrated to other processes and the agent ID might not have been updated to reflect
                        // the true position of the agent in the bag, lets use the index of the agent in the bag instead
                        int agentId = MASS.getAgents(agentsOpHandle).getAgentIndexInBag(localAgent);
                        arg = null; // either stays null or there's an arg array
                                                // for it
                        if (agentsOpCallAllArgs != null && agentsOpCallAllArgs.length > agentId) 
                        {
                            arg = agentsOpCallAllArgs[agentId];
                        }                 
                        result = localAgent.callMethod(agentsOpFunctionId, arg);
                        if(agentsOpCallAllResults != null)
                        {
                            synchronized(agentsOpCallAllResults)
                            {
                                //MASS.log("agent id = " + agentId + " result size =  " + agentsOpCallAllResults.length);
                                agentsOpCallAllResults[agentId] = result; // save result
                            }
                        }
                    } 
                    else 
                    { // single argument w/o return val
                        localAgent.callMethod(agentsOpFunctionId, agentsOpCallAllArg);
                    }
                }
            }
        }
        barrier();
    }

    /*
     * helper function to grab each agent of a particular agents from a place's
     * agents
     */
    private static Vector<Agent> getAllAgentWithHandle(Vector<Agent> agents, int agentsHandle) 
    {
        Iterator<Agent> itAgent = agents.iterator();
        Vector<Agent> vec = new Vector<Agent>();
        Agent agent;
        while (itAgent.hasNext()) 
        {
            agent = itAgent.next();
            if (agent.agentsHandle == agentsHandle)
                vec.add(agent);
        }
        return vec;
    }

    /**
     * Called by the MASS.Agents class to set up for the Manage All operation. At the
     * end, the MThreads waiting on STATUS are notified (triggering them to call
     * agentsManageAllPerThread) and then agentsManageAllPerThread is called
     * (allowing the main thread to follow the same path as the MThreads).
     */
    static void agentsManageAll(int handle, Places places) 
    {
        synchronized (OPERATION_LOCK) 
        {
            // validate args
            boolean validArgs = true;
            if (getAgents(handle) == null || places == null) 
            {
                validArgs = false;
            }
            if (validArgs && INITIALIZED) 
            {
                cleanAgentsOpVariables();
                // set up the variables
                agentsOpHandle = handle;
                agentsOpPlaces = places;
                if(MASS.myPid == 0 )
                {
                    Message m = new Message();
                    m.createAgentManageAllMessage(Constants.AGENTS_MANAGE_ALL, handle);
                    for(MNode node : mNodes)
                    {
                        node.sendMessage(m);
                    }                    
                }               
                // change status and notify
                synchronized (STATUS) 
                {
                    STATUS[0] = STATUS_AGENTS_MANAGE_ALL;
                    STATUS.notifyAll();
                }
                //RemoteAgentMigrateHostNames.clear();
                agentsManageAllPerThread();
                //redistributeHostNamesForAgentMigrate();
                processRemoteAgentRequest();
            }
        }
    }

    /**
     * Called by all threads involved in a Manage All request for the MASS.Agents
     * class. Processes the sleep, wakeup, spawn, migrate, and kill operations
     * for an MASS.Agents object.
     * 
     * The Manage All operation is done in this order at each place:
     * 
     * 1 collect all agents set to respond to a wakeupAll call, clearing
     * eventId's for those that will fire wakeup 2. nothing/deleted --- barrier
     * --- 3 call wakeup() on all the collected agents --- barrier --- 4 do
     * spawn requests --- barrier --- 5 kill or migrate
     * 
     * 
     * There are different policies that could work for this ordering. This
     * ordering is meant to: -gives every agent a chance to respond to events
     * scheduled when the agent is present at the place. -makes sure a wakeupAll
     * call affects only the agents present at the place when the call is made.
     * 
     * Drawbacks to this ordering: -wakeupAll call could be used to change the
     * migrate/spawn/kill status of an agent -a spawn request can make use of
     * the constructor to execute code and change kill/migrate status
     * 
     * Strategies for avoiding these drawbacks always seemed to lead to
     * unsettling scenarios. For example, if a migrate status is set one way,
     * and we try to conserve that status through the manageAll call, what will
     * we do if a wakeup call or spawn constructor changes the migrate status?
     * Will the agent migrate to its first destination with what is essentially
     * a ticket to the second destination? This seems unwanted. There is also
     * the questions of in which place the migrating agent should respond to
     * wakeupAll events. Ways of enforcing a manageAll adhering to a specific
     * snapshot of the agents and place seemed cumbersome and contrary to what a
     * programmer might well want to do. Also, with sleeping it seemed
     * reasonable to have an agent put to sleep before a manageAll call wake up
     * during the manageAll call. The alternative would have been to not let the
     * agent respond and have it still be asleep after the manageAll call. This
     * would have meant that an agent couldn't respond to a wakeup event at
     * every manageAll call, complicating matters a great deal for the
     * programmer.
     * 
     */

    static void agentsManageAllPerThread() 
    {
        // validate args
        boolean validArgs = true;
        Agents agents = null;
        if (agentsOpHandle < 0 || agentsOpPlaces == null) 
        {
            validArgs = false;
        } 
        else 
        {
            agents = getAgents(agentsOpHandle);
            if (agents == null) 
            {
                validArgs = false;
            }
        }
        if (validArgs) 
        {
            // find all the agents that will wake up
            agentsManageAllPerThreadOrganizeForWakeUp();
            threadsCatchUpBarrier();
            agentsManageAllPerThreadDoWakeUp();
            // find all spawners and queue up spawns
            agentsManageAllPerThreadStartSpawn();
            threadsCatchUpBarrier();
            // find all kill or queue up for migration
            agentsManageAllPerThreadKillOrStartMigrate();
            threadsCatchUpBarrier();
            // add spawned and migrating agents to place
            agentsManageAllPerThreadFinishSpawnAndMigrate();
            barrier();
        }
    }

    private static void agentsManageAllPerThreadOrganizeForWakeUp( ) 
    {
        Places.Iterator placesIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));
        if (placesIter != null) 
        {
            Place place = null; // reuseable variable
            Agent agent = null;
            // cycle through the places for this thread
            while (placesIter.hasNext()) 
            {
                place = placesIter.next();
                agentsManageAllPerThreadCleanEventFiringVectors(place);
                boolean[] eventsToFire = place.getAndResetEventsToFire();
                Vector<Agent> vecAgent = place.agents;
                // go through all the agents at this place
                for (int i = 0; i < vecAgent.size(); i++) 
                {
                    agent = vecAgent.get(i);
                    if (agent == null) 
                    {
                        vecAgent.remove(i--);// shouldn't happen, but just in case
                    } 
                    else if (agent.agentsHandle == agentsOpHandle) 
                    { // if this is one of the current MASS.Agents, see if an event is firing that wakes up this agent
                        synchronized (agent.schedulingSleep) 
                        {
                            scheduleAgentForFiring(eventsToFire, place, agent);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks an agent to see if it's waiting on an event set to fire at the
     * place, and if the agent is waiting, it is placed in a queue of agents on
     * which wakeup() will be called
     */
    private static void scheduleAgentForFiring(boolean[] eventsToFire, Place place, Agent agent) 
    {
        if (1 <= agent.eventId && agent.eventId <= 10 && eventsToFire[agent.eventId] == true) 
        {
            switch (agent.eventId) 
            {
            case 1:
                place.firingEvent1.add(agent);
                break;
            case 2:
                place.firingEvent2.add(agent);
                break;
            case 3:
                place.firingEvent3.add(agent);
                break;
            case 4:
                place.firingEvent4.add(agent);
                break;
            case 5:
                place.firingEvent5.add(agent);
                break;
            case 6:
                place.firingEvent6.add(agent);
                break;
            case 7:
                place.firingEvent7.add(agent);
                break;
            case 8:
                place.firingEvent8.add(agent);
                break;
            case 9:
                place.firingEvent9.add(agent);
                break;
            case 10:
                place.firingEvent10.add(agent);
                break;
            default:
                break;
            }
        }
    }

    private static void agentsManageAllPerThreadCleanEventFiringVectors(Place place) 
    {
        place.firingEvent1.clear();
        place.firingEvent2.clear();
        place.firingEvent3.clear();
        place.firingEvent4.clear();
        place.firingEvent5.clear();
        place.firingEvent6.clear();
        place.firingEvent7.clear();
        place.firingEvent8.clear();
        place.firingEvent9.clear();
        place.firingEvent10.clear();
    }

    private static void agentsManageAllPerThreadDoWakeUp() 
    {
        Places.Iterator placesIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));
        if (placesIter != null) 
        { // null when not enough MASS.Place objects for thread
            Place place = null; // reuseable variable
            Agent agent = null;
            Vector<Agent> firingVec;
            Iterator<Agent> iter;
            // cycle through the places for this thread
            while (placesIter.hasNext())
            {
                place = placesIter.next();
                for (int i = 1; i < 11; i++) 
                {
                    firingVec = null;
                    switch (i) 
                    {
                    case 1:
                        firingVec = place.firingEvent1;
                        break;
                    case 2:
                        firingVec = place.firingEvent2;
                        break;
                    case 3:
                        firingVec = place.firingEvent3;
                        break;
                    case 4:
                        firingVec = place.firingEvent4;
                        break;
                    case 5:
                        firingVec = place.firingEvent5;
                        break;
                    case 6:
                        firingVec = place.firingEvent6;
                        break;
                    case 7:
                        firingVec = place.firingEvent7;
                        break;
                    case 8:
                        firingVec = place.firingEvent8;
                        break;
                    case 9:
                        firingVec = place.firingEvent9;
                        break;
                    case 10:
                        firingVec = place.firingEvent10;
                        break;
                    default:
                        break;
                    }
                    if (firingVec != null && !firingVec.isEmpty()) 
                    {
                        iter = firingVec.iterator();
                        while (iter.hasNext()) 
                        {
                            agent = iter.next();
                            synchronized (agent.schedulingSleep) 
                            {
                                agent.eventId = 0;
                            }
                            agent.wakeup(i); // synchronization leak between eventId = 0 and wakeup
                        }
                        firingVec.clear();
                    }
                }
            }
        }
    }

    private static void agentsManageAllPerThreadStartSpawn() 
    {
        Places.Iterator placesIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));
        if (placesIter != null) 
        {
            Place place = null; // reuseable variables
            Agent agent = null;
            Object[] arguments;
            int newChildren;
            // cycle through the places for this thread
            while (placesIter.hasNext()) 
            {
                place = placesIter.next();
                Vector<Agent> vecAgent = place.agents;
                // go through all the agents at this place
                for (int i = 0; i < vecAgent.size(); i++) 
                {
                    agent = vecAgent.get(i);
                    if (agent == null) 
                    {
                        vecAgent.remove(i--);// shouldn't happen, but just in case
                    }
                    else if (agent.agentsHandle == agentsOpHandle) 
                    { // if this is one of the current MASS agent,
                        // gather spawn-related variables and reset them at
                        // agent
                        synchronized (agent.spawnLock) 
                        {
                            arguments = agent.arguments;
                            agent.arguments = null;
                            newChildren = agent.newChildren;
                            agent.newChildren = 0;
                        }
                        // see if there is a spawn request and make the new
                        // agents
                        if (newChildren > 0) 
                        {
                            for (int spawn = 0; spawn < agent.newChildren; spawn++) 
                            {
                                Object arg = (arguments == null || spawn > arguments.length - 1) ? null : arguments[spawn];
                                Agent child = agentsOpAgents.createAgent(arg, agent.agentId);
                                if (child != null) 
                                {
                                    child.index = agent.index.clone();
                                    child.place = agent.place;
                                    synchronized (place.immigrants) 
                                    {
                                        place.immigrants.add(child);
                                    }
                                } 
                                else 
                                {
                                    break; // failed to get a new agent, so stop
                                                    // spawning
                                }
                            }
                        }
                        // ----------------------------------- end spawn
                    }
                }
            }
        }
    }

    private static void agentsManageAllPerThreadKillOrStartMigrate() 
    {
        Places.Iterator placesIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));        
        if (placesIter != null) 
        { // null when not enough MASS.Place objects for thread
            Place place = null; // reuseable variable
            Agent agent = null;
            // cycle through the places for this thread
            while (placesIter.hasNext()) 
            {
                place = placesIter.next();
                Vector<Agent> vecAgent = place.agents;
                synchronized (place.agents) 
                {
                    // go through all the agents at this place
                    for (int i = 0; i < vecAgent.size(); i++) 
                    {
                        agent = vecAgent.get(i);
                        // handle the kill
                        if (agent == null) 
                        {
                            vecAgent.remove(i--);// null shouldn't happen, but
                                                                                // just in case
                        } 
                        else if (agent.agentsHandle == agentsOpHandle) 
                        { // if this is one of the current MASS agents
                            // migration time
                            Place newPlace = null;
                            // translate local linear index number to the global index number
                            int globalLinearIndex = Places.getGlobalLinearIndexFromGlobalArrayIndex(agent.index, place.size);
                            //MASS.log("Performing exchange all on index  " + globalLinearIndex);
                            int destinationLocalLinearIndex = Places.getLocalLinearIndexFromGlobalLinearIndex(globalLinearIndex);
                            try 
                            {

                                if(destinationLocalLinearIndex >= 0 && destinationLocalLinearIndex < agentsOpPlaces.length())
                                {
                                    newPlace = MASS.getPlaces(agent.placesHandle).get(destinationLocalLinearIndex);
                                }
                                else
                                {
                                    // remote local agent, it is migrating to a remote host
                                    vecAgent.remove(i--);
                                    MASS.getAgents(agentsOpHandle).removeAgent(agent);
                                    // remote, package agents up 
                                    RemoteAgentRequest agentReq = new RemoteAgentRequest(globalLinearIndex, agent);
                                     // get the host name
                                    String destHostName = ( ea_places != null ) ? eb_places.getHostname(globalLinearIndex)
																				: ea_places.getHostname(globalLinearIndex); 
                                    /*synchronized(RemoteAgentMigrateHostNames)
                                    {
                                        if(!RemoteAgentMigrateHostNames.contains(destHostName))
                                            RemoteAgentMigrateHostNames.add(destHostName);
                                    }*/
                                    //MASS.log("Agent migrate RemoteCall - index is : " + globalLinearIndex + " destination: " + destHostName);
                                    synchronized(remoteAgentRequestMap)
                                    {
                                        if(remoteAgentRequestMap.get(destHostName) == null)
                                        {
                                            ArrayList<RemoteAgentRequest> requests = new ArrayList<RemoteAgentRequest>();
                                            requests.add(agentReq);
                                            remoteAgentRequestMap.put(destHostName, requests);
                                        }
                                        else
                                        {
                                            remoteAgentRequestMap.get(destHostName).add(agentReq);
                                        }    
                                    }                                           

                                }
                            } 
                            catch (Exception e) 
                            {
                                newPlace = null;
                            } 
                            finally 
                            {
                                if (newPlace != null) 
                                { // agent will
                                                                                // have a new
                                                                                // home!!
                                    vecAgent.remove(i--);
                                    agent.place = newPlace;
                                    agent.index = newPlace.index.clone();// shouldn't be
                                                                            // necessary,
                                                                            // but to make
                                                                            // sure
                                    synchronized (newPlace.immigrants) 
                                    {
                                        newPlace.immigrants.add(agent); // agent
                                                                                                            // migrates
                                    }
                                }
                            }
                        }                            
                            // ****************
                   }
                }
            }
        }

    }

    /**
     * used during manageAll to take the agents that are new spawns and
     * immigrants and adds them to the agents Vector and then clears the
     * immigrants vector
     */
    private static void agentsManageAllPerThreadFinishSpawnAndMigrate() 
    {
        Places.Iterator placesIter = agentsOpPlaces.iterator(getLocalRange(agentsOpPlaces));
        if (placesIter != null) 
        { // null when not enough MASS.Place objects for thread
            Place place = null; // reuseable variable
            while (placesIter.hasNext()) 
            {
                place = placesIter.next();
                if (place.immigrants.size() > 0) 
                {
                    place.agents.addAll(place.immigrants);
                    place.immigrants.clear();
                }
            }
        }
    }

    /*
     * Has each thread wait all threads have called this function. Does not
     * change the value of STATUS
     */
    private static void threadsCatchUpBarrier() 
    {
        synchronized (threadsCatchUpBarrierLockObject) 
        {
            threadsCatchUpBarrierCounter++;
            if (threadsCatchUpBarrierCounter == threads.size() + 1) 
            {
                threadsCatchUpBarrierCounter = 0;
                threadsCatchUpBarrierLockObject.notifyAll();
            } 
            else 
            {
                try 
                {
                    threadsCatchUpBarrierLockObject.wait();
                } 
                catch (InterruptedException e) 
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /*private static void redistributeHostNamesForAgentMigrate( )
    {
        if(systemSize == 1) return;
        
        MASS.log("======= BEGINNING DISTRIBUTION OF HOST NAMES FOR AGENT MIGRATE ===========");
        if(myPid == 0)
        {
            // receive and redistribute host names that other ranks need to contact to complete migrate
            HashMap<String, ArrayList<String>> hostMap = new HashMap<String, ArrayList<String>>();
            // populate the map first
            for(MNode node : mNodes)
            {
                ArrayList<String> hosts = new ArrayList<String>();
                hostMap.put(node.getHostName(), hosts);
            }
            hostMap.put(networkMap.get(0), new ArrayList<String>());
            // put master rank's destinations on to the host map
            for(int i = 0; i < RemoteAgentMigrateHostNames.size(); i++)
            {
                String destinationHost = RemoteAgentMigrateHostNames.get(i);
                hostMap.get(destinationHost).add(networkMap.get(0));
                hostMap.get(networkMap.get(0)).add(destinationHost);
            }  
            MASS.log("Master rank - Sending host names to slave nodes");
            for(MNode node : mNodes)
            {
                ArrayList<String> hostNames = node.receiveMessage().getAgentMigrateHostNames();
                MASS.log("Master rank - Received host names from  " + node.getHostName() + " with " + hostNames.size() + " entries ");
                for(int i = 0; i < hostNames.size(); i++)
                {
                    String destinationHost = hostNames.get(i);
                    hostMap.get(destinationHost).add(node.getHostName());
                    hostMap.get(node.getHostName()).add(destinationHost);
                }                       
            }
            RemoteAgentMigrateHostNames.clear();                    
            java.util.Map.Entry hosts = null;
            String destinationHostName = null;
            Set entrySet = hostMap.entrySet();
            Iterator it = entrySet.iterator();
            MASS.log("Master rank - Host Map has " + hostMap.size() + " entries");
            while(it.hasNext())
            {
                hosts = (java.util.Map.Entry)it.next();
                int pid = nodePidMap.get(hosts.getKey());
                ArrayList<String> destinations = (ArrayList<String>)hosts.getValue();
                if(pid == 0)
                {
                    RemoteAgentMigrateHostNames.addAll(destinations);
                }
                else
                {
                    Message msg = new Message();
                    msg.createHostNamePackageForAgentMigrate(destinations);
                    mNodes[pid - 1].sendMessage(msg);
                }
            }
        }
        else
        {
            MProcess.sendAndReceiveHostNames();
        }
        MASS.log("HostNames = ");
        for(int i = 0; i < RemoteAgentMigrateHostNames.size(); i++)
        {
            MASS.log(RemoteAgentMigrateHostNames.get(i));
        }
        MASS.log("======= FINISHED DISTRIBUTION OF HOST NAMES FOR AGENT MIGRATE ===========");
    }*/
    
    /*private static void processRemoteAgentRequest( )
    {   
        if(systemSize == 1) return;
                             
        java.util.Map.Entry exgRequest = null;
        String destinationHostName = null;
        ExchangeHelper helper = null;
        Message  exchangeP = null;
        ArrayList<RemoteAgentRequest> requestList = null;        
        
        synchronized( remoteAgentRequestMap )
        {     
            if(!remoteAgentRequestMap.isEmpty()) 
            {
                Set requestsEntrySet = remoteAgentRequestMap.entrySet();
                Iterator it = requestsEntrySet.iterator();
                if( it.hasNext())
                {
                    exgRequest = (java.util.Map.Entry)it.next();
                    destinationHostName = (String)exgRequest.getKey();
                    requestList = (ArrayList<RemoteAgentRequest>)exgRequest.getValue(); 
                    remoteAgentRequestMap.remove(destinationHostName);
                    MASS.log("Beginning remote agent migrate mt version - My local thread id =  " + getThreadPosition() + " requests remaining: " + remoteAgentRequestMap.size() +
                            " exchange destination: " + destinationHostName );
                    remoteAgentRequestMap.notifyAll();                   
                }               
            }
        }
     
        // perform exchange request processing
        startRemoteAgentRequest(destinationHostName, requestList);
        
        // If there are more requests than threads allocated, one thread needs to pick up the remaining requests
        synchronized( remoteAgentRequestMap ) 
        {     
            if(remoteAgentRequestMap.isEmpty())
                startRemoteAgentRequest(null, null);
            
            while(!remoteAgentRequestMap.isEmpty())
            {
                Set requestsEntrySet = remoteAgentRequestMap.entrySet();
                Iterator it = requestsEntrySet.iterator();
                if( it.hasNext())
                {
                    exgRequest = (java.util.Map.Entry)it.next();
                    destinationHostName = (String)exgRequest.getKey();
                    requestList = (ArrayList<RemoteAgentRequest>)exgRequest.getValue(); 
                    remoteAgentRequestMap.remove(destinationHostName);
                    MASS.log("Handling remaining remote exchange - My local thread id =  " + getThreadPosition() + " requests remaining: " + exchangeAllRequestMap.size() +
                            " exchange destination: " + destinationHostName );
                    startRemoteAgentRequest(destinationHostName, requestList);
                }               
            }
        }
    }*/
    
    private static void processRemoteAgentRequest( ) 
    {         
        if(systemSize == 1) return;
        sendRemoteAgentRequest( );
        receiveAndProcessRemoteAgentRequest( );
        /* try
        {
            if(myPid == 0)
            {
                MASS.log("Agent Migrate Complete: Awaiting aknowledgement from remote nodes.....");
                for(MNode node: MASS.mNodes)
                {
                    node.receiveMessage();
                }
                MASS.log("Agent Migrate Complete: Received all Acks");
            }
            else
            {
                MASS.log("Agent Migrate Complete: sending aknowledgement to master node.....");
                MProcess.sendAck();
            }
        } catch( Exception e) { MASS.logException(e); MASS.finish(); } */
    }

    private static void sendRemoteAgentRequest( )
    {
        java.util.Map.Entry req = null;
        String destinationHostName = null;
        ArrayList<RemoteAgentRequest> requestList = null; 
        Set requestsEntrySet = nodePidMap.entrySet();
        Iterator it = requestsEntrySet.iterator(); 
        
        // send request to all nodes
        while(it.hasNext())
        {
            req = (java.util.Map.Entry)it.next();
            destinationHostName = (String)req.getKey();
            Integer pid = (Integer)req.getValue();
            if(pid == myPid) continue;
            exchangeHelper[0].establishConnection(destinationHostName);
            Message  msg = new Message();
            requestList = remoteAgentRequestMap.get(destinationHostName);
            if(requestList != null)
            {
                //MASS.log("Starting remote exchange with :" + destinationHostName);

                msg.createAgentMigrateRequestMessage(requestList);

                exchangeHelper[0].sendRequest(destinationHostName, msg);   
                MASS.log("Sent Agent migrate Request to " + destinationHostName );
                remoteAgentRequestMap.remove(destinationHostName);
            }
            else
            {
                msg.createAcknowlegementMessage();
                exchangeHelper[0].sendRequest(destinationHostName, msg);
                MASS.log("Nothing to send for agent migrate.. sending ack to " + destinationHostName );
            }                      
        }        
    }
    
    private static void receiveAndProcessRemoteAgentRequest( )
    {
        java.util.Map.Entry req = null;
        String destinationHostName = null;
        ArrayList<RemoteAgentRequest> requestList = null; 
        Set requestsEntrySet = nodePidMap.entrySet();
        Iterator it = requestsEntrySet.iterator(); 
        
        // send request to all nodes
        while(it.hasNext())
        {
            req = (java.util.Map.Entry)it.next();
            destinationHostName = (String)req.getKey();
            Integer pid = (Integer)req.getValue();
            if(pid == myPid) continue;
            exchangeHelper[0].processAgentMigrateRequest(destinationHostName);                       
        }        
    }    
        
    /*private static void startRemoteAgentRequest(String destinationHostName, ArrayList<RemoteAgentRequest> requestList)
    {     
        if(destinationHostName == null)
        {
            synchronized(RemoteAgentMigrateHostNames)
            {
                if(RemoteAgentMigrateHostNames.size() > 0)
                {
                    destinationHostName = RemoteAgentMigrateHostNames.get(0);
                    RemoteAgentMigrateHostNames.remove(0);
                }
            }          
        }
        // if at this point it is still null, then this rank does not need to communicate with others
        if(destinationHostName == null) return;
        
        exchangeHelper[0].establishConnection(destinationHostName);
        Message  msg = new Message();
        if(requestList != null)
        {
            //MASS.log("Starting remote exchange with :" + destinationHostName);

            msg.createAgentMigrateRequestMessage(requestList);

            exchangeHelper[0].sendRequest(destinationHostName, msg);   
            MASS.log("Sent Agent migrate Request to " + destinationHostName );
        }
        else
        {
            msg.createAcknowlegementMessage();
            exchangeHelper[0].sendRequest(destinationHostName, msg);
            MASS.log("Nothing to send for agent migrate.. sending ack to " + destinationHostName );
        }
        exchangeHelper[0].processAgentMigrateRequest(destinationHostName);  
        
    }*/
  
    static void doRemoteAgentMigrate(ArrayList<RemoteAgentRequest> requestList)
    {
        if(requestList == null) 
        {
            MASS.log("Nothing to process for agent migrate.. " );
            return;
        }
        
        Place destination;
        Agent agent;
        for(RemoteAgentRequest request : requestList)
        {            
            destination = agentsOpPlaces.get(Places.getLocalLinearIndexFromGlobalLinearIndex(request.getDestinationGlobalLinearIndex()));
            agent = Agents.createAgentForMigrate(request);    
            agent.place = destination;
            agent.index = destination.index.clone();// shouldn't be
                                                    // necessary,
                                                    // but to make
                                                    // sure
            //destination.agents.add(agent);
            synchronized (destination.agents) 
            {
                destination.agents.add(agent); // agent
                                                   // migrates
            }
        }
    }
    
    // not a multithreaded method
    static void agentsExchangeAll(int nativeAgentsHandle, int foreignAgentsHandle, Places places, int functionId) 
    {
        synchronized (OPERATION_LOCK) 
        {
            boolean validArgs = true;
            if (getAgents(nativeAgentsHandle) == null || getAgents(foreignAgentsHandle) == null || places == null) 
            {
                validArgs = false;
            }
            if (validArgs && INITIALIZED) 
            {
                // iterate through each MASS.Place
                // collect all the agentagent to call in a Vector
                // iterate through agentagent
                // make a vector for the results in each agent
                // do all the calls using the agentagent outMessages and store
                // the results
                // set the Vector to an array and store as inMessages of agent
            }
        }
    }

    // =========================================================================

    private static void cleanAgentsOpVariables() 
    {
        threadsCatchUpBarrierCounter = 0;
        agentsOpAgents = null;
        agentsOpPlaces = null;
        agentsOpCallAllArg = null;
        agentsOpCallAllArgs = null;
        agentsOpCallAllResults = null;
        agentsOpFunctionId = -1;
        agentsOpHandle = -1;
        // agentsOpForeignHandle = -1;
        agentsOpSaveResults = false;
    }
    
     /**
     * Logs a message to the appropriate stream, regardless of Process location.
     * Remote Process must use logger file while process 0 may use system output.
     * This helper allows for shared code across Process 0 and all remote nodes 
     * in the MASS.Places and MASS.Agents classes.
     * @param message a message to be logged
     */
    public static void log( String message ) {
        if( myPid != 0 ){
            MProcess.log( message );
        } else {
            System.err.println( message );
        }
    }
    
    public static int getPid(){
		return myPid;
	}
    
    static void logException( Exception e) {
        if(myPid != 0)
        {
            MProcess.logException(e);
        }
        else
        {
            e.printStackTrace();
        }
    }

     /**
     * Prints a message to the MASS_result folder.  Each remote process will create a result file with 
     * PID_#_hostname_result.txt as the file name, while the master node prints to the system.out
     * @param message a message to be printed
     */
    public static void printResult( String message ) {
        if( myPid != 0 ){
            MProcess.printResult( message );
        } else {
            System.out.println( message );
        }
    }    
    
    protected static boolean initializeThreads(int childThrds)
    {
        if (!INITIALIZED) 
        {
            placesHandles = new Hashtable<Integer, Places>();
            agentsHandles = new Hashtable<Integer, Agents>();

            int cores = childThrds == 0 ? getCores() : childThrds;
            threadsRunning[0] = 1;
            threads = new Vector<Thread>();
            while (threadsRunning[0] < cores) 
            { // cores - 1 to account for
              // main thread
                threadsRunning[0]++;
                threads.add(new Mthread());
            }
            Iterator<Thread> iter = threads.iterator();
            while (iter.hasNext()) 
            { // start the threads
                try 
                {
                        iter.next().start();
                } 
                catch (NoSuchElementException e) 
                {
                        System.err.println("Error: MASS.Mthread not found.");
                        e.printStackTrace();
                }
            }
            INITIALIZED = true;
            STATUS[0] = STATUS_READY;
            
            MASS.log("Initialized threads - # " + cores);
        } 
        else 
        {
            
            System.err.println("Error: the MASS.MASS environment is already initialized.");
            return false;
        }
        
        return true;
    }

}
