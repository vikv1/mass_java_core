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
 * MASSAckMessage encapsulates all information needed by a message provider implementation to return a receipt to the originating
 * node acknowledging reception of a message
 */
@SuppressWarnings("serial")
public class MASSAckMessage implements Serializable {

	private int address;
	private int messageID;
	
	/**
	 * Get the address of the node/place/agent that received the original message that is being acknowledged
	 * @return The address that received the original message
	 */
	public int getAddress() {
		return address;
	}
	
	/**
	 * Set the address of the node/place/agent that received the orginal message
	 * @param address The address that received the original message
	 */
	public void setAddress( int address ) {
		this.address = address;
	}
	
	/**
	 * Get the ID number of the original message that this acknowledgement references 
	 * @return The ID number of the original message
	 */
	public int getMessageID() {
		return messageID;
	}
	
	/**
	 * Set the ID number of the message that is being acknowledged
	 * @param messageID The original message ID
	 */
	public void setMessageID( int messageID ) {
		this.messageID = messageID;
	}
	
}
