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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;         // For socket input/output
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import ucar.ma2.Array.*;
import ucar.ma2.ArrayFloat;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;

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

	private LogLevel logLevel;			// custom logging level for this node
	private String logFileName;			// custom logging filename for this node
	private String hostName;			// the host name of this node
	private String userName;			// for SSH login, the username - optional
	private String javaHome;			// where the JVM is installed on this node - optional
	private String massHome;			// where MASS library is located - optional
	private String privateKey;		 	// path/filename containing the private key used for SSH connection to this node
	private boolean isMaster = false;	// is this the master node? - optional
	private int pid;              		// process ID
	private int port = 3400;			// the port number used for inter-node communications, defaults to 3400
	private ObjectInputStream mainIOS;  // from remote to master
	private ObjectOutputStream mainOOS; // from master to remote
	private int resetCounter = 0;
	private Log4J2Logger logger = Log4J2Logger.getInstance();
    
    /**
	 * Terminate all communications channels to the remote Node
	 */
	public void closeMainConnection( ) {

		try {

			mainIOS.close( );
			mainOOS.close( );

		} catch( Exception e ) {

			logger.error( "closeMainConnection error with rank[" + pid + 
					"] at " + hostName, e );
			System.exit( -1 );

		}

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
	 * Set the port number used to communicate with this node, for inter-node socket communications
	 * @return The port number
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Get the path/filename of the private key used for SSH connections to this node
	 * @return The private key path/filename
	 */
	@XmlElement(name = "privatekey")
	public String getPrivateKey() {
		return privateKey;
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

			// TODO - log error if streams not initialized
			
			// hostname should have been set already, if not, set to default
			if (getHostName() == null) setHostName(InetAddress.getLocalHost( ).getCanonicalHostName( ));
			
		}
		
		// TODO - need better method of handling errors here rather than terminating application
		catch( Exception e ) {	

			logger.error( "ERROR: mNode: Pid: {}", pid, e);
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

			logger.debug("Awaiting receipt of message...");
			m = ( Message ) mainIOS.readObject( );
			logger.debug("Message received!");

		}

		catch ( Exception e ) {

			logger.error( "receivMessage error from rank[" + pid + "] at " +
					hostName + ", message: " + e.getMessage(),  e );
			
			e.printStackTrace();

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

			logger.debug("Sending message to {}", getHostName());
			mainOOS.writeObject( m );
			logger.debug("Message sent!");
			mainOOS.flush( );
			logger.debug("Object outputstream flushed");
                        
			resetCounter++;
			if(resetCounter == 5){
				logger.debug("Resetting object outputstream...");
				mainOOS.reset();
				logger.debug("Stream reset!");
				resetCounter = 0;
			}

		}

		catch ( Exception e ) {

			logger.error( "sendMessage error to rank[" + pid + "] at " +
					hostName + ", Exception: " + e.getMessage());

			logStackTrace(e.getStackTrace());

			System.exit( -1 );

		}

	}

	private void logStackTrace(StackTraceElement[] elements) {
		for (StackTraceElement element : elements) {
			logger.error(element.toString());
		}
	}


	/**
	 * Set the Hostname or IP address of this Node
	 * @param hostName The Hostname/IP address
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	/**
	 * Once communications have been established with the remote node, set the
	 * InputStream used for receiving messages from the remote node
	 * @param inputStream InputStream established by SSH channel during INIT
	 * @throws IOException
	 */
	public void setInputStream(InputStream inputStream) throws IOException {
		mainIOS = new ObjectInputStream( inputStream );
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
	 * Once communications have been established with the remote node, set the
	 * OutputStream used for sending messages to the remote node
	 * @param outputStream OutputStream established by SSH channel during INIT
	 * @throws IOException
	 */
	public void setOutputStream(OutputStream outputStream) throws IOException {

		mainOOS = new ObjectOutputStream( outputStream );
		mainOOS.flush( );
	
	}

	/**
	 * Set the unique ID (process ID) for this Node
	 * @param pid The unique process ID number
	 */
	public void setPid(int pid) {
		this.pid = pid;
	}
	
	/**
	 * Set the port number used to communicate with this node, for inter-node socket communications
	 * @param port The port number to use
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Set the path/filename of the private key to use for SSH connections to this node
	 * @param privateKey The path/filename of the private key to use when connecting to this node
	 */
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	/**
	 * Set the SSH login username for this node
	 * @param userName The SSH login username
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

}