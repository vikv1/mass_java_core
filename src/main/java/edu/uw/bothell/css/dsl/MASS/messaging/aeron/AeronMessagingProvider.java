/*

 	MASS Java Software License
	© 2012-2021 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2021 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.messaging.aeron;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.agrona.BufferUtil;
import org.agrona.CloseHelper;
import org.agrona.concurrent.EpochClock;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.SystemEpochClock;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.commons.lang3.SerializationUtils;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.messaging.AbstractMessagingProviderImpl;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSAckMessage;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSMessage;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;

public class AeronMessagingProvider extends AbstractMessagingProviderImpl {

	private static final int MAX_MESSAGE_SIZE_BYTES = 16777216;
	private static final int RETURN_RECEIPT_TIMEOUT = 10000;
	private static final boolean CLEANUP_SHM_DIRECTORIES = true;
	
    private static final int FRAGMENT_COUNT_LIMIT = 10;

	private static final int NODE_COMMS_STREAM_ID = 1001;
	private static final int PLACE_COMMS_STREAM_ID = 1002;
	private static final int AGENT_COMMS_STREAM_ID = 1003;
	private static final int ACK_COMMS_STREAM_ID = 1004;

	private static final String AERON_URL_PREFIX = "aeron:udp?endpoint=";
	private static final String AERON_URL_SUFFIX = "";
	private static final String AERON_FLOW_CONTROL_STRATEGY = "|fc=min";
	private static final String AERON_DIRECTORY_SUFFIX = "_mass";
	
	private final IdleStrategy idle = new SleepingIdleStrategy();
	
	// individual subscriptions per channel
	private Subscription ackSubscription = null;
	private Subscription agentSubscription = null;
	private Subscription placeSubscription = null;
	private Subscription nodeSubscription = null;

	// publications for transmitting messages, one per channel
	private Publication ackPublication = null;
	private Publication agentPublication = null;
	private Publication placePublication = null;
	private Publication nodePublication = null;
	
	// subscriber threads for receiving and assembling messages (Java objects)
	private Subscriber ackSubscriber = null;
	private Subscriber agentSubscriber = null;
	private Subscriber placeSubscriber = null;
	private Subscriber nodeSubscriber = null;
	
	// buffers for transmitting messages
	private final UnsafeBuffer ackPublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( 1024, 64 ) );
	private final UnsafeBuffer agentPublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( MAX_MESSAGE_SIZE_BYTES, 64 ) );
	private final UnsafeBuffer placePublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( MAX_MESSAGE_SIZE_BYTES, 64 ) );
	private final UnsafeBuffer nodePublicationBuffer = new UnsafeBuffer( BufferUtil.allocateDirectAligned( MAX_MESSAGE_SIZE_BYTES, 64 ) );
	
	// operation timeout clock
	EpochClock clock = new SystemEpochClock();
	
	private MediaDriver mediaDriver = null;
	
	private final AtomicBoolean running = new AtomicBoolean( true );

	private Aeron aeron = null;
	
	@Override
	public void init( String clusterCommunicationsAddress ) {
		
		String url = AERON_URL_PREFIX + clusterCommunicationsAddress + AERON_URL_SUFFIX;
		
        // Create an embedded media driver within this application
		MediaDriver.Context mediaDriverCtx = new MediaDriver.Context();
		mediaDriverCtx.dirDeleteOnStart( CLEANUP_SHM_DIRECTORIES );
		mediaDriverCtx.dirDeleteOnShutdown( CLEANUP_SHM_DIRECTORIES );

		// consistent directory name to reduce memory leakage if app crashes
		mediaDriverCtx.aeronDirectoryName( CommonContext.getAeronDirectoryName() + AERON_DIRECTORY_SUFFIX );	
		
		mediaDriver = MediaDriver.launchEmbedded( mediaDriverCtx );
        
        // create context, using default temporary directory for memory-mapped IO
		Aeron.Context aeronCtx = new Aeron.Context();
        aeronCtx.aeronDirectoryName( mediaDriver.aeronDirectoryName() );
        aeron = Aeron.connect( aeronCtx ); 
		
        // set up subscriptions to receive messages
        ackSubscription = aeron.addSubscription( url, ACK_COMMS_STREAM_ID );
        agentSubscription = aeron.addSubscription( url, AGENT_COMMS_STREAM_ID );
        placeSubscription = aeron.addSubscription( url, PLACE_COMMS_STREAM_ID );
        nodeSubscription = aeron.addSubscription( url, NODE_COMMS_STREAM_ID );
        
        // associate handlers with subscriptions
        ackSubscriber = new Subscriber( receiveAckMessage(), FRAGMENT_COUNT_LIMIT, running, idle, ackSubscription );
        agentSubscriber = new Subscriber( receiveAgentMessage(), FRAGMENT_COUNT_LIMIT, running, idle, agentSubscription );
        placeSubscriber = new Subscriber( receivePlaceMessage(), FRAGMENT_COUNT_LIMIT, running, idle, placeSubscription );
        nodeSubscriber = new Subscriber( receiveNodeMessage(), FRAGMENT_COUNT_LIMIT, running, idle, nodeSubscription );
        ackSubscriber.start();
        agentSubscriber.start();
        placeSubscriber.start();
        nodeSubscriber.start();
        
        // set up publications to transmit messages
        ackPublication = aeron.addPublication( url + AERON_FLOW_CONTROL_STRATEGY, ACK_COMMS_STREAM_ID );
        agentPublication = aeron.addPublication( url + AERON_FLOW_CONTROL_STRATEGY, AGENT_COMMS_STREAM_ID );
        placePublication = aeron.addPublication( url + AERON_FLOW_CONTROL_STRATEGY, PLACE_COMMS_STREAM_ID );
        nodePublication = aeron.addPublication( url + AERON_FLOW_CONTROL_STRATEGY, NODE_COMMS_STREAM_ID );
        
        // make sure all connections are made before proceeding
        long timeout = clock.time() + getConnectionTimeout(); 		// determine exit time
        while ( clock.time() <= timeout && ( !ackPublication.isConnected() || !agentPublication.isConnected() || !placePublication.isConnected() || !nodePublication.isConnected() ) ) {
        	idle.idle();
        }
        
	}

	@Override
	public <T> void sendPlaceMessage( MASSMessage< Serializable > message) {
		transmitMessage( placePublication, placePublicationBuffer, message );
	}

	@Override
	public <T> void sendNodeMessage( MASSMessage< Serializable > message ) {
		
		// node messaging is SYNCHRONOUS - set message to require a return receipt
		message.setReceiptRequired( true );
		
		// send the message
		transmitMessage( nodePublication, nodePublicationBuffer, message );
		
	}

	@Override
	public <T> void sendAgentMessage( MASSMessage< Serializable > message ) {
		transmitMessage( agentPublication, agentPublicationBuffer, message );	
	}

	@Override
	public void shutdown() {
		
		// stop subscriber loops
		agentSubscriber.shutdown();
		placeSubscriber.shutdown();
		nodeSubscriber.shutdown();
		
		agentPublication.close();
		placePublication.close();
		nodePublication.close();
		
		CloseHelper.quietClose( aeron );
		CloseHelper.quietClose( mediaDriver );
	
	}

	// transmit a message using a specified publication
	private void transmitMessage( Publication publication, UnsafeBuffer buffer, MASSMessage< Serializable > message ) {
		
		// serialize the message for transmit
		byte[] payload = SerializationUtils.serialize( message );

    	// place the serialized object in the Aeron buffer
    	buffer.putBytes( 0, payload );

    	// wait until the message is accepted by Aeron for transmit
    	// TODO - implement a delivery timeout
    	while ( publication.offer( buffer, 0, payload.length ) < 0 ) {
    	    idle.idle();
    	}
    	
    	// wait for return ACK?
    	if ( message.isReceiptRequired() ) {

    		// return receipt timeout
    	    long timeout = clock.time() + RETURN_RECEIPT_TIMEOUT; 

    	    // poll message ACK channel looking for a return receipt
    	    while( clock.time() <= timeout ) {
    	        
    	    	// TODO - what about multiple destination addresses? TEST!
    	    	if ( hasAck( message.getMessageID(), message.getDestinationAddress() ) ) {
    	    		
    	    		// got an ACK - exit this method
    	    		return;
    	    	
    	    	}

    	    	else {
    	            
    	    		// wait a tick before checking again
    	    		idle.idle();
    	        
    	    	}
    	    	
    	    }
    		
    	}
    	
	}

	// Subscriber accepts messages for a channel and builds up buffers for deserialization into Java objects
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

			MASSBase.getLogger().debug( "Aeron messaging subscriber starting up..." );
			
			// assembler's job is to take fragmented messages (ones too large for a single packet)
			// and build a single message from it
			final FragmentAssembler assembler = new FragmentAssembler( fragmentHandler );

			// continue polling until shutdown method is called
			while ( running.get() ) {

				final int fragmentsRead = subscription.poll( assembler, limit );
				idleStrategy.idle( fragmentsRead );
		
			}

			MASSBase.getLogger().debug( "Aeron messaging subscriber shutting down..." );
			
		}
		
		// request shutdown of message subscriber
		public void shutdown() {
			running.set( false );
		}
		
	}
	
    /* 
     * This method is called upon receiving a message on the "Agent" channel.
     * It's job is to take a populated buffer, convert the bytes back to a 
     * java object (deserialize), and pass the message off for delivery
     */
    private FragmentHandler receiveAgentMessage() {
        
    	return ( buffer, offset, length, header ) -> {

			MASSMessage< Serializable > message = null;
    		
    		// deserialize buffer contents to a MASS Message
    		try {
    			byte[] objBytes = new byte[ length ];
    			buffer.getBytes( offset, objBytes );
    			message = SerializationUtils.deserialize( objBytes );
    		}
    		
    		catch ( Exception e ) {
    			MASSBase.getLogger().error( "Unable to deserialize message!", e );
    			return;
    		}
    		
    		// deliver the message
    		deliverAgentMessage( message );
    		
        };
    
    }

    /* 
     * This method is called upon receiving a message on the "Place" channel.
     * It's job is to take a populated buffer, convert the bytes back to a 
     * java object (deserialize), and pass the message off for delivery
     */
    private FragmentHandler receivePlaceMessage() {
        
    	return ( buffer, offset, length, header ) -> {

			MASSMessage< Serializable > message = null;
    		
    		// deserialize buffer contents to a MASS Message
    		try {
    			byte[] objBytes = new byte[ length ];
    			buffer.getBytes( offset, objBytes );
    			message = SerializationUtils.deserialize( objBytes );
    		}
    		
    		catch ( Exception e ) {
    			MASSBase.getLogger().error( "Unable to deserialize message!", e );
    			return;
    		}
    		
    		// deliver the message
    		deliverPlaceMessage( message );
    		
        };
    
    }

    /* 
     * This method is called upon receiving a message on the "Node" channel.
     * It's job is to take a populated buffer, convert the bytes back to a 
     * java object (deserialize), and pass the message off for delivery
     */
    private FragmentHandler receiveNodeMessage() {
        
    	return ( buffer, offset, length, header ) -> {

			MASSMessage< Serializable > message = null;
    		
    		// deserialize buffer contents to a MASS Message
    		try {
    			byte[] objBytes = new byte[ length ];
    			buffer.getBytes( offset, objBytes );
    			message = SerializationUtils.deserialize( objBytes );
    		}
    		
    		catch ( Exception e ) {
    			MASSBase.getLogger().error( "Unable to deserialize Message!", e );
    			return;
    		}
    		
    		// deliver the message
    		deliverNodeMessage( message );
    		
        };
    
    }

    /* 
     * This method is called upon receiving a message on the "ACK" channel.
     * It's job is to take a populated buffer, convert the bytes back to a 
     * java object (deserialize), and pass the message off for delivery
     */
    private FragmentHandler receiveAckMessage() {
        
    	return ( buffer, offset, length, header ) -> {

			MASSAckMessage message = null;
    		
    		// deserialize buffer contents to an ACK Message
    		try {
    			byte[] objBytes = new byte[ length ];
    			buffer.getBytes( offset, objBytes );
    			message = SerializationUtils.deserialize( objBytes );
    		}
    		
    		catch ( Exception e ) {
    			MASSBase.getLogger().error( "Unable to deserialize message!", e );
    			return;
    		}
    		
    		// deliver the message
    		deliverAckMessage( message );
    		
        };
    
    }

	@Override
	public void sendAck( MASSAckMessage ackMessage ) {

		// serialize the ACK for transmit
		byte[] payload = SerializationUtils.serialize( ackMessage );

    	// place the serialized object in the proper buffer
		ackPublicationBuffer.putBytes( 0, payload );

    	// wait until the message is accepted by Aeron for transmit
    	// TODO - implement a delivery timeout
    	while ( ackPublication.offer( ackPublicationBuffer, 0, payload.length ) < 0 ) {
    	    idle.idle();
    	}

	}

}