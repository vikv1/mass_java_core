package MASS;



import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import main.java.com.dlb.utils.DLBParams;


/**
 *
 * @author Tim Chuang
 */
public class MProcess
{
    protected static FileOutputStream logger;
    protected static FileOutputStream resultLogger;
    public static ObjectOutputStream MAIN_OOS;
    public static ObjectInputStream MAIN_IOS;

    public static Places PLACES;
    public static Agents AGENTS;
    public static HashMap<Integer, Places> PlacesMap;

    public MProcess(String hostName, int myPid, int nProc, int nThreads, int serverPort, String curDir)
    {
        MASS.myPid = myPid;
        MASS.systemSize = nProc;
        MASS.MASS_PORT = serverPort;
        MASS.CUR_DIR = curDir;
        PlacesMap = new HashMap<Integer, Places>();
                
        try
        {
            File massLogDir = new File(curDir + "/MASS_logs");
            if(!massLogDir.exists())
                  massLogDir.mkdir();
            logger = new FileOutputStream( massLogDir.getAbsolutePath() + "/PID_" + myPid + "_" + hostName + ".txt");
            File massResultLogDir = new File(curDir + "/MASS_result");
            if(!massResultLogDir.exists())
                  massResultLogDir.mkdir();
            resultLogger = new FileOutputStream( massResultLogDir.getAbsolutePath() + "/PID_" + myPid + "_" + hostName + "_result.txt");
        }
        catch (Exception e) 
        { 
            MASS.log("Unable to initiate mProcess.  System shutting down... " + e.getLocalizedMessage());  
            MASS.logException(e);
            finish();
        }

        // set up threads
        MASS.initializeThreads(nThreads);
    }
    
    public static void main(String[] args) throws Exception
    {
        String hostName = args[0];
        int myPid = Integer.parseInt(args[1]);
        int nProc = Integer.parseInt(args[2]);
        int nThreads = Integer.parseInt(args[3]);
        int serverPort = Integer.parseInt(args[4]);
        String curDir = args[5];
        
        MProcess process = new MProcess(hostName, myPid, nProc, nThreads, serverPort, curDir);
        
        log("Initialization Information - PID: " + MASS.myPid + " nProc: " + MASS.systemSize + " nThreads: " + nThreads + " mass_port = " + serverPort);
        
        try
        {
            setupMainConnection();
        }
        catch (Exception e ) 
        { 
            log("Error during main communication initialization - PID: " + myPid + " Msg: " + e.getMessage());
            MASS.logException(e);
            logger.close();
            //System.err.println("Error MAIN_IOS initialization - PID: " + myPid + " Msg: " + e.getMessage());
            System.exit(-1);
        }
        log("Successfully initlialzed connections!");
        
        try
        { 
            process.start(); 
        } 
        catch (Exception e) { MASS.log("An error occured - System shutting down... " + e.getLocalizedMessage()); }
        finally { finish(); }
    }
    
    static void setupMainConnection() throws Exception
    {
         try
         {
            try 
            {
                MAIN_IOS = new ObjectInputStream( System.in );
            } 
            catch( Exception e ) 
            {
                log( "MASS.MASS ERROR: ObjectInputStream Problem: " + e.toString() );
                MASS.logException(e);
                logger.close( );
                throw e;
            }
            MAIN_OOS = new ObjectOutputStream( System.out );
            MAIN_OOS.flush();
         }
         catch( Exception e )
         {
             throw e;
         }       
    }
      
    protected static void log( String s ) 
    {
        try
        {
            logger.write( s.concat("\n").getBytes( ) );
            //logger.writeChars(s + "\n\r");    
            logger.flush();
        } 
        catch( Exception e ) 
        {
            MASS.logException(e);
            // Unable to Write...
            finish( );
        }
    }
    
    protected static void logException( Exception e ) 
    {
        try
        {
            PrintStream ps = new PrintStream(logger);
            e.printStackTrace(ps);
            //logger.writeChars(s + "\n\r");            
        } 
        catch( Exception ex ) 
        {
            // Unable to Write...
            finish( );
        }
    }
    
    protected static void printResult( String s ) 
    {
        try
        {
            resultLogger.write( s.concat("\n").getBytes( ) );
            //logger.writeChars(s + "\n\r");            
        } 
        catch( Exception e ) 
        {
            MASS.logException(e);
            // Unable to Write...
            finish( );
        }
    }    
    
