package edu.uw.bothell.css.dsl.MASS;

import com.jcraft.jsch.*;  // Jsch used for Node connections
import java.io.*;         // For socket input/output

public class MNode {
    public MNode( String hostName, int pid, Channel channel ) { 
	this.hostName = hostName;
	this.pid = pid;
	this.channel = channel;
	
	try {
            // Setup Communication
            mainOOS = new ObjectOutputStream( channel.getOutputStream( ) );
            mainOOS.flush( );
            mainIOS = new ObjectInputStream( channel.getInputStream( ) );
        } catch( Exception e ) {
            MASS_base.log( "ERROR: mNode: Pid: " + pid + 
			   " setupMainConnection " + e );
	    System.exit( -1 );
        }
    }

    public void closeMainConnection( ) {
        try {
            mainIOS.close( );
	    mainOOS.close( );
	    Session session = channel.getSession( );
	    channel.disconnect( );
	    session.disconnect( );
        } catch( Exception e ) {
            MASS_base.log( "closeMainConnection error with rank[" + pid + 
			   "] at " + hostName );
            System.exit( -1 );
        }
    }

    public void sendMessage( Message m ) { 
        try {
	    mainOOS.writeObject( m );
	    mainOOS.flush( );
	}
        catch ( Exception e ) {
	    MASS_base.log( "sendMessage error to rank[" + pid + "] at " +
			   hostName );
	    System.exit( -1 );
	}
    }

    public Message receiveMessage( ) { 
	Message m = null;
        try {
		m = ( Message )mainIOS.readObject( );
	}
        catch ( Exception e ) {
	    MASS_base.log( "receivMessage error from rank[" + pid + "] at " +
			   hostName );
	    System.exit( -1 );
	}
        return m;
    }

    public String getHostName( ) {
	return hostName;
    }

    public int getPid( ) {
	return pid;
    }

    private final String hostName;      // the host name of this mnode
    private final int pid;              // process ID
    private Channel channel;            // JSCH channel
    private ObjectInputStream mainIOS;  // from mnode to master
    private ObjectOutputStream mainOOS; // from master to mnode
}