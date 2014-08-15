package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

class AgentMigrationRequest implements Serializable {
    public AgentMigrationRequest( int destIndex, Agent agent ) {
	this.destGlobalLinearIndex = destIndex;
	this.agent = agent;
    }

    int destGlobalLinearIndex;
    Agent agent;
}
