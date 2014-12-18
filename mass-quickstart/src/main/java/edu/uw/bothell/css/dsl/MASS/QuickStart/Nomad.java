package edu.uw.bothell.css.dsl.MASS.QuickStart;

import java.net.InetAddress;

import edu.uw.bothell.css.dsl.MASS.Agent;

public class Nomad extends Agent {

	private Object obj;
	
	public static final int GET_HOSTNAME = 0;
	public static final int MIGRATE = 1;
	
	
	/**
	 * This constructor will be called upon instantiation by MASS
	 * The Object supplied MAY be the same object supplied when Places was created
	 * @param obj
	 */
	public Nomad(Object obj) {
		this.obj = obj;
	}

	/**
	 * This method is called when "callAll" is invoked from the master node
	 */
	public Object callMethod(int method, Object o) {
		
		switch (method) {
		
			case GET_HOSTNAME:
				return findHostName(o);
		
			case MIGRATE:
				return move(o);
			
		
		
		default:
			return new String("Unknown Method Number: " + method);
		
		}
		
	}
	
	/**
	 * Return a String identifying where this Agent is actually located
	 * @param o
	 * @return
	 */
	public Object findHostName(Object o){

		try{
             return (String) "Agent located at: " + InetAddress.getLocalHost().getCanonicalHostName() + " " + Integer.toString(index[0]) + ":" + Integer.toString(index[1]) + ":" + Integer.toString(index[2]);
        }
        
        catch(Exception e) {
        	return "Error : " + e.getLocalizedMessage() + e.getStackTrace();
        }
        
    }
	
	/**
	 * Move this Agent to the next position in the X-coordinate
	 * @param o
	 * @return
	 */
	public Object move(Object o) {
		
		int xModifier = this.place.index[0];
        int yModifier = this.place.index[1];
        int zModifier = this.place.index[2];
        xModifier++;
	        
        migrate(xModifier, yModifier, zModifier);
        return o;

	}
	
}
