package main.java.com.dlb.utils;

/**
 * 
 * @author Bhargav Mistry
 *
 */
public class Slice {

	/**
	 * lower bound of the slice.
	 */
	private int lowerBound;
	/**
	 * upper bound of the slice.
	 */
	private int upperBound;
	
	public Slice() {
		
	}
	
	/**
	 * Constructor
	 * 
	 * @param lowerBound
	 * @param upperBound
	 */
	public Slice(int lowerBound, int upperBound) {
		this.setLowerBound(lowerBound);
		this.setUpperBound(upperBound);
	}

	/**
	 * returns lower bound.
	 * @return
	 */
	public int getLowerBound() {
		return lowerBound;
	}

	/**
	 * Set lower bound
	 * @param lowerBound
	 */
	public void setLowerBound(int lowerBound) {
		this.lowerBound = lowerBound;
	}

	/**
	 * Get upper bound
	 * @return
	 */
	public int getUpperBound() {
		return upperBound;
	}

	/**
	 * Set upper bound.
	 * @param upperBound
	 */
	public void setUpperBound(int upperBound) {
		this.upperBound = upperBound;
	}
}
