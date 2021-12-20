package edu.uw.bothell.css.dsl.MASS.infra;

import java.io.Serializable;

// MASSMapMsg is a message type for node-to-node communication that is used by 
// MASSSimpleDistributedMap to issue functions across related nodes.
public class MASSMapMsg implements Serializable {
    // Function is an enum that specifies the remote function being requested 
    // by the MASSMapMsg. These effectively map to the public functions of the 
    // distributed map.
    public static enum Function {
        CLEAR,
        CONTAINS_KEY,
        CONTAINS_VALUE,
        ENTRY_SET,
        GET,
        IS_EMPTY,
        KEY_SET,
        PUT,
        PUT_ALL,
        REMOVE,
        REVERSE_LOOKUP,
        SIZE,
        VALUES
    }

    // func is the function to be called.
    public Function func;
    
    // arg1 is an arbitrary argument used by a function call.
    public Object arg1;

    // arg2 is an arbitrary argument used by a function call.
    public Object arg2;

    // retVal is the value returned by a function, if it returns a value.
    public Object returnValue;

    // success is true if the remote function execution was successful, false otherwise.
    public boolean success;

    // errormsg contains a human readable error message that can be looked at
    // the function call is unsuccessful.
    public String errormsg;

    // Constructor used to call functions that do not take arguments.
    public MASSMapMsg(Function func) {
        this.func = func;
    }

    // Constructor used to call functions that take a single argument.
    public MASSMapMsg(Function func, Object arg) {
        this.func = func;
        this.arg1 = arg;
    }

    // Constructor used to call functions that take a single argument.
    public MASSMapMsg(Function func, Object arg1, Object arg2) {
        this.func = func;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public String toString() {
        return String.format(
            "MASSMapMsg{func=%s, arg1=%s, arg2=%s, returnValue=%s, success=%s, errormsg=%s}",
            this.func,
            this.arg1,
            this.arg2,
            this.returnValue,
            this.success,
            this.errormsg
        );
    }

    
}
