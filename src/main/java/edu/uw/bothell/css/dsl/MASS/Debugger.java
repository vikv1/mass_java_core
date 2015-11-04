
package edu.uw.bothell.css.dsl.MASS;

public class Debugger extends Debugger_base {
    
	public final static int INIT = 0;
    public final static int FETCH_DEBUG_DATA = 1;
    public final static int INJECT_DEBUG_DATA = 2;
    public final static int FETCH_AGENT_DEBUG_DATA = 3;

    public Debugger(Object argument){
        super(argument);
    }

    public Object callMethod(int functionId, Object argument) {
        
    	switch (functionId) {
        
    		case INIT: return super.init(argument);
    		case FETCH_DEBUG_DATA: return super.fetchDebugData(argument);
    		case INJECT_DEBUG_DATA: return super.InjectDebugData(argument);
    		case FETCH_AGENT_DEBUG_DATA: return super.fetchAgentDebugData(argument);
        
    		default: return null;
        
    	}
        
    }
    
    public static void sendDataToGUI(int status){
	    updateDataConnectionThread(status);
    }

}
