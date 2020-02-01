package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Stream;

public class GraphPlaces extends Places implements Graph {
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
                       GraphInitAlgorithm init_algorithm) {
        super(handle, className, new Object[] { filename, format, init_algorithm });

        if (init_algorithm != GraphInitAlgorithm.FULL_LIST || init_algorithm != GraphInitAlgorithm.PARTITIONED_LIST) {
            init_algorithm = GraphInitAlgorithm.FULL_LIST;
        }

        this.init_algorithm = init_algorithm;
        this.filename = filename;
        this.input_format = format;
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

    /**
     * Graph interface implementation
     */
    @Override
    public GraphModel getGraph() {
        GraphModel graph = new GraphModel();

        if (!(getPlaces()[0] instanceof VertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not VertexPlaces.");

            return graph;
        }

        for (Place place : getPlaces()) {
            VertexPlace vPlace = (VertexPlace) place;

            graph.addVertex(vPlace.getIndex()[0], vPlace.neighbors);
        }

        return graph;
    }

    @Override
    public boolean addEdge(int vertexId, int neighborId, double weight) {
        boolean added = false;

        Log4J2Logger logger = MASSBase.getLogger();

        if (vertexId < 0 || vertexId < getPlaces().length) {
            VertexPlace vertexPlace = (VertexPlace) getPlaces()[vertexId];

            try {
                vertexPlace.addNeighbor(neighborId);

                added = true;
            } catch (IllegalArgumentException iae) {
                logger.warning("Exception encountered addingEdge: " + iae);
            }
        } else {
            logger.warning("Cannot add edge: source is out of range(" + vertexId + ")");
        }

        return added;
    }

    @Override
    public boolean removeEdge(int vertexId, int neighborId) {
        return false;
    }

    @Override
    public int addVertex() {
        return addPlace();
    }

    @Override
    public boolean removeVertex(int vertexId) {
        return false;
    }
}
