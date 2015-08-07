
package edu.uw.bothell.css.dsl.MASS;

public class Debugger extends Debugger_base {
    public final static int init_ = 0;
    public final static int fetchDebugData_ = 1;
    public final static int injectDebugData_ = 2;
    public final static int fetchAgentDebugData_ = 3;

    public Debugger(Object argument){
        super(argument);
    }

    public Object callMethod(int functionId, Object argument) {
        switch (functionId) {
        case init_: return super.init(argument);
        case fetchDebugData_: return super.fetchDebugData(argument);
        case injectDebugData_: return super.InjectDebugData(argument);
        case fetchAgentDebugData_: return super.fetchAgentDebugData(argument);
        default: break;
        }
        return null;
    }
    
    public static void sendDataToGUI(int status){
	    updateDataConnectionThread(status);
    }

}
