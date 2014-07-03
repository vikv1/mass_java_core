package MASS;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

/**
 *
 * @author Tim Chuang
 */
public class Message implements Serializable
{
	// Message Variables
    private int ACTION;
    private String ARRAY_TYPE;
    private String PLACE_TYPE;
    private String CLASS_NAME;
    private Object ARGUMENT = null;
    private HashMap<String, Integer> PID_MAP = null; // map of user array index, host name responsible
    private HashMap<String, Object> message = null;
    private int[] SIZE = null;
    private int HANDLE;
    private int FUNCTION_ID;
    private Vector<int[]> EA_DESTINATIONS = null;

    // Exchange Boundary Variables
	private Vector<int[]> EB_DESTINATIONS = null;
    private int BNDRYLENGTH; 
    private boolean WRAPEDGES;

    private int AGENT_INIT_POPULATION;
    private int PLACES_HANDLE;
    private int NUM_AGENTS;
    private ArrayList<String> HostNames = null;
    private int[] INDEX;
    
    private int dlbCount = 0;
    private boolean historyBasedFlag = false;
    private boolean windowBasedFlag = false;
    private boolean slopeBasedFlag = false;
    
    public Message() {}


    // Create Initialization Message
    public void createInitializationMessage(int[] size, String arrayType, String placeType, int handle, 
    		String className, Object argument, HashMap<String, Integer> nodePidMap, int bndryLength, 
			boolean wrap, int dlbCnt, boolean historyBased, boolean windowBased, boolean slopeBased)
    {
        ACTION = Constants.INITIALIZE;
        ARRAY_TYPE = arrayType;
        PLACE_TYPE = placeType;
        this.SIZE = size;
        this.HANDLE = handle;
        CLASS_NAME = className;
        ARGUMENT = argument;
        PID_MAP = nodePidMap;
        BNDRYLENGTH = bndryLength;
        WRAPEDGES = wrap;
        this.dlbCount = dlbCnt;
        this.historyBasedFlag = historyBased;
        this.windowBasedFlag = windowBased;
        this.slopeBasedFlag = slopeBased;
    }
    
    // Create Agent Initialization Message
    public void createAgentnitializationMessage(int handle, String className, Object argument
															, int placesHandle, int initPopulation )
    {
        ACTION = Constants.AGENTS_INITIALIZE;
        this.HANDLE = handle;
        CLASS_NAME = className;
        ARGUMENT = argument;
        PLACES_HANDLE = placesHandle;
        AGENT_INIT_POPULATION = initPopulation;
    }    

    // Create Action Message
    public void createActionMessage(int action, int functionId, Object argument, int... index)
    {
        ACTION = action;
        FUNCTION_ID = functionId;
        ARGUMENT = argument;
        if(index != null)
            this.INDEX = index;
    }

    // Create ExchangeAll Message
    public void createExchangeAllMessage(int functionId, Vector<int[]> destinations)
    {
        ACTION = Constants.EXCHANGE_ALL;
        FUNCTION_ID = functionId;
        EA_DESTINATIONS = destinations;
    }

    // Create ExchangeBoundary Message
    public void createExchangeBoundaryMessage( int functionId, Vector<int[]> destinations )
    {
        ACTION          = Constants.EXCHANGE_BOUNDARY;
        FUNCTION_ID     = functionId;
        EB_DESTINATIONS = destinations;
    }
    
    // Create Acknowlegement Message
    public void createAcknowlegementMessage()
    {
        ACTION = Constants.ACK;
    }


    // Create Finish Message
    public void createFinishMessage()
    {
        ACTION = Constants.FINISH;
    }

    
    // Create CallAll Return Message
    public void createCallAllReturnMessage(Object[] retVals)
    {        
        ACTION = Constants.CALL_ALL_RETURN_OBJECT;
        message = new HashMap<String, Object>();
        message.put(Constants.CALL_ALL_RETURN_VALUES, retVals);
    }    
    
    // Create ExchangeAll Request Message
    public void createExchangeAllRequestMessage(ArrayList<RemoteExchangeRequest> exchangeReqList)
    {
        message = new HashMap<String, Object>();
        message.put(Constants.EXCHANGE_ALL_MESSAGE, exchangeReqList);       
    }
    
