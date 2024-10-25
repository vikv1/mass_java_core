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

/** 
 * Class of argument for QuadTreePlace instantiation.
 */
package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Vector;

@SuppressWarnings("serial")
public class QuadTreePlaceArgs implements Serializable {
    private int dimensions;
    private int level; //the depth of the place in the tree
    private Boundary boundary; //the boundary of the place
    private Vector<Integer> treeIndex;
    private Point point; //the data point reside in the place
    private QuadTreePlace parent; //the parent place

    public QuadTreePlaceArgs(){
        super();
    }

    public QuadTreePlaceArgs(int dimensions, int level, Boundary boundary, Vector<Integer> treeIndex, Point p, QuadTreePlace parent) {
        this.dimensions = dimensions;
        this.level = level;
        this.boundary = boundary;
        this.treeIndex = treeIndex;
        this.point = p;
        this.parent = parent;
    }

    public Boundary getBoundary() {
        return boundary;
    }

    public int getDimensions() {
        return dimensions;
    }

    public int getLevel() {
        return level;
    }

    public QuadTreePlace getParent() {
        return parent;
    }

    public Point getPoint() {
        return point;
    }

    public Vector<Integer> getTreeIndex() {
        return treeIndex;
    }
}