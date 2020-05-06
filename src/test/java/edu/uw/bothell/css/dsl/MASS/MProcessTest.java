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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.Ostermiller.util.CircularByteBuffer;

/**
 * Perform a series of unit tests against the MProcess class to verify proper
 * and consistent behavior of the class / methods
 */
@Disabled	// TODO - needs to be fixed to support Hazelcast addition
public class MProcessTest extends AbstractTest {

	private static final int AGENTS_HANDLE = 0;
	private static final int PLACES_HANDLE = 0;

	// a test martix for init'ing PlacesBase
	private int[] placesMatrix = new int[]{ 1, 1 };

	// this buffer is used by MProcess to receive messages
	CircularByteBuffer receiveCBB = new CircularByteBuffer( CircularByteBuffer.INFINITE_SIZE );
	
	// for sending messages to MProcess
	ObjectOutput out;
	
	// this buffer is used by MProcess to send messages
	CircularByteBuffer transmitCBB = new CircularByteBuffer( CircularByteBuffer.INFINITE_SIZE );

	// for receiving messages from MProcess
	ObjectInput in;
	
	// hostname collection for Places
	Vector<String> hosts = new Vector<>();

	@BeforeAll
	public static void beforeAll() {

		// make sure MASS is not running before starting these tests
		if (MASSBase.isInitialized() ) MASS.finish();
		
		// MASSBase should be made ready for use before tests are run
		MNode masterNode = new MNode();
		masterNode.setHostName( randomString() );
		masterNode.setMaster( true );
		MASSBase.addNode( masterNode );
		MASSBase.initMASSBase( masterNode );

	}
	
	@AfterAll
	public static void afterAll() {
		
		// clean up MASSBase
		resetMASSBase();
		
	}

	@BeforeEach
	public void onSetUp() {
		
		// make sure hosts are defined
		hosts.add( new String("localhost") );
		
	}
	
	@AfterEach
	public void onTearDown() {
		
		// clean up MASSBase
		MASSBase.getAgentsMap().clear();
		MASSBase.getPlacesMap().clear();
		MASSBase.setSystemSize( 0 );
		
		// scrub circular buffers
		receiveCBB.clear();
		transmitCBB.clear();
		
		// everything else
		hosts.clear();
		
	}
	
	@Test
	public void basicInit() throws Exception {

		Message initMessage = new Message();
		
		OutputStream outstream = new ByteArrayOutputStream();
		InputStream instream = new ByteArrayInputStream( serializeObject( initMessage ) );
		
		// this basic init should not throw an Exception
		new MProcess( "localhost", 0, 0, 0, 0, null, instream, outstream );
		
	}
	
