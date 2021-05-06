/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.AgentList;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.Place;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;
import edu.uw.bothell.css.dsl.MASS.messaging.aeron.AeronMessagingProvider;

/**
 * MASSMessaging provides messaging between Nodes (MNode/MProcess), Places, and Agents in a MASS cluster
 * 
 * This class primarily serves to insulate the messaging provider implementation from the rest of MASS-Core,
 * and to also provide helper methods that make the job of creating new messaging implementations easier.
 * 
 */
public class MASSMessaging {

	// the actual messaging implementation
	private MessagingProvider messagingProviderImpl = new AeronMessagingProvider();
	
	// local message queues
	private Queue< MASSMessage< Serializable > > placeMessageQueue = new ConcurrentLinkedQueue<>();
	private Queue< MASSMessage< Serializable > > agentMessageQueue = new ConcurrentLinkedQueue<>();

	// Cryptographically-strong random number generation
	private static SecureRandom sRand = new SecureRandom();
	
	/**
     * Initializes singleton.
     *
     * {@link SingletonHolder} is loaded on the first execution of {@link Singleton#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
    	private static final MASSMessaging INSTANCE = new MASSMessaging();
    }

    /**
     * Return this instance of the messaging provider, which is effectively a Singleton
     * @return The single instance of this messenger implementation
     */
    public static MASSMessaging getInstance() {
    	return SingletonHolder.INSTANCE;
    }

	/**
	 * Initialize the message provider
	 * @param masterNode The main cluster node
	 * @param remoteNodes The remote cluster members
	 */
	public void init( String clusterCommunicationsAddress ) {
		MASSBase.getLogger().debug("Messaging system initialization starting");
		messagingProviderImpl.init( clusterCommunicationsAddress );
	}

	/**
	 * Register an Agent with the messaging provider
	 * @param agent The Agent to register
	 */
	public void registerAgent(Agent agent) {
		messagingProviderImpl.registerAgent(agent);
	}

	/**
	 * Register a Place with the messaging provider
	 * @param place The Place to register
	 */
	public void registerPlace(Place place) {
		messagingProviderImpl.registerPlace(place);
	}

	/**
	 * Send a message to a single Agent by Agent ID, regardless of location in the cluster
	 * @param address The ID of the Agent that will receive the message
	 * @param message The message to send to the Agent
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendAgentMessage( int address, T message ) throws IllegalArgumentException {
		
		Objects.requireNonNull( message, "Must provide a message!" );
		agentMessageQueue.add( new MASSMessage<Serializable>( address, message ) );
		
	}

	/**
	 * Send a message to multiple Agents
	 * @param destination The enumerated MessageDestination representing which Agents should receive this message
	 * @param message The message to send to the Agents
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendAgentMessage( MessageDestination destination, T message ) {

		Objects.requireNonNull( destination, "Must provide a destination!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		sendAgentMessage( destination.getValue(), message );

	}

	/**
	 * Send a message to multiple Agents
	 * @param addresses A Set of addresses representing which Agents should receive this message
	 * @param message The message to send to the Agents
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendAgentMessage( Set< Integer > addresses, T message ) {

		Objects.requireNonNull( addresses, "Must provide destination addresses!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		addresses.forEach( destination -> sendAgentMessage( destination, message ) );
		
	}

	/**
	 * Send a message to a single cluster Node
	 * @param address The ID of the Node that will receive the message
	 * @param message The message to send to the Node
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendNodeMessage( int address, T message ) {

		Objects.requireNonNull( message, "Must provide a message!" );
		messagingProviderImpl.sendNodeMessage( new MASSMessage< Serializable >( address, message ) );

	}

	/**
	 * Send a message to multiple cluster Nodes
	 * @param destination The enumerated MessageDestination representing which Nodes should receive this message
	 * @param message The message to send to the Nodes
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendNodeMessage( MessageDestination destination, T message ) {

		Objects.requireNonNull( destination, "Must provide a destination!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		sendNodeMessage( destination.getValue(), message );
		
	}

	/**
	 * Send a message to multiple cluster Nodes
	 * @param addresses A Set of addresses representing which Nodes should receive this message
	 * @param message The message to send to the Nodes
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendNodeMessage(Set<Integer> addresses, T message ) {

		Objects.requireNonNull( addresses, "Must provide destination addresses!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		addresses.forEach( destination -> sendNodeMessage( destination, message ) );

	}

	/**
	 * Send a message to a single Place, regardless of which cluster Node contains this Place
	 * @param linearIndex The linear index location of the Place that will receive the message
	 * @param message The message to send to the Place
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendPlaceMessage( int linearIndex, T message ) {
		
		Objects.requireNonNull( message, "Must provide a message!" );
		placeMessageQueue.add( new MASSMessage< Serializable >( linearIndex, message ) );
		
	}

	/**
	 * Send a message to a single Place, regardless of which cluster Node contains this Place
	 * @param index The index location of the Place that will receive the message
	 * @param message The message to send to the Place
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendPlaceMessage(int[] index, T message ) {

		Objects.requireNonNull( index, "Must provide an index!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		sendPlaceMessage( MatrixUtilities.getLinearIndex( MASS.getCurrentPlacesBase().getSize() , index ), message );

	}
	
	/**
	 * Send a message to multiple Places
	 * @param destination The enumerated MessageDestination representing which Places should receive this message
	 * @param message The message to send to the Places
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendPlaceMessage( MessageDestination destination, T message ) {

		Objects.requireNonNull( destination, "Must provide a destination!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		sendPlaceMessage( destination.getValue(), message );

	}

	/**
	 * Send a message to multiple Places
	 * @param addresses A Set of linear indices representing which Places should receive this message
	 * @param message The message to send to the Places
	 * @param messageClazz The serializable message class
	 */
	public < T extends Serializable > void sendPlaceMessage( Set< Integer > linearIndexes, T message ) {

		Objects.requireNonNull( linearIndexes, "Must provide a collection of linear indices!" );
		Objects.requireNonNull( message, "Must provide a message!" );
		linearIndexes.forEach( destination -> sendPlaceMessage( destination, message ) );

	}
	
