package edu.uw.bothell.css.dsl.MASS;

import java.io.InputStream;
import java.io.ObjectInputStream;         // For socket input/output
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.jcraft.jsch.Channel;  // Jsch used for Node connections
import com.jcraft.jsch.Session;

/**
 * MNode represents a MASS compute Node and contains references
 * to communication channels with the Node which may be used to
 * sending/receiving Messages to/from the Node.
 * @author mfukuda
 *
 */
@XmlRootElement(name = "node")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class MNode {

    private String hostName;			// the host name of this node
    private String userName;			// for SSH login, the username - optional
    private String passWord;			// for SSH login, the password - optional
    private String javaHome;			// where the JVM is installed on this node - optional
    private String massHome;			// where MASS library is located - optional
    private boolean isMaster = false;	// is this the master node? - optional
    private int pid;              		// process ID
    private int port;					// the port number used for inter-node communications - optional
    private Channel channel;            // JSCH channel
    private ObjectInputStream mainIOS;  // from remote to master
    private ObjectOutputStream mainOOS; // from master to remote
    
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
	 * Get the JSCH communications channel connected to the node
	 * @return The JSCH communications channel
	 */
	@XmlTransient
	public Channel getChannel() {
		return channel;
	}

	/**
     * Return the Hostname or IP address of this Node
     * @return The Hostname/IP address
     */
	@XmlElement(name = "hostname", required = true)
    public String getHostName( ) {
    	return hostName;
    }

	/**
	 * Get the location on this node where the JVM is installed
	 * @return The JVM home location
	 */
	@XmlElement(name = "javahome")
    public String getJavaHome() {
		return javaHome;
	}

    /**
	 * Get the location where MASS (MASS.jar) resides on this node
	 * @return The location of MASS.jar
	 */
	@XmlElement(name = "masshome")
	public String getMassHome() {
		return massHome;
	}

	/**
	 * Get the SSH login password for this node
	 * @return The SSH login password
	 */
	@XmlElement(name = "password")
	public String getPassWord() {
		return passWord;
	}

	/**
     * Get the process ID (PID) of this Node. The process
     * ID is a number used within MASS to uniquely identify
     * each Node. This number is assigned during initialization
     * of the Node.
     * @return The unique process ID number for this Node
     */
	@XmlTransient
    public int getPid( ) {
    	return pid;
    }

	/**
	 * Get the SSH login username for this node
	 * @return The login username
	 */
	@XmlElement(name = "username")
	public String getUserName() {
		return userName;
	}

	/**
	 * Perform actions necessary to initialize communications with this node
	 */
	public void initialize() {
		
		try {

			// hostname should have been set already, if not, set to default
			if (getHostName() == null) setHostName(InetAddress.getLocalHost( ).getCanonicalHostName( ));
			
			// set input/output streams, then execute the command to start MProcess on the remote node
			InputStream is = channel.getInputStream();
			OutputStream os = channel.getOutputStream();
			channel.connect();
			
			// with input/output channels established, set object streams
			mainOOS = new ObjectOutputStream( os );
			mainOOS.flush( );
			mainIOS = new ObjectInputStream( is );
		
		}
		
		// TODO - need better method of handling errors here rather than terminating application
		catch( Exception e ) {	
			MASS_base.logException( "ERROR: mNode: Pid: " + pid, e);
			
			System.exit( -1 );
	
		}
		
	}

	/**
	 * Get the master status for this node - if true, then the node represented by this instance
	 * is the master node 
	 * @return True if this is the master node, false if a remote node
	 */
	@XmlElement(name = "master", required = false)
	public boolean isMaster() {
		return isMaster;
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

	/**
	 * Set the JSCH channel (already established) with the remote Node
	 * @param channel The initialized JSCH channel connected to the remote Node
	 */
	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	/**
	 * Set the Hostname or IP address of this Node
	 * @param hostName The Hostname/IP address
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Set the location where the JVM is installed on this node
	 * @param javaHome The JVM location
	 */
	public void setJavaHome(String javaHome) {
		this.javaHome = javaHome;
	}

	/**
	 * Set the location where MASS (MASS.jar) resides on this node
	 * @param massHome The location of MASS.jar
	 */
	public void setMassHome(String massHome) {
		this.massHome = massHome;
	}

	/**
	 * Set the master status for this node
	 * @param isMaster Set true if this instance represents the master node, false if it represents a remote node
	 */
	public void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	/**
	 * Set the SSH login password for this node
	 * @param passWord The SSH login password
	 */
	public void setPassWord(String passWord) {
		this.passWord = passWord;
	}

	/**
	 * Set the unique ID (process ID) for this Node
	 * @param pid The unique process ID number
	 */
	public void setPid(int pid) {
		this.pid = pid;
	}

	/**
	 * Set the SSH login username for this node
	 * @param userName The SSH login username
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Set the port number used to communicate with this node, for inter-node socket communications
	 * @return The port number
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the port number used to communicate with this node, for inter-node socket communications
	 * @param port The port number to use
	 */
	public void setPort(int port) {
		this.port = port;
	}

}