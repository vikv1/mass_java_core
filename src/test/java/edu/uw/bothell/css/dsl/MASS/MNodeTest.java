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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Perform a series of unit tests against the MNode class to verify proper
 * and consistent behavior of the class / methods
 */
public class MNodeTest extends AbstractTest {

	@TestSubject
	private MNode mNode = new MNode();

	@Mock
	private ObjectInputStream mockObjectInputStream;

	@Mock
	private ObjectOutputStream mockObjectOutputStream;

	@AfterEach
	public void tearDown() {

		// reset test subject fields
		mNode.setHostName( null );
		mNode.setJavaHome( null );
		mNode.setMassHome( null );
		mNode.setMaster( false );
		mNode.setPid( 0 );
		mNode.setPort( 3400 );	// reset back to default value
		mNode.setPrivateKey( null );
		mNode.setUserName( null );

	}
	
	@Test
	public void closeMainConnection() throws Exception {
		
		// when closing connections, MNode will close streams
		mockObjectInputStream.close();
		mockObjectOutputStream.close();
		
		replayAll();
		
		mNode.closeMainConnection();
		
	}
	
	@Test
	public void getSetHostname() {
		
		String hostname = " " + randomString() + " ";
		
		replayAll();
		
		// verify null to begin with
		assertNull( mNode.getHostName() );
		
		// set to a known value
		mNode.setHostName( hostname );
		
		// should match
		assertEquals( StringUtils.stripToNull( hostname ), mNode.getHostName() );
		
	}
	
	@Test
	public void getSetJavaHome() {
		
		String home = " " + randomString() + " ";
		
		replayAll();
		
		// verify null to begin with
		assertNull( mNode.getJavaHome() );
		
		// set to a known value
		mNode.setJavaHome( home );
		
		// should match
		assertEquals( StringUtils.stripToNull( home ), mNode.getJavaHome() );
		
	}
	
	@Test
	public void getSetMassHome() {
		
		String home = " " + randomString() + " ";
		
		replayAll();
		
		// verify null to begin with
		assertNull( mNode.getMassHome() );
		
		// set to a known value
		mNode.setMassHome( home );
		
		// should match
		assertEquals( StringUtils.stripToNull( home ), mNode.getMassHome() );
		
	}
	
	@Test
	public void getSetPid() {
		
		int pid = randomInt();
		
		replayAll();
		
		// verify zero to begin with
		assertEquals( 0, mNode.getPid() );
		
		// set to a known value
		mNode.setPid( pid );
		
		// should match
		assertEquals( pid, mNode.getPid() );
		
	}
	

	@Test
	public void getSetPort() {
		
		int port = randomInt();
		
		replayAll();
		
		// verify non-zero to begin with (should have a default port number assigned!)
		assertTrue( mNode.getPort() > 0 );
		
		// set to a known value
		mNode.setPort( port );
		
		// should match
		assertEquals( port, mNode.getPort() );
		
	}
	
	@Test
	public void getSetPrivateKey() {
		
		String key = " " + randomString() + " ";
		
		replayAll();
		
		// verify null to begin with
		assertNull( mNode.getPrivateKey() );
		
		// set to a known value
		mNode.setPrivateKey( key );
		
		// should match
		assertEquals( StringUtils.stripToNull( key ), mNode.getPrivateKey() );
		
	}

	@Test
	public void getSetUserName() {
		
		String name = " " + randomString() + " ";
		
		replayAll();
		
		// verify null to begin with
		assertNull( mNode.getUserName() );
		
		// set to a known value
		mNode.setUserName( name );
		
		// should match
		assertEquals( StringUtils.stripToNull( name ), mNode.getUserName() );
		
	}
	
	@Test
	public void initializeWithoutHostname() {

		replayAll();

		// MNode initialization currently only sets hostname

		// should be null to start with
		assertNull( mNode.getHostName() );
		
		// initialize the node
		mNode.initialize();
		
		// should now have a hostname set
		assertNotNull( mNode.getHostName() );
		
	}
	
	@Test
	public void initializeWithHostname() {

		replayAll();

		String hostname = randomString();
		
		// force a hostname
		mNode.setHostName( hostname );
		
		// verify that it was set
		assertEquals( hostname, mNode.getHostName() );
		
		// initialize the node
		mNode.initialize();
		
		// original hostname should have stuck
		assertEquals( hostname, mNode.getHostName() );
		
	}

	@Test
	public void getSetMasterNodeStatus() {
		
		replayAll();
		
		// verify false to begin with
		assertFalse( mNode.isMaster() );
		
		// set to a known value
		mNode.setMaster( true );
		
		// should match
		assertTrue( mNode.isMaster() );
		
	}