	/**
	 * Flush (transmit) all queued Agent messages
	 */
	public void flushAgentMessages() {
		Stream.generate( agentMessageQueue::poll ).takeWhile( Objects::nonNull ).forEach( message -> messagingProviderImpl.sendAgentMessage( message ) );
	}

	/**
	 * Flush (transmit) all queued Place messages
	 */
	public void flushPlaceMessages() {
		Stream.generate( placeMessageQueue::poll ).takeWhile( Objects::nonNull ).forEach( message -> messagingProviderImpl.sendPlaceMessage( message ) );
	}

	/**
	 * Signal the messaging provider to complete any outstanding tasks and perform an orderly shutdown
	 */
	public void shutdown() {
		
		MASSBase.getLogger().debug("Messaging system shutdown requested");
		messagingProviderImpl.shutdown();
		
	}
	
	protected Set<Agent> getLocalAgents() {
		
		Set<Agent> localAgents = new HashSet<>();
		
		// obtain the custom collection of local agents
		AgentList agentList = MASS.getCurrentAgentsBase().getAgents();
		
		// reset list to starting position and iterate through collection
		agentList.setIterator();
		while ( agentList.hasNext() ) {
			localAgents.add( agentList.next() );
		}

		return localAgents;
		
	}
	
	/**
	 * Get a random IPv4 address within the local multicast group range
	 * @return A random IPv4 address ("dotted quad") within the multicast address space
	 */
	public static String getRandomMulticastAddress() {
		return "239." + sRand.nextInt(256) + "." + sRand.nextInt(256) + ".1";
	}
	
	/**
	 * Get a random port number
	 * @return A random port number, in the range 1024-65535
	 */
	public static int getRandomPort() {
		return 1024 + sRand.nextInt( 64512 ); 
	}
	
	/**
	 * Unregister an Agent from the messaging provider
	 * @param agent The Agent to unregister
	 */
	public void unregisterAgent(Agent agent) {
		messagingProviderImpl.unregisterAgent(agent);
	}

	/**
	 * Unregister a Place from the messaging provider
	 * @param place The Place to unregister
	 */
	public void unregisterPlace(Place place) {
		messagingProviderImpl.unregisterPlace(place);
	}
	
}