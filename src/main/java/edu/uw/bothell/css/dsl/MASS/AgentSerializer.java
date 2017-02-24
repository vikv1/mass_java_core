package edu.uw.bothell.css.dsl.MASS;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.*;
import com.esotericsoftware.minlog.Log;

import org.objenesis.strategy.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

/**
 * Created by utku on 1/25/17.
 *
 * Responsible for serializing and de-serializing agent object
 */
public class AgentSerializer
{
    // Shared instance
    private static AgentSerializer instance = null;

    // Serialized object extension (must be unique)
    private static final String KRYO_SERIALIZATION_EXTENSION = "kryo.ser";

    // Classes to be registered (Kryo-only)
    private Class[] classes;

    // Class registration base id - important: 0-9 are used by Kryo
    private static final int KRYO_SERIALIZATION_CLASS_REG_BASE_ID = 10;

    // Max number of agent (default 40)
    private int maxNumberOfAgents = 40;

    // Serializer
    //private Kryo kryo;

    // De-serialized object to be read
    //private Input input;

    // Serialized object to be written
    //private Output output;

    // FileStream for input
    // private FileInputStream fileInputStream;

    // FileStream for output
    // private FileOutputStream fileOutputStream;



    protected AgentSerializer()
    {

    }

    public static AgentSerializer getInstance() {
        if(instance == null) {
            instance = new AgentSerializer();
        }
        return instance;
    }

    public String serializeAgent(Agent agent)
    {
        /*
        Kryo kryo = new Kryo();
        for (int idx = 0; idx < classes.length ; idx++)
        {
            kryo.register(classes[idx], KRYO_SERIALIZATION_CLASS_REG_BASE_ID + idx);
        }
        //kryo.register(Place.class, new SynchronizedCollectionsSerializer());
        //kryo.register(Agent.class, 1);
        //kryo.register(ArrayList.class, 2);
        //kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
        //kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        Log.set(Log.LEVEL_TRACE);
        //kryo.setDefaultSerializer(FieldSerializer.class);
        //kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(FieldSerializer.CachedFieldNameStrategy.EXTENDED);
        //kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        try
        {
            //serializationId++;
            //String serializedAgentIdentifier = serializationId + KRYO_SERIALIZATION_EXTENSION;
            String serializedAgentIdentifier =  System.currentTimeMillis() + KRYO_SERIALIZATION_EXTENSION;
            System.out.println("serializing agent with id: " + serializedAgentIdentifier);
            FileOutputStream fileOutputStream = new FileOutputStream(serializedAgentIdentifier);
            Output output = new Output(fileOutputStream);
            kryo.writeObject(output, agent);
            output.close();
            return serializedAgentIdentifier;

        }
        catch (java.io.IOException exx)
        {
            System.out.println("IOException at serializeAgent");
            return null;
        }
        */

        try
        {
            //System.out.println("----");
            String serializedAgentIdentifier =  System.currentTimeMillis() + KRYO_SERIALIZATION_EXTENSION;
            //System.out.println("serializing agent with id: " + serializedAgentIdentifier);
            //System.out.println("serialized agent's index is: " + agent.getIndex()[0] + " " + agent.getIndex()[1]);
            FileOutputStream fileOutputStream = new FileOutputStream(serializedAgentIdentifier);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(agent);
            objectOutputStream.close();
            fileOutputStream.close();
            //System.out.println("returning id: " + serializedAgentIdentifier);
            return serializedAgentIdentifier;
        }
        catch (java.io.IOException ex)
        {
            System.out.println("IOException at serializeAgent" + " : " + ex.toString());
            return null;
        }

    }

    public Agent deserializeAgent(String serializedAgentIdentifier)
    {
        /*
        Kryo kryo = new Kryo();
        for (int idx = 0; idx < classes.length ; idx++)
        {
            kryo.register(classes[idx], KRYO_SERIALIZATION_CLASS_REG_BASE_ID + idx);
        }
        //kryo.register(Place.class, new SynchronizedCollectionsSerializer());
        //kryo.register(Agent.class, 1);
        //kryo.register(ArrayList.class, 2);
        //kryo.setInstantiatorStrategy(new SerializingInstantiatorStrategy());
        //kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        Log.set(Log.LEVEL_TRACE);
        //kryo.setDefaultSerializer(FieldSerializer.class);
        //kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(FieldSerializer.CachedFieldNameStrategy.EXTENDED);
        //kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

        try
        {
            System.out.println("de-serializing agent with id: " + serializedAgentIdentifier);
            FileInputStream fileInputStream = new FileInputStream(serializedAgentIdentifier);
            Input input = new Input(fileInputStream);
            Agent deserializedAgent = (Agent)kryo.readObject(input, Agent.class);
            return deserializedAgent;
        }
        catch (java.io.FileNotFoundException ex)
        {
            System.out.println("FileNotFoundException at deserializeAgent");
            return null;
        }
        */

        try
        {
            //System.out.println("----");
            //System.out.println("de-serializing agent with id: " + serializedAgentIdentifier);
            FileInputStream fileInputStream = new FileInputStream(serializedAgentIdentifier);
            ObjectInputStream objectInputStream= new ObjectInputStream(fileInputStream);
            Agent deserializedAgent = (Agent) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
            return deserializedAgent;
        }
        catch(java.io.IOException ex)
        {
            System.out.println("IOException at deserializeAgent");
            return null;
        }
        catch(ClassNotFoundException c)
        {
            System.out.println("ClassNotFoundException at deserializeAgent");
            return null;
        }


    }

    /** Getter and Setters for private fields **/

    protected Class[] getRegisteredClasses()
    {
        return this.classes;
    }

    protected int getMaxNumberOfAgents() { return this.maxNumberOfAgents; }

    protected void setRegisteredClasses(Class[] classes)
    {
        this.classes = classes;
    }

    protected void setMaxNumberOfAgents(int maxNumberOfAgents) { if (maxNumberOfAgents > 0) this.maxNumberOfAgents = maxNumberOfAgents; }
}
