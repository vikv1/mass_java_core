package edu.uw.bothell.css.dsl.MASS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
public class Debugger_base extends Place {

    private int placesHandler;
    private int agentsHandler;
    private Places_base places;
    private Places_base debugger_places;
    private Agents_base agents;
    private int[] psize;
    
    @SuppressWarnings("unused")
	private int nDimen;
    
    private int pid;
    private int nTotalPlace;//total user places
    private int debugger_size;//total number of debugger place
    private Object[] placeDebugData;
    private Object[] agentDebugData;
    private ArrayList<AgentDebugData> tmp_agentDebugData;
    private static int dataConnectionThreadstatus;
    private boolean isPlaceAgentMode; //true: place+agent, false: place
    public static Debugger_base debuggerInstance;
    
    /*public final static int init_ = 0;
      public final static int fetchDebugData_ = 1;
      public final static int injectDebugData_ = 2;*/
    private final static int SOCKET_PORT = 40863;
    
    @SuppressWarnings("unused")
	private final static String CMD_NEW_APPLICATION = "cmdNewApplication"; 
    
    @SuppressWarnings("unused")
	private final static String CMD_PLACE_DATA = "cmdPlaceData";
    
    @SuppressWarnings("unused")
	private final static String CMD_AGENT_DATA = "cmdAgentData";
    
    private final static String CMD_PAUSE = "cmdPause";
    private final static String CMD_RESUME = "cmdResume";
    private final static String CMD_INJECT_PLACE = "cmdInjectPlace";
    protected final static int STATUS_READY = 0;
    protected final static int STATUS_SEND_PLACE_DATA = 1;
    protected final static int STATUS_SEND_AGENT_DATA = 2;
 
    /*
     * if sending_lock is true means debugger is sending data to GUI
     * if stop_lock is true means user has stopped the computation
     */
    public static boolean[] sending_lock;
    public static boolean[] stop_lock;
    private static boolean[] sending_place_lock;
    


    /**
     * @param places is the Places handler, agents is the Agents handler
     * */
    public Debugger_base(Object argument) {
	int[] handler = (int[]) argument;
	debuggerInstance = this;
	dataConnectionThreadstatus = STATUS_READY;
	this.placesHandler = handler[0];
	this.agentsHandler = handler[1];
    }
	
    protected Object init( Object args ) {
	sending_lock = new boolean[1];
	sending_lock[0] = false;
	stop_lock = new boolean[1];
	stop_lock[0] = false;
	sending_place_lock = new boolean[1];
	sending_place_lock[0] = false;

	debugger_size = getSize()[0];
	places = MASS_base.getPlacesMap().get(placesHandler);
	agents = MASS_base.getAgentsMap().get(agentsHandler);
	isPlaceAgentMode = agents == null ? false : true;
	debugger_places = MASS_base.getPlacesMap().get(99);
	pid = MASS_base.getMyPid();
	psize = places.getSize().clone();

	nTotalPlace = 1;
	for (int i = 0; i < psize.length; i++)
	    nTotalPlace *= psize[i];
	
	nDimen = psize.length;
	placeDebugData = new Double[places.getPlacesSize()];
	tmp_agentDebugData = new ArrayList<AgentDebugData>();
		
	// start a TCP socket server to communicate with GUI
	if(pid == 0){
	    System.out.println("System size: " + debugger_size);
	    System.out.println("Total number of place: " + nTotalPlace);
	    System.out.println("places size " + places.getPlacesSize());
	    System.out.println("places array size " + places.getPlaces().length);
	    System.out.println("place name " + places.getPlaces()[0].getClass().getName());

	    startTCPSocketServer();
	}
	return null;
    }

    /*public Object callMethod(int functionId, Object argument) {
      switch (functionId) {
      case init_: 
      case fetchDebugData_: return fetchDebugData(argument);
      case injectDebugData_: return InjectDebugData(argument);
      default: break;
      }
      return null;
      }*/

    protected Object fetchDebugData(Object argument) {
	// new Object[MASS_base.currentPlaces.places_size];
	tmp_agentDebugData.clear();
	for (int i = 0; i < places.getPlacesSize(); i++) {
		Place curPlace = places.getPlaces()[i];
		placeDebugData[i] = (Double)curPlace.getDebugData();
		//get agent debug data
		//	    for(int j=0; j<curPlace.agents.size(); j++){
		for (Agent agent : curPlace.getAgents()) {

			if(pid == 0){
				//System.out.println(curPlace.agents.get(j).agentId);
			}

			AgentDebugData tmp = (AgentDebugData)(agent.getDebugData());
			if(tmp != null){
				tmp_agentDebugData.add(tmp);
			}

	}

		if(pid !=0 && ((Double)placeDebugData[i]).doubleValue()!=0 && ((Double)placeDebugData[i]).doubleValue()-20.0 != 0){
		//MASS_base.log(String.valueOf(((Double)placeDebugData[i]).doubleValue()));
	    }
	}
	
	return (Object)placeDebugData;	
    }

