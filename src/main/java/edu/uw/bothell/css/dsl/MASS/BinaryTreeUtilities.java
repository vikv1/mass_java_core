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
 * Class for the Binary Tree Utility functions.
 * 
 */
package edu.uw.bothell.css.dsl.MASS;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

public class BinaryTreeUtilities { 

    /**
     * Calculate the boundary of current computing node
     * @param pivot the sorted partition points
     * @param totalNodes total number of computing nodes
     * @param pid the pid of current computing node
     * @return Boundary of current computing node
     */
    public static Boundary calculateCurrNodeBoundary(double[] pivot, int totalNodes, int pid) {
        MASSBase.getLogger().debug("BinaryTreeUtilities.java calculateCurrNodeRange(): pivot = " + Arrays.toString(pivot) + 
            ", totalNodes = " + totalNodes + ", pid = " + pid);
        
        // if single node
        if (pivot == null) return new Boundary(Double.MIN_VALUE, Double.MAX_VALUE);

        if (pid == 0) {
            MASSBase.getLogger().debug("1");
            return new Boundary(Double.MIN_VALUE, pivot[0]);
        } else if (pid == (totalNodes - 1)) {
            MASSBase.getLogger().debug("currNodeBoundary: " + pivot[pivot.length - 1] + "," + Double.MAX_VALUE);
            return new Boundary(pivot[pivot.length - 1], Double.MAX_VALUE);
        } else {
            MASSBase.getLogger().debug("2");
            return new Boundary(pivot[pid - 1], pivot[pid]);
        }
        
    }


    // calculate the max divide time which the partition part <= n
    public static int calculateDivideCount(int n) {
        int count = 0;
        while (Math.pow(2.0, count) < n) {
            count++;
        }
        return count;
    }

    /**
     * Method to partition the data items.
     * Default partition: partition data by value in first dimension.
     * Data could only be partitioned into 2^n parts (e.g. 1, 2, 4, 8,....)
     */
    public static double[] calculatePivot(String filename, int numOfParts) {
        Vector<Double> values = new Vector<>(); 
        try{
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = null;
       
            while((line = in.readLine()) != null) {
                
                String[] items = line.split(" ");
                values.add(Double.parseDouble(items[0]));
            }
        } catch(FileNotFoundException e) { 
            MASS.getLogger().debug("FileNotFoundException:" + e);
        } catch(IOException e) {
            MASS.getLogger().debug("IO Exception: " + e);  
        }
        Collections.sort(values);
    
        Vector<Double> pivot = new Vector<>();

        // calculate the max divide time which the partition part <= numOfNodes
        int divideCount = calculateDivideCount(numOfParts);
        findMedianIndex(pivot, values, 0, values.size() - 1, divideCount);

        if (pivot.size() == 0) {
            return null;
        } else {
            Collections.sort(pivot);
            double[] pivot_arr = new double[pivot.size()];
            int i = 0;
            for (Double d : pivot) {
                pivot_arr[i] = (double) d;
                i++;
            }
            return pivot_arr;
        }   
    }

    /**
     * Search the partition points that divide all data(values) into N parts (N = 2^(divideCount)).
     * @param pivot a vector of sorted partition points
     * @param values all data
     * @param low low index
     * @param high high index
     * @param divideCount how many times to divide data
     */
    public static void findMedianIndex(Vector<Double> pivot, Vector<Double> values, int low, int high, int divideCount) {
        if (divideCount <= 0) {
            return;
        }
        int median = low + (high - low) / 2;
        pivot.add(values.get(median));
        findMedianIndex(pivot, values, low, median, divideCount - 1);
        findMedianIndex(pivot, values, median + 1, high, divideCount - 1);
        Collections.sort(pivot);
    }

    /**
     * Reads all data items that are in the boundary from an input file. 
     * @param bounary the boundary of data to be read
     * @param filename input file
     * @return an array of Data objects
     */
    public static Data[] inputData(Boundary boundary, String filename) {

        Vector<Data> dataCurrNode = new Vector<>();

        // read data from file and save data that withinCurrNodeBoundary into Vector<Data> dataCurrNode
        try{
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = null;
                    
//            int index = 0;
            while((line = in.readLine()) != null) {
                       
                String[] items = line.split(" ");
                double[] coordinates = new double[items.length];
    
                for (int i = 0; i < items.length; i++) {
                    coordinates[i] = Double.parseDouble(items[i]);
                }
    
                if (BinaryTreeUtilities.withinBoundary(coordinates, boundary, 0)) {
                    // the initial compareDimension of data was set to 0
                    Data data = new Data(coordinates, 0);
                    dataCurrNode.add(data);
                    MASS.getLogger().debug(data.toString() + "added.");
                }
            }
            MASS.getLogger().debug("finished data input. dataCurrNode.size() = " + dataCurrNode.size());
                    
        } catch(FileNotFoundException e) {
            MASS.getLogger().debug("BinaryTreePlaces --> init_all_binaryTree(): FileNotFoundException:" + e);
        } catch(IOException e) {
            MASS.getLogger().debug("BinaryTreePlaces --> init_all_binaryTree(): IO Exception: " + e);  
        } 
        return dataCurrNode.toArray(new Data[dataCurrNode.size()]);
    }

    /**
     * print the binary tree into the log file
     */
    public static void printBinaryTree(BinaryTreePlace node) {
        MASSBase.getLogger().debug("start printing binary tree...");
        if (node == null) return;
        MASSBase.getLogger().debug(printHelper(node, "", ""));
    }

    public static String printHelper(BinaryTreePlace node, String prefix, String childrenPrefix) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(node.getData().toString());
        sb.append(", index = [" + node.getIndex()[0] + ", " + node.getIndex()[1] + "]");
        sb.append('\n');
    
        sb.append(printHelper(node.getLeftNode(), childrenPrefix + "├── ", childrenPrefix + "│   "));
        
        sb.append(printHelper(node.getRightNode(), childrenPrefix + "├── ", childrenPrefix + "│   "));
        
        return sb.toString();
    }

    public static boolean sameIndex(int[] index1, int[] index2) {
        if (index1.length != index2.length) {
            MASSBase.getLogger().debug("ERROR: BinaryTreeUtilities.java, sameIndex() index length are different");
            return false;
        }
        for (int i = 0; i < index1.length; i++) {
            if (index1[i] != index2[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * check if the coordinates is within the boundary at (index + 1)th dimension
     */
    public static boolean withinBoundary(double[] coordinates, Boundary boundary, int index) {
        double min = boundary.getMin()[index];
        double max = boundary.getMax()[index];
        
        if (coordinates[index] <= min || coordinates[index] > max) {
            return false;
        } else {
            return true;
        }

    }

    public static void writeToFile(Vector<Data> data, String filename) throws IOException{

        FileWriter vd = new FileWriter(filename);
        PrintWriter vdWriter = new PrintWriter(vd);

        StringBuilder sb = new StringBuilder();
        for (Data d : data) {
            double[] value = d.getValue();
            
            for (double element : value) {
                sb.append(element);
                sb.append(" ");
            }
            sb.append("\n");
            
        }
        vdWriter.println(sb.toString());
        vdWriter.close(); 
    }

   

   

    
}
    

