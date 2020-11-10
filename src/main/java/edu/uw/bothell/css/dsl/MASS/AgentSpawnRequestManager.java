/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    BlockingQueue<AgentSpawnRequest> agentSpawnRequestQueue = new LinkedBlockingQueue<AgentSpawnRequest>();

    // Set for available agent ids for new agents to be spawned
    BlockingQueue<Integer> availableAgentIdsQueue = new LinkedBlockingQueue<>();

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

    /**
     * Initialize the manager specifying maximum number of active Agents
     * @param maxActiveAgentSize The maximum number of active Agents to allow
     */
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
            byte[] serializedAgent = agentSerializer.serializeAgent(agent);
            // setup spawn request object
            AgentSpawnRequest agentSpawnRequest = new AgentSpawnRequest();
            agentSpawnRequest.setSerializedAgent(serializedAgent);
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
            return agentSerializer.deserializeAgent(agentSpawnRequestQueue.poll().getSerializedAgent());
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
    protected void addAvailableAgentId(Integer availableAgentId)
    {
        availableAgentIdsQueue.add(availableAgentId);
    }
}
