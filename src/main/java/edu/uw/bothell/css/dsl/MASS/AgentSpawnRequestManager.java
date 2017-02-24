package edu.uw.bothell.css.dsl.MASS;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by utku on 1/24/17.
 *
 * In each computing node, this class is responsible for evaluating agent spawn requests
 *  and deciding that whether they should be actively running in the system or they
 *  should be serialized and stored in the queue after allocation.
 */
public class AgentSpawnRequestManager
{
    // Queue for agent spawn requests
    Queue<AgentSpawnRequest> agentSpawnRequestQueue = new LinkedList<AgentSpawnRequest>();

    // Set for available agent ids for new agents to be spawned
    Queue<Integer> availableAgentIdsQueue = new LinkedList<>();

    // Agent serializer
    AgentSerializer agentSerializer;

    // Max active agents allowed in the node
    private final int MAX_ACTIVE_AGENT_SIZE;
    private final int MAX_ACTIVE_AGENT_SIZE_DEFAULT_VALUE = 64;

    public AgentSpawnRequestManager()
    {
        agentSerializer = AgentSerializer.getInstance();
        MAX_ACTIVE_AGENT_SIZE = MAX_ACTIVE_AGENT_SIZE_DEFAULT_VALUE;
    }

    public AgentSpawnRequestManager(int maxActiveAgentSize)
    {
        agentSerializer = AgentSerializer.getInstance();
        // check if max active agent size is legit
        if (maxActiveAgentSize > 0)
        {
            MAX_ACTIVE_AGENT_SIZE = maxActiveAgentSize;
        }
        else
        {
            MAX_ACTIVE_AGENT_SIZE = MAX_ACTIVE_AGENT_SIZE_DEFAULT_VALUE;
        }
    }

    /**
     * Returns true if max agent size is not yet reached and spawned agent
     *  should run in the system. Serializes the agent and returns false, otherwise.
    * */
    protected boolean shouldAgentRunInTheSystem(Agent agent, int currentActiveAgentSize)
    {
        //System.out.println("shouldAgentRunInTheSystem - currentActiveAgentSize: " + currentActiveAgentSize);
        // let it run in the system
        if ((currentActiveAgentSize + 1) <= MAX_ACTIVE_AGENT_SIZE)
        {
            //System.out.println("YES - agent is let run");
            return true;
        }
        // serialize the agent object
        else
        {
            //System.out.println("NO - agent is serialized");
            // serialization
            String serializedAgentIdentifier = agentSerializer.serializeAgent(agent);
            // setup spawn request object
            AgentSpawnRequest agentSpawnRequest = new AgentSpawnRequest();
            agentSpawnRequest.setSerializedAgentIdentifier(serializedAgentIdentifier);
            // TODO index storing might be unnecessary
            //agentSpawnRequest.setIndex(agent.getIndex());
            agentSpawnRequestQueue.add(agentSpawnRequest);
            //System.out.println("serialization done.. added to the queue with id: " + serializedAgentIdentifier);
            /*
            System.out.print("in queue: ");
            for (AgentSpawnRequest asr : agentSpawnRequestQueue)
            {
                System.out.print(asr.getSerializedAgentIdentifier());
            }
            System.out.println();
            */
            return false;
        }
    }

    /**
     * Returns next agent spawn request in the queue, null if there is none.
     * */
    protected Agent getNextAgentSpawnRequest()
    {
        //System.out.println("getNextAgentSpawnRequest");
        // check if there is an element in the queue
        if (agentSpawnRequestQueue.size() > 0)
        {
            //System.out.println("agent is de-serialized");
            // deserialization
            return agentSerializer.deserializeAgent(agentSpawnRequestQueue.poll().getSerializedAgentIdentifier());
        }
        // no agent spawn request
        else
        {
            //System.out.println("returning null");
            return null;
        }
    }

    /**
     * Returns next available agent id in the queue, -1 if there is none.
     * */
    protected Integer getNextAvailableAgentId()
    {
        // check if there is an element in the queue
        if (availableAgentIdsQueue.size() > 0)
        {
            return availableAgentIdsQueue.poll();
        }
        // no available agent id
        else
        {
            return -1;
        }
    }

    /**
     * Adds available agent id to the queue.
     *
     * @param availableAgentId agent id to be added.
     */
    protected void addAvailabeAgentId(Integer availableAgentId)
    {
        availableAgentIdsQueue.add(availableAgentId);
    }
}