	@Test
	public void receiveMessage() throws Exception {

		// mock objects not being used for this test
		replayAll();

		// make up a new MNode with real streams for testing
		MNode realMNode = new MNode();
		
		// test message for comparison
		Message message = new Message( Message.ACTION_TYPE.AGENTS_MANAGE_ALL, 100 );

		// serialize the Message, "inject" the stream
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream( out );
	    os.writeObject( message );
	    realMNode.setInputStream( new ByteArrayInputStream( out.toByteArray() ) );

	    // for convenience, uncomment if you'd like to see the size/contents of a serialized Message
//	    byte[] serializedMessage = out.toByteArray();
//	    int serializedMessageLength = serializedMessage.length;
	    
		Message returnedMessage = realMNode.receiveMessage();

		// can't do a simple equality test - need to spot check a couple of items to verify deserialization
		assertEquals( message.getAction(), returnedMessage.getAction() );
		assertEquals( message.getAgentPopulation(), returnedMessage.getAgentPopulation() );
		
	}

	@Test
	public void sendMessage() throws Exception {

		// mock objects not being used for this test
		replayAll();

		// make up a new MNode with real streams for testing
		MNode realMNode = new MNode();
		
		// test message for comparison
		Message message = new Message( Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT, 200 );

		// "inject" a real output stream, send the Message
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    realMNode.setOutputStream( out );
	    realMNode.sendMessage( message );

	    // deserialize the object from the outputstream
		ObjectInputStream is = new ObjectInputStream( new ByteArrayInputStream( out.toByteArray() ) );
		Message sentMessage = (Message) is.readObject();

		// can't do a simple equality test - need to spot check a couple of items to verify serialization
		assertEquals( message.getAction(), sentMessage.getAction() );
		assertEquals( message.getAgentPopulation(), sentMessage.getAgentPopulation() );
		
	}

	@Test
	public void validationCheckValidPortNumber() {
		
		// mock objects not being used for this test
		replayAll();
		
		// only one node, treat it as the master node
		mNode.setMaster( true );

		// initially, MNode should have no validation failures
		assertThat( mNode.validate().size() ).as( "Node configuration should not fail default validation rules").isEqualTo( 0 );
		
		// negative numbers should not be allowed
		mNode.setPort( -1 );
		assertThat( mNode.validate().size() ).as( "Node port number should fail validation if outside the range of 1024 to 65535").isGreaterThan( 0 );

		// port numbers less than 1024 should not be allowed
		mNode.setPort( 1023 );
		assertThat( mNode.validate().size() ).as( "Node port number should fail validation if outside the range of 1024 to 65535").isGreaterThan( 0 );

		// port numbers > 65535 should not be allowed
		mNode.setPort( 65536 );
		assertThat( mNode.validate().size() ).as( "Node port number should fail validation if outside the range of 1024 to 65535").isGreaterThan( 0 );

	}

	@Test
	public void validationCheckPrivateKey() {
		
		// mock objects not being used for this test
		replayAll();

		// set private key and username to some value
		mNode.setPrivateKey( randomString() );
		mNode.setUserName( randomString() );
		
		// initially, MNode should have no validation failures
		assertThat( mNode.validate().size() ).as( "Node configuration should not fail default validation rules").isEqualTo( 0 );
		
		// Null private key for remote node should not be allowed
		mNode.setPrivateKey( null );
		assertThat( mNode.validate().size() ).as( "Remote node should fail validation if private key not specified").isGreaterThan( 0 );

	}

	@Test
	public void validationCheckUsername() {
		
		// mock objects not being used for this test
		replayAll();

		// set private key and username to some value
		mNode.setPrivateKey( randomString() );
		mNode.setUserName( randomString() );
		
		// initially, MNode should have no validation failures
		assertThat( mNode.validate().size() ).as( "Node configuration should not fail default validation rules").isEqualTo( 0 );
		
		// Null username for remote node should not be allowed
		mNode.setUserName( null );
		assertThat( mNode.validate().size() ).as( "Remote node should fail validation if username not specified").isGreaterThan( 0 );

	}

	@Test
	public void getSeMaxHeapSize() {
		
		String heap = " " + randomString() + " ";
		
		replayAll();
		
		// verify null to begin with
		assertNull( mNode.getMaxHeapSize() );
		
		// set to a known value
		mNode.setMaxHeapSize( heap );
		
		// should match
		assertEquals( StringUtils.stripToNull( heap ), mNode.getMaxHeapSize() );
		
	}

}