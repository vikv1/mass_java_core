package edu.uw.bothell.css.dsl.MASS;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

public class GraphPlaces extends Places {
    private final GraphInitAlgorithm init_algorithm;
    private final String filename;
    private final GraphInputFormat input_format;

    /**
     * Instantiate a PlacesBase for this node
     *
     * @param handle         The Handle ID identifying this PlacesBase
     * @param className      The class that represents a Place
     * @param boundary_width The width of the boundary between nodes, used to calculate shadow space
     * @param argument       The argument to supply to the Place during initialization
     * @param size           Matrix dimensions, as an array of integers representing dimension sizes
     */
    private GraphPlaces(int handle, String className, int boundary_width, Object argument, int[] size) {
        super(handle, className, boundary_width, argument, size);

        init_algorithm = GraphInitAlgorithm.FULL_LIST;
        filename = "graph_n.txt";
        input_format = GraphInputFormat.CSV;
    }

    public GraphPlaces(int handle, String className, String filename, GraphInputFormat format,
                       GraphInitAlgorithm init_algorithm, int nVertices, Object argument) {
        super(handle, className, argument);

        if (init_algorithm != GraphInitAlgorithm.FULL_LIST || init_algorithm != GraphInitAlgorithm.PARTITIONED_LIST) {
            init_algorithm = GraphInitAlgorithm.FULL_LIST;
        }

        this.init_algorithm = init_algorithm;
        this.filename = filename;
        this.input_format = format;
    }
    /**
     * Calls the method specified with functionId of all array elements. Done
     * in parallel among multi-processes/threads.
     * @param functionId The ID of the function to call
     */
    public void callAll( int functionId ) {
    }

    /**
     * Calls the method specified with functionId of all array elements as
     * passing an argument to the method. Done in parallel among multi-
     * processes/threads.
     * @param functionId The ID of the function to call
     * @param argument An argument to supply to the function being called in each Place
     */
    public void callAll( int functionId, Object argument ) {
    }

    /**
     * Calls the method specified with functionId of all array elements as
     * passing arguments[i] to element[i]’s method, and receives a return
     * value from it into (void *)[i] whose element’s size is return_size. Done
     * in parallel among multi-processes/threads. In case of a multi-
     * dimensional array, "i" is considered as the index when the array is
     * flattened to a single dimension.
     * @param functionId The ID of the function to call
     * @param argument An argument to supply to the function being called in each Place
     * @return An Object (actually, an Object[]) with each element set to the return value
     * 			supplied by each Place in the cluster
     */
    public Object[] callAll( int functionId, Object argument[] ) {
        return null;
    }

    /**
     * Send the place_initialize_graph message to all nodes
     * @param argument
     * @param boundaryWidth - unused
     */
    @Override
    protected void init_master(Object argument, int boundaryWidth) {
        MASSBase.getLogger().debug("GraphPlaces - init_master");

        Vector<String> hosts = getHosts();

        Message message = new Message(Message.ACTION_TYPE.PLACES_INITIALIZE_GRAPH, getSize(),
                getHandle(), getClassName(),
                argument, boundaryWidth, hosts );

        init_master_base(message);
    }


    @Override
    protected void init_all(Object argument) {
        Object [] arguments = (Object[]) argument;

        String [] graphArguments = (String [])Arrays.copyOfRange(arguments, 0, 2);
        Object [] initArguments = (Object [])Arrays.copyOfRange(arguments, 2, arguments.length);

        init_all_graph(graphArguments, initArguments);
    }
}
