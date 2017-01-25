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

    // Max active agents allowed in the node
    private final int MAX_ACTIVE_AGENT_SIZE;
    private final int MAX_ACTIVE_AGENT_SIZE_DEFAULT_VALUE = 64;

    public AgentSpawnRequestManager()
    {
        MAX_ACTIVE_AGENT_SIZE = MAX_ACTIVE_AGENT_SIZE_DEFAULT_VALUE;
    }

    public AgentSpawnRequestManager(int maxActiveAgentSize)
    {
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
        // let it run in the system
        if ((currentActiveAgentSize + 1) <= MAX_ACTIVE_AGENT_SIZE)
        {
            return true;
        }
        // serialize the agent object
        else
        {
            // serialization
            return false;
        }
    }
}
