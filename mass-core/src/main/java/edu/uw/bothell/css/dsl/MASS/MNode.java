package edu.uw.bothell.css.dsl.MASS;

import com.jcraft.jsch.*;  // Jsch used for Node connections
import java.io.*;         // For socket input/output

/**
 * MNode represents a MASS compute Node and contains references
 * to communication channels with the Node which may be used to
 * sending/receiving Messages to/from the Node.
 * @author mfukuda
 *
 */
public class MNode {

    private String hostName;      		// the host name of this mnode
    private int pid;              		// process ID
    private Channel channel;            // JSCH channel
    private ObjectInputStream mainIOS;  // from mnode to master
    private ObjectOutputStream mainOOS; // from master to mnode

	/**
	 * Constructor that initializes connections
	 * @param hostName Hostname or IP address of this Node
	 * @param pid Unique Process ID number to assign to this Node
	 * @param channel An active JSCH channel connected to this Node
	 */
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

	/**
	 * Terminate all communications channels to the remote Node
	 */
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

	/**
     * Return the Hostname or IP address of this Node
     * @return The Hostname/IP address
     */
    public String getHostName( ) {
    	return hostName;
    }

	/**
     * Get the process ID (PID) of this Node. The process
     * ID is a number used within MASS to uniquely identify
     * each Node. This number is assigned during initialization
     * of the Node.
     * @return The unique process ID number for this Node
     */
    public int getPid( ) {
    	return pid;
    }

    /**
	 * Get a Message send to this Node
	 * @return The Message received by this Node
	 */
	public Message receiveMessage( ) { 

		Message m = null;

		try {

			m = ( Message ) mainIOS.readObject( );

		}

		catch ( Exception e ) {

			MASS_base.log( "receivMessage error from rank[" + pid + "] at " +
					hostName );

			System.exit( -1 );

		}

		return m;

	}

    /**
	 * Send a message to the remote Node
	 * @param m The Message to send
	 */
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

}