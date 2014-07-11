package MASS;

class AgentMigrationRequest {
    public AgentMigrationRequest( int destIndex, Agent agent ) {
	this.destGlobalLinearIndex = destIndex;
	this.agent = agent;
    }

    int destGlobalLinearIndex;
    Agent agent;
}
