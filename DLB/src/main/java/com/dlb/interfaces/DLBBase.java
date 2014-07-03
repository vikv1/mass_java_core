package main.java.com.dlb.interfaces;

/**
 * 
 * @author Bhargav Mistry
 *
 */
public interface DLBBase {
	
	/**
	 * calculate slope.
	 * 
	 * @param xVal
	 * @return
	 */
	public double predict(double xVal);
	
	/**
	 * predict slope based on 
	 * x and y coordinates.
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
	public double predict(double x1, double y1, double x2, double y2);
}
