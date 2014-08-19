package edu.uw.bothell.css.dsl.MASS;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Test;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
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
		
		// command will be executed within the Channel
		mockChannelExec.setCommand( command );
		mockChannelExec.connect();
		
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
	
}
