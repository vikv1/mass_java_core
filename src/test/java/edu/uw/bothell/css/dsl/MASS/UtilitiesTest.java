/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Properties;

import org.easymock.Capture;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Perform a series of unit tests against the Utilities class to verify proper
 * and consistent behavior of the class / methods
 */
public class UtilitiesTest extends AbstractTest {

	private static final int DEFAULT_PORT = 22;
	private static final int DEFAULT_SESSION_TIMEOUT_MS = 30000;
	
	@TestSubject
	private Utilities utilities = new Utilities();
	
	@Mock
	private MNode mockRemoteNode;
	
	@Mock
	private JSch mockJsch;
	
	@Mock
	private Session mockSession;

	@Mock
	private ChannelExec mockChannelExec;
	
	@Mock
	private ObjectInputStream mockObjectInputStream;

	@Mock
	private ObjectOutputStream mockObjectOutputStream;
	
	@Test
	public void launchRemoteProcess() throws Exception {
		
		String command = randomString( 32 );
		
		MNode remoteNode = new MNode();
		remoteNode.setHostName( randomString( 32 ));
		remoteNode.setUserName( randomString( 32 ));
		remoteNode.setPrivateKey( randomString( 32 ));
		
		// first, the JSCH library will define a session for the remote host
		expect( mockJsch.getSession( remoteNode.getUserName(), remoteNode.getHostName(), DEFAULT_PORT ) ).andReturn( mockSession );
		
		// should set private key
		mockJsch.addIdentity( remoteNode.getPrivateKey() );
		
		// Session will have a configuration property added to disable strict host checking
		Capture<Properties> capturedProperties = new Capture<Properties>();
		mockSession.setConfig( capture(capturedProperties) );
		
		// connection will be completed, via Session
		mockChannelExec.connect( DEFAULT_SESSION_TIMEOUT_MS );
		mockSession.connect();
		
		// a Channel will be opened, in "exec mode"
		expect( mockSession.openChannel( "exec" ) ).andReturn( mockChannelExec );
		
		// command set within the Channel, but not executed yet
		mockChannelExec.setCommand( command );
		
		// input/output streams will be associated with the session now
		// the exact streams aren't important, just the fact that they're bound to the node
		ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject( new Message() );
		expect( mockChannelExec.getOutputStream() ).andReturn( out );
		expect( mockChannelExec.getInputStream() ).andReturn( new ByteArrayInputStream( out.toByteArray() ) );
		
		replayAll();
		
		// call the method under test
		utilities.launchRemoteProcess( command, remoteNode );
		
		// make sure the strict host key check disable property was set
		capturedProperties.getValue().containsKey( "StrictHostKeyChecking" );
		assertEquals( "no", ( String ) capturedProperties.getValue().get( "StrictHostKeyChecking" ) );

		// make sure public key is the preferred method of authentication
		capturedProperties.getValue().containsKey( "PreferredAuthentications" );
		assertEquals( "publickey", ( String ) capturedProperties.getValue().get( "PreferredAuthentications" ) );
		
	}
	
	@Test
	public void disconnectRemoteNode() throws Exception {
		
		// debug message will get PID of remote node
		expect( mockRemoteNode.getPid() ).andReturn( randomInt() );
		
		// Utilities will instruct MNode to close connections
		mockRemoteNode.closeMainConnection();
		
		replayAll();
		
		utilities.disconnectRemoteNode( mockRemoteNode );
		
		// TODO - need to verify channels are being closed, but hashmap in Utilities prevents this
		
	}
	
	@Test
	public void getLocalHostname() throws Exception {

		// put mocks into replay mode (even though this method isn't using mock
		// objects, the ones that exist must be in replay mode for teardown
		replayAll();

		String hostname = utilities.getLocalHostname();
		
		// logging filename should include a real hostname or IP address
		assertNotNull(hostname);
		
	}
	
	@Test( expected = IllegalArgumentException.class)
	public void launchRemoteProcessNullExecCommand() throws Exception {
		replayAll();
		utilities.launchRemoteProcess( null , null );
	}

	@Test( expected = IllegalArgumentException.class)
	public void launchRemoteProcessZeroLengthExecCommand() throws Exception {
		replayAll();
		utilities.launchRemoteProcess( "" , null );
	}

	@Test
	public void disconnectNullMNodeNoException() throws Exception {
		replayAll();
		utilities.disconnectRemoteNode( null );
	}

}
