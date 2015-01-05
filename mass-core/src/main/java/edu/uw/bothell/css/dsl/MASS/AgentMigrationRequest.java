package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
class AgentMigrationRequest implements Serializable {

	int destGlobalLinearIndex;
    Agent agent;
   
	public AgentMigrationRequest( int destIndex, Agent agent ) {
		
		this.destGlobalLinearIndex = destIndex;
		this.agent = agent;
    
	}

}
