/*

 	MASS Java Software License
	© 2012-2019 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2019 University of Washington. MASS was developed by Computing and Software Systems at University of 
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
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.Place;
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
	private static final String AGENT_BROADCAST_TOPIC = "AgentBroadcast";
	private static final String PLACE_BROADCAST_TOPIC = "PlaceBroadcast";
	private static final String NODE_BROADCAST_TOPIC = "NodeBroadcast";
	
	private HazelcastInstance instance;
	
	
	@Override
	public void registerAgent( Agent agent ) {
		
		// create/obtain a topic and listener for this particular agent
		String agentSpecificTopicName = AGENT_ADDRESS_PREFIX + agent.getAgentId();  
		
		// set configuration for agent-specific topic
		Config agentSpecificTopicConfig = new Config();
		ReliableTopicConfig agentSpecificRTTopicConfig = agentSpecificTopicConfig.getReliableTopicConfig( agentSpecificTopicName );
		agentSpecificRTTopicConfig.setTopicOverloadPolicy( TopicOverloadPolicy.BLOCK );
		agentSpecificRTTopicConfig.setReadBatchSize( 1 );
		agentSpecificRTTopicConfig.setStatisticsEnabled( true );	

		// create agent-specific topic and register listener
		ITopic<MASSMessage<Serializable>> agentSpecificTopic = instance.getReliableTopic( agentSpecificTopicName );
		HazelcastAgentMessageListener agentMessageListener = new HazelcastAgentMessageListener();
		agentMessageListener.setSubject( agent );
		agentSpecificTopic.addMessageListener(agentMessageListener);

		// set configuration for agent broadcast topic
		Config agentBroadcastTopicConfig = new Config();
		ReliableTopicConfig agentBroadcastRTTopicConfig = agentBroadcastTopicConfig.getReliableTopicConfig( agentSpecificTopicName );
		agentBroadcastRTTopicConfig.setTopicOverloadPolicy( TopicOverloadPolicy.BLOCK );
		agentBroadcastRTTopicConfig.setReadBatchSize( 1 );
		agentBroadcastRTTopicConfig.setStatisticsEnabled( true );	

		// create/obtain agent broadcast topic and listener
		ITopic<MASSMessage<Serializable>> agentBroadcastTopic = instance.getReliableTopic( AGENT_BROADCAST_TOPIC );
		HazelcastAgentMessageListener broadcastMessageListener = new HazelcastAgentMessageListener();
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
	public <T> void sendAgentMessage(MASSMessage<Serializable> message) {

		if ( !Objects.nonNull( message ) ) throw new IllegalArgumentException( "Must provide a message to send!" );
		if ( !Objects.nonNull( message.getMessage() ) ) throw new IllegalArgumentException( "Must provide a message to send!" );

		// broadcast to all Agents?
		if ( message.getDestinationAddress() == MessageDestination.ALL_AGENTS.getValue() ) {
			
			// TODO - implement
			
		}
		
		// broadcast to all local Agents?
		else if ( message.getDestinationAddress() == MessageDestination.ALL_LOCAL_AGENTS.getValue() ) {
			
			// TODO - implement
			
		}
		
		// specific Agent
		else {
			
			// publish the message to a single agent
			ITopic<Object> topic = instance.getReliableTopic( AGENT_ADDRESS_PREFIX + message.getDestinationAddress() );
			topic.publish( message.getMessage() );
			
		}
			
	}

	@Override
	public <T> void sendPlaceMessage(MASSMessage<Serializable> message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> void sendNodeMessage(MASSMessage<Serializable> message) {
		// TODO Auto-generated method stub
		
	}

}
