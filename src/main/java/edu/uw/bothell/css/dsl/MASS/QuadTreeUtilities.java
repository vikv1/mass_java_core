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
 * Utilities functions for Quad Tree. 
 * 
 */
package edu.uw.bothell.css.dsl.MASS;

import java.util.Arrays;
import java.util.Vector;

public class QuadTreeUtilities {
    static Boundary calculateBoundaryOfCurrNode(Boundary boundaryAll, int totalNodes, int pid) {
        // calculate boundary of current computing node
        int leftNodes = totalNodes;
        int count = 0;

        // partitions in each dimension 
        int dimensions = boundaryAll.getMin().length;
        int[] partition = new int[dimensions];
        Arrays.fill(partition, 1);
        while (leftNodes >= 2) {
            partition[count % dimensions] *= 2;
            leftNodes /= 2;
            count++;
        }

        // calcualte current node boundary
        int[] nodeIndex = new int[]{pid % partition[0], pid/ partition[0]};
        double[] minAll = boundaryAll.getMin();
        double[] maxAll = boundaryAll.getMax();
        double[] interval = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            interval[i] = (maxAll[i] - minAll[i]) / partition[i]; 
        }
        double[] min = new double[]{minAll[0] + interval[0] * nodeIndex[0], minAll[1] + interval[1] * nodeIndex[1]};
        double[] max = new double[]{min[0] + interval[0], min[1] + interval[1]};
        return new Boundary(min, max);
    }

    /**
     * calculate the subindex according to the coordinates.
     * @param coordinates
     * @param boundary boundary of the place
     * @param interval interval of sub-place in the place
     * @return int[] of subindex
     */
    public static int[] calculateSubIndex(double[] coordinates, Boundary boundary, double[] interval) {
        MASSBase.getLogger().debug("coordinates = [" + coordinates[0] + "," + coordinates[1] + ", boundary = " + 
            boundary.toString() + ",interval = [" + interval[0] + "," + interval[1] + "]");
        int dimensions = interval.length;
        int[] subIndex = new int[dimensions];
        double[] min = boundary.getMin();
        
        for (int i = 0; i < dimensions; i++) {
            subIndex[i] = (int) ((coordinates[i] - min[i]) / interval[i]);
        }
        return subIndex;
    }

    /**
     * Print QuadTreePlace info to the log file. (for debugging)
     */
    static String display_tree (QuadTreePlace tree) { 
        StringBuilder sb = new StringBuilder();
        if (tree != null) {
            if (tree.getPoint() != null) {
                sb.append("Index: " + tree.getTreeIndex() + "Point: " + tree.getPoint().toString() + ", isLeaf: " + tree.getIsLeaf());
                sb.append("\n");
            } else {
                sb.append("Index: " + tree.getTreeIndex() + "Point: null" + ", isLeaf: " + tree.getIsLeaf());
                sb.append("\n");
            }
            if (tree.getTopLeft() != null) sb.append(display_tree(tree.getTopLeft()));
            if (tree.getTopRight() != null) sb.append(display_tree(tree.getTopRight()));
            if (tree.getBottomLeft() != null) sb.append(display_tree(tree.getBottomLeft()));
            if (tree.getBottomRight() != null) sb.append(display_tree(tree.getBottomRight()));
        }
        return sb.toString();
    }

    public static Vector<double[]> getNeighborPatterns(String neighborPattern, double[] subInterval){

        switch(neighborPattern){

            case "moore":{

                Vector<double[]> mooreNeigbors = new Vector<>();
                mooreNeigbors.add(new double[]{subInterval[0], 0.0});
                mooreNeigbors.add(new double[]{0.0, subInterval[1]});
                mooreNeigbors.add(new double[]{subInterval[0] * (-1.0), 0.0});
                mooreNeigbors.add(new double[]{0.0, subInterval[1] * (-1.0)});
                mooreNeigbors.add(new double[]{subInterval[0] * (-1.0), subInterval[1]});
                mooreNeigbors.add(new double[]{subInterval[0], subInterval[1]});
                mooreNeigbors.add(new double[]{subInterval[0] * (-1.0), subInterval[1] * (-1.0)});
                mooreNeigbors.add(new double[]{subInterval[0], subInterval[1] * (-1.0)});

                return mooreNeigbors;

            }
            
            case "vonNeumann":{

                Vector<double[]> vonNeumannNeigbors = new Vector<>();
                vonNeumannNeigbors.add(new double[]{subInterval[0], 0.0});
                vonNeumannNeigbors.add(new double[]{0.0, subInterval[1]});
                vonNeumannNeigbors.add(new double[]{subInterval[0] * (-1.0), 0.0});
                vonNeumannNeigbors.add(new double[]{0.0, subInterval[1] * (-1.0)});

                return vonNeumannNeigbors;

            }

            default: return null;
        }
    }

    static boolean sameCoordinates(double[] currentCoordinates, double[] nextCoordinates) {
        if (currentCoordinates == null || nextCoordinates == null || currentCoordinates.length != nextCoordinates.length) {
            return false;
        }
        int len = currentCoordinates.length;
        for (int i = 0; i < len; i++) {
            if (currentCoordinates[i] != nextCoordinates[i]) {
                return false;
            }
        }
        return true;      
    }

    public static boolean withinBoundary(double[] coordinates, Boundary boundary) {
        double[] min = boundary.getMin();
        double[] max = boundary.getMax();
        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i] < min[i] || coordinates[i] >= max[i]) {
                return false;
            }
        }
        return true;
    }
    
}