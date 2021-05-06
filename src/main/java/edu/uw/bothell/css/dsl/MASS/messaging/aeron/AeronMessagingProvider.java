package edu.uw.bothell.css.dsl.MASS.messaging.aeron;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.agrona.BufferUtil;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.commons.lang3.SerializationUtils;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.messaging.AbstractMessagingProviderImpl;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSMessage;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;

public class AeronMessagingProvider extends AbstractMessagingProviderImpl {

    private static final int FRAGMENT_COUNT_LIMIT = 10;

	private static final int NODE_COMMS_STREAM_ID = 1001;
	private static final int PLACE_COMMS_STREAM_ID = 1002;
	private static final int AGENT_COMMS_STREAM_ID = 1003;

	private static final String AERON_URL_PREFIX = "aeron:udp?endpoint=";
	private static final String AERON_URL_SUFFIX = "";
	
	private final IdleStrategy idle = new SleepingIdleStrategy();
	
	Subscription agentSubscription = null;
	Subscription placeSubscription = null;
	Subscription nodeSubscription = null;

	Publication agentPublication = null;
	Publication placePublication = null;
	Publication nodePublication = null;
	
	final UnsafeBuffer agentPublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( 1024, 64 ) );
	final UnsafeBuffer placePublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( 1024, 64 ) );
	final UnsafeBuffer nodePublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( 1024, 64 ) );
	
	final AtomicBoolean running = new AtomicBoolean( true );

	Aeron aeron = null;
	
	@Override
	public void init( String clusterCommunicationsAddress ) {
		
		String url = AERON_URL_PREFIX + clusterCommunicationsAddress + AERON_URL_SUFFIX;
		
        // Create an embedded media driver within this application
		MediaDriver mediaDriver = MediaDriver.launchEmbedded();
        
        // create context, using default temporary directory for memory-mapped IO
		Aeron.Context ctx = new Aeron.Context();
        ctx.aeronDirectoryName( mediaDriver.aeronDirectoryName() );
        aeron = Aeron.connect( ctx ); 
		
        // set up subscriptions to receive messages
        agentSubscription = aeron.addSubscription( url, AGENT_COMMS_STREAM_ID );
        placeSubscription = aeron.addSubscription( url, PLACE_COMMS_STREAM_ID );
        nodeSubscription = aeron.addSubscription( url, NODE_COMMS_STREAM_ID );
        
        // associate handlers with subscriptions
        Subscriber agentSubscriber = new Subscriber( receiveAgentMessage(), FRAGMENT_COUNT_LIMIT, running, idle, agentSubscription );
        Subscriber placeSubscriber = new Subscriber( receivePlaceMessage(), FRAGMENT_COUNT_LIMIT, running, idle, placeSubscription );
//        Subscriber nodeSubscriber = new Subscriber( receiveAgentMessage(), FRAGMENT_COUNT_LIMIT, running, idle, agentSubscription );
        agentSubscriber.start();
        placeSubscriber.start();
        
        // set up publications to transmit messages
        agentPublication = aeron.addPublication( url, AGENT_COMMS_STREAM_ID );
        placePublication = aeron.addPublication( url, PLACE_COMMS_STREAM_ID );
        nodePublication = aeron.addPublication( url, NODE_COMMS_STREAM_ID );
        
	}

	@Override
	public <T> void sendPlaceMessage(MASSMessage<Serializable> message) {
		transmitMessage( placePublication, placePublicationBuffer, message );
	}

	@Override
	public <T> void sendNodeMessage(MASSMessage<Serializable> message) {
		transmitMessage( nodePublication, nodePublicationBuffer, message );
	}

	@Override
	public <T> void sendAgentMessage(MASSMessage<Serializable> message) {
		transmitMessage( agentPublication, agentPublicationBuffer, message );	
	}

	@Override
	public void shutdown() {
		aeron.close();
	}

	// transmit a message using a specified publication
	private void transmitMessage( Publication publication, UnsafeBuffer buffer, MASSMessage<Serializable> message ) {
		
		// serialize the message for transmit
		byte[] payload = SerializationUtils.serialize( message );

    	// place the serialized object in the Aeron buffer
    	buffer.putBytes( 0, payload );

    	// wait until the message is accepted by Aeron for transmit
    	while ( publication.offer( buffer, 0, payload.length ) < 0 ) {
    	    idle.idle();
    	}
		
	}

	private class Subscriber extends Thread {

		FragmentHandler fragmentHandler;
		int limit;
		AtomicBoolean running;
		IdleStrategy idleStrategy;
		Subscription subscription;
		
		Subscriber( final FragmentHandler fragmentHandler, final int limit, final AtomicBoolean running, final IdleStrategy idleStrategy, final Subscription subscription ) {
			
			this.fragmentHandler = fragmentHandler;
			this.limit = limit;
			this.running = running;
			this.idleStrategy = idleStrategy;
			this.subscription = subscription;
			
		}
		
		@Override
		public void run() {

			final FragmentAssembler assembler = new FragmentAssembler( fragmentHandler );

			while ( running.get() ) {

				final int fragmentsRead = subscription.poll( assembler, limit );
				idleStrategy.idle( fragmentsRead );
		
			}

		}
		
	}
	
    private FragmentHandler receiveAgentMessage() {
        
    	return ( buffer, offset, length, header ) -> {

    		@SuppressWarnings("rawtypes")
			MASSMessage message = null;
    		
    		// deserialize buffer contents to a MASS Message
    		try {
    			byte[] objBytes = new byte[ length ];
    			buffer.getBytes( offset, objBytes );
    			message = SerializationUtils.deserialize( objBytes );
    		}
    		
    		catch ( Exception e ) {
    			MASSBase.getLogger().error( "Unable to deserialize MASSMessage!", e );
    			return;
    		}
    		
    		// deliver the message
    		deliverAgentMessage( message );
    		
        };
    
    }

    private FragmentHandler receivePlaceMessage() {
        
    	return ( buffer, offset, length, header ) -> {

    		@SuppressWarnings("rawtypes")
			MASSMessage message = null;
    		
    		// deserialize buffer contents to a MASS Message
    		try {
    			byte[] objBytes = new byte[ length ];
    			buffer.getBytes( offset, objBytes );
    			message = SerializationUtils.deserialize( objBytes );
    		}
    		
    		catch ( Exception e ) {
    			MASSBase.getLogger().error( "Unable to deserialize MASSMessage!", e );
    			return;
    		}
    		
    		// deliver the message
    		deliverPlaceMessage( message );
    		
        };
    
    }

}