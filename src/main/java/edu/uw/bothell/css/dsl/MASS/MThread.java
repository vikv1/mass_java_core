/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

public class MThread extends Thread {

	/**
	  * Status Type
	  * A list of possible statuses
	  */
	public enum STATUS_TYPE { 
		STATUS_READY,              // 0
		STATUS_TERMINATE,          // 1
		STATUS_CALLALL,            // 2
		STATUS_EXCHANGEALL,        // 3
		STATUS_AGENTSCALLALL,      // 4
		STATUS_MANAGEALL,          // 5
	}

    private static Object lock;
    private static int barrierCount;
    private static STATUS_TYPE status;
    private static int threadCreated;
    private static int agentBagSize;
    private static int barrierPhases;
    private int tid;                  // this mthread's id

    public MThread( int id ) {
    	this.tid = id;
    }

	public static void barrierThreads( int tid ) {

    	synchronized( lock ) {
    		
    		if ( ++barrierCount < MASSBase.getThreads().length ) {
    			
    			MASSBase.getLogger().debug( "tid[" + tid + "] waiting: barrier = " + barrierPhases );
    			
    			try {
    				lock.wait( );
    			} 
    			catch( Exception e ) {
    				MASSBase.getLogger().error("Unknown exception thrown in barrierThreads", e);
    			}
    		
    		} 
    		else {
    			
    			barrierCount = 0;
    			status = STATUS_TYPE.STATUS_READY;
    			MASSBase.getLogger().debug( "tid[" + tid + "] woke up all: barrier = " + barrierPhases );
    			barrierPhases++;
    			MASSBase.getLogger().debug("Attempting to notifyAll...");
    			lock.notifyAll( );
    			MASSBase.getLogger().debug("notifyAll success!");
    		
    		}
    	
    	}
    
    }

	public static void init( ) {

		lock = new Object( );
		status = STATUS_TYPE.STATUS_READY;
		barrierCount = 0;
		barrierPhases = 0;
	
	}

    public static void resumeThreads( STATUS_TYPE new_status ) {

    	synchronized( lock ) {
    		status = new_status;
    		lock.notifyAll( );
    	}
    
    }
    
    public void run( ) {
      try {
    	 // Initialization portion
    	synchronized( lock ) {
    		threadCreated = tid;  // to inform MASS_base of my invocation
    	}

    	// breath message
    	MASSBase.getLogger().debug( "Mthread[{}] invoked", tid );

    	// the following variables are used to call callAll( )
    	PlacesBase places = null;
    	PlacesBase destinationPlaces = null;
    	AgentsBase agents = null;

    	int functionId = 0;
    	Object argument = null;
    	Message.ACTION_TYPE msgType = Message.ACTION_TYPE.EMPTY;
    	//Vector<int[]> destinations = null;

    	// END Initialization
    	boolean running = true;
    	while ( running ) {
    		
    		// wait for a new command
    		synchronized( lock ) {
    			
    			if ( status == STATUS_TYPE.STATUS_READY ) {
    					lock.wait( );
    			}

    			// wake-up message
    			MASSBase.getLogger().debug( "Mthread[" + tid + "] woken up " + status );
    		
    		}

    		// perform each task
    		switch( status ) {
    		
    		case STATUS_READY:
    			
    			MASSBase.getLogger().error( "Mthread reached STATUS_READY in switch" );
    			System.exit( -1 );
    			break;
    		
    		case STATUS_TERMINATE:
    			
    			running = false;
    			break;
    		
    		case STATUS_CALLALL:
    			
    			places = MASSBase.getCurrentPlacesBase( );
    			functionId = MASSBase.getCurrentFunctionId( );
    			argument = MASSBase.getCurrentArgument( );
    			msgType = MASSBase.getCurrentMsgType( );

    			MASSBase.getLogger().debug( "Mthread[" +tid + "] works on CALLALL:" +
    						" placese = " + places +
    						" functionId = " + functionId +
    						" argument = " + argument +
    						" msgType = " + msgType );

    			if ( msgType == Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT ) {
    				places.callAll( functionId, argument, tid );
    			}
    			else {
    				places.callAll( functionId, (Object[])argument, 
    						((Object[])argument).length, tid );
    			}
    			break;

    		case STATUS_EXCHANGEALL:
    			
    			MASSBase.getLogger().debug( "Mthread[{}] works on EXCHANGEALL", tid );

    			places = MASSBase.getCurrentPlacesBase( );
    			functionId = MASSBase.getCurrentFunctionId( );
    			destinationPlaces = MASSBase.getDestinationPlaces( );
    			places.exchangeAll( destinationPlaces, functionId, tid );
    			break;

    		case STATUS_AGENTSCALLALL:
    			
    			agents = MASSBase.getCurrentAgentsBase( );
    			functionId = MASSBase.getCurrentFunctionId( );
    			argument = MASSBase.getCurrentArgument( );
    			msgType = MASSBase.getCurrentMsgType( );

    			MASSBase.getLogger().debug( "Mthread[" + tid + 
    						"] works on AGENST_CALLALL:" +
    						" agents = " + agents +
    						" functionId = " + functionId +
    						" argument = " + argument +
    						" msgType = " + msgType );

    			if( msgType==Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT) {
    				
    				agents.callAll( functionId, argument, tid );
    			
    			}
    			else {
    				
    				agents.callAll( functionId, (Object[])argument, tid) ;
    			
    			}
    			break;

    		case STATUS_MANAGEALL:
    			
    			//Get agents to be called with Manageall
    			agents = MASSBase.getCurrentAgentsBase( );

    			//Send logging message
    			MASSBase.getLogger().debug( "Mthread[" + tid + "] works on MANAGEALL: agents = " + agents );

    			//Sent message for manageall
    			agents.manageAll( tid );

    			break;
    		}
    		// barrier
    		barrierThreads( tid );
    	}
      }catch(Exception e) {
    	  MASSBase.getLogger().error("Thread {} fails", tid, e);
      }
    
    	// last message
      MASSBase.getLogger().debug( "Mthread[{}] terminated", tid );
    
    }

	public static int getAgentBagSize() {
		return agentBagSize;
	}

	public static void setAgentBagSize(int agentBagSize) {
		MThread.agentBagSize = agentBagSize;
	}

	public static Object getLock() {
		return lock;
	}

	public static void setLock(Object lock) {
		MThread.lock = lock;
	}

	public static int getThreadCreated() {
		return threadCreated;
	}

	public static void setThreadCreated(int threadCreated) {
		MThread.threadCreated = threadCreated;
	}

}