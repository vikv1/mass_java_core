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

/**
 * Common message destination addresses
 */
public enum MessageDestination {

	// cluster-wide destination addresses
	ALL_NODES( - 1 ),
	ALL_PLACES( -1 ),
	ALL_AGENTS( -1 ),
	
	// single-node destination addresses
	ALL_LOCAL_PLACES( -2 ),
	ALL_LOCAL_AGENTS( -2 );
	
	private int address;

	/**
	 * Set the address for this enumeration
	 * @param address The address
	 */
	private MessageDestination(int address) {
		this.address = address;
	}
	
	/**
	 * Get the address value for this enumeration
	 * @return The address value
	 */
	public int getValue() {
		return address;
	}
	
}