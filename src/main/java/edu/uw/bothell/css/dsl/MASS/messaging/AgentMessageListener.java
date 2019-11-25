package edu.uw.bothell.css.dsl.MASS.messaging;

import edu.uw.bothell.css.dsl.MASS.Agent;

public interface AgentMessageListener {

	public <T extends Agent> void setSubject(T agent);
	
}
