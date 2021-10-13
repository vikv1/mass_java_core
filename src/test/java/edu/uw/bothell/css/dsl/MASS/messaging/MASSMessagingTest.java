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

package edu.uw.bothell.css.dsl.MASS.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.capture;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.PlacesBase;
import edu.uw.bothell.css.dsl.MASS.SimpleTestAgent;
import edu.uw.bothell.css.dsl.MASS.SimpleTestPlace;

/**
 * Tests functionality common to all implementations of messaging
 */
public class MASSMessagingTest extends AbstractTest {

	@Mock
	private MessagingProvider mockMessagingProvider;
	
	// the class under test
	private static MASSMessaging messenger = MASSMessaging.getInstance();
	
	// establish a simulation space for generating linear indexes
	private static final int[] size = new int[] { 3 };
	
	@BeforeAll
	public static void configureMASS() {
		MASS.setCurrentPlacesBase( new PlacesBase( 0, SimpleTestPlace.class.getName(), 0, null, size ) );
	}

	@AfterAll
	public static void resetMASS() {
		MASS.setCurrentPlacesBase( null );
	}
	
	@BeforeEach
	public void setup() {
		
		// make sure the messaging system is using the mock messaging provider
		messenger.setMessagingProvider( mockMessagingProvider );

	}
	
	@Test
	public void getRandomMulticastAddress() throws Exception {

		// must get *something* from calling this method
		String address = MASSMessaging.getRandomMulticastAddress();
		assertThat( address ).as( "Must return an IP address!" ).isNotNull();
		
		InetAddress ip = InetAddress.getByName( address );
		assertThat( ip.isMulticastAddress() ).as( "Must be a valid multicast address" ).isTrue();
		
		replayAll();
		
	}
	
	@Test
	public void getRandomPort() {
		
		// test a small population of port numbers to make sure they're in the right range
		for ( int i = 0; i < 500; i ++ ) {
			
			int portNum = MASSMessaging.getRandomPort();
			
			assertThat( portNum ).as( "Random port number must be between 1024 and 65535!" ).isBetween( 1024, 65535 );
			
		}
		
		replayAll();
		
	}
	
	@Test
	public void sendSingleAgentMessage() {
		
		SimpleTestAgent agent = new SimpleTestAgent( null );
		String message = randomString();

		// instruct messaging system to send the Agent message and grab the object sent
		Capture<MASSMessage> capturedMASSMessage = EasyMock.newCapture();
		mockMessagingProvider.sendAgentMessage( capture( capturedMASSMessage ) );

		replayAll();

		// queue up a message to be sent to the Agent
		messenger.sendAgentMessage( agent.getAgentId(), message );

		// "transmit" the message
		messenger.flushAgentMessages();

		// make sure the message sent was "transmitted" correctly
		String txMessage = ( String ) capturedMASSMessage.getValue().getMessage();
		assertThat( txMessage.contentEquals( message ) ).as( "Message should be delivered to the Agent unmodified" ).isTrue();
		assertThat( capturedMASSMessage.getValue().getDestinationAddress() ).as( "Message should be delivered to the correct Agent" ).isEqualTo( agent.getAgentId() );
		
		verifyAll();
		
	}

	@Test
	public void sendBroadcastAgentMessage() {
		
		SimpleTestAgent agent = new SimpleTestAgent( null );
		String message = randomString();

		// instruct messaging system to send the message and grab the object sent
		Capture<MASSMessage> capturedMASSMessage = EasyMock.newCapture();
		mockMessagingProvider.sendAgentMessage( capture( capturedMASSMessage ) );

		replayAll();

		// queue up a message to be sent to all Agents
		messenger.sendAgentMessage( MessageDestination.ALL_AGENTS, message );

		// "transmit" the message
		messenger.flushAgentMessages();

		// make sure the message sent was "transmitted" correctly
		String txMessage = ( String ) capturedMASSMessage.getValue().getMessage();
		assertThat( txMessage.contentEquals( message ) ).as( "Message should be delivered to the Agent unmodified" ).isTrue();
		assertThat( capturedMASSMessage.getValue().getDestinationAddress() ).as( "Message should be delivered to the correct Place" ).isEqualTo( MessageDestination.ALL_AGENTS.getValue() );
		
		verifyAll();
		
	}

	@Test
	public void sendBroadcastPlaceMessage() {
		
		String message = randomString();
		
		// instruct messaging system to send the Place message and grab the object sent
		Capture<MASSMessage> capturedMASSMessage = EasyMock.newCapture();
		mockMessagingProvider.sendPlaceMessage( capture( capturedMASSMessage ) );

		replayAll();

		// queue up a message to be sent to all Places
		messenger.sendPlaceMessage( MessageDestination.ALL_PLACES, message );

		// "transmit" the message
		messenger.flushPlaceMessages();

		// make sure the message sent was "transmitted" correctly
		String txMessage = ( String ) capturedMASSMessage.getValue().getMessage();
		assertThat( txMessage.contentEquals( message ) ).as( "Message should be delivered to the Place unmodified" ).isTrue();
		assertThat( capturedMASSMessage.getValue().getDestinationAddress() ).as( "Message should be delivered to the correct Place" ).isEqualTo( MessageDestination.ALL_PLACES.getValue() );
		
		verifyAll();
		
	}

