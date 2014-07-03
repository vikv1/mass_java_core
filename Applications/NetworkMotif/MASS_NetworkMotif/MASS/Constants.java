package MASS;


/**
 *
 * @author Tim Chuang
 */
public class Constants
{
    // Command types
    public static final String COMMAND = "COMMAND";
    public static final String ARRAY_TYPE = "ARRAY_TYPE";
    public static final String PLACES_METHOD_NAME = "PLACES_METHOD_NAME";
    public static final String PLACE_TYPE = "PLACE_TYPE";

    // action types
    public static final int INITIALIZE = 0;
    public static final int FINISH =  -1;
    public static final int ACK = 1;
    
    public static final int CALL_ALL_VOID_OBJECT = 2;
    public static final int CALL_ALL_VOID_INT = 3;
    public static final int CALL_ALL_VOID_FLOAT = 4;
    public static final int CALL_ALL_VOID_DOUBLE = 5;
    public static final int CALL_ALL_RETURN_OBJECT = 6;
    public static final int CALL_ALL_RETURN_INT = 7;
    public static final int CALL_ALL_RETURN_FLOAT = 8;
    public static final int CALL_ALL_RETURN_DOUBLE = 9;

    public static final int CALL_SOME_VOID_OBJECT = 10;
    public static final int EXCHANGE_ALL = 11;
    
    public static final int AGENTS_INITIALIZE = 12;
    public static final int AGENTS_CALL_ALL_VOID = 13;
    public static final int AGENTS_CALL_ALL_RETURN_OBJECT = 14;
    public static final int AGENTS_MANAGE_ALL = 15;

    // exchange Boundary
    public static final int EXCHANGE_BOUNDARY = 16;  

    // array types
    public static final String OBJECT_ARRAY = "OBJECT_ARRAY";
    public static final String INT_ARRAY = "INT_ARRAY";
    public static final String FLOAT_ARRAY = "FLOAT_ARRAY";
    public static final String DOUBLE_ARRAY = "DOUBLE_ARRAY";

    // place types
    public static final String PLACE = "PLACE";
    public static final String AGENT =  "AGENT";
    public static final String PRIMITIVE = "PRIMITIVE";

    // Initialization components
    public static final String PLACES_HANDLE = "PLACES_HANDLE";
    public static final String RANK_OFFSET = "RANK_OFFSET";
    public static final String RANK_RANGE = "RANK_RANGE";
    public static final String CLASS_NAME = "CLASS_NAME";
    public static final String ARGUMENT = "ARGUMENT";
    public static final String NETWORKMAP = "NETWORKMAP";
    
    // Message types
    public static final String CALL_ALL_RETURN_VALUES = "CALL_ALL_RETURN_VALUES";
    public static final String EXCHANGE_ALL_MESSAGE = "EXCHANGE_ALL_MESSAGE";
    public static final String AGENT_MIGRATE_MESSAGE = "AGENT_MIGRATE_MESSAGE";
    public static final int MASTER_PID = 0;
}
