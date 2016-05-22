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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.easymock.Capture;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Perform a series of unit tests against the Utilities class to verify proper
 * and consistent behavior of the class / methods
 * 
 * @author Matthew Sell
 *
 */
public class UtilitiesTest extends AbstractTest {

	@TestSubject
	private Utilities utilities = new Utilities();
	
	@Mock
	private JSch mockJsch;
	
	@Mock
	private Session mockSession;

	@Mock
	private ChannelExec mockChannelExec;
	
	@Test
	public void testLaunchRemoteProcess() throws Exception {
		
		String command = randomString( 32 );
		String hostName = randomString( 32 );
		String passWord = randomString( 32 );
		int portNumber = randomInt();
		String userName = randomString( 32 );
		
		// first, the JSCH library will attempt to connect to the remote host
		expect( mockJsch.getSession( userName, hostName, portNumber )).andReturn( mockSession );
		
		// second, user information is associated with the Session
		Capture<UserInfo> capturedUserInfo = new Capture<UserInfo>();
		mockSession.setUserInfo( capture( capturedUserInfo ) );
		
		// connection will be completed, via Session
		mockSession.connect();
		
		// a Channel will be opened, in "exec mode"
		expect( mockSession.openChannel( "exec" ) ).andReturn( mockChannelExec );
		
		// command set within the Channel, but not executed yet
		mockChannelExec.setCommand( command );
		
		// put mocks into replay mode
		replayAll();
		
		// call the method under test
		utilities.LaunchRemoteProcess(hostName, portNumber, command, userName, passWord);
		
		// make sure the proper credentials were supplied to the library
		UserInfo ui = capturedUserInfo.getValue();
		assertEquals( passWord, ui.getPassword() );
		assertNull( ui.getPassphrase() );

		// test proper (consistent!) behavior of the user credentials object
		assertTrue( ui.promptPassphrase( randomString( 32 ) ) );	// any passphrase prompt returns TRUE
		assertTrue( ui.promptPassword( randomString( 32 ) ) );		// any password prompt returns TRUE
		assertTrue( ui.promptYesNo( randomString( 32 ) ) );			// any yes/no prompt returns TRUE
		
		// attempting to show a message should NOT result in an Exception
		ui.showMessage( randomString( 32 ) );
		
	}
	
	@Test
	public void handleConnectionException() throws Exception {

		Channel returnChannel = null;

		expect( mockJsch.getSession("username", "host", 22) ).andThrow(new JSchException());

		// put mocks into replay mode
		replayAll();

		try {
			
			returnChannel = utilities.LaunchRemoteProcess("host", 22, null, "username", null);
			
		}
		
		catch(Exception e) {
			
			// we NOT expect an exception
			fail("Should have swallowed a connection exception!");
			
		}
		
		assertNull(returnChannel);
		
	}
	
	@Test
	public void testHostnameDetect() throws Exception {

		// put mocks into replay mode (even though this method isn't using mock
		// objects, the ones that exist must be in replay mode for teardown
		replayAll();

		String hostname = utilities.getLocalHostname();
		
		// logging filename should include a real hostname or IP address
		assertNotNull(hostname);
		
	}

}
