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

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.Place;

/**
 * A MessagingProvider implementation provides messaging between Nodes (MNodes), Places, and Agents in a MASS cluster
 */
public interface MessagingProvider {

	/**
	 * Initialize this messaging provider
	 * @param clusterCommunicationsAddress The address all cluster members use for communications
	 */
	public void init( String clusterCommunicationsAddress );
	
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
	 * Send a message to one or mode Places ASYNCHRONOUSLY. This is a NON-BLOCKING method.
	 * @param message The message to send to the Place(s)
	 */
	public <T> void sendPlaceMessage( MASSMessage< Serializable > message );

	/**
	 * Send a message to one or mode Nodes SYNCHRONOUSLY. This is a BLOCKING method that will return once the remote
	 * node has acknowledged receipt of the message or upon reaching timeout period.
	 * @param message The message to send to the Node(s)
	 */
	public <T> void sendNodeMessage( MASSMessage< Serializable > message );

	/**
	 * Send a message to one or mode Agents ASYNCHRONOUSLY. This is a NON-BLOCKING method.
	 * @param message The message to send to the Agent(s)
	 */
	public <T> void sendAgentMessage( MASSMessage< Serializable > message );
	
	/**
	 * Return a message delivery acknowledgement
	 * @param ackMessage The ACK message to send
	 */
	public void sendAck( MASSAckMessage ackMessage );

	/**
	 * Signal the messaging provider to complete any outstanding tasks and perform an orderly shutdown
	 */
	public void shutdown();

	/**
	 * Unregister an Agent from the messaging provider
	 * @param agent The Agent to unregister
	 */
	public void unregisterAgent( Agent agent);

	/**
	 * Unregister a Place from the messaging provider
	 * @param place The Place to unregister
	 */
	public void unregisterPlace( Place place);

	/**
	 * Get the timeout period, in milliseconds, for initiating messaging connections to remote nodes
	 * @return The connection timeout period
	 */
	public int getConnectionTimeout();

	/**
	 * Set the connection timeout period
	 * @param timeout The number of milliseconds to wait before aborting a connection to a remote node
	 */
	public void setConnectionTimeout( int timeout );

}