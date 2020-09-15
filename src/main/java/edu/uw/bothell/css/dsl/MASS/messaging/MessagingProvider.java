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

package edu.uw.bothell.css.dsl.MASS.messaging;

import java.io.Serializable;
import java.util.Collection;

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
	 * Send a message to one or mode Places
	 * @param message The message to send to the Place(s)
	 */
	public <T> void sendPlaceMessage( MASSMessage< Serializable > message );

	/**
	 * Send a message to one or mode Nodes
	 * @param message The message to send to the Node(s)
	 */
	public <T> void sendNodeMessage( MASSMessage< Serializable > message );

	/**
	 * Send a message to one or mode Agents
	 * @param message The message to send to the Agent(s)
	 */
	public <T> void sendAgentMessage( MASSMessage< Serializable > message );

	/**
	 * Signal the messaging provider to complete any outstanding tasks and perform an orderly shutdown
	 */
	public void shutdown();

}