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

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.PlacesBase;
import edu.uw.bothell.css.dsl.MASS.SimpleTestAgent;
import edu.uw.bothell.css.dsl.MASS.SimpleTestPlace;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

/**
 * Tests functionality common to all implementations of messaging
 */
public class MASSMessagingTest extends AbstractTest {

	@Mock
	private MessagingProvider mockMessagingProvider;
	
	// the class under test
	private static MASSMessaging messenger = MASSMessaging.getInstance();
	
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
	public void sendAgentMessage() {
		
		SimpleTestAgent agent = new SimpleTestAgent( null );
		String message = new String( "1995TransAm" );

		// messaging provider will be asked to register this Agent
		mockMessagingProvider.registerAgent( agent );
		
		// instruct messaging system to send the Agent message and grab the object sent
		Capture<MASSMessage> capturedMASSMessage = EasyMock.newCapture();
		mockMessagingProvider.sendAgentMessage( capture( capturedMASSMessage ) );

		// clean up
		mockMessagingProvider.unregisterAgent( agent );

		replayAll();

		// register this Agent with the messaging provider
		messenger.registerAgent( agent );
		
		// queue up a message to be sent to the Agent
		messenger.sendAgentMessage( agent.getAgentId(), message );

		// "transmit" the message
		messenger.flushAgentMessages();

		messenger.unregisterAgent( agent );

		// make sure the message sent was "transmitted" correctly
		String txMessage = ( String ) capturedMASSMessage.getValue().getMessage();
		assertThat( txMessage.contentEquals( message ) ).as( "Message should be delivered to the Agent unmodified" ).isTrue();
		assertThat( capturedMASSMessage.getValue().getDestinationAddress() ).as( "Message should be delivered to the correct Agent" ).isEqualTo( agent.getAgentId() );
		
		verifyAll();
		
	}
	
	@Test
	public void sendPlaceMessage() {
		
		SimpleTestPlace place = new SimpleTestPlace( null );
		String message = new String( "2002TransAm" );
		
		// messaging provider will be asked to register this Place
		mockMessagingProvider.registerPlace( place );
		
		// instruct messaging system to send the Place message and grab the object sent
		Capture<MASSMessage> capturedMASSMessage = EasyMock.newCapture();
		mockMessagingProvider.sendPlaceMessage( capture( capturedMASSMessage ) );

		// clean up
		mockMessagingProvider.unregisterPlace( place );

		replayAll();

		// register this Place with the messaging provider
		messenger.registerPlace( place );
		
		// queue up a message to be sent to the Place
		messenger.sendPlaceMessage( MessageDestination.ALL_PLACES, message );

		// "transmit" the message
		messenger.flushPlaceMessages();

		messenger.unregisterPlace( place );

		// make sure the message sent was "transmitted" correctly
		String txMessage = ( String ) capturedMASSMessage.getValue().getMessage();
		assertThat( txMessage.contentEquals( message ) ).as( "Message should be delivered to the Place unmodified" ).isTrue();
		assertThat( capturedMASSMessage.getValue().getDestinationAddress() ).as( "Message should be delivered to the correct Place" ).isEqualTo( MessageDestination.ALL_PLACES.getValue() );
		
		verifyAll();
		
	}
	
}
