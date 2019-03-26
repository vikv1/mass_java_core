package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.annotations.OnArrival;
import edu.uw.bothell.css.dsl.MASS.annotations.OnCreation;
import edu.uw.bothell.css.dsl.MASS.annotations.OnDeparture;

@SuppressWarnings("serial")
public class SimpleTestAgent extends Agent {

	// the value of this field is used to determine if event methods were actually invoked
	private int privateAgentData = 0;
	
	public SimpleTestAgent( Object obj ) {
		
	}
	
	public int getPrivateAgentData() {
		return privateAgentData;
	}

	@OnArrival
	public void onArrival() {
		
		// simple increment to verify method was actually called
		privateAgentData ++;
		
	}

	@OnCreation
	public void onCreation() {
		
		// simple increment to verify method was actually called
		privateAgentData += 10;
		
	}

	@OnDeparture
	public void onDeparture() {
		
		// simple decrement to verify method was actually called
		privateAgentData --;
		
	}

	public void setPrivateAgentData(int privateAgentData) {
		this.privateAgentData = privateAgentData;
	}
	
}