    protected Object fetchAgentDebugData(Object argument){
	int n = tmp_agentDebugData.size();
	agentDebugData = new AgentDebugData[n];
	for(int i=0; i<n; i++){
	    AgentDebugData a = tmp_agentDebugData.get(i);
	    agentDebugData[i] = new AgentDebugData(a.x, a.y, a.value);
	}
	/*
	int nAgents = ((Agents)agents).nAgents();
	agentDebugData = new AgentDebugData[nAgents];
	int i = Mthread.agentBagSize, j=0;
	if(pid==0) System.out.println("agent bag size: "+i);
	for(; i > 0; i--){
	    agentDebugData[j++] = agents.agents.get(i).getDebugData();
	    if(pid == 0){
		System.out.println(((AgentDebugData)agentDebugData[j-1]).x+
				   " "+((AgentDebugData)agentDebugData[j-1]).y+
				   " "+((AgentDebugData)agentDebugData[j-1]).value);
	    }
	    }*/
	return (Object)agentDebugData;
	
    }

    protected Object InjectDebugData(Object argument) {
	//debugger places are one dimention
	SinglePlaceAgentData spaData = (SinglePlaceAgentData)argument;
	int x = spaData.getX();
	int y = spaData.getY();
	int place_val = spaData.getPlace_val();

	int stripe = nTotalPlace/debugger_size;
	int offset = (x*psize[0]+y)/stripe;
	if(offset != pid){
	    return null;
	}
	//check if x and y belong to this node
	int i = (x*psize[0]+y)-stripe*pid;
	places.getPlaces()[i].setDebugData(new Double(place_val));
	return null;
    }
    protected Object injectAgentDebugData(Object argument){
	//the argument contains place index and agent index
	return null;
    }
	
    protected static void updateDataConnectionThread(int status){
	if(status == STATUS_SEND_PLACE_DATA){
	    //System.out.println("STATUS_SEND_PLACE_DATA");
	    synchronized(sending_place_lock){
		sending_place_lock[0] = true;
	    }
	    synchronized(sending_lock){
		sending_lock[0] = true;
	    }
	}else if(status == STATUS_SEND_AGENT_DATA){
	    //System.out.println("STATUS_SEND_AGENT_DATA");
	    synchronized(sending_place_lock){
		if(sending_place_lock[0]){
		    System.out.println("pending to send agent data");
		    try{
			sending_place_lock.wait();
		    }catch (Exception e){
			System.out.println("wait failed");
		    }
		}
	    }
	}else{

	}
	synchronized(debuggerInstance){
	    dataConnectionThreadstatus = status;
	}
    }

    private void startTCPSocketServer() {

	new Thread() {
	    @Override
	    public void run() {
		ObjectInputStream input = null;
		ObjectOutputStream output = null;
		ServerSocket server = null;
		Socket guiClient = null;
		try{
		    server = new ServerSocket(SOCKET_PORT);
		} catch (IOException e){
		    e.printStackTrace();
		}
		while (true) {
		    try {
			guiClient = server.accept();
		    } catch (SocketTimeoutException ste){
			
		    } catch (IOException e) {
			e.printStackTrace();
		    }

		    if (guiClient != null) {
			break;
		    }
		}
		
		try {
		    System.out.println(guiClient.getRemoteSocketAddress());
		    System.out.println("local socket address: "+guiClient.getLocalSocketAddress());
		    guiClient.setSoTimeout(100);
		    output = new ObjectOutputStream(guiClient.getOutputStream());
		    output.flush();
		    input = new ObjectInputStream(guiClient.getInputStream());
		} catch (IOException e) {
		    e.printStackTrace();
		}

		SocketGUIConnection guiConnection = new SocketGUIConnection(output, input);
		guiConnection.start();
	    }
	}.start();
    }

    private class SocketGUIConnection extends Thread {
	private ObjectOutputStream output;
	private ObjectInputStream input;

	public SocketGUIConnection(ObjectOutputStream output,ObjectInputStream input) {
	    this.output = output;
	    this.input = input;
	}
	
