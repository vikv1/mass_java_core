package main.java.com.dlb.impl;

import main.java.com.dlb.interfaces.DLBBase;

/**
 * 
 * @author Bhargav Mistry
 *
 */
public class SlopeBasedAlgo implements DLBBase {

	@Override
	public double predict(double xVal) {
		return 0;
	}

	@Override
	public double predict(double x1, double y1, double x2, double y2) {
		return ((y2-y1)/(x2-x1));
	}
	
	public double predictLoad(double x1, double y1, double x2, double slope) {
		return (slope * (x2-x1)) + y1;
	}

}
