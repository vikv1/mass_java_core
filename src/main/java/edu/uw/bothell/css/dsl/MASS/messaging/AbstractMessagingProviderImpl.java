/*

 	MASS Java Software License
	© 2012-2021 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2021 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.messaging;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public abstract class AbstractMessagingProviderImpl implements MessagingProvider {

	private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000;
	
	// references to all Agents and Places located on this node - for delivery of messages
	private Map< Integer, Agent > agents = new ConcurrentHashMap<>();
	private Map< Integer, Place > places = new ConcurrentHashMap<>();
	
	// a collection of received ACKs (message ID and originating addresses)
	private Map< Integer, Set< Integer > > receivedAcks = new ConcurrentHashMap<>();
	
	// connection timeout period
	private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_MS;
	
	@Override
	public void registerAgent(Agent agent) {
		if ( !Objects.isNull( agent ) ) agents.put( agent.getAgentId(), agent );
	}

	@Override
	public void registerPlace(Place place) {
		if ( !Objects.isNull( place ) ) places.put( MatrixUtilities.getLinearIndex( place.getSize(), place.getIndex() ), place );
	}

	@Override
	public void unregisterAgent(Agent agent) {
		if ( !Objects.isNull( agent ) ) agents.remove( agent.getAgentId() );
	}

	@Override
	public void unregisterPlace(Place place) {
		if ( !Objects.isNull( place ) ) places.remove( MatrixUtilities.getLinearIndex( place.getSize(), place.getIndex() ) );
	}

	/**
	 * Deliver a MASS Message to an Agent
	 * @param message The MASS Message to deliver (must have destination address field set)
	 */
	@SuppressWarnings("rawtypes")
	protected void deliverAgentMessage( MASSMessage message ) {
		
		// addressed to all Agents?
		if ( message.getDestinationAddress() == MessageDestination.ALL_AGENTS.getValue() ) {
			
			for ( Agent agent : agents.values() ) {
				MASS.getEventDispatcher().queueAsync( OnMessage.class, agent, message.getMessage() );
			}
			
		}
		
		else {
			
			// addressed to a single Agent
			Agent agent = agents.get( message.getDestinationAddress() );
			
			// deliver the message
			if ( !Objects.isNull( agent ) ) MASS.getEventDispatcher().queueAsync( OnMessage.class, agent, message.getMessage() );
			
		}
		
	}
	
	/**
	 * Deliver a MASS Message to a Place
	 * @param message The MASS Message to deliver (must have destination address field set)
	 */
	@SuppressWarnings("rawtypes")
	protected void deliverPlaceMessage( MASSMessage message ) {
		
		// addressed to all Places?
		if ( message.getDestinationAddress() == MessageDestination.ALL_PLACES.getValue() ) {
			
			for ( Place place : places.values() ) {
				MASS.getEventDispatcher().queueAsync( OnMessage.class, place, message.getMessage() );
			}
			
		}
		
		else {
			
			// addressed to a single Place
			Place place = places.get( message.getDestinationAddress() );
			
			// deliver the message
			if ( !Objects.isNull( place ) ) MASS.getEventDispatcher().queueAsync( OnMessage.class, place, message.getMessage() );
			
		}
		
	}

	/**
	 * Deliver a MASS Message to a Node
	 * @param message The MASS Message to deliver (must have destination address field set)
	 */
	@SuppressWarnings("rawtypes")
	protected void deliverNodeMessage( MASSMessage message ) {

		// deliver the message to MASSBase
		
		// yes - exceptions are swallowed. What else could we do here?
		try {
			MASS.getEventDispatcher().invokeImmediate( OnMessage.class, MASSBase.class, message.getMessage() );
		} catch (IllegalArgumentException e) {
			MASSBase.getLogger().error( "IllegalArgumentException caught while delivering message to MASSBase", e );
		} catch (IllegalAccessException e) {
			MASSBase.getLogger().error( "IllegalAccessException caught while delivering message to MASSBase", e );
		} catch (InvocationTargetException e) {
			MASSBase.getLogger().error( "InvocationTargetException caught while delivering message to MASSBase", e );
		}

	}

	/**
	 * Deliver a MASS Message to a Node
	 * @param message The MASS Message to deliver (must have destination address field set)
	 */
	protected void deliverAckMessage( MASSAckMessage message ) {

		// has this message ID been ACKd previously?
		Set<Integer> receivedAddresses = receivedAcks.get( message.getMessageID() );
		if ( Objects.isNull( receivedAddresses ) ) {
			
			// no - this is the first ACK for the message
			Set< Integer > addresses = new HashSet<>();
			addresses.add( message.getSourceAddress() );
			receivedAcks.put( message.getMessageID(), addresses );
			
		}
		
		else {
			
			// yes - this message ID has other ACKs, add this new source address
			receivedAddresses.add( message.getSourceAddress() );
			
		}
		
	}

	@Override
	public int getConnectionTimeout() {
		return connectionTimeout;
	}

	@Override
	public void setConnectionTimeout( int timeout ) {
		connectionTimeout = timeout;
	}
	
	/**
	 * Determine if a message has been acknowledged or not
	 * @param messageID The ID number of the original messsage
	 * @param address The address the original message was sent to
	 * @return TRUE if a delivery acknowledgement has been received for the specified message and address, FALSE if not (yet...)
	 */
	protected boolean hasAck( int messageID, int address ) {
		
		// has this message ID been ACKd yet?
		Set<Integer> receivedAddresses = receivedAcks.get( messageID );
		if ( Objects.isNull( receivedAddresses ) ) return false;
		
		// a message by this ID has been ACKd, how about the originating address?
		return receivedAddresses.contains( address );
		
	}
	
	/**
	 * When this node is satisfied that a message has been received by all addresses, the ACK reference must be removed.
	 * This method removes all ACK references for a specified message ID.
	 * @param messageId The message ID number for removing ACK references
	 */
	protected void clearAck( int messageID ) {
		receivedAcks.remove( messageID );
	}

}
