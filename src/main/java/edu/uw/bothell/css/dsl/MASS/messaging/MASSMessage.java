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

import java.io.Serializable;

/**
 * MASSMessage encapsulates all information needed by a message provider implementation to actually send a
 * message to destination Nodes, Places, or Agents
 */
@SuppressWarnings("serial")
public class MASSMessage< T extends Serializable > implements Serializable {

	private int destinationAddress;
	private T message;
	private boolean receiptRequired = false;
	private int messageID;
	
	public MASSMessage( int destinationAddress, T message ) {
	
		this.destinationAddress = destinationAddress;
		this.message = message;
		
		this.messageID = MASSMessaging.generateMessageID();
		
	}
	
	/**
	 * Get the destination address for this message
	 * @return The address (Node number/rank, Agent ID, Place linear index) this message is intended for
	 */
	public int getDestinationAddress() {
		return destinationAddress;
	}
	
	/**
	 * Get the message payload/contents
	 * @return The contents/payload of this message
	 */
	public T getMessage() {
		return message;
	}
	
	/**
	 * Get the randomly-generated ID number for this message. Message ID numbers are used by receiving nodes to
	 * reply to messages, and by sending nodes to match up delivery receipts with messages to verify delivery.
	 * @return The ID number of this message
	 */
	public int getMessageID() {
		return messageID;
	}
	
	/**
	 * Will this message return a receipt acknowledging successful reception?
	 * @return TRUE if a reception acknowledgement is returned (SYNCHRONOUS) or FALSE if not (ASYNCHRONOUS)
	 */
	public boolean isReceiptRequired() {
		return receiptRequired;
	}

	/**
	 * Set message delivery (receipt) required flag
	 * @param receiptRequired Set to TRUE to force remote node acknowledgement of message reception
	 */
	public void setReceiptRequired( boolean receiptRequired ) {
		this.receiptRequired = receiptRequired;
	}
	
}