	@Test
	public void sendSinglePlaceMessage() {
		
		String message = randomString();
		int address = Math.abs( randomInt() );
		
		// instruct messaging system to send the Place message and grab the object sent
		Capture<MASSMessage> capturedMASSMessage = EasyMock.newCapture();
		mockMessagingProvider.sendPlaceMessage( capture( capturedMASSMessage ) );

		replayAll();

		// queue up a message to be sent to all Places
		messenger.sendPlaceMessage( address, message );

		// "transmit" the message
		messenger.flushPlaceMessages();

		// make sure the message sent was "transmitted" correctly
		String txMessage = ( String ) capturedMASSMessage.getValue().getMessage();
		assertThat( txMessage.contentEquals( message ) ).as( "Message should be delivered to the Place unmodified" ).isTrue();
		assertThat( capturedMASSMessage.getValue().getDestinationAddress() ).as( "Message should be delivered to the correct Place" ).isEqualTo( address );
		
		verifyAll();
		
	}
	
	@Test
	public void init() {
		
		String address = randomString();
		
		// message provider should receive the cluster communications address during init
		mockMessagingProvider.init( address );
		
		replayAll();
		
		// perform the init
		messenger.init( address );
		
		verifyAll();
		
	}
	
	@Test
	public void shutdown() {
		
		// messaging provider should be instructed to shut down
		mockMessagingProvider.shutdown();
		
		replayAll();
		
		// request shutdown
		messenger.shutdown();
		
		verifyAll();
		
	}
	
	@Test
	public void sendLinearIndexedPlaceMessage() {
		
		String message = randomString();
		
		// we're only sending messages to a single Place, with linear index of "1"
		int[] destPlaces = new int[] { 1 };
		
		// instruct messaging system to send the Place messages and grab the objects sent
		Capture<MASSMessage> capturedMASSMessagePlace = EasyMock.newCapture();
		mockMessagingProvider.sendPlaceMessage( capture( capturedMASSMessagePlace ) );

		replayAll();

		// queue up a message to be sent to the Place
		messenger.sendPlaceMessage( destPlaces, message );

		// "transmit" the message
		messenger.flushPlaceMessages();

		// make sure the message sent was "transmitted" correctly
		String txMessageA = ( String ) capturedMASSMessagePlace.getValue().getMessage();
		assertThat( txMessageA.contentEquals( message ) ).as( "Message should be delivered to Place unmodified" ).isTrue();
		assertThat( capturedMASSMessagePlace.getValue().getDestinationAddress() ).as( "Place at linear index 1 should receive transmitted message" ).isEqualTo( 1 );
		
		verifyAll();
		
	}

	@Test
	public void sendMultiplePlaceMessage() {
		
		String message = randomString();
		
		// sending messages to multiple places, by linear index
		Set< Integer > addresses = new HashSet<>();
		addresses.add( 5 );
		addresses.add( 42 );
		
		// instruct messaging system to send the Place messages and grab the objects sent
		Capture<MASSMessage> capturedMASSMessagePlaceA = EasyMock.newCapture();
		mockMessagingProvider.sendPlaceMessage( capture( capturedMASSMessagePlaceA ) );
		Capture<MASSMessage> capturedMASSMessagePlaceB = EasyMock.newCapture();
		mockMessagingProvider.sendPlaceMessage( capture( capturedMASSMessagePlaceB ) );

		replayAll();

		// queue up a message to be sent to the Place
		messenger.sendPlaceMessage( addresses, message );

		// "transmit" the message
		messenger.flushPlaceMessages();

		// make sure the messages sent were "transmitted" correctly
		String txMessageA = ( String ) capturedMASSMessagePlaceA.getValue().getMessage();
		String txMessageB = ( String ) capturedMASSMessagePlaceB.getValue().getMessage();
		assertThat( txMessageA.contentEquals( message ) ).as( "Message should be delivered to Place unmodified" ).isTrue();
		assertThat( txMessageB.contentEquals( message ) ).as( "Message should be delivered to Place unmodified" ).isTrue();
		assertThat( capturedMASSMessagePlaceA.getValue().getDestinationAddress() ).as( "Place at linear index 5 should receive transmitted message" ).isEqualTo( 5 );
		assertThat( capturedMASSMessagePlaceB.getValue().getDestinationAddress() ).as( "Place at linear index 42 should receive transmitted message" ).isEqualTo( 42 );
		
		verifyAll();
		
	}

	@Test
	public void sendMultipleAgentMessage() {
		
		String message = randomString();
		
		// sending messages to multiple Agents, by ID
		Set< Integer > addresses = new HashSet<>();
		addresses.add( 4 );
		addresses.add( 21 );
		
		// instruct messaging system to send the Agent messages and grab the objects sent
		Capture<MASSMessage> capturedMASSMessageAgentA = EasyMock.newCapture();
		mockMessagingProvider.sendAgentMessage( capture( capturedMASSMessageAgentA ) );
		Capture<MASSMessage> capturedMASSMessageAgentB = EasyMock.newCapture();
		mockMessagingProvider.sendAgentMessage( capture( capturedMASSMessageAgentB ) );

		replayAll();

		// queue up a message to be sent to the Agents
		messenger.sendAgentMessage( addresses, message );

		// "transmit" the message
		messenger.flushAgentMessages();

		// make sure the messages sent were "transmitted" correctly
		String txMessageA = ( String ) capturedMASSMessageAgentA.getValue().getMessage();
		String txMessageB = ( String ) capturedMASSMessageAgentB.getValue().getMessage();
		assertThat( txMessageA.contentEquals( message ) ).as( "Message should be delivered to Agent unmodified" ).isTrue();
		assertThat( txMessageB.contentEquals( message ) ).as( "Message should be delivered to Agent unmodified" ).isTrue();
		assertThat( capturedMASSMessageAgentA.getValue().getDestinationAddress() ).as( "Agent at linear index 4 should receive transmitted message" ).isEqualTo( 4 );
		assertThat( capturedMASSMessageAgentB.getValue().getDestinationAddress() ).as( "Agent at linear index 21 should receive transmitted message" ).isEqualTo( 21 );
		
		verifyAll();
		
	}
	
}
