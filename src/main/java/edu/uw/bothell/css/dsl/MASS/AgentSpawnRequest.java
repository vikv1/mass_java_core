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

package edu.uw.bothell.css.dsl.MASS;

/**
 * Created by utku on 1/24/17.
 *
 * This class simply contains the serialized version of the new agent object
 *  and the coordinates that new agent will become active.
 */
public class AgentSpawnRequest
{
    // Serialized agent (byte array)
    private byte[] serializedAgent;

    // Serialized agent identifier
    //private String serializedAgentIdentifier;

    // index[0], index[1], and index[2] correspond to coordinates of x, y, and z, or those of i, j, and k.
    //private int[] index;

    /**
     * Get the serialized Agent represented by this spawn request
     * @return The serialized Agent referenced by this request
     */
    public byte[] getSerializedAgent() { return serializedAgent; }

    //public String getSerializedAgentIdentifier() { return serializedAgentIdentifier; }

    //public int[] getIndex() { return index; }

    /**
     * Set the serialized Agent created by this request
     * @param serializedAgent The serialized Agent
     */
    public void setSerializedAgent(byte[] serializedAgent) { this.serializedAgent = serializedAgent; }

    //public void setSerializedAgentIdentifier(String serializedAgentIdentifier) { this.serializedAgentIdentifier = serializedAgentIdentifier; }

    //public void setIndex(int[] index) { this.index = index; }
}
