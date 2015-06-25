package edu.uw.bothell.css.dsl.MASS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Perform a series of unit tests against the RemoteExchangeRequest class
 * to verify proper and consistent behavior of the class / methods
 * 
 * @author Matthew Sell
 *
 */
public class RemoteExchangeRequestTest extends AbstractTest {

	private RemoteExchangeRequest remoteExchangeRequest;

	@Test
	public void testObjectConstruction() throws Exception {
		
		int destIndex = randomInt();
		int orgIndex = randomInt();
		int inMsgIndex = randomInt();
		Integer intObj = new Integer( randomInt() );
		
		remoteExchangeRequest = new RemoteExchangeRequest( destIndex, orgIndex, inMsgIndex, intObj );
		
		assertEquals( destIndex, remoteExchangeRequest.getDestGlobalLinearIndex() );
		assertEquals( orgIndex, remoteExchangeRequest.getOrgGlobalLinearIndex() );
		assertEquals( inMsgIndex, remoteExchangeRequest.getInMessageIndex() );
		assertTrue( intObj == remoteExchangeRequest.getOutMessage() );
		
	}
	
}
