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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

/**
 * MASS Utilities
 * 
 * A series of helper methods for various components of MASS
 * 
 * @author Dr. Munehiro Fukuda
 *
 */
class Utilities {
	
	private static final int SSH_PORT = 22;
	private static final int CONNECT_TIMEOUT_MILLISECONDS = 30000;

	// reference to the SSH library - not initialized by default so it can be
	// replaced by a mock object for testing
	private JSch jsch = null;
	
	// keep track of all remote sessions, so that MNode does not need library-specific references
	private Map<MNode, Channel> remoteSessions = new HashMap<MNode, Channel>();
	
	// logging
	private Log4J2Logger logger = Log4J2Logger.getInstance();
	
	/**
	 * Obtain a communications channel with a remote host and execute a command.
	 * This method sets Input and Output streams in the supplied MNode, so that 
	 * consumers will have access to the host. Also note that the
	 * consumers need to explicitly call the channel.disconnect(); & 
	 * channel.getSession().disconnect(); when communications with the remote host
	 * are no longer required.
	 *
	 * @param command The "exec" command to execute upon connection
	 * @param remoteNode An MNode instance representing the remote host
	 */
    protected void LaunchRemoteProcess( String command, MNode remoteNode ) {
    	
    	// must provide required parameters
    	if ( command == null || command.length() == 0 )
    		throw new IllegalArgumentException( "Command is empty or equal to null" );
    	if ( remoteNode == null ) //return null;
    		throw new IllegalArgumentException( "remoteNode is equal to null" );
    	
    	ChannelExec channel = null;
    	Properties config = new Properties();
    	
    	try {
    		
    		// instantiate the SSH library if necessary (might be replaced
    		// by a mock object during unit testing)
    		if ( jsch == null ) jsch = new JSch( );
    		
    		// add reference to SSH key
    		logger.debug( "Adding private key: {}", remoteNode.getPrivateKey() );
    		jsch.addIdentity( remoteNode.getPrivateKey() );
    		
            // set SSH connection properties
    		logger.debug( "Setting hostname to {}", remoteNode.getHostName() );
    		logger.debug( "Setting username to {}", remoteNode.getUserName() );
    		logger.debug( "Connecting to port {}", SSH_PORT );
            Session session = jsch.getSession( remoteNode.getUserName(), remoteNode.getHostName(), SSH_PORT );

            logger.debug( "Setting preferred authentication method");
            config.put("PreferredAuthentications", "publickey");

            logger.debug( "Disabling strict host key checking" );
            config.put( "StrictHostKeyChecking", "no" );  
            
            // authenticate and complete connection sequence to the remote host
            logger.debug( "Attempting to connect and authenticate..." );
            session.setConfig( config );
            session.connect( );
            logger.debug( "Connected!" );

            // set the command to be executed upon channel connection
            logger.debug( "Executing remote command: {}", command );
            channel = ( ChannelExec ) session.openChannel( "exec" );
            channel.setCommand( command );
            logger.debug( "Command executed!" );

            logger.debug( "Setting object input/output streams with remote node..." );
    		channel.connect( CONNECT_TIMEOUT_MILLISECONDS );
    		remoteNode.setOutputStream( channel.getOutputStream() );
    		remoteNode.setInputStream( channel.getInputStream() );
    		// TODO - error stream?
    		logger.debug( "Streams set!" );
    		
    		// keep track of this session for orderly disconnect later
    		remoteSessions.put( remoteNode, channel );
    		logger.debug( "Communications established with remote node" );

    		
    	} catch ( Exception e ) {
    		
    		// log the error message
    		logger.error("Caught exception while attempting to connect/authenticate/execute on remote node", e);
    		
    	}
    	
    }

    /**
     * Disconnect from a remote node
     * @param remoteNode The node from which to terminate communications
     */
    protected void disconnectRemoteNode(MNode remoteNode) {
    	
    	if ( remoteNode == null ) return;
    	
    	logger.debug( "Attempting to terminate communcations with node PID: {}", remoteNode.getPid() );
    	
    	// close streams in use by MNode
    	remoteNode.closeMainConnection();
    	
    	Channel channel = remoteSessions.get(remoteNode);
    	if ( channel != null ) {
    		
    		Session session = null;

    		try {
				
    			session = channel.getSession();
                channel.disconnect();
                session.disconnect();

            	logger.debug( "Communcations terminated!" );
            	
    		} catch (JSchException e) {

        		// log the error message
        		logger.error("Caught exception while attempting to disconnect from remote node", e);

    		}
    		
    	}
    	
    }
    
    /**
     * Get the hostname or IP address of this node
     * @return The network address of this node
     */
    public String getLocalHostname() {

		String hostname = null;
		
		try {
			
			hostname = InetAddress.getLocalHost().getHostName();
			
		}
		catch (UnknownHostException e) {

			// no biggie, at least not now
			
		}

		return hostname;
		
    }

}