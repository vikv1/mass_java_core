// ClimateData.java  --- Cherie Wasous 3.5.2014

import java.util.Iterator;
import java.util.Vector;

import MASS.Place; // Library for Multi-Agent Spatial Simulation

public class ClimateData extends Place {

	// Function identifiers
	public static final int compute_ = 0;
	public static final int collect_ = 1;
	public static final int dummyRoutine_ = 2;
	public static final int myMax_ = 3;

	public int time, day;
	public double moisture_flux[][];
	public double direction[][];

	// these are the max flux, x, y for this ClimateData place
	// that are determined as original data file is read and flux computed
	public int maxX, maxY;
	public double maxFlux; // what is ultimate minimum -(double_max) ??

	private static final int XRANGE = 123; // this relates to weather grid for
											// the NW region
	private static final int YRANGE = 162; // this relates to weather grid for
											// the NW region

	public boolean footprint; // indicates if agent has visited
	public int chunk; // size of each chunk on each node (to determine who
						// instantiates Vector)

	public static Vector<MaxClimateData> maxSeen;

	@Override
	public Object callMethod(int funcId, Object args) {
		switch (funcId) {
		case compute_:
			return compute(args);
		case dummyRoutine_:
			return dummyRoutine(args);
		case collect_:
			return collect(args);
		case myMax_:
			return myMax(args);
		}
		return null;
	}

	public ClimateData(Object arg) { // constructor
		chunk = (int) arg;
		footprint = false;
		time = index[0];
		day = index[1];
		moisture_flux = new double[XRANGE][YRANGE];
		direction = new double[XRANGE][YRANGE];
		maxFlux = 0.0;

		// Only instantiate collection Vector at the place element that is
		// at position "0,0" for each node
		if ((time == (MASS.MASS.getPid() * chunk)) && (day == 0)) {
			MASS.MASS.log("I'm place element[ " + time + ", " + day
					+ " ] and I'm instantiating maxSeen Vector");
			maxSeen = new Vector<MaxClimateData>();
		}
	}

	public Object compute(Object arg) {

		// MASS.MASS
		// .printResult("Got inside compute function of ClimateData time="
		// + time + ", day=" + day);

		// just spending time on some dummy calculations & assignments
		for (int x = 0; x < XRANGE; x++) {
			for (int y = 0; y < YRANGE; y++) {
				moisture_flux[x][y] = (time + 1) * (day + 1)
						* (x + (y / 1000.0));
			}
		}

		for (int x = 0; x < XRANGE; x++) {
			for (int y = 0; y < YRANGE; y++) {
				direction[x][y] = x + (y / 1000.0);
			}
		}

		for (int x = 0; x < XRANGE; x++) {
			for (int y = 0; y < YRANGE; y++) {
				if (maxFlux < moisture_flux[x][y]) { // TODO: What if more than
														// 1 have same maximum
														// value ??!!
					maxFlux = moisture_flux[x][y];
					maxX = x;
					maxY = y;
				}
			}
		}

		/*
		 * eventually, the previous "dummy" code will be replaced with real
		 * code, something like this: NetcdfFile input = NetcdfFile.open(
		 * getFileName( time, day ) ); iterX = ( input.findVariable( XWIND )
		 * ).read( ).getIndexIterate( ); iterY = ( input.findVariable( YWIND )
		 * ).read( ).getIndexIterate( ); iterQ = ( input.findVariable( MOISTURE
		 * ) ).read( ).getIndexIterate( );
		 * 
		 * while ( iterX.hasNext( ) ) { iterX.next( ); iterY.next( );
		 * iterQ.next( ); xW = ( double )iterX.getFloatCurrent( ); xY = ( double
		 * )iterY.getFloatCurrent( ); xQ = ( double )iterQ.getFloatCurrent( );
		 * moisture_flux[x][y] = q * Math.sqrt( Math.pow( xW, 2.0 ) + Math.pow(
		 * yW, 2.0 ) ); direction[x][y] = Math.atan2( xW, xY );
		 * 
		 * if ( maxFlux < moisture_flux[x][y] ) { maxFlux = moisture_flux[x][y];
		 * maxX = x; maxY = y; } }
		 */

		return null;
	}

	// just for an investigation/debugging
	public Object dummyRoutine(Object arg) {

		return null;
	}

	// public MaxClimateData collect( Object arg ) {
	public Object collect(Object arg) {

		// only one place at each node will be called to collect (main will use
		// callSome (eventually!))
		// (but then user app must know the particular ones to call (not good,
		// complicated for user))

		if ( ( (time == (MASS.MASS.getPid() * chunk) ) && (day == 0) ) ) {
			// only do this for one place element (the virtual "0,0") on each node

			// all place elements on this node have put their max into maxSeen,
			// so let place 0, 0 just look thru & find the max for this node &
			// return it
			MaxClimateData thisNodeMax = new MaxClimateData();

			// iterate over contents of maxSeen and find the maximum flux entry
			// and return it
			Iterator<MaxClimateData> iter = maxSeen.iterator();
			MASS.MASS
					.printResult("\nInside ClimateData.collect at this node's virtual place '0,0' (actual ["
							+ time + ", " + day + "]).  maxSeen is size: "
							+ maxSeen.size());
			while (iter.hasNext()) {
				MaxClimateData temp = iter.next();
				if (thisNodeMax.mcdFlux < temp.mcdFlux) {
					/*
					 * MASS.MASS.printResult("New max found in collect: " +
					 * thisNodeMax.mcdFlux + " < " + temp.mcdFlux + ",  dir=" +
					 * temp.mcdDir + ",  day=" + temp.mcdDay + ",  time=" +
					 * temp.mcdTime );
					 */
					thisNodeMax = temp;
				}
			}

			MASS.MASS
					.printResult("*** Inside collect, iteration done, thisNodeMax is flux= "
							+ thisNodeMax.mcdFlux
							+ ", dir= "
							+ thisNodeMax.mcdDir
							+ ",  day="
							+ thisNodeMax.mcdDay
							+ ",  time="
							+ thisNodeMax.mcdTime);

			// return (Object) thisNodeMax;
			return thisNodeMax;

		} else {
			// return (Object) null;
			return null;
		}
	}

	public Object myMax(Object arg) {

		MaxClimateData myMaxCD = new MaxClimateData();
		myMaxCD.mcdFlux = moisture_flux[maxX][maxY];
		myMaxCD.mcdDir = direction[maxX][maxY];
		myMaxCD.mcdDay = day;
		myMaxCD.mcdTime = time;
		myMaxCD.mcdX = maxX;
		myMaxCD.mcdY = maxY;
		
		MASS.MASS
		.printResult("*** Inside place element's myMax, setup to return: flux= "
				+ (int)myMaxCD.mcdFlux
				+ ", dir= "
				+ (int)myMaxCD.mcdDir
				+ ",  day="
				+ myMaxCD.mcdDay
				+ ",  time="
				+ myMaxCD.mcdTime
				+ ",  maxX="
				+ myMaxCD.mcdX
				+ ",  maxY="
				+ myMaxCD.mcdY);

		return myMaxCD;
	}

}
