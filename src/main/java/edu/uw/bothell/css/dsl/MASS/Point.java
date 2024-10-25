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

/**
 * Class defines a point representing a location in (x,y) coordinate space
 * Functionality includes defining a 2D point, returning requested x and y
 * coordinate values and returning distance between two points in (x,y)
 * coordinate space
 *
 * @author Yuna Guo
 *
 */
@SuppressWarnings("serial")
public class Point implements Serializable{

    private double[] coordinates;
    private int dimension;
    private int originalId;

    /**
     * Constructor
     * pre: none
     * post: instance variables x and y are initialized
     *
     */
    public Point(double[] coordinates) {
        dimension = coordinates.length;
        this.coordinates = new double[dimension];
        for (int i = 0; i < coordinates.length; i++) {
            this.coordinates[i] = coordinates[i];
        }
    }

    public Point(double[] coordinates, int id) {
        dimension = coordinates.length;
        this.coordinates = new double[dimension];
        for (int i = 0; i < coordinates.length; i++) {
            this.coordinates[i] = coordinates[i];
        }
        this.originalId = id;
    }

    /**
     * get each dimension's coordinates
     *
     * @return double[] coordinate of the multi-dimensional point in double precision
     */
    public double[] getCoordinates() {
        return coordinates;
    }

    public int getOriginalId() {
        return originalId;
    }

    /**
     * method to calculate distance between two points in space
     *
     * @param pt - point to which distance has to be calculated from current
     *           point
     * @return the distance between two points
     */
    public double distance(Point pt) {
        double dist = 0;
        for (int i = 0; i < dimension; i++) {
            dist += Math.pow(pt.getCoordinates()[i], 2);
        }
        return Math.sqrt(dist);
    }

    /**
     * toString - method to represent the Object values as a string
     * @return - returns the converted string value
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < dimension; i++) {
            sb.append(coordinates[i] + ", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * equals - method to compare two Point objects
     * @param obj - second point
     * @return - returns true if both the points have same x & y values,
     * false otherwise.
     */
    @Override
    public boolean equals(Object obj) {

        Point point = (Point)obj;
        for (int i = 0; i < dimension; i++) {
            if (this.coordinates[i] != point.getCoordinates()[i]) {
                return false;
            }
        }
        return true;
    }
    
    // depends only on point's coordinates
    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(coordinates[0]);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(coordinates[1]);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
    
}
