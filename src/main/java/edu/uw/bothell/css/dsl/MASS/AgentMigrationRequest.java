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

package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
class AgentMigrationRequest implements Serializable {

	int destGlobalLinearIndex;
	int[] destGlobalIndex = null; // --------- modified by Yuna
	int[] destSubIndex = null;  // --------- modified by Yuna
	double[] destCoordinates = null;
    Agent agent;
   
    /**
     * Construct a migration request for an Agent
     * @param destIndex The destination for the Agent
     * @param agent The Agent to be migrated
     */
	public AgentMigrationRequest( int destIndex, Agent agent ) {
		
		this.destGlobalLinearIndex = destIndex;
		this.agent = agent;
    
	}

	// this constructor is for BinaryAgent migration reqeust --- add by Yuna
	public AgentMigrationRequest( int[] destIndex, Agent agent ) {
		
		this.destGlobalIndex = destIndex;
		this.agent = agent;
    
	}

	// this constructor is for SpaceAgent migration reqeust --- add by Yuna
	public AgentMigrationRequest( int destIndex, int[] destSubIndex, double[] destCoordinates, Agent agent ) {
		
		this.destGlobalLinearIndex = destIndex;
		this.destSubIndex = destSubIndex.clone();
		this.destCoordinates = destCoordinates.clone();
		this.agent = agent;
    
	}

	// this constructor is for TreeAgent migration reqeust --- add by Yuna
	public AgentMigrationRequest(double[] destCoordinates, Agent agent ) {

		this.destCoordinates = destCoordinates.clone();
		this.agent = agent;
    
	}

}