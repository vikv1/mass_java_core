package edu.uw.bothell.css.dsl.MASS.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public abstract class AbstractMessagingProviderImpl implements MessagingProvider {

	private Map< Integer, Agent > agents = new HashMap<>();
	private Map< Integer, Place > places = new HashMap<>();
	
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
	
	protected void deliverPlaceMessage( MASSMessage message ) {
		
		// addressed to all Places?
		if ( message.getDestinationAddress() == MessageDestination.ALL_PLACES.getValue() ) {
			
			for ( Place place : places.values() ) {
				MASS.getEventDispatcher().queueAsync( OnMessage.class, place, message.getMessage() );
			}
			
		}
		
		else {
			
			// addressed to a single Places
			Place place = places.get( message.getDestinationAddress() );
			
			// deliver the message
			if ( !Objects.isNull( place ) ) MASS.getEventDispatcher().queueAsync( OnMessage.class, place, message.getMessage() );
			
		}
		
	}
	

}