	@Test
	public void simpleMessagesAndResponses() throws Exception {

		// send MProcess an ACK first
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( new Message( Message.ACTION_TYPE.ACK ) );
		
		// an empty message 
		out.writeObject( new Message( Message.ACTION_TYPE.EMPTY ) );
		
		// messages that should not produce a response
		out.writeObject( new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_REQUEST ) );
		out.writeObject( new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT ) );
		out.writeObject( new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY_REMOTE_REQUEST ) );
		out.writeObject( new Message( Message.ACTION_TYPE.PLACES_CALL_SOME_VOID_OBJECT ) );
		out.writeObject( new Message( Message.ACTION_TYPE.AGENTS_MIGRATION_REMOTE_REQUEST ) );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// basic init, just for testing
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		
		// start MProcess
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get another ACK for the first ACK message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// "EMPTY" message ACK
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receiveAgentsManageAllMessage() throws Exception {

		// need an AgentsBase and PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		AgentsBase agentsBase = new AgentsBase( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, PLACES_HANDLE, 1 );
		MASSBase.getAgentsMap().put( AGENTS_HANDLE, agentsBase );
		
		// test message, containing action and handle (third argument is ignored)
		Message m = new Message( Message.ACTION_TYPE.AGENTS_MANAGE_ALL, AGENTS_HANDLE, 0 );		
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK containing agent population for the AGENTS_MANAGE_ALL message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );
		assertTrue( returnedMessage.getAgentPopulation() > 0 );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receivePlacesInitializeMessage() throws Exception {

		// need an AgentsBase and PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		
		// test message, containing action and handle (third argument is ignored)
		Message m = new Message( Message.ACTION_TYPE.PLACES_INITIALIZE, placesMatrix, PLACES_HANDLE, SimpleTestPlace.class.getName(), null, 0, hosts );		
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}
	
	@Test
	public void receiveAgentsInitializeMessage() throws Exception {

		// need an AgentsBase and PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		AgentsBase agentsBase = new AgentsBase( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, PLACES_HANDLE, 1 );
		MASSBase.getAgentsMap().put( AGENTS_HANDLE, agentsBase );
		
		// test message, containing action and handle (third argument is ignored)
		Message m = new Message( Message.ACTION_TYPE.AGENTS_INITIALIZE, 1, AGENTS_HANDLE, PLACES_HANDLE, SimpleTestAgent.class.getName(), null);		
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receivePlacesCallAllVoidObjectMessage() throws Exception {
		
		// need a PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		
		// test message
		Message m = new Message( Message.ACTION_TYPE.PLACES_CALL_ALL_VOID_OBJECT, PLACES_HANDLE, 0, null );
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receivePlacesCallAllReturnObjectMessage() throws Exception {
		
		// need a PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		
		// test message
		Message m = new Message( Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT, PLACES_HANDLE, 0, new Object[ 5 ] );
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receivePlacesExchangeAllMessage() throws Exception {
		
		// need a PlacesBase to work with, and a neighbors vector
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		Place[] places = placesBase.getPlaces();
		places[ 0 ].setNeighbors( new Vector<int[]>() );
		
		// test message
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, PLACES_HANDLE, PLACES_HANDLE, 0, null );
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receivePlacesExchangeBoundaryMessage() throws Exception {
		
		// need a PlacesBase to work with, and a neighbors vector
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		
		// test message, containing action and handle (third argument is ignored)
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_BOUNDARY, PLACES_HANDLE, 0 );		
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receiveAgentsCallAllVoidObjectMessage() throws Exception {

		// need an AgentsBase and PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		AgentsBase agentsBase = new AgentsBase( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, PLACES_HANDLE, 1 );
		MASSBase.getAgentsMap().put( AGENTS_HANDLE, agentsBase );
		
		// test message, containing action and handle (third argument is ignored)
		Message m = new Message( Message.ACTION_TYPE.AGENTS_CALL_ALL_VOID_OBJECT, AGENTS_HANDLE, 0, null );		
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

	@Test
	public void receiveAgentsCallAllReturnObjectMessage() throws Exception {

		// need an AgentsBase and PlacesBase to work with
		PlacesBase placesBase = new PlacesBase( PLACES_HANDLE, SimpleTestPlace.class.getName(), 1, null, placesMatrix );
		MASSBase.getPlacesMap().put( PLACES_HANDLE, placesBase );
		AgentsBase agentsBase = new AgentsBase( AGENTS_HANDLE, SimpleTestAgent.class.getName(), null, PLACES_HANDLE, 1 );
		MASSBase.getAgentsMap().put( AGENTS_HANDLE, agentsBase );
		
		// test message, containing action and handle (third argument is ignored)
		Message m = new Message( Message.ACTION_TYPE.AGENTS_CALL_ALL_RETURN_OBJECT, AGENTS_HANDLE, 0, (Object[]) new String[]{"Argument"} );		
		
		// send MProcess the test message
    	out = new ObjectOutputStream( receiveCBB.getOutputStream() );
		out.writeObject( m );
		
		// finish up the test by instructing MProcess to terminate
		out.writeObject( new Message( Message.ACTION_TYPE.FINISH ) );
		
		// init and start MProcess
		MProcess mprocess = new MProcess( "localhost", 0, 0, 0, 0, null, receiveCBB.getInputStream(), transmitCBB.getOutputStream() );
		mprocess.start();
		
		// should get an ACK after start is called
		in = new ObjectInputStream( transmitCBB.getInputStream() );
		Message returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get an ACK for the test message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// should get a final ACK for the termination ("FINISH") message we sent
		returnedMessage = (Message) in.readObject();
		assertEquals( Message.ACTION_TYPE.ACK, returnedMessage.getAction() );

		// MProcess should not have sent anything else after terminating
		assertEquals( 0, transmitCBB.getAvailable() );
		
	}

}