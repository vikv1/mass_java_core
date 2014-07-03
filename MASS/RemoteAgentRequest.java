package MASS;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.Serializable;


/**
 *
 * @author TC
 */
public class RemoteAgentRequest implements Serializable
{
    private int destinationGlobalLinearIndex;
    private int agentsHandle;
    private int placesHandle;
    private int parentId;
    private int agentId;
    private Object[] arguments;
    private int eventId;
    private Object[] outMessages;
    private Object[] inMessages;
    
    public RemoteAgentRequest(int destIndex, Agent agent)
    {
        destinationGlobalLinearIndex = destIndex;
        serializeAgentParameters(agent);
    }   
    
    public int getDestinationGlobalLinearIndex() { return destinationGlobalLinearIndex; }
    
    /*
    * Creates a new agent for this MASS.Agents. agentId && parentId are set, as are
    * the agentsHandle and placesHandle. New agent has NO INDEX and NO PLACE.
    * @param argument the argument to pass to the constructor for the MASS.Agent subclass
    * @param parentId the ID number for the parent, -1 if agent has no parent
    * @return the new MASS.Agent, a part of the MASS.Agents, but IS NOT added to the MASS.Places
    */
    private void serializeAgentParameters(Agent agent)
    {
        agentsHandle = agent.agentsHandle;
        placesHandle = agent.placesHandle;
        parentId = agent.parentId;
        agentId = agent.agentId;
        arguments = agent.arguments;
        eventId = agent.eventId;
        outMessages = agent.outMessages;
        inMessages = agent.inMessages;
    } 
    
    public int getAgentsHandle() { return agentsHandle; }
    public int getPlacesHandle() { return placesHandle; }
    public int getParentId() { return parentId; }
    public int getAgentId() { return agentId; }
    public Object[] getArguments() { return arguments; }
    public int getEventId() { return eventId; }
    public Object[] getOutMessages() { return outMessages; }
    public Object[] getInMessages() { return inMessages; } 
}
