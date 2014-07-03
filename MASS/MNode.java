package MASS;



import com.jcraft.jsch.*;	// Jsch used for Node connections
import java.io.*;		// For Input and Ouput
import java.util.logging.Level;

/**
 * @author Tim Chuang
 */
class MNode 
{

    // private data membets
    private String hostName;                            // HostName of the remote node
    private ObjectInputStream mainIOS;                  // Main communication channel with the master node
    private ObjectOutputStream mainOOS;                 // ain communication channel with the master node
    protected int pid;                                  // Process ID

    // Constructor for remote process
    protected MNode( String host, int newPid )
    {
        hostName = host; pid = newPid;
    }
    
    protected void setupMainConnection( Channel channel ) throws Exception {
       try {
            // Setup Communication
            mainOOS = new ObjectOutputStream( channel.getOutputStream( ) );
            mainOOS.flush( );
            mainIOS = new ObjectInputStream( channel.getInputStream( ) );
        } catch( Exception e ) { 
            MASS.log("ERROR: mNode: Pid: " + pid + " setupMainConnection ");
            MASS.logException(e);
            throw e;
        }
    }


    protected void closeMainConnection( ) {
        try{
            mainIOS.close( ); //exgOIS_A.close( );  exgOIS_B.close( ); // Add MASS.Agent Connection Close
            mainOOS.close( ); // exgOOS_A.close( );  exgOOS_B.close( );
        } catch( Exception e ) {
            MASS.log("ERROR: mNode: Pid: " + pid + " closeConnection " );
            MASS.logException(e);
            System.exit( -1 );
        }
    }

    public void sendMessage(Message m)
    {
        try
        {
            mainOOS.writeObject(m);            
            mainOOS.flush(); 
        }
        catch (Exception e)
        {
            MASS.log("ERROR: mNode: Pid: " + pid + " sendPackage " );
            MASS.logException(e);
            System.exit( -1 );
        }
    }
    

    public Message receiveMessage()
    {
        Message m = null;
        try
        {
            m = (Message)mainIOS.readObject();
        }
        catch (Exception e)
        {
            MASS.log("ERROR: mNode: Pid: " + pid + " readPackage ");
            MASS.logException(e);
            System.exit( -1 );
        }
        
        return m;
    }
    
    public String getHostName() { return hostName; }
    public int getPid() { return pid; }
}