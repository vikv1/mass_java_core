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
import java.util.Vector;

@SuppressWarnings("serial")
public class QuadTreeAgentArgs implements Serializable {

    private double[] originalCoordinates;
    private double[] currentCoordinates;
    private double[] newCoordinates;
    private Vector<Integer> index;
    private int[] subIndex;
//    private double[] interval;
    private int generation;

    public QuadTreeAgentArgs(){
        super();
    }

    public QuadTreeAgentArgs(double[] originalCoordinates, double[] currentCoordinates, double[] newCoordinates, Vector<Integer> index, 
                    int[] subIndex, int generation) {
       
    	this.originalCoordinates = originalCoordinates.clone();
        this.currentCoordinates = currentCoordinates.clone();
        this.newCoordinates = newCoordinates;
        this.index = (Vector<Integer>) index.clone();
        this.subIndex = subIndex.clone();
        this.generation = generation; 
    
    }

    public Vector<Integer> getAgentIndex() {
        return index;
    }
    
    public int[] getAgentSubIndex() {
        return subIndex;
    }
    
    public double[] getCurrentCoordinates() {
        return currentCoordinates;
    }
   
    public int getGeneration() {
        return generation;
    }
    
    public double[] getNewCoordinates() {
        return newCoordinates;
    }

    public double[] getOriginalCoordinates() {
        return originalCoordinates;
    }
    
    public String toString() {
    
    	StringBuilder sb = new StringBuilder();
        sb.append("original coordinates : " + originalCoordinates[0] + ", " + originalCoordinates[1] + "]\n");
        sb.append("current coordinates : " + currentCoordinates[0] + ", " + currentCoordinates[1] + "]\n");
        sb.append("index agent resides = [" + index.get(0) + ", " + index.get(1) + "]\n");
        sb.append("sub-index agent resides = [" + getAgentSubIndex()[0] + ", " + getAgentSubIndex()[1] + "]\n");
        return sb.toString();
    
    }
    
    
}
