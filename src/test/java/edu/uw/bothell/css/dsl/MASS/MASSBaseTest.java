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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Test;

public class MASSBaseTest extends AbstractTest {

	@TestSubject
	private MASSBase massBase = new MASSBase();
	
	@After
	public void tearDown() {

		// perform normal cleanup activities
		super.tearDown();
		
		// reset test subject fields
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void hasValidLogFilenameAutoDetectHostname() throws Exception {

		// testing in isolation...
		MASSBase mb = new MASSBase();

		// init without specifying a hostname in node config
		mb.initMASSBase(new MNode());
		
		// should generate a valid logging filename, with a valid host
		String loggingFilename = mb.getLogFileName();
		assertNotNull(loggingFilename);
		assertFalse(loggingFilename.toLowerCase().contains("null"));
		
	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getAllNodesPreInit() throws Exception {
		
		// testing in isolation...
		MASSBase mb = new MASSBase();
		
		// should not have any nodes when first instantiated
		assertEquals( 0, mb.getAllNodes().size() );
		assertEquals( 0, mb.getHosts().size() );
		assertEquals( 0, mb.getRemoteNodes().size() );
		assertEquals( 0, mb.getSystemSize() );
		assertNull( mb.getMasterNode() );
		
	}

	@Test
	@SuppressWarnings("static-access")
	public void addNode() throws Exception {
		
		// testing in isolation...
		MASSBase mb = new MASSBase();
		
		// add a single master node
		MNode masterNode = new MNode();
		masterNode.setHostName( "master" );
		masterNode.setMaster( true );
		mb.addNode( masterNode );
		
		// verify node representations
		assertEquals( 1, mb.getAllNodes().size() );
		assertEquals( 1, mb.getHosts().size() );
		assertEquals( 0, mb.getRemoteNodes().size() );
		assertEquals( 1, mb.getSystemSize() );
		assertEquals( masterNode, mb.getMasterNode() );
		
		// master node should have PID of zero
		assertEquals( 0, masterNode.getPid() );
		
		// add a remote node
		MNode remoteNode = new MNode();
		remoteNode.setHostName( "remote" );
		mb.addNode( remoteNode );
		
		// verify node representations
		assertEquals( 2, mb.getAllNodes().size() );
		assertEquals( 2, mb.getHosts().size() );
		assertEquals( 1, mb.getRemoteNodes().size() );
		assertEquals( 2, mb.getSystemSize() );
		assertEquals( masterNode, mb.getMasterNode() );

		// remote node should have PID of one (auto incremented)
		assertEquals( 1, remoteNode.getPid() );

	}
	
	@Test
	@SuppressWarnings("static-access")
	public void getLogger() throws Exception {
		assertNotNull( massBase.getLogger() );
		
	}

}
