package edu.uw.bothell.css.dsl.MASS;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by utku on 1/25/17.
 *
 * Responsible for serializing and de-serializing agent object
 */
public class AgentSerializer
{
    // Serialized object extension (must be unique)
    private static final String KRYO_SERIALIZATION_EXTENSION = "kryo.ser";

    // Serializer
    private Kryo kryo;

    // De-serialized object to be read
    private Input input;

    // Serialized object to be written
    private Output output;

    // FileStream for input
    private FileInputStream fileInputStream;

    // FileStream for output
    private FileOutputStream fileOutputStream;


    public AgentSerializer()
    {
        kryo = new Kryo();
        kryo.register(Agent.class);
    }

    public String serializeAgent(Agent agent)
    {
        try
        {
            String serializedAgentIdentifier = agent.getAgentId() + KRYO_SERIALIZATION_EXTENSION;
            fileOutputStream = new FileOutputStream(serializedAgentIdentifier);
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
    }

    public Agent deserializeAgent(String serializedAgentIdentifier)
    {
        try
        {
            fileInputStream = new FileInputStream(serializedAgentIdentifier);
            Input input = new Input(fileInputStream);
            Agent deserializedAgent = (Agent)kryo.readObject(input, Agent.class);
            return deserializedAgent;
        }
        catch (java.io.FileNotFoundException ex)
        {
            System.out.println("FileNotFoundException at deserializeAgent");
            return null;
        }
    }
}
