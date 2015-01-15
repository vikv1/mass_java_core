package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Agents extends Agents_base implements Serializable {

	//Used to toggle comments from Places_base.java
	private static final boolean printOutput = false;
	//private static final boolean printOutput = true;

	private int[] localAgents; // localAgents[i] = # agents in rank[i]
	private int total;

	public Agents( int handle, String className, Object argument, Places places, int initPopulation ) {
		
		super( handle, className, argument, places.getHandle( ), initPopulation );
		localAgents = new int[MASS_base.getSystemSize()];
		init_master( argument );
	
	}

	@SuppressWarnings("unused")
	Object ca_setup( int functionId, Object argument, Message.ACTION_TYPE type ) {

		// calculate the total number of agents
		total = 0;
		for ( int i = 0; i < MASS_base.getSystemSize(); i++ )
			total += localAgents[i];

		// send a AGENTS_CALL_ALL message to each slave
		Message m = null;
		for ( int i = 0; i < MASS.getRemoteNodes().size( ); i++ ) {
			
			// create a message
			if ( type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT )
				
				m = new Message( type, this.getHandle(), functionId, argument );
			
			else {
				
				// calculate argument position
				int arg_pos = 0;
				for ( int dest = 0; dest <= i; dest++ ) {
					arg_pos += localAgents[dest];

					if ( printOutput == true )
						System.err.println( "Agents.callAll: calc arg_pos = " 
								+ arg_pos + 
								" localAgents[" + ( dest + 1) + 
								"] = " + localAgents[dest + 1] );
				
				}

				Object[] partitioned_argument = 
						new Object[localAgents[i + 1]];
				
				System.arraycopy( (Object[])argument, arg_pos, 
						partitioned_argument, 0,
						localAgents[i + 1] );

				m = new Message( type, this.getHandle(), functionId,
						partitioned_argument );

				if ( printOutput == true )
					System.err.println( "Agents.callAll: to rank[" + (i + 1) +
							"] arg_pos = " + arg_pos );
			
			}

			// send it
			MASS.getRemoteNodes().get(i).sendMessage( m );
			
			if ( printOutput == true ) {
				
				System.err.println( "AGENTS_CALL_ALL " + m.getAction( ) +
						" sent to " + i );

				System.err.println( "Bag Size is: " + 
						MASS_base.getAgentsMap().
						get( new Integer(getHandle()) ).
						getAgents().size_unreduced() );
			
			}

		}

		Mthread.agentBagSize = MASS_base.getAgentsMap().
				get( new Integer( getHandle() ) ).getAgents().size_unreduced( );

		//Check for correct behavior post-Agents_base implementation
		// retrieve the corresponding agents
		
		// shared between agents
		MASS_base.setCurrentAgents(this);
		MASS_base.setCurrentFunctionId(functionId);
		MASS_base.setCurrentArgument(argument);
		MASS_base.setCurrentMsgType(type);

		if (type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT) {
			MASS_base.setCurrentReturns(null);
		} else {
			MASS_base.setCurrentReturns(new Object[ total ]); // prepare an  entire return space
		}

		// resume threads
		if ( printOutput == true ) {
			
			MASS_base.log( "MASS_base.currentgAgents = " +
					MASS_base.getCurrentAgents() );
			
			MASS_base.log( "MASS_base.getCurrentgAgents = " +
					MASS_base.getCurrentAgents( ) );
		
		}

		Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_AGENTSCALLALL );

		// callall implementatioin
		if ( type == Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT )
			super.callAll( functionId, argument, 0 ); //0 = main tid
		else
			super.callAll( functionId, (Object[])argument, 
					( (Object[])argument ).length, 0 );

		// confirm all threads are done with agents.callAll
		Mthread.barrierThreads( 0 );
		localAgents[0] = getLocalPopulation();

		// Synchronized with all slave processes by main thread.
		MASS.barrier_all_slaves( MASS_base.getCurrentReturns(), 0, 
				localAgents );

		total = 0;
		for ( int i = 0; i < MASS_base.getSystemSize(); i++ ) {
			
			total += localAgents[i];
			
			// for debugging
			if ( printOutput == true )
				System.err.println( "rank[" + i + 
						"]'s local agent population = " +
						localAgents[i] );
		
		}
		
		return MASS_base.getCurrentReturns();
	
	}

	public void callAll( int functionId ) {
		ca_setup( functionId, null, 
				Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT );
	}

	public void callAll( int functionId, Object argument ) {
		ca_setup( functionId, argument,
				Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT );
	}

	public Object callAll( int functionId, Object[] argument ) {
		return ca_setup( functionId, argument,
				Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT );
	}

	@SuppressWarnings("unused")
	public void init_master( Object argument ) {
		
		// check if MASS_base.hosts is empty (i.e., Places not yet created)
		if ( MASS_base.getHosts().isEmpty( ) ) {
			System.err.println( "Agents(" + getClassName() + 
					") can't be created without Places!!" );
			System.exit( -1 );
		}

		// create a new list for message
		Message m = new Message( Message.ACTION_TYPE.AGENTS_INITIALIZE, 
				getInitPopulation(), getHandle(), getPlacesHandle(), 
				getClassName(), argument );

		// send a AGENT_INITIALIZE message to each slave
		for (MNode node : MASS.getRemoteNodes()) {

			node.sendMessage( m );
			if ( printOutput == true ) MASS_base.log( "AGENT_INITIALIZE sent to " + node.getPid() );

		}

		// Synchronized with all slave processes
		MASS.barrier_all_slaves( localAgents );
		localAgents[0] = getLocalPopulation();

		total = 0;
		for ( int i = 0; i < MASS_base.getSystemSize(); i++ ) {
			
			total += localAgents[i];
			// for debugging

			if ( printOutput == true )
				System.err.println( "rank[" + i + 
						"]'s local agent population = " +
						localAgents[i] );
		
		}

		// register this agents in the places hash map
		MASS_base.getAgentsMap().put( new Integer( getHandle() ), this );
	
	}

	@SuppressWarnings("unused")
	public void ma_setup( ) {
		
		// send an AGENTS_MANAGE_ALL message to each slave
		Message m = null;
		for (MNode node : MASS.getRemoteNodes()) {

			// create a message
			m = new Message( Message.ACTION_TYPE.AGENTS_MANAGE_ALL, 
					this.getHandle(), 0 );

			//send it
			node.sendMessage( m );

			// MThread Update
			Mthread.agentBagSize = MASS_base.getAgentsMap().
					get( new Integer( getHandle() ) ).getAgents().size_unreduced( );

		
		}

		// retrieve the corresponding agents
		MASS_base.setCurrentAgents(this);
		MASS_base.setCurrentMsgType(Message.ACTION_TYPE.AGENTS_MANAGE_ALL);

		// resume threads
		Mthread.resumeThreads( Mthread.STATUS_TYPE.STATUS_MANAGEALL );

		// callall implementatioin
		super.manageAll( 0 ); // 0 = the main thread id

		// confirm all threads are done with agents.callAll
		Mthread.barrierThreads( 0 );

		// Synchronized with all slave processes
		MASS.barrier_all_slaves( localAgents );
		localAgents[0] = getLocalPopulation();

		total = 0;
		for ( int i = 0; i < MASS_base.getSystemSize(); i++ ) {
			
			total += localAgents[i];
			
			// for debugging
			if ( printOutput == true )
				System.err.println( "rank[" + i + 
						"]'s local agent population = "
						+ localAgents[i] );
		
		}
	
	}

	public void manageAll( ) {
		ma_setup( );
	}

	public int nAgents( ) {
		
		int nAgents = 0;
		for ( int i = 0; i < MASS_base.getSystemSize(); i++ )
			nAgents += localAgents[i];
		
		return nAgents;
	
	}

}