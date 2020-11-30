/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Created by utku on 1/25/17.
 *
 * Responsible for serializing and de-serializing agent object
 * Singleton instance is thread-safe !
 */
public class AgentSerializer
{
    // Shared instance
//    private static AgentSerializer instance = null;

    // Serialized object extension (must be unique)
    // private static final String KRYO_SERIALIZATION_EXTENSION = "kryo.ser";

    // Classes to be registered (Kryo-only)
    @SuppressWarnings("rawtypes")
	private Class[] classes;

    // Class registration base id - important: 0-9 are used by Kryo
//    private static final int KRYO_SERIALIZATION_CLASS_REG_BASE_ID = 10;

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

    private static class LazyHolder
    {
        private static final AgentSerializer INSTANCE = new AgentSerializer();
    }

    public static AgentSerializer getInstance()
    {
        /*
        if(instance == null) {
            instance = new AgentSerializer();
        }
        return instance;
        */
        return LazyHolder.INSTANCE;
    }

    /**
     * Serialize an Agent
     * @param agent The Agent to serialize
     * @return An array of bytes representing the Agent
     */
    public byte[] serializeAgent(Agent agent)
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
            //System.out.println("serializing agent with id: " + serializedAgentIdentifier);
            //System.out.println("serialized agent's index is: " + agent.getIndex()[0] + " " + agent.getIndex()[1]);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(agent);
            objectOutputStream.flush();
            objectOutputStream.close();
            byte[] serializedAgent = byteArrayOutputStream.toByteArray();
            //System.out.println("returning id: " + serializedAgentIdentifier);
            return serializedAgent;
        }
        catch (java.io.IOException ex)
        {
            System.out.println("IOException at serializeAgent" + " : " + ex.toString());
            return null;
        }

    }

    /**
     * Deserialize an Agent
     * @param serializedAgent An array of bytes representing the serialized form of the Agent
     * @return The Agent, deserialized from the byte array
     */
    public Agent deserializeAgent(byte[] serializedAgent)
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
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedAgent);
            ObjectInputStream objectInputStream= new ObjectInputStream(byteArrayInputStream);
            Agent deserializedAgent = (Agent) objectInputStream.readObject();
            objectInputStream.close();
            return deserializedAgent;
        }
        catch(java.io.IOException ex)
        {
            MASS.getLogger().error( "IOException at deserializeAgent", ex );
            return null;
        }
        catch(ClassNotFoundException c)
        {
            MASS.getLogger().error( "ClassNotFoundException at deserializeAgent", c );
            return null;
        }


    }

    /**
     * Get the classes registered with this serializer
     * @return The classes that this serializer will be able to serialize/deserialize
     */
    @SuppressWarnings("rawtypes")
	protected Class[] getRegisteredClasses()
    {
        return this.classes;
    }

    /**
     * Get the maximum number of Agents that this serializer will support 
     * @return The maximum number of supported Agents
     */
    protected int getMaxNumberOfAgents() { 
    	return this.maxNumberOfAgents;
    }

    /**
     * Set the classes that this serializer will support
     * @param classes The classes to support
     */
    @SuppressWarnings("rawtypes")
	protected void setRegisteredClasses(Class[] classes) {
        this.classes = classes;
    }

    /**
     * Set the maximum number of Agents to support
     * @param maxNumberOfAgents The maximum number of supported Agents
     */
    protected void setMaxNumberOfAgents(int maxNumberOfAgents) { 
    	
    	if (maxNumberOfAgents > 0) this.maxNumberOfAgents = maxNumberOfAgents;
    	
    }

}
