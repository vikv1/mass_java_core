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

import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.event.EventDispatcher;
import edu.uw.bothell.css.dsl.MASS.event.SimpleEventDispatcher;
import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;

public class SpacePlaces extends SpacePlacesBase {

    private double[] interval;
    private int[] nextIndex;
	//private ArrayList<Double[]> inputData;
    private int lowerBoundary;
    private int upperBoundary;
    private int placesSize;
    private int total;  // total number of place defined by user
    private ObjectFactory objectFactory = SimpleObjectFactory.getInstance();
    private EventDispatcher eventDispatcher = SimpleEventDispatcher.getInstance();
    /**
     * SpacePlaces constructor that creates spacePlaces with given dimension and granularity.
     * @param handle	A unique identifier that designates a group of places.
     * 					Must be unique over all machines.
     * @param className	The user-implemented class Places are constructed from.
     * @param dimensions initialization parameters of spaces.
     * @param granularity
     * @param inputData
     * @param size defined by user, integer from 1 to n; default = 1.
     */
    public SpacePlaces(int handle, String className, int dimensions, int granularity, double[] min, double[] max, int[] size, Object argument) {
        super(handle, className, dimensions, granularity, min, max, size, argument);
        init_master_space(argument);
    }

    /**
     * SpacePlaces constructor that creates spacePlaces which reads data from inputfile.
     * @param handle	A unique identifier that designates a group of places.
     * 					Must be unique over all machines.
     * @param className	The user-implemented class Places are constructed from.
     * @param dimensions initialization parameters of spaces.
     * @param granularity
     * @param inputFile
     */
    public SpacePlaces(int handle, String className, int dimensions, int granularity, String inputFile, Object argument) {
        super(handle, className, dimensions, granularity, inputFile, argument);
        init_master_space(argument);
    }

    /**
     * Initializes the spacePlaces with the given arguments and boundary width.
     * @param argument
     * @param boundaryWidth
     */
    private void init_master_space( Object argument) {

		// create a list of all host names;  
		// the master IP name
		Vector<String> hosts = new Vector<String>( );
		
		try {
		    hosts.add( MASS.getMasterNode().getHostName() );
		} catch ( Exception e ) {
			MASSBase.getLogger().error( "init_master_space: InetAddress.getLocalHost( ) ", e );
		    System.exit( -1 );
		}
		
		// all the slave IP names
		for ( MNode node : MASS.getRemoteNodes() ) {
		    hosts.add( node.getHostName( ) );
		}
	
		// create a new list for message
		Message m = new Message( Message.ACTION_TYPE.SPACE_PLACES_INITIALIZE, getSize(), getHandle(), getDimensions(),
					 getGranularity(), getClassName(), getMin(), getMax(), argument, hosts );
		MASSBase.getLogger().debug( "Message: handle = "+ getHandle() + ", dimensions = " + getDimensions() + ", granularity = "+ getGranularity() + 
                ", classname = " + getClassName() + ", min = " + getMin()[0] + "," + getMin()[1] + "], max = [" + getMax()[0] + "," + getMax()[1]+ 
                ", argument = " + argument );	

		// send a SPACE_PLACES_INITIALIZE message to each slave
		MASSBase.getLogger().debug( "SPACE_PLACES_INITIALIZE sent to all remote nodes" );
		MASS.getRemoteNodes().forEach( place -> place.sendMessage( m ) );
		
		// establish all inter-node connections within setHosts( )
		MASSBase.setHosts( hosts );
	
		// register this places in the places hash map
		MASSBase.getPlacesMap().put( getHandle(), this );
		
		// Synchronized with all slave processes
		MASS.barrierAllSlaves( );

    }
    
    public int getTotal() {
        return total;
    }

 
  
}
