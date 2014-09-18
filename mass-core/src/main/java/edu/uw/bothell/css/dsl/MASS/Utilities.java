package edu.uw.bothell.css.dsl.MASS;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * MASS Utilities
 * 
 * A series of helper methods for various components of MASS
 * 
 * @author Dr. Munehiro Fukuda
 *
 */
class Utilities {

	// reference to the SSH library - not initialized by default so it can be
	// replaced by a mock object for testing
	private JSch jsch = null;
	
	/**
	 * Obtain a communications channel with a remote host and execute a command.
	 * This method returns a channel, so that consumers will have access to Input
	 * and Outputstream (for interacting with the host). Also note that the
	 * consumers need to explicitly call the channel.disconnect(); & 
	 * channel.getSession().disconnect(); when communications with the remote host
	 * are no longer required.
	 *
	 * @param Host The hostname or IP address of the remote host
	 * @param PortNumber The port number of the listener on the remote host
	 * @param Command The "exec" command to execute upon connection
	 * @param UserName When connecting to the remote host, use the supplied username
	 * @param Password When connecting to the remote host, use the supplied password
	 * @return An open communications channel with the remote host
	 */
    protected Channel LaunchRemoteProcess( String Host, int PortNumber, 
					   String Command, String UserName, 
					   String Password ) {
        
    	ChannelExec channel = null;
        
    	try {
            
    		// instantiate the SSH library if necessary (might be replaced
    		// by a mock object during unit testing)
    		if (jsch == null) jsch = new JSch( );

            // initiate SSH connection to the remote host
            Session session = jsch.getSession( UserName, Host, PortNumber );

            // username and password will be given via UserInfo interface.
            UserInfo ui = new MyUserInfo( Password );
            session.setUserInfo( ui );
            
            // authenticate and complete connection sequence to the remote host
            session.connect( );

            // set the command to be executed upon channel connection
            channel = (ChannelExec) session.openChannel( "exec" );
            channel.setCommand( Command );
            
            // set input/output streams and execute the command
            channel.connect( );
        
    	}
        
    	catch ( Exception e ) {
            
    		// "display" the error message
    		System.err.println( e );
    		
    		// TODO - should we return NULL here to prevent the return of a partially connected channel?

        }
        
        return channel;

    }
  
    /**
     * User credentials for initiating remote connections using SSH
     * @author Dr. Munehiro Fukuda
     */
    private class MyUserInfo implements UserInfo {
	
    	// Private data members
    	private String _passwd = null;	// Users password
	
    	// Constructor sets up password
    	public MyUserInfo( String passwd ) {
            this._passwd = passwd;
    	}

    	// Because passphrase does not apply use null
    	public String getPassphrase( ) { return null; };
	
    	// Returns the password of the user
    	public String getPassword( ) { return _passwd; };
	
    	// You may only set password during construction of UserInfo
    	public boolean promptPassword( String Message ) { return true; };
	
    	// Because passphrase does not apply this function simply returns true
    	public boolean promptPassphrase( String message ) { return true; };
	
    	// Because the program is run remotely we don't want to prompt the user
    	public boolean promptYesNo( String message ) { return true; };
    	public void showMessage( String message ) { };
    
    }

}