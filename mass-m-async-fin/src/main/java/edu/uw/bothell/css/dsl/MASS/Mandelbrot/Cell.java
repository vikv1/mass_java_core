package edu.uw.bothell.css.dsl.MASS.Mandelbrot;

import java.net.InetAddress;

import edu.uw.bothell.css.dsl.MASS.Place;

public class Cell extends Place {

	private Object obj;
	
	public static final int GET_HOSTNAME = 0;
	
	
	/**
	 * This constructor will be called upon instantiation by MASS
	 * The Object supplied MAY be the same object supplied when Places was created
	 * @param obj
	 */
	public Cell(Object obj) {
		this.obj = obj;
	}
	
	/**
	 * This method is called when "callAll" is invoked from the master node
	 */
	public Object callMethod(int method, Object o) {
		
		switch (method) {
		
		case GET_HOSTNAME:
			return findHostName(o);
		
		
		
		default:
			return new String("Unknown Method Number: " + method);
		
		}
		
	}
	
	public Object findHostName(Object o){
		
		try{
        	return (String) "Place located at: " + InetAddress.getLocalHost().getCanonicalHostName() +" " + Integer.toString(getIndex()[0]) + ":" + Integer.toString(getIndex()[1]) + ":" + Integer.toString(getIndex()[2]);
        }
		
		catch (Exception e) {
			return "Error : " + e.getLocalizedMessage() + e.getStackTrace();
		}
    
	}

}