    public void start() throws Exception // always listen on the main communication channel
    {
        while(true)
        {
            Message m = null;
            try
            {
                m = (Message) MAIN_IOS.readObject();
                
                if(m != null)
                {
				  Integer h = new Integer(m.getHandle()); 
                  if(PlacesMap.containsKey(h)) //Figure out which Places message is for
                  {					
                  	PLACES = PlacesMap.get(h);
                  }
		  if ( AGENTS != null ) { // Previously AGENTS created .. added by Fukuda on 11-22-13
		      AGENTS =  ( MASS.getAgents( h ) != null ) ? MASS.getAgents( h ) : AGENTS;
		  }
                    switch(m.getAction())
                    {
                        case Constants.INITIALIZE: // initialization
                            //log("Initialization Params: Action: " + m.getAction() + " ClassName: " + m.getClassName());
                            MASS.nodePidMap = m.getNodePidMap();
                            DLBParams.HISTORY_BASED = m.isHistoryBasedFlag();
                            DLBParams.WINDOW_BASED = m.isWindowBasedFlag();
                            DLBParams.SLOPE_BASED = m.isSlopeBasedFlag();
                           
                            if( m.getBndryLength() > 0 ) {
                                PLACES = new Places( m.getHandle(), m.getClassName(), m.getArgument(),
                                                                m.getBndryLength(), m.wrapEdges(), m.getSize());
                                log("Initialization - Shadow Boundaries Enabled");
                            } else {
                                PLACES = new Places(m.getHandle(), m.getClassName(), m.getArgument(), m.getSize());
                                log("Initialization - Shadow Boundaries Disabled");
                            }

                            PLACES.dlbCount = m.getDlbCount();
			    if ( DLBParams.HISTORY_BASED || DLBParams.WINDOW_BASED || DLBParams.SLOPE_BASED ) // fukuda 4-7-14
				MASS.initBoundaries(PLACES);				// thread boundaries, 
                            PlacesMap.put(h,PLACES); 					//Add places to map

                            MASS.log("dlb values : "+PLACES.dlbCount+"|"+DLBParams.HISTORY_BASED+"|"
                            		+DLBParams.WINDOW_BASED+"|"+DLBParams.SLOPE_BASED);
                            //log("Initialization Complete.. sending ack package");
                            sendAck();
                            //log("Initialization Complete.. ack package sent");
                            break;
                        case Constants.CALL_ALL_VOID_OBJECT:
                            // do call all
                            MASS.log("============Calling CallAllVoidObject: FuncID: " + m.getFunctionId() + "=============");
                            PLACES.callAll(m.getFunctionId(), (Object)m.getArgument());
                            //log("=============Finished CallAllVoidObject: FuncID: " + m.getFunctionId() + "=============");
			    sendAck( );
                            break;
                        case Constants.CALL_ALL_RETURN_OBJECT:
                        {
                            MASS.log("=============Calling CallAllReturnObject: FuncID: " + m.getFunctionId()+ "=============");
                            Object[] retVal = PLACES.callAll(m.getFunctionId(), (Object[])m.getArgument());
                            sendReturnValues(retVal);
                            //log("=============Finished CallAllReturnObject: FuncID: " + m.getFunctionId() + "=============");
                            break;
                        }
                        case Constants.CALL_SOME_VOID_OBJECT:
                            //log("=============Calling CallSomeVoidObject: FuncID: " + m.getFunctionId()+ "=============");
                            PLACES.callSome(m.getFunctionId(), m.getArgument(), m.getIndex());
                            //log("=============Finished CallSomeVoidObject: FuncID: " + m.getFunctionId() + "=============");
			    sendAck( );
                            break;
                        case Constants.EXCHANGE_ALL:
                            //log("=============Calling ExchangeAll: FuncID: " + m.getFunctionId()+ "=============");
                            PLACES.exchangeAll(h, m.getFunctionId(), m.getEADestinations());
                            //log("=============Finished ExchangeAll: FuncID: " + m.getFunctionId() + "=============");
			    sendAck( );
                            break;
                        case Constants.EXCHANGE_BOUNDARY:
                            //log("=============Calling ExchangeBoundary: FuncID: " + m.getFunctionId()+ "=============");
                            PLACES.exchangeBoundary(1, m.getFunctionId(), m.getEBDestinations());
                            //log("=============Finished ExchangeBoundary: FuncID: " + m.getFunctionId() + "=============");
			    sendAck( );
                            break;
                        case Constants.AGENTS_INITIALIZE:
                          //  log("============== Agent Initialization Params: Action: " + m.getAction() + " ClassName: " + m.getClassName() + "===================");
                            AGENTS = new Agents(m.getHandle(), m.getClassName(), m.getArgument(), MASS.getPlaces(m.getPlacesHandle()), m.getAgentInitPopulation());
                           // log("Initialization Complete.. sending ack");
                            sendAck();
                            break; 
                        case Constants.AGENTS_CALL_ALL_VOID:
                            // do call all
			    //log("============Calling AgentsCallAllVoidObject: FuncID: " + m.getFunctionId() + "=============");
                            AGENTS.callAll(m.getFunctionId(), (Object)m.getArgument());
			    //log("=============Finished AgentsCallAllVoidObject: FuncID: " + m.getFunctionId() + "=============");
			   sendAck( );
                            break;
                        case Constants.AGENTS_CALL_ALL_RETURN_OBJECT:
                        {
                            // send the current number of agents this rank holds to the host
                            sendNumOfAgents(m.getHandle());
                            // log("=============Calling AgentsCallAllReturnObject: FuncID: " + m.getFunctionId()+ "=============");
                            Object[] retVal = AGENTS.callAll(m.getFunctionId(), (Object[])m.getArgument());
                            sendReturnValues(retVal);
                            // log("=============Finished AgentsCallAllReturnObject: FuncID: " + m.getFunctionId() + "=============");
                            break;
                        }                            
                        case Constants.AGENTS_MANAGE_ALL:
                            //log("=============Calling Agents ManageAll: FuncID: " + m.getFunctionId()+ "=============");
                            AGENTS.manageAll();
                            //log("=============Finished Agents ManageAll: FuncID: " + m.getFunctionId() + "=============");
			    sendNumOfAgents(m.getHandle()); // added by FUKUDA on 11-22-13

                            break;                            
                        case Constants.FINISH:
                            log("=============Received FINISH command=============");
                            sendAck( );
                            finish( );                            
                            break;
                        default:
                            log("Received unknown action command ");
                            finish( );
                            break;                            
                    }
                }

            }
            catch(Exception e) { MASS.logException(e); throw e; }
        }
    }
    
