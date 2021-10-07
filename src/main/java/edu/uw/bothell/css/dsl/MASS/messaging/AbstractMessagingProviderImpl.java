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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public abstract class AbstractMessagingProviderImpl implements MessagingProvider {

	// references to all Agents and Places located on this node - for delivery of messages
	private Map< Integer, Agent > agents = new ConcurrentHashMap<>();
	private Map< Integer, Place > places = new ConcurrentHashMap<>();
	
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

}
