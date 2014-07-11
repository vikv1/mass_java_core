package MASS;

import com.jcraft.jsch.*;
import java.util.*;
import java.io.*;
//import sun.net.util.IPAddressUtil;

class Utilities {
    // This method return channel, so that consumers will have access to Input 
    // and outputstream.
    // Also note that the consumers need to explicitly call the
    // channel.disconnect(); & channel.getSession().disconnect();

    protected Channel LaunchRemoteProcess( String Host, int PortNumber, 
					   String Command, String UserName, 
					   String Password ) {
        Channel channel = null;
        try {
            JSch jsch = new JSch( );

            // ssh connection
            Session session = jsch.getSession( UserName, Host, PortNumber );

            // username and password will be given via UserInfo interface.
            UserInfo ui = new MyUserInfo( Password );
            session.setUserInfo( ui );
            session.connect( );

            channel=session.openChannel( "exec" );
            ( (ChannelExec)channel ).setCommand( Command );

            channel.connect( );
        }
        catch( Exception e ) {
            System.err.println( e );
        }
        
        return channel;
    }
  
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