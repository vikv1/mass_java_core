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

import java.util.Arrays;
import java.util.Vector;

import javax.xml.namespace.QName;

import java.util.ArrayList;

import edu.uw.bothell.css.dsl.MASS.annotations.OnCreation;
import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;
import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public class SpacePlacesBase extends PlacesBase {
    
	private String inputFile;
    private int dimensions;
    private int granularity;
    private double[] min;
    private double[] max;
    private double[] interval;
    private double[] subInterval;

    public SpacePlacesBase(int handle, String className, int dimensions, int granularity, double[] min, double[] max, int[] size, Object argument) {
    
        super(handle, className, size);
        this.granularity = granularity;
        this.dimensions = dimensions;
        this.min = min.clone();
        this.max = max.clone();

        //get interval
        double[] interval_temp = new double[dimensions];
        double[] subInterval_temp = new double[dimensions];
        for (int i = 0; i < min.length; i++) {
            interval_temp[i] = (max[i] - min[i]) / size[i];
            subInterval_temp[i] = interval_temp[i] / granularity;
        }
        this.interval = interval_temp.clone();
        this.subInterval = subInterval_temp.clone();

        // restrict to 2D space
        MASSBase.getLogger().debug( "SpacePlaces handle = " + getHandle() + ", class = " + getClassName() + 
                                        ", dimensions = " + dimensions + ", granularity = " + granularity + 
                                        "min = [" + min[0] + "," + min[1] + "], max = [" + max[0] + "," + 
                                        max[1] + "], size = " + getSize()[0] + "x" + getSize()[1] + ", interval = " + 
                                        interval[0] + "," + interval[1] + ", subInterval = " + subInterval[0] + "," + subInterval[1] + "." );
        //initialize space        
        init_all_space(argument);   
    }


    //constructor for creating SpacePlaces from inputFile
    public SpacePlacesBase(int handle, String className, int dimensions, int granularity, String inputFile, Object argument) {
    
        super(handle, className);
        this.granularity = granularity;
        this.dimensions = dimensions;
        this.inputFile = inputFile;

        //get the min, max and total number of input data
        double[][] parameters = SpaceUtilities.getInputParameters(inputFile, dimensions);
        this.min = parameters[0].clone();
        this.max = parameters[1].clone();


        //minus 1.0 on the min, and plus 1.0 on the max to avoid data points falling on the boundary
        //need a better solution to replace -/+1.0 as we do not know the data range 
        for (int i = 0 ; i < dimensions; i++) {
            this.min[i]--;
            this.max[i]++;
        }

        //get the size[] of Places 
        double totalNumOfPoints = parameters[2][0];
        //MASSBase.getLogger().debug("totalNumOfPoints = " + totalNumOfPoints);
        int[] size_temp = new int[dimensions];
        int size_singleDimension = (int)(Math.ceil(Math.pow((double)totalNumOfPoints, (1.0 / dimensions))) + 1.0);
        //MASSBase.getLogger().debug("size_single = " + size_singleDimension);
        Arrays.fill(size_temp, size_singleDimension);
        setSize(size_temp);

        //get interval
        double[] interval_temp = new double[dimensions];
        double[] subInterval_temp = new double[dimensions];
        for (int i = 0; i < min.length; i++) {
            interval_temp[i] = (max[i] - min[i]) / getSize()[i];
            subInterval_temp[i] = interval_temp[i] / granularity;
        }
        this.interval = interval_temp.clone();
        this.subInterval = subInterval_temp.clone();

        // restrict to 2D space
        MASSBase.getLogger().debug( "SpacePlaces handle = " + getHandle() + ", class = " + getClassName() + 
                                        ", dimensions = " + dimensions + ", granularity = " + granularity + 
                                        "min = [" + min[0] + "," + min[1] + "], max = [" + max[0] + "," + 
                                        max[1] + "], size = " + getSize()[0] + "x" + getSize()[1] + ", interval = " + 
                                        interval[0] + "," + interval[1] + ", subInterval = " + subInterval[0] + "," + subInterval[1] + "." );
        //initialize space        
        init_all_space(argument);   
    }

    private void init_all_space(Object argument) {

        MASSBase.getLogger().debug("SpacePlace - init_all_space");
        // TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation;
        if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( this );

        // For debugging
        MASSBase.getLogger().debug( "init_all_space handle" + getHandle() + ", class = " + getClassName() + 
            ", dimensions = " + dimensions + ", granularity = " + granularity + "min = [" + min[0] + "," + 
            min[1] + "], max = [" + max[0] + "," + max[1] + "], size = " + getSize()[0] + "x" + getSize()[1] );
                
        // calculate "total", which is equal to the number of dimensions in "size" 
        int total = MatrixUtilities.getMatrixSize( getSize() );
        //System.out.println("total = " + total);
        
        // stripe size is total number of places divided by the number of nodes
        MASSBase.getLogger().debug( "Calculating stripe size, total number of Places is {}", total );
        MASSBase.getLogger().debug( "Calculating stripe size, system size (number of nodes) is {}", MASSBase.getSystemSize() );
        int stripeSize = total / MASSBase.getSystemSize();
        //System.out.println("stripe size = " + stripeSize);
            
        // lower_boundary is the first place managed by this node
        lowerBoundary = stripeSize * MASSBase.getMyPid();

        // upperBoundary is the last place managed by this node
        upperBoundary = (MASSBase.getMyPid() < MASSBase.getSystemSize() - 1) ? lowerBoundary + stripeSize - 1 : total - 1;

        // placesSize is the total number of places managed by this node
        placesSize = upperBoundary - lowerBoundary + 1;

        MASSBase.getLogger().debug(String.format("init_all_space: { totalSize: %d, lowerBoundary: %d, upperBoundary: %d, placesSize: %d }",
                total, lowerBoundary, upperBoundary, placesSize));
           
        //  maintaining an entire set
        places = new SpacePlace[placesSize];
   
        // initialize all Places objects
        for (int i = 0; i < placesSize; i++) {
            // TODO - hack! should be able to set index on a Place without having to resort to calling back for it (should be pushed, not pulled)
            nextIndex = MatrixUtilities.getIndex(getSize(), lowerBoundary + i);
                
            Object[] finalArgs = new Object[2];
            SpacePlaceArgs spacePlaceArgs =  new SpacePlaceArgs(getHandle(), min, max, getSize(), granularity, nextIndex);
            finalArgs[0] = (Object) spacePlaceArgs;
            finalArgs[1] = argument;

            // instantiate and configure new place
            SpacePlace newPlace = null;
            try {
            
                    newPlace = objectFactory.getInstance(getClassName(), (Object) finalArgs);

                
            } catch (Exception e) {
                MASSBase.getLogger().error("SpacePlaces.init_all_space: {} not loaded and/or instantiated", getClassName(), e);
            }
				
			newPlace.setIndex( nextIndex );		// this is better behavior, not optimal, though
			MASSBase.getLogger().debug( "SpacePlace " + i + " {" + newPlace.toString() + "} created");

            // Place has been created
            eventDispatcher.queueAsync(OnCreation.class, newPlace);
            places[i] = newPlace;

        }
        // TODO - what to do when this exception is caught?
        
      
    }

    public int getDimensions() {
        return dimensions;
    }

    public double[] getInterval() {            
        return interval;
    }

    public double[] getSubInterval() {
        return subInterval;
    }

    public double[] getMin() {
        return min;
    }

    public double[] getMax() {
        return max;
    }

    public int getGranularity() {
        return granularity;
    }
}