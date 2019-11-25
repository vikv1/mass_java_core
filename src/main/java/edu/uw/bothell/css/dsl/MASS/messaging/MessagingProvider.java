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

package edu.uw.bothell.css.dsl.MASS.messaging;

import java.util.Collection;
import java.util.Set;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MNode;
import edu.uw.bothell.css.dsl.MASS.Place;

/**
 * A MessagingProvider implementation provides messaging between Nodes (MNodes), Places, and Agents in a MASS cluster
 */
public interface MessagingProvider {

	/**
	 * Initialize this messaging provider
	 * @param masterNode The main cluster node
	 * @param remoteNodes The remote cluster members
	 */
	public void init( MNode masterNode, Collection<MNode> remoteNodes );
	
	/**
	 * Get an instance of the messaging provider
	 * @return The messaging provider
	 */
	public MessagingProvider getInstance();
	
	/**
	 * Register an Agent with the messaging provider
	 * @param agent The Agent to register
	 */
	public void registerAgent( Agent agent );
	
	/**
	 * Register a Place with the messaging provider
	 * @param place The Place to register
	 */
	public void registerPlace( Place place );
	
	/**
	 * Send a message to a single Agent, regardless of it's location in the cluster
	 * @param <T>
	 * @param address The ID of the Agent to receive the message
	 * @param message The object to send to the Agent
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendAgentMessage( int address, T message, Class<T> messageClazz);

	/**
	 * Broadcast a message to Agents
	 * @param <T>
	 * @param destination The Agents that will receive the message 
	 * @param message The object to send to the Agents
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendAgentMessage( MessageDestination destination, T message, Class<T> messageClazz);

	/**
	 * Send a message to multiple Agents, regardless of their location in the cluster
	 * @param <T>
	 * @param address The IDs of the Agent(s) to receive the message
	 * @param message The object to send to the Agent(s)
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendAgentMessage( Set<Integer> addresses, T message, Class<T> messageClazz);
	
	/**
	 * Send a message to a single Node
	 * @param <T>
	 * @param address The ID of the Node to receive the message
	 * @param message The object to send to the Node
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendNodeMessage( int address, T message, Class<T> messageClazz);

	/**
	 * Broadcast a message to Nodes
	 * @param <T>
	 * @param destination The Nodes that will receive the message 
	 * @param message The object to send to the Nodes
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendNodeMessage( MessageDestination destination, T message, Class<T> messageClazz);

	/**
	 * Send a message to multiple Nodes
	 * @param <T>
	 * @param address The IDs of the Node(s) to receive the message
	 * @param message The object to send to the Node(s)
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendNodeMessage( Set<Integer> addresses, T message, Class<T> messageClazz);

	/**
	 * Send a message to a single Place, regardless of it's location in the cluster
	 * @param <T>
	 * @param address The ID of the Place to receive the message
	 * @param message The object to send to the Place
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendPlaceMessage( int address, T message, Class<T> messageClazz);

	/**
	 * Broadcast a message to Places
	 * @param <T>
	 * @param destination The Places that will receive the message 
	 * @param message The object to send to the Places
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendPlaceMessage( MessageDestination destination, T message, Class<T> messageClazz);

	/**
	 * Send a message to multiple Places, regardless of their location in the cluster
	 * @param <T>
	 * @param address The IDs of the Place(s) to receive the message
	 * @param message The object to send to the Place(s)
	 * @param messageClazz The class of message being sent
	 */
	public <T> void sendPlaceMessage( Set<Integer> addresses, T message, Class<T> messageClazz);

	/**
	 * Signal the messaging provider to complete any outstanding tasks and perform an orderly shutdown
	 */
	public void shutdown();

}
