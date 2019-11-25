package edu.uw.bothell.css.dsl.MASS.messaging.hazelcast;

import java.util.Collection;
import java.util.Set;

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.TopicConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.topic.TopicOverloadPolicy;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.messaging.MessageDestination;
import edu.uw.bothell.css.dsl.MASS.messaging.MessagingProvider;



public class HazelcastMessagingProvider implements MessagingProvider {

	// topic prefixes
	private static final String AGENT_ADDRESS_PREFIX = "A";
	private static final String PLACE_ADDRESS_PREFIX = "P";
	private static final String NODE_ADDRESS_PREFIX = "N";
	
	// common topic names
	private static final String AGENT_BROADCAST_TOPIC = "AgentBroadcast";
	private static final String PLACE_BROADCAST_TOPIC = "PlaceBroadcast";
	private static final String NODE_BROADCAST_TOPIC = "NodeBroadcast";
	
	private HazelcastInstance instance;
	
	
	/**
     * Initializes singleton.
     *
     * {@link SingletonHolder} is loaded on the first execution of {@link Singleton#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
    	private static final HazelcastMessagingProvider INSTANCE = new HazelcastMessagingProvider();
    }
    
	@Override
	public < T > void sendAgentMessage( int address, T message, Class< T > messageClazz ) {
		
		// publish the message
		ITopic<Object> topic = instance.getReliableTopic( AGENT_ADDRESS_PREFIX + address );
		topic.publish(message);
		
	}

	@Override
	public < T > void sendAgentMessage( Set< Integer > addresses, T message, Class< T > messageClazz ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public < T > void sendNodeMessage( int address, T message, Class< T > messageClazz ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public < T > void sendNodeMessage( Set< Integer > addresses, T message, Class< T > messageClazz ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public < T > void sendPlaceMessage( int address, T message, Class< T > messageClazz ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public < T > void sendPlaceMessage( Set< Integer > addresses, T message, Class< T > messageClazz ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MessagingProvider getInstance() {
		return SingletonHolder.INSTANCE;
	}

	@Override
	public void registerAgent( Agent agent ) {
		
		// create/obtain a topic and listener for this particular agent
		String agentSpecificTopicName = AGENT_ADDRESS_PREFIX + agent.getAgentId();  
		
		Config config = new Config();
		ReliableTopicConfig rtConfig = config.getReliableTopicConfig( agentSpecificTopicName );
		rtConfig.setTopicOverloadPolicy( TopicOverloadPolicy.BLOCK )
			.setReadBatchSize( 1 )
		    .setStatisticsEnabled( true );	
		
		ITopic<Object> agentSpecificTopic = instance.getReliableTopic( agentSpecificTopicName );
		HazelcastMessageListener agentMessageListener = new HazelcastMessageListener();
		agentMessageListener.setSubject( agent );
		agentSpecificTopic.addMessageListener(agentMessageListener);

		// create/obtain a broadcast topic and listener
		ITopic<Object> agentBroadcastTopic = instance.getReliableTopic( AGENT_BROADCAST_TOPIC );
		HazelcastMessageListener broadcastMessageListener = new HazelcastMessageListener();
		broadcastMessageListener.setSubject( agent );
		agentBroadcastTopic.addMessageListener(broadcastMessageListener);
		
	}

	@Override
	public void registerPlace (Place place ) {

		// create/obtain a topic for this agent
//		ITopic<Object> topic = instance.getTopic( "P" + place.g );

		
		
	}

	@Override
	public void init( MNode masterNode, Collection< MNode > remoteNodes ) {
		
		Config config = new Config();
		instance = Hazelcast.newHazelcastInstance( config );
		
		
	}

	@Override
	public void shutdown() {
		instance.shutdown();
	}

	@Override
	public <T> void sendAgentMessage(MessageDestination destination, T message, Class<T> messageClazz) {
		sendAgentMessage( destination.getValue(), message, messageClazz );
	}

	@Override
	public <T> void sendNodeMessage(MessageDestination destination, T message, Class<T> messageClazz) {
		sendNodeMessage( destination.getValue(), message, messageClazz );
	}

	@Override
	public <T> void sendPlaceMessage(MessageDestination destination, T message, Class<T> messageClazz) {
		sendPlaceMessage( destination.getValue(), message, messageClazz );
	}

}