	@Override
	public void run() {

	    // send array dimension and program mode (PLACE_ONLY, PLACE_AGENT)
	    try{
		output.writeObject("cmdNewApplication");
		output.writeObject(places.getPlaces()[0].getClass().getName());
		output.writeInt((psize[0]));//for simple just send the size[0], currently assume its N*N array 
		output.writeBoolean(isPlaceAgentMode);//send the mode of user application
		output.flush();
	    } catch (IOException e){
		e.printStackTrace();
		return;
	    }

	    while (true) {
		try{
		    Thread.sleep(1000);
		}catch(Exception e){
		    System.out.println("exception in sleep");
		}

		System.out.println("status:"+dataConnectionThreadstatus);
		synchronized(debuggerInstance){
		    probeGUICmd();
		    switch (dataConnectionThreadstatus){
		    case STATUS_SEND_PLACE_DATA:
			//dataConnectionThreadstatus = STATUS_READY;
			synchronized(sending_lock){
			    if(!sending_lock[0]) {System.out.println("sending lock is false"); break;}
			    try{
				output.writeObject("cmdPlaceData");
				
				for(int i=0; i<MASS_base.getCurrentReturns().length; i++){
				    Double[] d = (Double[])(MASS_base.getCurrentReturns()[i]);
				    for(int j=0; j<d.length; j++){
					output.writeDouble(d[j].doubleValue());
				    }
				}
				output.flush();
				
				
			    } catch (IOException ioe){
				//ioe.printStackTrace();
			    } finally{
		
			    }
			    //clear sending_lock if user applications is only place based
			    if(!isPlaceAgentMode){
				sending_lock[0] = false;
				sending_lock.notifyAll();
			    }
			}
			synchronized(sending_place_lock){
			    sending_place_lock[0] = false;
			    sending_place_lock.notify();
			}
			break;
		    case STATUS_SEND_AGENT_DATA:
			//dataConnectionThreadstatus = STATUS_READY;
			synchronized(sending_lock){
			    if(!sending_lock[0]) break;
			    try{
				output.writeObject("cmdAgentData");
				int nAgents = 0;
				for(int i=0; i<MASS_base.getCurrentReturns().length; i++){
				    nAgents += ((AgentDebugData[])(MASS_base.getCurrentReturns()[i])).length;
				}
				//send the number of agents
				output.writeObject(Integer.toString(nAgents));
				for(int i=0; i<MASS_base.getCurrentReturns().length; i++){
                                    AgentDebugData[] next_agents= (AgentDebugData[])(MASS_base.getCurrentReturns()[i]);
				    for(int j=0; j<next_agents.length; j++){
					output.writeObject(next_agents[j]);
				    }
                                }
				output.flush();
			    } catch (IOException ioe){
				
			    } finally{
			    }
			    sending_lock[0] = false;
			    sending_lock.notifyAll();
			}
		    case STATUS_READY:
			break;
		    default: break;
		    }
		}
		//Debugger_base.updateDataConnectionThread(STATUS_READY);
	    }
	}

	private void probeGUICmd(){
	    try{
		//if(input.available() <= 0) return;
		String cmd = "";
		cmd = (String)input.readObject();//block 100ms
		System.out.println("Received:"+cmd);
		if(cmd != null && cmd.length()>0){
		    processCommand(cmd);
		}
	    }catch(IOException e){
		//System.out.println("no data from GUI");
	    }catch(ClassNotFoundException e){
		e.printStackTrace();
	    }finally{

	    }
	}

	//private void 
	private void processCommand(String cmd){
	    if(cmd.equals(CMD_PAUSE)){//user stop the computation
		synchronized(stop_lock){
		    stop_lock[0] = true;
		}
	    }else if(cmd.equals(CMD_RESUME)){//user resume the computation
		synchronized(stop_lock){
		    stop_lock[0] = false;
		    stop_lock.notifyAll();
		}
	    }else if(cmd.equals(CMD_INJECT_PLACE)){
		try{
		    //SinglePlaceAgentData spaData = (SinglePlaceAgentData)input.readObject();
		    int x = Integer.parseInt((String)input.readObject());
		    int y = Integer.parseInt((String)input.readObject());
		    int v = Integer.parseInt((String)input.readObject());
		    //System.out.println("x:"+x+"y:"+y+"v:"+v);
		    SinglePlaceAgentData data = new SinglePlaceAgentData(x,y,v,null);
		    debugger_places.callAll(2, data, 0);
		}catch(IOException e){
		    e.printStackTrace();
		}
		catch(ClassNotFoundException e){
		  e.printStackTrace();
		}
	    }
	}

    }

}

class SinglePlaceAgentData implements Serializable{

	private static final long serialVersionUID = 1L;
	private int x;
	private int y;
	private int place_val;
	private ArrayList<Integer> agents;

	@SuppressWarnings("unchecked")
	public SinglePlaceAgentData(int x, int y, int val, ArrayList<Integer> agents){

		this.x = x;
		this.y = y;
		this.place_val = val;
		this.agents = new ArrayList<Integer>();
		if(agents != null){
			this.agents = (ArrayList<Integer>)(agents.clone());
		}
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getPlace_val() {
		return place_val;
	}
	public void setPlace_val(int place_val) {
		this.place_val = place_val;
	}
	public ArrayList<Integer> getAgents() {
		return agents;
	}
	public void setAgents(ArrayList<Integer> agents) {
		this.agents = agents;
	}
}

class AgentDebugData implements Serializable {

	private static final long serialVersionUID = 1L;
	int x;
	int y;
	double value;

	AgentDebugData(int x, int y, double value) {
		this.x = x;
		this.y = y;
		this.value = value;
	}

}