/*

 	MASS Java Software License
	© 2012-2015 University of Washington

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

package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Boundary of QuadTree (2D)
 */
@SuppressWarnings("serial")
public class Boundary implements Serializable{
    
	//double[] topLeft, topRight, bottomLeft, bottomRight;
    double[] min, max;
    
    public Boundary(double[] min, double[] max) {
		/*
        this.topLeft = new double[]{min[0], max[1]};
        this.topRight = new double[]{max[0], max[1]};
        this.bottomLeft = new double[]{min[0], min[1]};
		this.bottomRight = new double[]{max[0], min[1]};
		*/
		this.min = Arrays.copyOf(min, min.length);
		this.max = Arrays.copyOf(max, max.length);
	}
	
    /*
	public double[] getTopLeft() {
		return topLeft;
	}

	public double[] getTopRight() {
		return topRight;
	}

	public double[] getBottomLeft() {
		return bottomLeft;
	}

	public double[] getBottomRight() {
		return bottomRight;
	}
	*/
	
    public double[] getMin() {
		return min;
	}

	public double[] getMax() {
		return max;
	}
	
	// toString() for 2D
	public String toString() {
		/*
		return "Boundary: { TL = [" + topLeft[0] + "," + topLeft[1] + "] TR = " + + topRight[0] + "," + topRight[1] 
			+ "] BL = " + bottomLeft[0] + "," + bottomLeft[1] + "] BR = " + + bottomRight[0] + "," + bottomRight[1] + "] }";
			*/

		return "Boundary: { Min = [" + min[0] + "," + min[1] + "] Max = [" + max[0] + "," + max[1] + "] }";
	}

}