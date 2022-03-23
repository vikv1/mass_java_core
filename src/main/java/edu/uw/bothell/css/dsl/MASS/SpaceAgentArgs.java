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

public class SpaceAgentArgs implements Serializable {

    private double[] currentCoordinates;
    private double[] nextCoordinates;
    private double[] originalCoordinates;  //the original coordinates of the agent
    private int[] index;
    private int[] subIndex;
    private int generation;
    private int originalId; // it is the position of data point in the input file, start from 0
 

    public SpaceAgentArgs(){
        super();
    }

    public SpaceAgentArgs(double[] currentCoordinates, double[] nextCoordinates, int[] index, int[] subIndex, int generation, int originalId) {

        //super(SpaceAgent_,currentCoordinates,nextCoordinates,originalCoordinates,index,subIndex,generation,originalId);
        this.currentCoordinates = currentCoordinates.clone();
        //this.originalCoordinates = originalCoordinates.clone();
        this.nextCoordinates = nextCoordinates.clone();
        this.index = index.clone();
        this.subIndex = subIndex.clone();
        this.generation = generation;
        this.originalId = originalId;
        
    }

    public SpaceAgentArgs(double[] currentCoordinates, double[] nextCoordinates, double[] originalCoordinates, int[] index, int[] subIndex, int generation, int originalId) {

        this.currentCoordinates = currentCoordinates.clone();
        this.originalCoordinates = originalCoordinates.clone();
        this.nextCoordinates = nextCoordinates.clone();
        this.index = index.clone();
        this.subIndex = subIndex.clone();
        this.generation = generation;
        this.originalId = originalId;

    }

    
    public double[] getCurrentCoordinates() {
        return this.currentCoordinates;
    }

    public double[] getOriginalCoordinates() {
        return this.originalCoordinates;
    }
    
    public double[] getNextCoordinates() {
        return this.nextCoordinates;
    }

    public int[] getIndex() {
        return this.index;
    }

    public int[] getSubIndex() {
        return this.subIndex;
    }

    public int getGeneration() {
        return this.generation;
    }

    public int getOriginalId() {
        return this.originalId;
    }

   
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("current coordinates : " + currentCoordinates[0] + ", " + currentCoordinates[1] + "]\n");
        sb.append("next coordinates : " + nextCoordinates[0] + ", " + nextCoordinates[1] + "]\n");
        sb.append("generation : " + generation + "]\n");
        sb.append("originalId : " + originalId + "]\n");
        return sb.toString();
    }
    
    
}
