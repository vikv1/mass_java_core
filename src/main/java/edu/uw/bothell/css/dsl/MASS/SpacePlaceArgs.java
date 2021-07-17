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

public class SpacePlaceArgs implements Serializable {

    private int handle;
    private int granularity;
    private double[] min;
    private double[] max;
    private double[] interval;
    private double[] subInterval;
    private double[] placeMin;
    private double[] placeMax;
    private int[] totalPlacesSize;
    private int[] index;

    public SpacePlaceArgs(){
        super();
    }

    public SpacePlaceArgs(int handle, double[] min, double[] max, int[] size, int granularity, int[] index) {
        this.handle = handle;
        this.granularity = granularity;
        this.min = min.clone();
        this.max = max.clone();
        this.totalPlacesSize = size.clone();
        this.index = index.clone();

        //calculate min and max of the place
        int dim = min.length;
        interval = new double[dim];  //interval of place
        subInterval = new double[dim];
        placeMin = new double[dim];
        placeMax = new double[dim];
        for (int i = 0; i < dim; i++) {
            interval[i] = (max[i] - min[i]) / (double) size[i];
            placeMin[i] = min[i] + interval[i] * index[i];
            placeMax[i] = placeMin[i] + interval[i];
            subInterval[i] = interval[i] / (double) granularity;
        }

    }

    public int getHandle() {
        return handle;
    }
    
    public int getGranularity() {
        return granularity;
    }
    
    public double[] getPlaceMin() {
        return placeMin;
    }
    
    public double[] getPlaceMax() {
        return placeMax;
    }
    
    public double[] getInterval() {
        return interval;
    }

    public double[] getSubInterval() {
        return subInterval;
    }
    
}
