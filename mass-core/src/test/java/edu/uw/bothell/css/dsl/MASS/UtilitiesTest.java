package edu.uw.bothell.css.dsl.MASS;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Perform a series of unit tests against the Utilities class to verify proper
 * behavior of the class / methods
 * 
 * @author msell
 *
 */
@RunWith(EasyMockRunner.class)
public class UtilitiesTest extends EasyMockSupport {

	@TestSubject
	private Utilities utilities = new Utilities();
	
	@Mock
	private JSch mockJsch;
	
	@Mock
	private Session mockSession;

	@Mock 
	private Channel mockChannel;
	
	@Mock
	private ChannelExec mockChannelExec;
	
	@Before
	public void setUp() {
		
		// make sure SSH library is "injected" into the test class
		utilities.setSSHCommunicationLibrary(mockJsch);
		
	}
	
	@After
	public void tearDown() {
		
		// reset all mock objects to prepare for next test
		resetAll();
		
	}
	
	@Test
	public void testLaunchRemoteProcess() throws Exception {
		
		String command = TestUtils.randomString( 32 );
		String hostName = TestUtils.randomString( 32 );
		String passWord = TestUtils.randomString( 32 );
		int portNumber = TestUtils.randomInt();
		String userName = TestUtils.randomString( 32 );
		
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
		assertTrue( ui.promptPassphrase( TestUtils.randomString( 32 ) ) );	// any passphrase prompt returns TRUE
		assertTrue( ui.promptPassword( TestUtils.randomString( 32 ) ) );		// any password prompt returns TRUE
		assertTrue( ui.promptYesNo( TestUtils.randomString( 32 ) ) );			// any yes/no prompt returns TRUE
		
		// attempting to show a message should NOT result in an Exception
		ui.showMessage( TestUtils.randomString( 32 ) );
		
		// make sure all mock objects were called as expected
		verifyAll();

	}
	
}
