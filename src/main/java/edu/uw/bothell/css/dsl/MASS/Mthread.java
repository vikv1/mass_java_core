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

public class Mthread extends Thread {

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
		STATUS_AGENTSCALLALL_ASYNC // 6
	}

    private static Object lock;
    private static int barrierCount;
    private static STATUS_TYPE status;
    private static int threadCreated;
    private static int agentBagSize;
    private static int barrierPhases;
    private int tid;                  // this mthread's id

    public Mthread( int id ) {
    	this.tid = id;
    }

	public static void barrierThreads( int tid ) {

    	synchronized( lock ) {
    		
    		if ( ++barrierCount < MASS_base.getThreads().length ) {
    			
    			if( MASS.isConsoleLoggingEnabled() )
    				MASS_base.log( "tid[" + tid + 
    						"] waiting: barrier = " + barrierPhases );
    			
    			try {
    				lock.wait( );
    			} 
    			catch( Exception e ) {
            MASS.logException(null, e);
    			}
    		
    		} 
    		else {
    			
    			barrierCount = 0;
    			status = STATUS_TYPE.STATUS_READY;
    			if( MASS.isConsoleLoggingEnabled() ) 
    				MASS_base.log( "tid[" + tid + "] woke up all: barrier = " 
    						+ barrierPhases );
    			barrierPhases++;
    			lock.notifyAll( );
    		
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
    	if ( MASS.isConsoleLoggingEnabled() )
    		MASS_base.log( "Mthread[" + tid + "] invoked" );

    	// the following variables are used to call callAll( )
    	Places_base places = null;
    	Places_base destinationPlaces = null;
    	Agents_base agents = null;

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
    			if(MASS.isConsoleLoggingEnabled() )
    				MASS_base.log( "Mthread[" + tid + "] woken up " + status );
    		
    		}
    		if(status == Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL_ASYNC) {
          agents = MASS_base.getCurrentAgents( );
          agents.callAllAsync(tid);
    		  }
    		else {
    		// perform each task
    		switch( status ) {
    		
    		case STATUS_READY:
    			
    			if (MASS.isConsoleLoggingEnabled())
    				MASS_base.log( "Mthread reached STATUS_READY in switch" );
    			System.exit( -1 );
    			break;
    		
    		case STATUS_TERMINATE:
    			
    			running = false;
    			break;
    		
    		case STATUS_CALLALL:
    			
    			places = MASS_base.getCurrentPlaces( );
    			functionId = MASS_base.getCurrentFunctionId( );
    			argument = MASS_base.getCurrentArgument( );
    			msgType = MASS_base.getCurrentMsgType( );

    			if ( MASS.isConsoleLoggingEnabled() )
    				MASS_base.log( "Mthread[" +tid + "] works on CALLALL:" +
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
    			
    			if ( MASS.isConsoleLoggingEnabled() )
    				MASS_base.log( "Mthread[" + tid + 
    						"] works on EXCHANGEALL" );

    			places = MASS_base.getCurrentPlaces( );
    			functionId = MASS_base.getCurrentFunctionId( );
    			destinationPlaces = MASS_base.getDestinationPlaces( );
    			//destinations = MASS_base.getCurrentDestinations( );

    			//		places.exchangeAll( destinationPlaces, functionId, 
    			//			    destinations, tid );
    			places.exchangeAll( destinationPlaces, functionId, tid );
    			break;

    		case STATUS_AGENTSCALLALL:
    			
    			agents = MASS_base.getCurrentAgents( );
    			functionId = MASS_base.getCurrentFunctionId( );
    			argument = MASS_base.getCurrentArgument( );
    			msgType = MASS_base.getCurrentMsgType( );

    			if ( MASS.isConsoleLoggingEnabled() )
    				MASS_base.log( "Mthread[" + tid + 
    						"] works on AGENST_CALLALL:" +
    						" agents = " + agents +
    						" functionId = " + functionId +
    						" argument = " + argument +
    						" msgType = " + msgType );

    			if( msgType==Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT) {
    				
    				//System.err.println( "Mthread[" + tid + 
    				//			"] call all agent void object" );

    				agents.callAll( functionId, argument, tid );
    			
    			}
    			else {
    				
    				//System.err.println( "Mthread[" + tid + 
    				//			"] call all agents return object" );

    				agents.callAll( functionId, (Object[])argument, tid) ;
    			
    			}
    			break;

    		case STATUS_MANAGEALL:
    			
    			//Get agents to be called with Manageall
    			agents = MASS_base.getCurrentAgents( );

    			//Send logging message
    			if ( MASS.isConsoleLoggingEnabled() )
    				MASS_base.log( "Mthread[" + tid + "] works on MANAGEALL:" +
    						" agents = " + agents );

    			//Sent message for manageall
    			agents.manageAll( tid );

    			break;
    		}
    		// barrier
    		barrierThreads( tid );
    	}
    	}
      }catch(Throwable e) {
        MASS_base.logException("Thread " + tid + " fails", e);
      }
    
    	// last message
    	if (MASS.isConsoleLoggingEnabled())
    		MASS_base.log( "Mthread[" + tid + "] terminated" );
    
    }

	public static int getAgentBagSize() {
		return agentBagSize;
	}

	public static void setAgentBagSize(int agentBagSize) {
		Mthread.agentBagSize = agentBagSize;
	}

	public static Object getLock() {
		return lock;
	}

	public static void setLock(Object lock) {
		Mthread.lock = lock;
	}

	public static int getThreadCreated() {
		return threadCreated;
	}

	public static void setThreadCreated(int threadCreated) {
		Mthread.threadCreated = threadCreated;
	}

}