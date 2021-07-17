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

import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.List;

public class SpaceUtilities {
    public static final int MOORE2D = 2;
    public static ArrayList<Point> readInputData(String fileName) {
        
        ArrayList<Point> inputPoints = new ArrayList<>();  
        try{
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line = null;
            while((line = in.readLine()) != null) {
                String[] items = line.split(" ");
                double[] coordinates = new double[items.length];
                for (int i = 0; i < items.length; i++) {
                    coordinates[i] = Double.parseDouble(items[i]);
                }
   
                Point p = new Point(coordinates);
                
                inputPoints.add(p);
            }
        } catch(FileNotFoundException e) {
            MASS.getLogger().debug("FileNotFoundException:" + e);
        } catch(IOException e) {
            MASS.getLogger().debug("IO Exception: " + e);  
        }
        return inputPoints;
    }


    public static double[][] getInputParameters(String fileName, int dimensions) { 
    
        double[][] result = new double[3][dimensions];
        double[] min = new double[dimensions];
        double[] max = new double[dimensions]; 
        double[] size = new double[dimensions];
        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);
        Arrays.fill(size, 0.0);

        int count = 0;
        try{
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line = null;
            while((line = in.readLine()) != null) {
                String[] items = line.split(" ");
                double[] coordinates = new double[items.length];
                for (int i = 0; i < items.length; i++) {
                    coordinates[i] = Double.parseDouble(items[i]);
                    min[i] = Math.min(min[i], coordinates[i]);
                    max[i] = Math.max(max[i], coordinates[i]);
                }
                count++;

            }
        } catch(FileNotFoundException e) {
            MASS.getLogger().debug("FileNotFoundException:" + e);
        } catch(IOException e) {
            MASS.getLogger().debug("IO Exception: " + e);  
        }
        
        size[0] = count;

        result[0] = min; 
        result[1] = max;
        result[2] = size;

        return result;
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
    
    static boolean sameIndex(int[] index1, int[] index2) {
        if (index1 == null || index2 == null || index1.length != index2.length) {
            return false;
        }
        int len = index1.length;
        for (int i = 0; i < len; i++) {
            if (index1[i] != index2[i]) {
                return false;
            }
        }
        return true;      
    }
    
    static int[] findDestIndex(double[] coordinates, SpacePlacesBase curPlaces) {
        
        int[] dest_index = new int[coordinates.length];
        for (int i = 0; i < dest_index.length; i++) {
            dest_index[i] = (int) (Math.floor(((coordinates[i] - curPlaces.getMin()[i]) / curPlaces.getInterval()[i])));
            
            //out of range
            if ((dest_index[i] < 0) || (dest_index[i] >= curPlaces.getSize()[i])) {
                Arrays.fill(dest_index, -1);
                return dest_index;
            }
        }
        return dest_index;
    }

    static int[] findSubIndex(double[] coordinates, int[] index, double[] interval, double[] subInterval, double[] min){
        int dimensions = coordinates.length;
        int[] subIndex = new int[dimensions];
    
        for(int i = 0; i < dimensions; i++) {
            
            int n = (int)((coordinates[i] - index[i] * interval[i] - min[i]) / subInterval[i]);
            
            subIndex[i] = n;
        } 
        return subIndex;
    }

    public static boolean withinBoundary(double[] coordinates, double[] min, double[] max) {
        
        for (int i = 0; i < coordinates.length; i++) {
            if (coordinates[i] < min[i] || coordinates[i] >= max[i]) {
                return false;
            }
        }
        return true;
    }

}