    public static void sendAck() throws Exception
    {
        Message ackMsg = new Message();
        ackMsg.createAcknowlegementMessage();
        MAIN_OOS.writeObject(ackMsg);
        MAIN_OOS.flush();
    }
    
    public static void sendReturnValues(Object[] retVal) throws Exception
    {
        Message retMsg = new Message();
        retMsg.createCallAllReturnMessage(retVal);
        MAIN_OOS.writeObject(retMsg);
        MAIN_OOS.flush();
    }
   
    
    /*public static void sendAndReceiveHostNames( ) 
    {
        try
        {
            Message msg = new Message();
            msg.createHostNamePackageForAgentMigrate(MASS.RemoteAgentMigrateHostNames);
            MASS.log("Sending " + MASS.RemoteAgentMigrateHostNames.size() + " host names to master");
            MAIN_OOS.writeObject(msg);
            MAIN_OOS.flush();

            // receive message from host
            MASS.RemoteAgentMigrateHostNames.clear();
            ArrayList<String> hostNames = ((Message) MProcess.MAIN_IOS.readObject()).getAgentMigrateHostNames();
            MASS.RemoteAgentMigrateHostNames.addAll(hostNames);
            MASS.log("Received host names from master");
        }
        catch(Exception e)
        {
            MASS.log("Error in sending remote agent migrate host names.. system shutting down. cause: " + e.getMessage());
            finish();
        }         
    }*/
    
    public static void sendNumOfAgents(int handle ) throws Exception
    {
        Message retMsg = new Message();
        Agents agents = MASS.getAgents(handle);
	MASS.log( "local nAgents = " + agents.nAgents( ) ); // added by Fukuda on 11-22-13
        retMsg.createAgentsReportSizeMessage(agents.nAgents());
        MAIN_OOS.writeObject(retMsg);
        MAIN_OOS.flush();
	//MASS.log( "sendNumOfAgents done" );
    }
    
    public static void finish()
    {
        try 
        {
            MASS.log("Finishing remote process........");
            sendAck(); 
            MASS.finish();
            
            // close connections
            MAIN_IOS.close();
            MAIN_OOS.close(); 
            logger.close();
        }
        catch(Exception e){ MASS.log("Error closing connections - system shutting down " + e.getMessage()); }
        finally
        {
            System.exit(0); // All MProcesses will get terminated, while the master process still keeps alive.
        }
       

    }
}
