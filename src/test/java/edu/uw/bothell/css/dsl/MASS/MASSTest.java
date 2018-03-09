/*

 	MASS Java Software License
	© 2012-2017 University of Washington

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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.easymock.Mock;
import org.junit.After;
import org.junit.Test;

import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

public class MASSTest extends AbstractTest {

	@Mock
	private MNode mnode;
	
	@Test
	public void getSetNumThreads() throws Exception {

		replayAll();

		// by default, only one
		assertEquals( 1, MASS.getNumThreads() );
		
		// change it
		MASS.setNumThreads( 2 );
		assertEquals( 2, MASS.getNumThreads() );
		
		// shouldn't be able to set to zero or less
		MASS.setNumThreads( 0 );
		assertEquals( 2, MASS.getNumThreads() );
		MASS.setNumThreads( -1 );
		assertEquals( 2, MASS.getNumThreads() );
		
		// revert
		MASS.setNumThreads( 1 );
		assertEquals( 1, MASS.getNumThreads() );
		
	}

	@Test
	public void getSetDefaultUsername() throws Exception {
		
		String newUsername = randomString();
		String originalUsername = MASS.getDefaultUsername();

		replayAll();

		// set new value, and test
		MASS.setDefaultUsername( newUsername );
		assertEquals( newUsername, MASS.getDefaultUsername() );
		
		// revert back
		MASS.setDefaultUsername( originalUsername );
		
	}

	@Test
	public void getSetNodeFilePath() throws Exception {
		
		String newPath = randomString();
		String originalPath = MASS.getNodeFilePath();

		replayAll();

		// set new value, and test
		MASS.setNodeFilePath( newPath );
		assertEquals( newPath, MASS.getNodeFilePath() );
		
		// revert back
		MASS.setNodeFilePath( originalPath );
		
	}

	@Test
	public void setLoggingLevel() throws Exception {

		replayAll();

		// should not result in an Exception
		MASS.setLoggingLevel( LogLevel.DEBUG );
		
	}
	
	@After
	public void afterEach() {
		
		// clean up MASSBase
		resetMASSBase();
		
	}
	
	@Test
	public void barrierAllSlavesNoArgumentsNoAgents() throws Exception {

		replayAll();

		// should not throw an Exception...
		MASS.barrierAllSlaves();
		
	}

	@Test
	public void barrierAllSlavesNoArgumentsWithAgents() throws Exception {

		Message ack = new Message( Message.ACTION_TYPE.ACK );
		
		// use mock MNode as the single remote
		MASS.getRemoteNodes().add( mnode );

		// with logging enabled, there will be a couple of hostname lookups
		expect( mnode.getHostName() ).andReturn( "localhost" );
		expect( mnode.getHostName() ).andReturn( "localhost" );

		// should receive an ACK message from the node
		expect( mnode.receiveMessage() ).andReturn( ack );
		
		replayAll();
		
		MASS.barrierAllSlaves();
		
		
	}

	@Test
	public void barrierAllSlavesWithArgumentsWithAgents() throws Exception {

		Message ack = new Message( Message.ACTION_TYPE.ACK, new String[]{ "arg1", "arg2" }, 0 );
		
		// need an array containing enough elements for the message argument and stripe size
		Object[] returnValues = new Object[ 4 ];
		
		// use mock MNode as the single remote
		MASS.getRemoteNodes().add( mnode );
		
		// with logging enabled, there will be a couple of hostname lookups
		expect( mnode.getHostName() ).andReturn( "localhost" );
		expect( mnode.getHostName() ).andReturn( "localhost" );
		
		// should receive an ACK message from the node
		expect( mnode.receiveMessage() ).andReturn( ack );
		
		replayAll();
		
		MASS.barrierAllSlaves( returnValues, 1 );
		
		// should not have thrown an exception
		
	}
	
	@Test
	public void barrierAllSlavesAgentIDsOnly() throws Exception {

		Message ack = new Message( Message.ACTION_TYPE.ACK );
		
		// use mock MNode as the single remote
		MASS.getRemoteNodes().add( mnode );

		// with logging enabled, there will be a couple of hostname lookups
		expect( mnode.getHostName() ).andReturn( "localhost" );
		expect( mnode.getHostName() ).andReturn( "localhost" );

		// should receive an ACK message from the node
		expect( mnode.receiveMessage() ).andReturn( ack );
		
		replayAll();
		
		MASS.barrierAllSlaves( null );
		
		
	}


}