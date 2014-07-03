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
    private int ACTION;
    private String ARRAY_TYPE;
    private String PLACE_TYPE;
    private String CLASS_NAME;
    private Object ARGUMENT = null;
    private HashMap<Integer, String> NETWORK_MAP = null; // map of user array index, host name responsible
    private HashMap<String, Integer> PID_MAP = null; // map of user array index, host name responsible
    private HashMap<String, Object> message = null;
    private int[] SIZE = null;
    private int HANDLE;
    private int FUNCTION_ID;
    private Vector<int[]> EA_DESTINATIONS = null;
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

    public void createInitializationMessage(int[] size, String arrayType, String placeType, int handle, 
    		String className, Object argument, HashMap<Integer, String> networkMap, HashMap<String, Integer> nodePidMap, int dlbCnt, boolean historyBased,
    		boolean windowBased, boolean slopeBased)
    {
        ACTION = Constants.INITIALIZE;
        ARRAY_TYPE = arrayType;
        PLACE_TYPE = placeType;
        this.SIZE = size;
        this.HANDLE = handle;
        CLASS_NAME = className;
        ARGUMENT = argument;
        NETWORK_MAP = networkMap;
        PID_MAP = nodePidMap;
        this.dlbCount = dlbCnt;
        this.historyBasedFlag = historyBased;
        this.windowBasedFlag = windowBased;
        this.slopeBasedFlag = slopeBased;
    }
    
    public void createAgentnitializationMessage(int handle
		, String className
		, Object argument
		, int placesHandle
		, int initPopulation )
    {
        ACTION = Constants.AGENTS_INITIALIZE;
        this.HANDLE = handle;
        CLASS_NAME = className;
        ARGUMENT = argument;
        PLACES_HANDLE = placesHandle;
        AGENT_INIT_POPULATION = initPopulation;
    }    

    public void createActionMessage(int action, int functionId, Object argument, int... index)
    {
        ACTION = action;
        FUNCTION_ID = functionId;
        ARGUMENT = argument;
        if(index != null)
            this.INDEX = index;
    }

    public void createExchangeAllMessage(int functionId, Vector<int[]> destinations)
    {
        ACTION = Constants.EXCHANGE_ALL;
        FUNCTION_ID = functionId;
        EA_DESTINATIONS = destinations;
    }
    
    public void createAcknowlegementMessage()
    {
        ACTION = Constants.ACK;
    }

    public void createFinishMessage()
    {
        ACTION = Constants.FINISH;
    }

    
    public void createCallAllReturnMessage(Object[] retVals)
    {        
        ACTION = Constants.CALL_ALL_RETURN_OBJECT;
        message = new HashMap<String, Object>();
        message.put(Constants.CALL_ALL_RETURN_VALUES, retVals);
    }    
    
    public void createExchangeAllRequestMessage(ArrayList<RemoteExchangeRequest> exchangeReqList)
    {
        message = new HashMap<String, Object>();
        message.put(Constants.EXCHANGE_ALL_MESSAGE, exchangeReqList);       
    }
    
    public void createAgentActionMessage(int action, int handle, int functionId, Object argument)
    {
        this.HANDLE = handle;
        ACTION = action;
        FUNCTION_ID = functionId;
        ARGUMENT = argument;
    }
    
    public void createAgentManageAllMessage(int action, int handle)
    {
        ACTION = action;
        HANDLE = handle;
    }
    
    public void createAgentsReportSizeMessage(int size)
    {
        NUM_AGENTS = size;
    }
    
    public void createAgentMigrateRequestMessage(ArrayList<RemoteAgentRequest> agentReqList)
    {
        message = new HashMap<String, Object>();
        message.put(Constants.AGENT_MIGRATE_MESSAGE, agentReqList);       
    }
    
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
    public HashMap<Integer, String> getNetworkMap() { return this.NETWORK_MAP; }
    public HashMap<String, Integer> getNodePidMap() { return this.PID_MAP; }
    public void setMessage(HashMap<String, Object> message) { this.message = message; }
    public HashMap<String, Object> getMessage() { return this.message; }
    public ArrayList<RemoteExchangeRequest> getExchangeAllMessage()
    {
        return (ArrayList<RemoteExchangeRequest>) this.message.get(Constants.EXCHANGE_ALL_MESSAGE);
    }
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
