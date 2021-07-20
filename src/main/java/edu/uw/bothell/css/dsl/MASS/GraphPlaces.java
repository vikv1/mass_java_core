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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.VertexMetaValues;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;

public class GraphPlaces extends Places implements Graph {
    // DEFAULT_EDGE_WEIGHT is the default edge weight applied when an
    // edge is created without providing a weight.
    public static final double DEFAULT_EDGE_WEIGHT = 1.0;

    // nextVertexID tracks the vertex ID associated vertices added to the graph.
    // It is kept up to date such that it's current value represents the ID
    // to be assigned tot he next Vertex.
    protected int nextVertexID = 0;

    // idQueue is used to store the IDs of vertices that have been removed 
    // so that they may be reused for newly added nodes.
    // Note: If this isn't accessed concurrently we can replace this with 
    // a linked list. Similarly with the VertexPlace vectors.
    protected Queue<Integer> idQueue = new ConcurrentLinkedQueue<Integer>();

    // objectFactory is used to generate objects of the places class provided
    // when instantiating GraphPlaces.
    protected ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

    // places is used to stored VertexPlaces added after instantiating
    // a GraphPlaces.
    protected Vector<VertexPlace> places = new Vector<VertexPlace>();

    /**
     * Constructs a GraphPlaces object populated with data from the 
     * "graph_n.txt" CSV text file.
     * 
     * @param handle The Handle ID identifying this GraphPlaces.
     * @param className The class that represents a VertexPlace.
     * @param graphArgs 
     * @param initArgs 
     * @deprecated 
     */
    public GraphPlaces(int handle, String className, String[] graphArgs, Object[] initArgs) {
        super(handle, className);
    }

    // Empty constructor to all for remote instantiation and serialization.
    // This should be reserved for the system and not called by users
    // directly.
    public GraphPlaces() {
        super();
    }

    /**
     * Constructs a GraphPlaces object populated with data contained in
     * the provided filename.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param filename The filename of the file with which to extract graph data.
     * @param format The format of the file (e.g., CSV, HIPPIE, etc..).
     * @param init_algorithm The initialization algorithm used 
     * (e.g., FULL_LIST or PARTITIONED_LIST).
     */
    public GraphPlaces(int handle, String className, String filename, GraphInputFormat format,
                       GraphInitAlgorithm init_algorithm) {
        super(handle, className, new Object[] { filename, format, init_algorithm });

        if (init_algorithm != GraphInitAlgorithm.FULL_LIST || init_algorithm != GraphInitAlgorithm.PARTITIONED_LIST) {
            init_algorithm = GraphInitAlgorithm.FULL_LIST;
        }
    }

    /**
     * Constructs a GraphPlaces object populated with data contained in
     * the provided filename.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param filename The filename of the file with which to extract graph data.
     * @param format The format of the file (e.g., CSV, HIPPIE, etc..).
     * @param init_algorithm The initialization algorithm used 
     * (e.g., FULL_LIST or PARTITIONED_LIST).
     * @param nVertices This is unused.
     * @param argument The arguments to be supplied to the VertexPlace during
     * initialization.
     */
    public GraphPlaces(int handle, String className, String filename, GraphInputFormat format,
                       GraphInitAlgorithm init_algorithm, int nVertices, Object argument) {
        super(handle, className, argument);

        if (init_algorithm != GraphInitAlgorithm.FULL_LIST || init_algorithm != GraphInitAlgorithm.PARTITIONED_LIST) {
            init_algorithm = GraphInitAlgorithm.FULL_LIST;
        }
    }

    /**
     * Constructs an empty GraphPlaces object.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param size The number of vertices in the graph.
     */
    public GraphPlaces(int handle, String className, int size) {
        super(handle, className, size, new int[] { size });
    }
    
    /**
     * Constructs an empty GraphPlaces object.
     * 
     * @param handle The Handle ID identifying this GraphPlaces instance.
     * @param className The class that represents a VertexPlace.
     * @param size The number of vertices in the graph.
     * @param _remote_node This is unused.
     */
    public GraphPlaces(int handle, String className, int size, boolean _remote_node) {
        super(handle, className);
        
        init_all_graph_blank(size);
    }

    /**
     * Constructs a basic GraphPlaces object with no pre-allocated space.
     * @param handle
     * @param className
     */
    public GraphPlaces(int handle, String className) {
        super(handle, className);

        // Only call init_master if we're actually the master node.
        int myPid = MASS.getMyPid();

        if (myPid == 0) {
            init_graph_master();
        }
    }

    /**
     * Instantiates a new GraphPlaces instance, initalized with data from the 
     * provided DSL Graph file.
     * 
     * @param handle The handle associated with this GraphPlaces Instance.
     * @param className The class name of the Vertex class.
     * @param filePath The path to the Distributed Systems Lab (DSL) Graph File.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public GraphPlaces(int handle, String className, String filePath) throws IOException,FileNotFoundException {
        super(handle, className);

        int myPid = MASS.getMyPid();

        if (myPid == 0) {
            init_graph_master();
        }

        loadDSLFile(filePath);
    }

    /**
     * GraphPlaces constructor for initializing graph places with a graph file.
     * 
     * @param handle The places handle.
     * @param className The name of the VertexPlace class.
     * @param filePath Path to the graph data file.
     * @param fileType The file type of the graph data.
     */
    public GraphPlaces(int handle, String className, String filePath, GraphInputFormat fileType) throws IOException,FileNotFoundException {
        // super(handle, className);
        // Need to figure out why the above doesn't set places remotely but this does...
        super(handle, className, 1, new int[] { 1 });
    }

