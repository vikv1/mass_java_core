package edu.uw.bothell.css.dsl.MASS.messaging.hazelcast;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.messaging.AgentMessageListener;

public class HazelcastMessageListener implements MessageListener, AgentMessageListener {

	private Agent agent;
	
	@Override
	public void onMessage( Message message ) {

		// call the appropriate method in the Agent to receive the message
		Integer test = ( Integer ) message.getMessageObject();
		System.out.println("Message received: " + test);

	}

	@Override
	public <T extends Agent> void setSubject( T agent ) {

		this.agent = agent;
		
	}

}
