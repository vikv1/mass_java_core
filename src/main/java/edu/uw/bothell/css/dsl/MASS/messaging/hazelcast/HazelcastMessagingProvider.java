/*

 	MASS Java Software License
	Â© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	Â© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.messaging.hazelcast;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.topic.TopicOverloadPolicy;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSMessage;
import edu.uw.bothell.css.dsl.MASS.messaging.MessageDestination;
import edu.uw.bothell.css.dsl.MASS.messaging.MessagingProvider;


@SuppressWarnings("unused")    // TODO - remove once all methods implemented
public class HazelcastMessagingProvider implements MessagingProvider {

	// topic prefixes
	private static final String AGENT_ADDRESS_PREFIX = "A";
	private static final String PLACE_ADDRESS_PREFIX = "P";
	private static final String NODE_ADDRESS_PREFIX = "N";
	
	// common topic names
	private static final String AGENT_GLOBAL_BROADCAST_TOPIC = "AgentGlobalBroadcast";
	private static final String PLACE_GLOBAL_BROADCAST_TOPIC = "PlaceGlobalBroadcast";
	private static final String NODE_GLOBAL_BROADCAST_TOPIC = "NodeGlobalBroadcast";
//	private static final String AGENT_LOCAL_BROADCAST_TOPIC_PREFIX = "AgentLocalBroadcastNode";
//	private static final String PLACE_LOCAL_BROADCAST_TOPIC_PREFIX = "PlaceLocalBroadcastNode";
	
	private static final String HAZELCAST_LOGGING_TYPE = "log4j2";
	private static final String HAZELCAST_LOGGING_LEVEL = "ERROR";
	
	private HazelcastInstance instance;
	private static final boolean useMulticast = true;		// experiment with this, might be good to have a setter
	
	@Override
	public void init( MNode masterNode, Collection< MNode > remoteNodes ) {
		
		MASSBase.getLogger().debug( "Hazelcast Messaging Provider initializing..." );
		
		Config config = new Config();
        config.setProperty( "hazelcast.logging.type", HAZELCAST_LOGGING_TYPE );
        config.setProperty( "hazelcast.logging.level", HAZELCAST_LOGGING_LEVEL );

        // common network config options
        config.getNetworkConfig().setPortAutoIncrement( true );		// automatically find an open port to use
        config.getNetworkConfig().setReuseAddress( true );			// attempt to reuse port within two minutes of last shutdown

        if ( useMulticast ) {
		
        	// using multicast for node discovery and binding
        	MASSBase.getLogger().debug( "Using multicast for cluster discovery and binding..." );
        	config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled( true );
        	
        }
        
        else {
        
			// explicitly add remote nodes rather than using multicast
        	MASSBase.getLogger().debug( "Adding individual cluster members via TCP..." );
	        if ( remoteNodes != null) {
		        for ( MNode node : remoteNodes ) {
		        	MASSBase.getLogger().debug( "Adding {} as a cluster member", node.getHostName() );
					config.getNetworkConfig().getJoin().getTcpIpConfig().addMember( node.getHostName() ).setEnabled( true );
				}
	        }
		
        }
        
    	MASSBase.getLogger().debug( "Instantiating Hazelcast instance..." );
		instance = Hazelcast.newHazelcastInstance( config );
		
		MASSBase.getLogger().debug( "Hazelcast Messaging Provider initialized!" );
		
	}

	@Override
	public void registerAgent( Agent agent ) {
		
		// create/obtain a topic and listener for this particular agent
		String agentSpecificTopicName = AGENT_ADDRESS_PREFIX + agent.getAgentId();  
		
		// set configuration for agent-specific topic
		setGlobalTopicConfiguration( agentSpecificTopicName );

		// create agent-specific topic and register listener
		ITopic<MASSMessage<Serializable>> agentSpecificTopic = instance.getReliableTopic( agentSpecificTopicName );
		HazelcastAgentMessageListener agentMessageListener = new HazelcastAgentMessageListener( agent );
		agentSpecificTopic.addMessageListener(agentMessageListener);

		// set configuration for agent broadcast topic
		setGlobalTopicConfiguration( AGENT_GLOBAL_BROADCAST_TOPIC );

		// create/obtain agent broadcast topic and listener
		ITopic<MASSMessage<Serializable>> agentBroadcastTopic = instance.getReliableTopic( AGENT_GLOBAL_BROADCAST_TOPIC );
		HazelcastAgentMessageListener broadcastMessageListener = new HazelcastAgentMessageListener( agent );
		agentBroadcastTopic.addMessageListener(broadcastMessageListener);
		
	}

	@Override
	public void registerPlace (Place place ) {

		// create/obtain a topic and listener for this particular place
		String placeSpecificTopicName = PLACE_ADDRESS_PREFIX + MatrixUtilities.getLinearIndex( place.getSize(), place.getIndex() );  
		
		// set configuration for place-specific topic
		setGlobalTopicConfiguration( placeSpecificTopicName );
		
		// create place-specific topic and register listener
		ITopic<MASSMessage<Serializable>> placeSpecificTopic = instance.getReliableTopic( placeSpecificTopicName );
		HazelcastPlaceMessageListener placeMessageListener = new HazelcastPlaceMessageListener( place );
		placeSpecificTopic.addMessageListener(placeMessageListener);

		// set configuration for place broadcast topic
		setGlobalTopicConfiguration( PLACE_GLOBAL_BROADCAST_TOPIC );

		// create/obtain place broadcast topic and listener
		ITopic<MASSMessage<Serializable>> placeBroadcastTopic = instance.getReliableTopic( PLACE_GLOBAL_BROADCAST_TOPIC );
		HazelcastPlaceMessageListener broadcastMessageListener = new HazelcastPlaceMessageListener( place );
		placeBroadcastTopic.addMessageListener(broadcastMessageListener);

	}
	
	@Override
	public < T > void sendAgentMessage( MASSMessage< Serializable > message ) {

		if ( !Objects.nonNull( message ) ) throw new IllegalArgumentException( "Must provide a message to send!" );
		if ( !Objects.nonNull( message.getMessage() ) ) throw new IllegalArgumentException( "Must provide a message to send!" );

		// broadcast to all Agents?
		if ( message.getDestinationAddress() == MessageDestination.ALL_AGENTS.getValue() ) {
			instance.getReliableTopic( AGENT_GLOBAL_BROADCAST_TOPIC ).publish( message.getMessage() );
		}
		
		// broadcast to all local Agents?
		else if ( message.getDestinationAddress() == MessageDestination.ALL_LOCAL_AGENTS.getValue() ) {
			
			
			
			
			
		}
		
		// specific Agent
		else {
			instance.getReliableTopic( AGENT_ADDRESS_PREFIX + message.getDestinationAddress() ).publish( message.getMessage() );
		}
			
	}

	@Override
	public < T > void sendNodeMessage( MASSMessage< Serializable > message ) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public < T > void sendPlaceMessage( MASSMessage< Serializable > message ) {

		if ( !Objects.nonNull( message ) ) throw new IllegalArgumentException( "Must provide a message to send!" );
		if ( !Objects.nonNull( message.getMessage() ) ) throw new IllegalArgumentException( "Must provide a message to send!" );

		// broadcast to all Places?
		if ( message.getDestinationAddress() == MessageDestination.ALL_PLACES.getValue() ) {
			instance.getReliableTopic( PLACE_GLOBAL_BROADCAST_TOPIC ).publish( message.getMessage() );
		}
		
		// broadcast to all local Agents?
		else if ( message.getDestinationAddress() == MessageDestination.ALL_LOCAL_PLACES.getValue() ) {
			
			// TODO - implement
			
		}
		
		// specific Place
		else {
			instance.getReliableTopic( PLACE_ADDRESS_PREFIX + message.getDestinationAddress() ).publish( message.getMessage() );
		}
		
	}

	private void setGlobalTopicConfiguration( String topicName ) {
		
		Config agentSpecificTopicConfig = new Config();
		ReliableTopicConfig agentSpecificRTTopicConfig = agentSpecificTopicConfig.getReliableTopicConfig( topicName );
		agentSpecificRTTopicConfig.setTopicOverloadPolicy( TopicOverloadPolicy.BLOCK );
		agentSpecificRTTopicConfig.setReadBatchSize( 1 );
		agentSpecificRTTopicConfig.setStatisticsEnabled( true );	
		
	}

	@Override
	public void shutdown() {
		
		if ( instance != null ) {
		
			MASSBase.getLogger().debug("Hazelcast Messaging Provider shutdown requested");
			instance.shutdown();
		
		}
		
		else {
			
			MASSBase.getLogger().debug("Hazelcast was not initialized, ignoring shutdown command");
			
		}
		
	}

}