    // reinitialize reinitializes the GraphPlaces object by setting the 
    // local index trackers and the placesVector to 0.
    @Override
    protected void reinitialize() {
        super.reinitialize();

        nextVertexID = 0;
        places = new Vector<VertexPlace>();
    }

    // reinitializeGraph calls reinitialize locally and sends MAINTENANCE_REINITIALIZE
    // messages to each of the worker nodes to reinitialize them as well.
    private void reinitializeGraph() {
        // TODO: This feels like something that could be handled by an internal callAll or something similarly
        // utilizing the infrastructure the code already has
        reinitialize();

        //Send reinitialize message to all remote nodes
        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_REINITIALIZE, getHandle(), null);

        // This needs to remove neighbors anyways so just send to everyone else
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(message));

        MASSBase.reinitializeMap();

        // Early clear is inconsequential. We just need to make sure we don't move forward before all nodes are done
        MASS.barrierAllSlaves();
    }

    /**
     * init_master initializes GraphPlaces on the master and sends messages
     * to all of the worker nodes to do the same.
     * 
     * @param argument The arguments to be supplied to the VertexPlace during
     * initialization.
     */
    @Override
    protected void init_master(Object argument, int boundaryWidth) {
        MASSBase.getLogger().debug("GraphPlaces - init_master");

        Vector<String> hosts = getHosts();

        Message message = new Message(Message.ACTION_TYPE.PLACES_INITIALIZE_GRAPH, getSize(),
                getHandle(), getClassName(),
                argument, 0, hosts );

        init_master_base(message);
    }

    // InitArgs are initialization args to be passed to instances of 
    // GraphPlaces that are being instantiated on remote nodes.
    public static class InitArgs implements Serializable {
        public int handle;
        public String className;
        public String vertexClassName;
        public Object[] initArgs;

        public InitArgs(int handle, String vertexClassName, String className, Object... initArgs) {
            this.handle = handle;
            this.vertexClassName = vertexClassName;
            this.className = className;
            this.initArgs = initArgs;
        }
    }

    private void init_graph_master() {
        MASSBase.getLogger().debug("GraphPlaces - init_graph_master");

        Vector<String> hosts = getHosts();

        InitArgs initArgs = new InitArgs(this.getHandle(), this.getClassName(), this.getClass().getName());

        Message message = new Message(Message.ACTION_TYPE.PLACES_INITIALIZE_GRAPH, getSize(),
                getHandle(), getClassName(),
                initArgs, 0, hosts );

        init_master_base(message);
    }

    // init_all initializes GraphPlaces using the provided argument.
    @Override
    protected void init_all(Object argument) {
        if (argument instanceof Integer) {
            init_all_graph_blank((Integer) argument);
        } else {
            Object[] arguments = (Object[]) argument;

            // TODO: This is failing in kotlin
            // String[] graphArguments = (String[]) Arrays.copyOfRange(arguments, 0, 2);
            // Object[] initArguments = (Object[]) Arrays.copyOfRange(arguments, 2, arguments.length);

            String [] graphArguments = new String[2];
            Object [] initArguments = null;

            graphArguments[0] = arguments[0].toString();
            graphArguments[1] = arguments[1].toString();

            if (arguments.length > 2) {
                initArguments = new Object[arguments.length - 2];

                for (int i = 0; i < initArguments.length; i++) {
                    initArguments[i] = arguments[i + 2];
                }
            }

            init_all_graph(graphArguments, initArguments);
        }
    }

    /* Graph Interfeace Implementation ***************************************/
    @Override
    public GraphModel getGraph() {
        return getGraph(true);
    }

    /**
     * setGraph - replace in-memory graph with the supplied model
     */
    @Override
    public void setGraph(final GraphModel newGraph) {
        // Re-initialize graph across cluster
        reinitializeGraph();

        // apply the new model
        // TODO: This would be a good area for improvements. As it stands I will just use the new graph maintenance
        // functions to apply the model

        // Add all the vertices first
        newGraph.getVertices().forEach(vertex -> addVertex(vertex.id));

        // FIXME (#153): Missing weights
        newGraph.getVertices()
                .forEach(vertex -> vertex.neighbors
                        .forEach(neighbor -> addEdge(vertex.id, neighbor, DEFAULT_EDGE_WEIGHT)));
    }

    /**
     * merge merges the remoteGraphs into the source graph model.
     * 
     * @param source The source graph to merge nodes into.
     * @param remoteGraphs The remote graph to merge nodes from.
     */
    public void merge(GraphModel source, GraphModel remoteGraphs) {
        source.getVertices().addAll(remoteGraphs.getVertices());
    }

    
    /**
     * getGraph returns a GraphModel copy of the distributed graph.
     */
    @Override
    public GraphModel getGraph(boolean all) {
        GraphModel graph = new GraphModel();

        if (getPlaces() != null && getPlaces().length > 0 && !(getPlaces()[0] instanceof VertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not VertexPlaces.");

            return graph;
        }

        // Places on master node
        if (getPlaces() != null) {
            for (Place place : Arrays.stream(getPlaces()).filter(p -> p != null).collect(Collectors.toList())) {
                VertexPlace vPlace = (VertexPlace) place;

                Object attribute = MASSBase.distributed_map.reverseLookup(place.getIndex()[0]);

                if (attribute == null) {
                    attribute = place.getIndex()[0];
                }
                
                // FIXME (#154): Missing weights
                graph.addVertex(attribute, vPlace.neighbors);
            }
        }

        for (VertexPlace place : this.places) {
            Object attribute = MASSBase.distributed_map.reverseLookup(place.getIndex()[0]);

            graph.addVertex(attribute, place.neighbors);
        }

        if (all) {
            merge(graph, getRemoteGraphs());
        }

        return graph;
    }

    // getRemoteGraphs sends messages to all worker nodes requesting the graph
    // models containing their respective vertices.
    private GraphModel getRemoteGraphs() {
        GraphModel graph = new GraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES, getHandle(), null));

            Message m = node.receiveMessage();

            if (m.getAction() != Message.ACTION_TYPE.MAINTENANCE_GET_PLACES_RESPONSE) {
                throw new RuntimeException("Received incorrect response from node");
            } else {
                GraphModel model = (GraphModel) m.getArgument();

                merge(graph, model);
            }
        }

        return graph;
    }
    /*************************************************************************/

    /**
     * addVertex creates a new vertex with the provided vertexId.
     * 
     * @param vertexId the ID of the vertex.
     * 
     * @return The vertexID if the vertex was successfully added, -1 otherwise.
     */
    @Override
    public int addVertex(Object vertexId) {
        if (MASS.distributed_map.containsKey(vertexId)) {
            MASS.getLogger().debug("the provided vertexId already exists");
            return -1;
        }

        int vertId = this.addVertex();
        MASS.distributed_map.put(vertexId, vertId);
        
        return vertId;
    }

    /**
     * addVertex creates a new vertex with the provided vertexId and
     * vertex init params.
     * 
     * @param vertexId the ID of the vertex.
     * @param vertexInitParam The parameters to pass to the vertex constructor.
     * 
     * @return The vertexID if the vertex was successfully added, -1 otherwise.
     */
    @Override
    public int addVertex(Object vertexId, Object vertexInitParam) {
        if (MASS.distributed_map.containsKey(vertexId)) {
            MASS.getLogger().debug("the provided vertexId already exists");
            return -1;
        }

        int vertId = this.addVertexWithParams(vertexInitParam);
        MASS.distributed_map.put(vertexId, vertId);
        
        return vertId;
    }

    /**
     * addPlaceLocally creates a new vertex.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please migrate to using addVertex.
     * 
     * @param vertexId the ID of the vertex.
     * @param vertexInitParam the init parameters to be passed to the 
     * vertex constructor.
     * 
     * @return The vertexID if the vertex was successfully added, -1 otherwise.
     */
    public int addPlaceLocally(Object vertexId, Object vertexInitParam) {
        MASS.getLogger().warning("addPlaceLocally is deprecated and will be removed " +
        "in a future release. Please migrate to using addVertex.");

        return this.addVertex(vertexId, vertexInitParam);
    }

    /**
     * addVertex adds an empty vertex to the graph.
     * 
     * @return The ID of the vertex if successful, -1 otherwise.
     */
    public int addVertex() {
        return addVertexWithParams(null);
    }

    /**
     * addVertexWithParams constructs a new vertex with the provided init params and 
     * adds it to the graph.
     * @param initParams The parameters to pass to the vertex constructor.
     * 
     * @return The vertexID if the vertex was successfully added, -1 otherwise.
     */
    public int addVertexWithParams(Object initParams) {
        int vertexID;
        boolean fromIDQueue = false;

        // Get a new vertexID
        try {
            vertexID = idQueue.remove();
            fromIDQueue = true;

        } catch (NoSuchElementException e) {
            vertexID = nextVertexID;
        }

        boolean success = addVertexOnNode(
            getOwnerID(vertexID),
            vertexID,
            initParams
        );

        // If unsuccessful
        if (!success) {
            // If we got the ID from our queue, re-enqueue it.
            if (fromIDQueue) {
                idQueue.add(vertexID);
            }
            
            // return -1 to indicate as such.
            return -1; 
        }

        // Otherwise, increment if needed and return the vertexID.
        if (!fromIDQueue) { nextVertexID++; }

        return vertexID;
    }

    /**
     * addVertexOnNode creates a new VertexPlace at the node with the provided
     * nodeID and instantiates it with the provided vertex parameters.
     * 
     * @param nodeID The node ID of the node with which to add the vertex.
     * @param vertexInitParams The init paramters for the VertexPlace.
     * @param vertexID The ID of the vertex.
     * @return true if successful, false otherwise.
     */
    public boolean addVertexOnNode(int nodeID, int vertexID, Object vertexInitParams) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add it.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteVertex(nodeID, vertexID, vertexInitParams);
        }
        // Get local index and size of places array.
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist,
        // return false.
        if (localIndex > localSize) { return false; }

        // Create new VertexPlace
        VertexPlace vertexPlace;
        try {
            vertexPlace = objectFactory.getInstance(getClassName(), vertexInitParams);
        } catch (Exception e) {
            MASS.getLogger().error("expection trying to instantiate a new vertex: ", e);
            return false;
        }
        // Set it at the appropriate index if this vertex is to occupy 
        // preallocated space or reclaiming space from a previously removed
        // vertex.
        if (localIndex < localSize) {
            places.set(localIndex, vertexPlace);
            return true;
        }

        // Otherwise, add it to the back.
        places.add(vertexPlace);
        return true;
    }

    /**
     * addRemoteVertex sends a message to the node with the provided nodeID to
     * add a vertex with the provided vertexID and init parameters.
     */
    private boolean addRemoteVertex(int nodeID, int vertexID, Object vertexInitParams) {
        // Get the remote node
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to add vertex.
        Object[] msgContent = new Object[]{vertexID, vertexInitParams};
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_ADD_VERTEX,
            getHandle(),
            msgContent
        );

        // Send message and wait for reply
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // getAgentPopulation is currently overloaded to return the success/failure
        // of adding the vertex to the remote node.
        if (replyMsg.getAgentPopulation() < 0) {
            MASS.getLogger().debug("remote node with pid {} failed to add vertex", nodeID);
            return false;
        }

        return true;
    }

    /**
     * removeVertex removes the vertex with the provided vertexId.
     * 
     * @param vertexId the ID of the vertex.
     * 
     * @return true if the vertex was successfully removed, false otherwise.
     */
    @Override
    public boolean removeVertex(Object vertexId) {
        int sourceId = MASS.distributed_map.getOrDefault(vertexId, -1);
        if (sourceId == -1) {
            MASS.getLogger().debug("the provided vertex id doesn't exist");
            return false;
        }

        // If we fail to remove the vertex, return false and do not remove it
        // from the distributed map.
        if (!this.removeVertex(sourceId)) {
            return false;
        }

        // remove the key from the distributed map.
        MASS.distributed_map.remove(vertexId);

        return true;
    }

    /**
     * removeVertexLocally removes the vertex with the provided vertexId from 
     * the calling node if it exists.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please migrate to using removeVertex.
     * 
     * @param vertexId the ID of the vertex.
     * 
     * @return true if the vertex was successfully removed, false otherwise.
     */
    public void removeVertexLocally(Object vertexId) {
        MASS.getLogger().warning("removeVertexLocally is deprecated and will be removed " +
        "in a future release. Please migrate to using removeVertex.");
        
        int sourceId = MASSBase.distributed_map.getOrDefault(vertexId, -1);
        if (sourceId == -1) {
            MASS.getLogger().debug("vertex ID doesn't exist");
            return;
        }

        // If the vertex cannot be removed locally, fail under the assumption that
        // it MUST be.
        if (getOwnerID(sourceId) != MASS.getMyPid()) {
            MASS.getLogger().debug("the vertex ID is not owned by the local node");
            return;
        }

        if (!this.removeVertex(vertexId)) {
            MASS.getLogger().debug("unable to remove the vertex");
        }
    }

    /**
     * removeVertex removes the vertex associated with the provided vertexID
     * from the graph.
     * 
     * Note that if a user attempts to remove a vertex ID that has been
     * queued for recycling (i.e., already removed) this function will still
     * return true. This is to avoid a linear traversal of IDs queued for
     * recycling just to check if a vertex that's going to be removed exists.
     * 
     * @param vertexID The ID of the vertex to be removed.
     * @return true if successful, false otherwise.
     */
    public boolean removeVertex(int vertexID) {
        // If the vertex to be deleted doesn't exist, return false.
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 

            return false; 
        }

        // If we're unable to remove the vertex, return false.
        if (!removeVertexOnNode(getOwnerID(vertexID),vertexID)) { 
            return false; 
        }

        // Create a thread to remove the neighbor from all remote
        // vertices. If creating the thread has too much overhead we 
        // can implement a thread pool for use by the class as a whole.
        Thread t = new Thread(() -> removeNeighborFromRemoteVertices(vertexID));
        t.start();

        // While that work is being done, delete it locally.
        removeNeighborFromLocalVertices(vertexID);

        // Wait for thread to complete
        try { t.join(); } catch (Exception e) {
            MASS.getLogger().error("error occurred while removing a vertex: " + e);
        }

        return true;
    }

    /**
     * removeVertexOnNode removes the vertex associated with the provided
     * vertexID from the node associated with the provided node ID. This
     * method is a MASS internal method. DO NOT use it to remove vertices. 
     * It is public for use by the MASS messaging system. It does not 
     * remove the target vertex from other vertex neighbor lists.
     * 
     * @param nodeID The ID of the node with which to remove this vertex.
     * @param vertexID The ID of the vertex to be removed.
     * 
     * @return true if successful, false otherwise.
     */
    public boolean removeVertexOnNode(int nodeID, int vertexID) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to remove it.
        if (nodeID != MASS.getMyPid()) {
            boolean success = removeRemoteVertex(nodeID, vertexID);
            // If successful, enqueue ID for use with next added vertex.
            if (success) { recycleID(vertexID); }

            return success;
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        // Set vertex as null to indicate it's unused and add it to the 
        // idQueue.
        places.set(localIndex, null);
        recycleID(vertexID);

        return true;
    }

    // removeRemoteVertex sends a message to the node with the provided
    // nodeID to remove a vertex with the provided vertexID.
    private boolean removeRemoteVertex(int nodeID, int vertexID) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to remove the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_REMOVE_VERTEX,
            getHandle(),
            Integer.valueOf(vertexID)
        );

        // Send message and wait for reply.
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // Message system currently only returns an ACK if successful
        // so if we do not receive one, assume failure.
        if (replyMsg.getAction() != Message.ACTION_TYPE.ACK) {
            return false;
        }

        return true;
    }

    /**
     * removeNeighborFromLocalVertices removes the provided neighbor
     * vertex from all local VertexPlaces.
     * 
     * @param neighborID The ID of the neighbor vertex to have removed.
     */
    public void removeNeighborFromLocalVertices(int neighborID) {
        for (VertexPlace place : this.places) {
            // We do not actually remove vertices, just mark them unused
            // so it's possible for them to be null.
            if (place == null) { continue; }

            place.removeNeighborSafely(neighborID);
        }
    }

    // removeNeighborFromRemoteVertices sends a message to all remote
    // nodes to remove the provide neighbor vertex from all vertex 
    // neighbors.
    private void removeNeighborFromRemoteVertices(int neighborID) {
        // Send each message
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_REMOVE_NEIGHBOR,
            getHandle(),
            Integer.valueOf(neighborID)
        )));

        // Wait for each reply. This isn't fused with the above so that
        // each node can work concurrently while we wait for ACKS
        MASS.getRemoteNodes().forEach(node -> {
            Message msg = node.receiveMessage();
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASS.getLogger().debug("failed to remove neighbor from remote node: " + node);
            }
        });
    }

    /**
     * Get the VertexPlace associated with the provided
     * vertex ID.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please migrate to using getVertex.
     * 
     * @param vertexID The ID of the vertex to retrieve.
     * @return The VertexPlace associated with the provided vertex ID.
     */
    public VertexPlace getVertexPlace(int vertexId) {
        MASS.getLogger().warning("getVertexPlace is deprecated and will be removed " +
        "in a future release. Please migrate to using getVertex.");

        return this.getVertex(vertexId);
    }

    /**
     * getPlacesVector returns a copy of the places vector. It's returned 
     * wrapped in another vector for legacy reasons.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please cease from using it.
     */
    public Vector<Vector<VertexPlace>> getPlacesVector() {
        MASS.getLogger().warning("getPlacesVector is deprecated and will be removed " +
        "in a future release. Please cease from using it.");
        Vector<Vector<VertexPlace>> placesVector = new Vector<Vector<VertexPlace>>();
        Vector<VertexPlace> placesClone = new Vector<VertexPlace>(this.places.size());
        placesClone.addAll(this.places);
        placesVector.add(placesClone);

        return placesVector;
    }

    /**
     * @return The GraphPlaces places vector containing the VertexPlaces
     * for the calling node.
     */
    public Vector<VertexPlace> getGraphPlaces() {
        return this.places;
    }

    public VertexPlace getVertex(Object vertex) {
        int vertexID = MASS.distributed_map.getOrDefault(vertex, -1);
        if (vertexID == -1) { return null; }

        return getVertex(vertexID);
    }

    /**
     * getVertex returns the VertexPlace associated with the provided
     * vertex ID.
     * 
     * @param vertexID The ID of the vertex to retrieve.
     * @return The VertexPlace associated with the provided vertex ID.
     */
    public VertexPlace getVertex(int vertexID) {
        // If the vertex doesn't exist return null.
        if (MASS.getMyPid() == 0 && vertexID >= nextVertexID) {
            return null; 
        }
        
        return getVertexFromNode(
            getOwnerID(vertexID),
            vertexID
        );
    }

    /**
     * getVertexFromNode retrieves the vertex associated with the provided
     * vertexID from the node associated with the provided node ID.
     * @param nodeID The ID of the node with which to remove this vertex.
     * @param vertexID The ID of the vertex to be removed.
     * @return the requested vertex place.
     */
    public VertexPlace getVertexFromNode(int nodeID, int vertexID) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { 
            return null; 
        };

        // If another node owns this vertex, send it a message to
        // retrieve it.
        if (nodeID != MASS.getMyPid()) {
            return getRemoteVertex(nodeID, vertexID);
        }
        
        // Get local index and size of places array.
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return null.
        if (localIndex >= localSize) { 
            return null; 
        }

        return places.get(localIndex);
    }

    private VertexPlace getRemoteVertex(int nodeID, int vertexID) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return null.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return null;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to get the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_GET_VERTEX,
            getHandle(),
            Integer.valueOf(vertexID)
        );

        // Send message and wait for reply.
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        return (VertexPlace)replyMsg.getArgument();
    }

    /**
     * addEdge adds an edge between the provided vertexId and neighborId.
     * The created edge is given a weight of 1.0.
     * 
     * @param vertexId the vertex ID of the source vertex.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    @Override
    public boolean addEdge(Object vertexId, Object neighborId) {
        // Weight is set as 1.0 to bring it in line with what the 
        // setGraph method does.
        return this.addEdge(vertexId, neighborId, DEFAULT_EDGE_WEIGHT);
    }

    /**
     * addEdge adds an edge between the provided vertexId and neighborId and
     * assigns it the provided weigth value.
     * 
     * @param vertexId the vertex ID of the source vertex.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * @param weight the weight of the edge.
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    @Override
    public boolean addEdge(Object vertexId, Object neighborId, double weight) {
        int sourceId = MASSBase.distributed_map.getOrDefault(vertexId, -1);
        int destinationId = MASSBase.distributed_map.getOrDefault(neighborId, -1);

        if (sourceId == -1 || destinationId == -1) {
            MASS.getLogger().debug("vertex or neighbor ID doesn't exist");
            return false;
        }

        return this.addEdge(sourceId, destinationId, weight);
    }

    /**
     * addEdgeLocally adds an edge between the provided local vertexId and 
     * the provided neighborId, and assigns the provided weigth as the edge 
     * weigth.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please migrate to using addEdge.
     * 
     * @param vertexId the vertex ID of the source vertex. Must be local to 
     * the calling node.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * @param weight the weight of the edge.
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    public boolean addEdgeLocally(Object vertexId, Object neighborId, double weight) {
        MASS.getLogger().warning("addEdgeLocally is deprecated and will be removed " +
        "in a future release. Please migrate to using addEdge.");

        int sourceId = MASSBase.distributed_map.getOrDefault(vertexId, -1);
        int destinationId = MASSBase.distributed_map.getOrDefault(neighborId, -1);

        if (sourceId == -1 || destinationId == -1) {
            MASS.getLogger().debug("vertex or neighbor ID doesn't exist");
            return false;
        }

        // If the edge cannot be added locally, fail under the assumption that
        // it MUST be.
        if (getOwnerID(sourceId) != MASSBase.getMyPid()) {
            MASS.getLogger().debug("the vertex ID is not owned by the local node");
            return false;
        }

        return this.addEdge(sourceId, destinationId, weight);
    }

    /**
     * addEdge adds an edge between the provided vertex and neighbor
     * IDs using a default weight of 1.0.
     * 
     * @param vertexID The ID of source vertex.
     * @param neighborID The ID of the destination vertex (its "neighbor").
     * 
     * @return true if the edge was successfully added, false otherwise.
     */
    public boolean addEdge(int vertexID, int neighborID) {
        return addEdge(vertexID, neighborID, 1.0);
    }

    /**
     * addEdge adds an edge between the provided vertex and neighbor
     * IDs with a the provided edge weight.
     * 
     * @param vertexID The ID of the source vertex.
     * @param neighborID The ID of the destination vertex.
     * @param weight The weight of the edge.
     *
     * @return true if the edge was successfully added, false otherwise.
     */
    public boolean addEdge(int vertexID, int neighborID, double weight) {
        // Check vertexId exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
            
            return false; 
        }

        // Check neighborId exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
                
            return false; 
        }

        return addEdgeOnNode(
            getOwnerID(vertexID),
            vertexID,
            neighborID,
            weight
        );
    }

    /**
     * addEdgeOnNode attempts to add an edge between the provided vertex
     * and neighbor IDs on the node associated with the provided node ID.
     * If the node associated with the provided node ID does not own the
     * source vertex, a message is created and sent to the remote node
     * that does own the source vertex to add the edge.
     * 
     * @param nodeID The ID of the node that owns the source vertex.
     * @param vertexID The source vertex ID.
     * @param neighborID The destination vertex ID (its neighbor).
     * @param weight The weight of the edge.
     * 
     * @return true if the edge was successfully created, false otherwise.
     */
    public boolean addEdgeOnNode(int nodeID, int vertexID, int neighborID, double weight) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add the edge.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteEdge(nodeID, vertexID, neighborID, weight);
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        VertexPlace vertex = places.get(localIndex);
        vertex.addNeighbor(neighborID, weight);
        places.set(localIndex, vertex);

        return true;
    }

    /**
     * addRemoteEdge sends a MASS message to the node associated with the provided node ID
     * to add an edge between the vertexID and neighborID with the provided edge
     * weight.
     */
    private boolean addRemoteEdge(int nodeID, int vertexID, int neighborID, double weight) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to remove the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_ADD_EDGE_V2,
            getHandle(),
            new Object[]{vertexID, neighborID, weight }
        );
        
        // Send message and wait for reply
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();

        // Message systems currently only returns ACK if successful
        // so if we do not receive one, assume failure.
        if (replyMsg.getAction() != Message.ACTION_TYPE.ACK) {
            return false;
        }

        return true;
    }

    /**
     * removeEdge removes the edge between the provided vertexId
     * and neighborId.
     * 
     * @param vertexId the vertex ID of the source vertex.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    @Override
    public boolean removeEdge(Object vertexId, Object neighborId) {
        int sourceId = MASSBase.distributed_map.getOrDefault(vertexId, -1);
        int destinationId = MASSBase.distributed_map.getOrDefault(neighborId, -1);

        if (sourceId == -1 || destinationId == -1) {
            MASS.getLogger().debug("vertex or neighbor ID doesn't exist");
            return false;
        }

        return this.removeEdge(sourceId, destinationId);
    }

    /**
     * removeEdgeLocally removes an edge between the provided local vertexId
     * and the provided neighborId.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please migrate to using removeEdge.
     * 
     * @param vertexId the vertex ID of the source vertex. Must be local to 
     * the calling node.
     * @param neighborId the vertex ID of the destination vertex (its neighbor).
     * 
     * @return true if the edge is added successfully, false otherwise.
     */
    public boolean removeEdgeLocally(Object vertexId, Object neighborId) {
        MASS.getLogger().warning("removeEdgeLocally is deprecated and will be removed " +
        "in a future release. Please migrate to using removeEdge.");
        int sourceId = MASSBase.distributed_map.getOrDefault(vertexId, -1);
        int destinationId = MASSBase.distributed_map.getOrDefault(neighborId, -1);

        if (sourceId == -1 || destinationId == -1) {
            MASS.getLogger().debug("vertex or neighbor ID doesn't exist");
            return false;
        }

        // If the edge cannot be removed locally, fail under the assumption that
        // it MUST be.
        if (getOwnerID(sourceId) != MASSBase.getMyPid()) {
            MASS.getLogger().debug("the vertex ID is not owned by the local node");
            return false;
        }

        return this.removeEdge(sourceId, destinationId);
    }

    /**
     * removeEdge removes the edge between the provided vertex and neighbor
     * IDs.
     * 
     * @param vertexID The source vertex ID.
     * @param neighborID The destination vertex ID.
     * 
     * @return true if the edge was successfully removed, false otherwise.
     */
    public boolean removeEdge(int vertexID, int neighborID) {
        // Check vertexID exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
            
            return false; 
        }

        // Check neighborID exists
        if (MASS.getMyPid() == 0 && 
            vertexID >= nextVertexID || idQueue.contains(vertexID)) { 
                
            return false; 
        }

        return removeEdgeOnNode(
            getOwnerID(vertexID),
            vertexID,
            neighborID
        );
    }

    /**
     * removeEdgeOnNode removes the edge between the provided vertex and neighbor
     * IDs on the node associated with the provided nodeID. If the node does not
     * own the source vertex a message is created and sent to the remote node that
     * does to remove the edge.
     * 
     * @param nodeID The ID of the node that owns the source vertex.
     * @param vertexID The source vertex ID.
     * @param neighborID The destination vertex ID (its neighbor).
     * 
     * @return true if the edge was successfully removed, false otherwise.
     */
    public boolean removeEdgeOnNode(int nodeID, int vertexID, int neighborID) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to remove the edge.
        if (nodeID != MASS.getMyPid()) {
            return removeRemoteEdge(nodeID, vertexID, neighborID);
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        VertexPlace vertex = places.get(localIndex);
        vertex.removeNeighbor(neighborID);
        places.set(localIndex, vertex);
        
        return true;
    }

    /**
     * removeRemoteEdge sends a MASS message to the node associated with the provided
     * node ID to remove the edge between the provided vertex and neighbor IDs.
     */
    private boolean removeRemoteEdge(int nodeID, int vertexID, int neighborID) {
        // Get the remote node.
        Optional<MNode> optionalNode = MASS.getRemoteNodes().stream().filter(node -> {
            return node.getPid() == nodeID;
        }).findFirst();

        // If the remote node could not be located, return false.
        if (!optionalNode.isPresent()) {
            MASS.getLogger().debug("remote node with pid {} could not be found", nodeID);
            return false;
        }
        MNode remoteNode = optionalNode.get();

        // Create message to ask remote node to remove the vertex.
        Message msg = new Message(
            Message.ACTION_TYPE.MAINTENANCE_REMOVE_EDGE_V2,
            getHandle(),
            new Object[]{ vertexID, neighborID }
        );

        // Send message and wait for reply
        remoteNode.sendMessage(msg);
        Message replyMsg = remoteNode.receiveMessage();
        
        // Message system currently only returns ACK if successful
        // so if we do not recieve one, assume failure.
        if (replyMsg.getAction() != Message.ACTION_TYPE.ACK) {
            return false;
        }

        return true;
    }

    /**
     * @return The number of vertices contained in the graph.
     */
    public int size() {
        // The number of vertex IDs issued - the number queued to be
        // recycled.
        return nextVertexID - idQueue.size();
    }

    /**
     * getExtendedPlacesSize returns the size of the places vector.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please cease from using it.
     */
    public int getExtendedPlacesSize() {
        MASS.getLogger().warning("getExtendedPlacesSize is deprecated and will be removed " +
        "in a future release. Please cease from using it.");

        return this.size();
    }

    /**
     * getOwnerID returns the ID of the node that owns the provided
     * global index.
     * 
     * @param vertexID The vertex ID for which the owner is being requested.
     * @return the ID of the owning node.
     */
    public int getOwnerID(int vertexID) {
        return vertexID % MASS.getSystemSize();
    }

    /**
     * getNodeIdFromGlobalLinearIndex returns the ID of the node that owns
     * the provided vertexID.
     * @deprecated
     * This method is deprecated and will be removed in a future release.
     * Please migrate to using getOwnerID.
     * 
     * @param vertexID The vertex ID for which you would like
     * the owner.
     * @return the owner ID of the provided vertex ID.
     */
    public int getNodeIdFromGlobalLinearIndex(final int vertexID) {
        MASS.getLogger().warning("getNodeIdFromGlobalLinearIndex is deprecated and will be removed " +
        "in a future release. Please migrate to using getOwnerID.");

        return getOwnerID(vertexID);
    }

    /**
     * getVertexMetaValues creates and returns a pairing fo the global
     * index and owning node for the provided vertexId.
     * 
     * @param vertexId The ID of the vertex to lookup.
     * @return The global index and ID of the node that owns it.
     */
    public VertexMetaValues getVertexMetaValues(Object vertexId) {
        int id = -1;
        int pid = -1;

        int vertId = MASSBase.distributed_map.getOrDefault(vertexId, -1);

        if (vertId != -1) {
            id = vertId;
            pid = getOwnerID(vertId);
        }

        return new VertexMetaValues(id, pid);
    }

    /**
     * recycleID adds the provided vertexID to the idQueue
     * to be recycled in the next call to addVertex.
     * 
     * @param vertexID The ID of the vertex to be recycled.
     */
    private void recycleID(int vertexID) {
        // If we're the master node, add the vertex ID to our
        // idQueue to be recycled.
        if (MASS.getMyPid() == 0) {
            idQueue.add(vertexID);
        }
    }

    // loadDSLFile loads graph data concurrently over each available node from a 
    // Distributed Systems Lab (DSL) graph file.
    public void loadDSLFile(String filePath)  throws IOException, FileNotFoundException {
        // Send to each node.
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_LOAD_DSL_FILE,
            getHandle(),

            // Send all nodes the current vertex offset and path of file to load.
            new Object[]{Integer.valueOf(nextVertexID), filePath}
        )));

        // Handle local operations
        int vertexCount = loadDSLGraphData(nextVertexID, filePath);

        // Wait for all other nodes to complete
        for (MNode node : MASS.getRemoteNodes()) {
            Message msg = node.receiveMessage();
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASS.getLogger().debug("failed to load DSL file");
            }
            vertexCount += msg.getAgentPopulation();
        }

        // update index counter
        nextVertexID += vertexCount;
    }

    // loadDSLGraphData is intended to be called by the system and not by 
    // users. It's purpose is to load data from the provided file in a
    // distributed manner, only taking in data for vertices it owns.
    protected int loadDSLGraphData(int vertexOffset, String filePath) throws IOException, FileNotFoundException {
        int myRank = MASS.getMyPid();

        Path path = Paths.get(filePath);
        BufferedReader br = new BufferedReader(new FileReader(path.toString()));
        String line = "";
        int vertexID = -1;
        String[] parts;
        String[] edges;
        String[] edgeAttribs;
        int neighbor;
        double weight;

        // Track the number of vertices added.
        int vertexCount = 0;

        // Start reading file data.
        while((line = br.readLine()) != null) {
            parts = line.split("=");
            vertexID = Integer.valueOf(parts[0]) + vertexOffset;

            // If we don't own this vertex, skip it.
            if (getOwnerID(vertexID) != myRank) { continue; }

            // Attempt to add the vertex
            if (!addVertexOnNode(myRank, vertexID, null)) {
                MASS.getLogger().error("unable to add vertex from DSL file");
                br.close();
                return vertexCount;
            }

            vertexCount++;

            // Parse edge list and add them to the vertex.
            edges = parts[1].split(";");
            for (String edge : edges) {
                edgeAttribs = edge.split(",");
                neighbor = Integer.valueOf(edgeAttribs[0]) + vertexOffset;
                weight = Double.valueOf(edgeAttribs[1]);

                if (!addEdgeOnNode(myRank, vertexID, neighbor, weight)) {
                    MASS.getLogger().error("unable to add edge from DSL file to vertex");
                    br.close();
                    return vertexCount;
                }
            }
        }

        return vertexCount;
    }

    public void reallyCallAll(int functionId, Object argument, int tid) {
        for (VertexPlace place : places) {
            place.callMethod(functionId, argument);
        }
    }

    /**
     * reallyCallAllWithReturns calls the function associated with the provided functionId
     * on all VertexPlaces on the local node and stores the results in the provided 
     * object array.
     * 
     * @param functionId The ID of the function to call on the VertexPlace.
     * @param returns The array of return values from the function calls.
     * @param arguments The arguments to pass each function.
     */
    public void reallyCallAllWithReturns(int functionId, Object[] returns, Object[] arguments) {
        Object args;
        for (int i = 0; i < this.places.size(); i++) {
            args = arguments == null ? null : arguments[i];
            returns[i] = this.places.get(i).callMethod(functionId, args);
        }
    }

    /**
     * exchangeAll calls the provided function ID on all the neighbors of each
     * VertexPlace stored in places and aggregates the results within the 
     * source VertexPlace.
     * 
     * @param currentFunctionId The function Id of the function to be called on the 
     * neighboring VertexPlace.
     */
    public void exchangeAll(int currentFunctionId) {
        // do serially but this should be multi-threaded. Maybe we can just use a thread pool
        for (VertexPlace place : this.places) {
            Object[] neighbors = place.getNeighbors();
            place.prepareForExchangeAll();

            for (Object neighborKey : neighbors) {
                int neighborIndex = (Integer)neighborKey;
                int ownerId = getOwnerID(neighborIndex);

                // If it's a remote node, send exchange all method to owning node, retrieve
                // and store result, and continue...
                if (ownerId != MASS.getMyPid()) {
                    place.setNeighborResult(
                        neighborKey,
                        exchangeAllOnRemoteNode(ownerId, neighborKey, currentFunctionId)
                    );
                    continue;
                }

                // else if it's a local node, call the method and store result.
                VertexPlace neighborPlace = getVertex(neighborIndex);
                Object result = neighborPlace.callMethod(currentFunctionId, null);

                place.setNeighborResult(neighborKey, result);
            }
        }
    }

    // exchangeAllOnRemoteNode sends a GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT
    // message to the provided node along with the function ID of the function to
    // execute. It then waits for and returns the result of the execution.
    private Object exchangeAllOnRemoteNode(int nodeId, Object neighborKey, int functionId) {
        Optional<MNode> hostOption = MASS.getAllNodes().
            stream().filter(node -> node.getPid() == nodeId).findFirst();
        
        // If the host is not present, something went wrong, log error 
        // and return null.
        if (!hostOption.isPresent()) {
            MASSBase.getLogger().error("host '" + nodeId + "' not found");
            return null;
        }
        MNode remoteHost = hostOption.get();

        remoteHost.sendMessage(new Message(
            Message.ACTION_TYPE.GRAPH_PLACES_EXCHANGE_ALL_REMOTE_RETURN_OBJECT,
            getHandle(),
            neighborKey
        ));
        Message m = remoteHost.receiveMessage();
        return m.getArgument();
    }

    /**
     * exchangeNeighbor calls the function associated with the provided
     * function ID on the neighbor vertex associated with the provided
     * neighbor ID and returns the result.
     * 
     * @param functionId The ID of the function to be called.
     * @param neighborId The ID of the neighbor to call the function on.
     * 
     * @return The result of having called the function.
     */
    public Object exchangeNeighbor(int functionId, int neighborId) {
        // Get local index and size of places array.
        VertexPlace vertex = this.getVertex(neighborId);
        if (vertex == null) {
            MASS.getLogger().debug("the vertex ID does not exist");
            return null;
        }

        return vertex.callMethod(functionId, null);
    }
}
