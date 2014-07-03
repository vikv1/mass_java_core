package MASS;



import com.jcraft.jsch.*;
import java.util.*;
import java.io.*;
import sun.net.util.IPAddressUtil;

class Utilities
{

  protected ArrayList<String> GetNetworkNodesList()
  {
    ArrayList<String> networkNodes = new ArrayList<String>();
    String line= null;
    try
    {
    Process p= Runtime.getRuntime().exec("cat /etc/hosts");
    BufferedReader stdInput= new BufferedReader(new InputStreamReader(p.getInputStream()));

    while ((line = stdInput.readLine()) != null) {

       // if(line.startsWith("#")) continue;

        String [] tokens = line.split(" ");

        //Sometimes IP addresses are delimited by TAB which is not caught by above statement.
        if(tokens.length == 1)
        {
            tokens = line.split("\t");
        }

        String ipaddress = tokens[0].trim().toString();

        IPAddressUtil util=new IPAddressUtil();
        if(util.isIPv4LiteralAddress(ipaddress) && !ipaddress.equalsIgnoreCase("127.0.0.1"))
        {
            String nodeName= "";
            for(int loop=1; loop < tokens.length; loop++)
            {
                nodeName = tokens[loop].trim().toString();
                if(nodeName.trim().length() > 0)
                {
                    networkNodes.add(nodeName);
                    break;
                }
            }
            if(nodeName.trim().length() == 0)
            {
                networkNodes.add(ipaddress);
            }
        }
      }
     }
     catch(Exception e)
     {
         e.printStackTrace();
     }

     return networkNodes;
  }
  
  //This method return channel, so that consumers will have access to Input and outputstream.
  //Also note that the consumers need to explicitly call the  channel.disconnect(); & channel.getSession().disconnect();
  protected Channel LaunchRemoteProcessEx(String Host, int PortNumber, String Command, String UserName, String Password)
  {
        Channel channel = null;
        try
        {
            JSch jsch = new JSch();

            // ssh connection
            Session session = jsch.getSession(UserName, Host, PortNumber);

            // username and password will be given via UserInfo interface.
            UserInfo ui = new MyUserInfo( Password );
            session.setUserInfo( ui );
            session.connect( );

            channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(Command);

            channel.connect();
                //Thread.sleep(3000);			// Causes Error in Mass Proccess InputStream Read: Misses read
            
        }
        catch(Exception e)
        {
            System.err.println(e);
        }
        
        return channel;
  }

  //If consumer doesn't care about result(Outputstream) they can use this method.
  protected boolean LaunchRemoteProcess(String Host, int PortNumber, String Command, String UserName, String Password)
  {

      Channel channel = null;
      boolean success = false;
      try
      {
        channel = this.LaunchRemoteProcessEx(Host, PortNumber, Command, UserName, Password);
        if(channel != null)
        {
            success = true;
            channel.disconnect();
            channel.getSession().disconnect();
        }
      }
      catch(Exception e)
      {
          success = false;
          System.err.println("Exception : MASS.Utilities:LaunchRemoteProcess : " + e.getMessage() + " \n"  + e);
      }
      return success;
  }
  
    private class MyUserInfo implements UserInfo 
    {
         // Private data members
         private String _passwd = null;	// Users password

         // Constructor sets up password
         public MyUserInfo( String passwd ) 
         {
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
         // Because this application is run remotely we do not want to prompt the user
         public boolean promptYesNo( String message ) { return true; };
         public void showMessage( String message ) { };
  }
    
}