    // Create Agent Action Message
    public void createAgentActionMessage(int action, int handle, int functionId, Object argument)
    {
        this.HANDLE = handle;
        ACTION = action;
        FUNCTION_ID = functionId;
        ARGUMENT = argument;
    }
    
    // Create Agent ManageAll Message
    public void createAgentManageAllMessage(int action, int handle)
    {
        ACTION = action;
        HANDLE = handle;
    }
    
    // Create Agents ReportSize Message
    public void createAgentsReportSizeMessage(int size)
    {
        NUM_AGENTS = size;
    }
    
    // Create Agent Migrate Request Message
    public void createAgentMigrateRequestMessage(ArrayList<RemoteAgentRequest> agentReqList)
    {
        message = new HashMap<String, Object>();
        message.put(Constants.AGENT_MIGRATE_MESSAGE, agentReqList);       
    }
   
	// Create Host Name Package for Agent Migration 
    public void createHostNamePackageForAgentMigrate(ArrayList<String> hostNames)
    {
        HostNames = hostNames;
    }
    
    public int getAction() { return this.ACTION; }
    public String getArrayType() { return this.ARRAY_TYPE; }
    public String getPlaceType() { return this.PLACE_TYPE; }
    public int[] getSize() { return this.SIZE; }
    public int getHandle() { return this.HANDLE; }
    public int getFunctionId() { return this.FUNCTION_ID; }
    public String getClassName() { return this.CLASS_NAME; }
    public Object getArgument() { return this.ARGUMENT; }
    public HashMap<String, Integer> getNodePidMap() { return this.PID_MAP; }
    public void setMessage(HashMap<String, Object> message) { this.message = message; }
    public HashMap<String, Object> getMessage() { return this.message; }
    public ArrayList<RemoteExchangeRequest> getExchangeAllMessage() {
        return (ArrayList<RemoteExchangeRequest>) this.message.get(Constants.EXCHANGE_ALL_MESSAGE);
    }
    public ArrayList<RemoteExchangeRequest> getExchangeBoundaryMessage() {
        return (ArrayList<RemoteExchangeRequest>) this.message.get(Constants.EXCHANGE_BOUNDARY);
    }
    public Vector<int[]> getEBDestinations() { return EB_DESTINATIONS; }
    public Vector<int[]> getEADestinations() { return EA_DESTINATIONS; }
    public int getAgentInitPopulation() { return AGENT_INIT_POPULATION; }
    public int getPlacesHandle() { return PLACES_HANDLE; }
    public int getNumAgents() { return NUM_AGENTS; }
    public ArrayList<RemoteAgentRequest> getRemoteAgentMigrateRequest()
    {
        if(this.message == null) return null;
        
        return (ArrayList<RemoteAgentRequest>) this.message.get(Constants.AGENT_MIGRATE_MESSAGE);
    }  
    public ArrayList<String> getAgentMigrateHostNames() { return HostNames; }
    public int[] getIndex() { return INDEX; }
    public int getBndryLength() { return BNDRYLENGTH; }
    public boolean wrapEdges() { return WRAPEDGES; }

	public int getDlbCount() {
		return dlbCount;
	}

	public void setDlbCount(int dlbCount) {
		this.dlbCount = dlbCount;
	}

	public boolean isHistoryBasedFlag() {
		return historyBasedFlag;
	}

	public void setHistoryBasedFlag(boolean historyBasedFlag) {
		this.historyBasedFlag = historyBasedFlag;
	}

	public boolean isWindowBasedFlag() {
		return windowBasedFlag;
	}

	public void setWindowBasedFlag(boolean windowBasedFlag) {
		this.windowBasedFlag = windowBasedFlag;
	}

	public boolean isSlopeBasedFlag() {
		return slopeBasedFlag;
	}

	public void setSlopeBasedFlag(boolean slopeBasedFlag) {
		this.slopeBasedFlag = slopeBasedFlag;
	}
	
	public void setHandle(int h){
		this.HANDLE = h;
	}
	
}
