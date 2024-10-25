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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;
import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.graph.Graph;
import edu.uw.bothell.css.dsl.MASS.graph.VertexMetaValues;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;
import edu.uw.bothell.css.dsl.MASS.graph.SharedGraph;
import edu.uw.bothell.css.dsl.MASS.graph.LocalMessagingProvider;
import edu.uw.bothell.css.dsl.MASS.infra.MASSSimpleDistributedMap;

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
    protected ConcurrentLinkedQueue<Integer> idQueue = new ConcurrentLinkedQueue<Integer>();

    // objectFactory is used to generate objects of the places class provided
    // when instantiating GraphPlaces.
    protected ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

    // places is used to stored VertexPlaces added after instantiating
    // a GraphPlaces.
    protected Vector<VertexPlace> places = new Vector<VertexPlace>();

    // sharedGraph is used to do shared-memory operations
    protected SharedGraph sharedGraph = null;

    // sharedPlaceName is the identifier for a shared place
    protected String sharedPlaceName = null;

    // localMessaging is the Aeron messaging provider for sharing messages to all other users working on this same sharedPlace
    protected LocalMessagingProvider localMessaging = null;

    //Attributes for adding Left and Right node when used as a tree
    protected static int LEFTNODE_ = 1;
    protected static int RIGHTNODE_ = 2;

    // InitArgs are initialization args to be passed to instances of 
    // GraphPlaces that are being instantiated on remote nodes.
    public static class InitArgs implements Serializable {
        public int handle;
        public String className;
        public String vertexClassName;
        public String sharedPlaceName;
        public Object[] initArgs;

        public InitArgs(int handle, String vertexClassName, String className, String sharedPlaceName, Object... initArgs) {
            this.handle = handle;
            this.vertexClassName = vertexClassName;
            this.className = className;
            this.sharedPlaceName = sharedPlaceName;
            this.initArgs = initArgs;
        }
    }

    // Empty constructor to all for remote instantiation and serialization.
    // This should be reserved for the system and not called by users
    // directly.
    public GraphPlaces() {
        super();
    }

    /**
     * Constructs a basic GraphPlaces object with no pre-allocated space, non-shared by default
     * @param handle
     * @param className
     */
    public GraphPlaces(int handle, String className) {
        super(handle, className);

        // Only call init_master if we're actually the master node.
        int myPid = MASS.getMyPid();

        MASS.getLogger().debug("Trying to Initialize Graph : "+myPid);

        if (myPid == 0) {
            init_graph_master();
        }
    }

    /**
     * Constructs a basic GraphPlaces object with no pre-allocated space, can be shared or non-shared
     * @param handle
     * @param className
     * @param sharedPlaceName if null then non-shared
     */
    public GraphPlaces(int handle, String className, String sharedPlaceName) {
        super(handle, className);
        this.sharedPlaceName = sharedPlaceName;
        if (sharedPlaceName != null) {
            MASSBase.getLogger().debug("This is a shared graph places, trying to retrieve data from shared memory");
            // shared
            sharedGraph = new SharedGraph(sharedPlaceName);
            // read related files from shm
            if (sharedGraph.placesFileExists()) {
                Vector<VertexPlace> newPlaces = sharedGraph.readPlacesFromShm();
                if (newPlaces != null) {
                    places = newPlaces;
                }
                for (VertexPlace vp : this.places) {
                    vp.reinitialize();
                }
            }
            if (sharedGraph.distributedMapFileExists()) {
                MASSSimpleDistributedMap<Object, Integer> newdisMap = sharedGraph.readDistributedMapFromShm();
                if (newdisMap != null) {
                    MASSBase.setDistributedMap(newdisMap);
                }
            }
            if (sharedGraph.idQueueFileExists()) {
                ConcurrentLinkedQueue<Integer> newIdQueue = sharedGraph.readIdQueueFromShm();
                if (newIdQueue != null) {
                    idQueue = newIdQueue;
                }
            }
            if (sharedGraph.nextVertexIDFileExists()) {
                int newNextVertexID = sharedGraph.readNextVertexIDFromShm();
                if (newNextVertexID != -1) {
                    nextVertexID = newNextVertexID;
                }
            }
            MASSBase.getLogger().debug("Initializing the Aeron local message provider on node " + MASS.getMyPid());
            // init the local messageing provider
            localMessaging = new LocalMessagingProvider(this);
            // init again based on messages from other user's running processes, since now the shared memory does not store the most up to date data
            Message m = new Message(Message.ACTION_TYPE.DSG_INIT_REQUEST, MASSBase.getUserName(), getSharedPlaceName());
            localMessaging.sendMessage(m);
        }
        
        // Only call init_master if we're actually the master node.
        int myPid = MASS.getMyPid();

        MASS.getLogger().debug("Trying to Initialize Graph : "+myPid);

        if (myPid == 0) {
            init_graph_master();
        }
    }

    // reinitialize reinitializes the GraphPlaces object by setting the 
    // local index trackers and the placesVector to 0.
    @Override
    protected void reinitialize() {
        super.reinitialize();

        nextVertexID = 0;

        MASSBase.reinitializeMap();

        idQueue = new ConcurrentLinkedQueue<Integer>();

        places = new Vector<VertexPlace>();
        
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_REINITIALIZE, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }
    }

    public void reinitializeLocal() {
        super.reinitialize();

        nextVertexID = 0;

        MASSBase.reinitializeMap();

        idQueue = new ConcurrentLinkedQueue<Integer>();

        places = new Vector<VertexPlace>();
    }

    // reinitializeGraph calls reinitialize locally and sends MAINTENANCE_REINITIALIZE
    // messages to each of the worker nodes to reinitialize them as well.
    protected void reinitializeGraph() {
        // TODO: This feels like something that could be handled by an internal callAll or something similarly
        // utilizing the infrastructure the code already has
        reinitialize();

        //Send reinitialize message to all remote nodes
        Message message = new Message(Message.ACTION_TYPE.MAINTENANCE_REINITIALIZE, getHandle(), null);

        // This needs to remove neighbors anyways so just send to everyone else
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(message));

        

        // Early clear is inconsequential. We just need to make sure we don't move forward before all nodes are done
        MASS.barrierAllSlaves();
    }

    protected void init_graph_master() {
        MASSBase.getLogger().debug("GraphPlaces - init_graph_master");

        Vector<String> hosts = getHosts();

        InitArgs initArgs = new InitArgs(this.getHandle(), this.getClassName(), this.getClass().getName(), this.sharedPlaceName);

        Message message = new Message(Message.ACTION_TYPE.PLACES_INITIALIZE_GRAPH, getSize(),
                getHandle(), getClassName(),
                initArgs, 0, hosts );

        init_master_base(message);
    }

    @Override
    public int[] getSize() {
        return new int[]{size()};
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

        if (all) {
            merge(graph, getRemoteGraphs());
        }

        return graph;
    }

    // getRemoteGraphs sends messages to all worker nodes requesting the graph
    // models containing their respective vertices.
    protected GraphModel getRemoteGraphs() {
        GraphModel graph = new GraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            
            Message k = new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES, getHandle(), null);
            MASS.getLogger().debug(getClassName() + "getRemoteGraph send message: " + k.toString());
            
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES, getHandle(), null));

						// Message m = node.receiveMessage();
            // change to use exchangeHelper for better performance
						Message m = MASSBase.getExchange().receiveMessage(node.getPid());
            MASS.getLogger().debug(getClassName() + " getRemoteGraph receive message: " + m.toString());
            
            

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
        if (sharedPlaceName != null) {
            // broadcast to local users
            Message m = new Message(Message.ACTION_TYPE.DSG_DISTRIBUTED_MAP_PUT, MASSBase.getUserName(), vertexId, vertId, sharedPlaceName);
            localMessaging.sendMessage(m);
        }
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
        if (sharedPlaceName != null) {
            // broadcast to local users
            Message m = new Message(Message.ACTION_TYPE.DSG_DISTRIBUTED_MAP_PUT, MASSBase.getUserName(), vertexId, vertId, sharedPlaceName);
            localMessaging.sendMessage(m);
        }
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
            if (sharedPlaceName != null) {
                // broadcast update locally
                Message m = new Message(Message.ACTION_TYPE.DSG_IDQUEUE_REMOVE, MASSBase.getUserName(), sharedPlaceName);
                localMessaging.sendMessage(m);
            }
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
                if (sharedPlaceName != null) {
                    // broadcast update locally
                    Message m = new Message(Message.ACTION_TYPE.DSG_IDQUEUE_ADD, MASSBase.getUserName(), vertexID, sharedPlaceName);
                    localMessaging.sendMessage(m);
                }
            }
            
            // return -1 to indicate as such.
            return -1; 
        }

        // Otherwise, increment if needed and return the vertexID.
        if (!fromIDQueue) { 
            nextVertexID++;
            if (sharedPlaceName != null) {
                // broadcast update locally
                Message m = new Message(Message.ACTION_TYPE.DSG_NEXT_VERTEXID_UPDATE, MASSBase.getUserName(), nextVertexID, sharedPlaceName);
                localMessaging.sendMessage(m);
            }
        }

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
            MASS.getLogger().debug(getClassName() + "adding remote vertex, node ID: " + nodeID);
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
            vertexPlace.setIndex(new int[]{vertexID});
        } catch (Exception e) {
            MASS.getLogger().error("expection trying to instantiate a new vertex: ", e);
            return false;
        }
        // Set it at the appropriate index if this vertex is to occupy 
        // preallocated space or reclaiming space from a previously removed
        // vertex.
        if (localIndex < localSize) {
            places.set(localIndex, vertexPlace);
            if (sharedPlaceName != null) {
                // broadcast update locally
                Message m = new Message(Message.ACTION_TYPE.DSG_SET_VERTEX_LOCAL, localIndex, vertexPlace, MASSBase.getUserName(), sharedPlaceName);
                localMessaging.sendMessage(m);
            }
            return true;
        }

        // Otherwise, add it to the back.
        places.add(vertexPlace);
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_ADD_VERTEX_LOCAL, vertexPlace, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }

        return true;
    }

    /**
     * addRemoteVertex sends a message to the node with the provided nodeID to
     * add a vertex with the provided vertexID and init parameters.
     */
    protected boolean addRemoteVertex(int nodeID, int vertexID, Object vertexInitParams) {
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

        MASS.getLogger().debug(getClassName() + " addRemoteNode send message: " + msg.toString());

        Message replyMsg = remoteNode.receiveMessage();

        MASS.getLogger().debug(getClassName() + " addRemoteNode receive message: " + replyMsg.toString());

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
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_DISTRIBUTED_MAP_REMOVE, MASSBase.getUserName(), vertexId, sharedPlaceName);
            localMessaging.sendMessage(m);
        }
        
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
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_REMOVE_VERTEX_LOCAL, localIndex, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }
        recycleID(vertexID);

        return true;
    }

    // removeRemoteVertex sends a message to the node with the provided
    // nodeID to remove a vertex with the provided vertexID.
    protected boolean removeRemoteVertex(int nodeID, int vertexID) {
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

        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_REMOVE_NEIGHBOR_LOCAL, neighborID, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }
    }

    // removeNeighborFromRemoteVertices sends a message to all remote
    // nodes to remove the provide neighbor vertex from all vertex 
    // neighbors.
    protected void removeNeighborFromRemoteVertices(int neighborID) {
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

    @Override
    public Place[] getPlaces() {
        return this.places.toArray(new Place[]{});
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

    protected VertexPlace getRemoteVertex(int nodeID, int vertexID) {
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

        // Message replyMsg = remoteNode.receiveMessage();
        // change to use exchange helper for performance
        Message replyMsg = MASSBase.getExchange().receiveMessage(remoteNode.getPid());

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
     * addTreeNode adds an LEFT and RIGHT Nodes for the provided vertex
     *
     * @param vertexID The ID of the source vertex.
     * @param neighborID The ID of the destination vertex.
     * @param TreeNode that denotes if it is Left or Right.
     *
     * @return true if the edge was successfully added, false otherwise.
     */
    public boolean addTreeNode(int vertexID, int neighborID, int TreeNode) {
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

        return addTreeBranchOnNode(
                getOwnerID(vertexID),
                vertexID,
                neighborID,
                TreeNode
        );
    }


    /**
     * addTreeBranchOnNode attempts to add an TreeNode for the provided vertex
     * If the node associated with the provided node ID does not own the
     * source vertex, a message is created and sent to the remote node
     * that does own the source vertex to add the edge.
     *
     * @param nodeID The ID of the node that owns the source vertex.
     * @param vertexID The source vertex ID.
     * @param neighborID The destination vertex ID (its neighbor).
     * @param TreeNode that denotes if it is Left or Right.
     *
     * @return true if the Node was successfully created, false otherwise.
     */
    public boolean addTreeBranchOnNode(int nodeID, int vertexID, int neighborID, int TreeNode) {
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { return false; }

        // If another node owns this vertex, send it a message to add the edge.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteTreeBranch(nodeID, vertexID, neighborID, TreeNode);
        }

        // Get local index and size of places array
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist
        // return false.
        if (localIndex >= localSize) { return false; }

        VertexPlace vertex = places.get(localIndex);
        if (TreeNode == LEFTNODE_)
            vertex.left = neighborID;

        if (TreeNode == RIGHTNODE_)
            vertex.right = neighborID;

        places.set(localIndex, vertex);
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_SET_VERTEX_LOCAL, localIndex, vertex, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }
        return true;
    }

    /**
     * addRemoteTreeBranch sends a MASS message to the node associated with the provided node ID
     * to add an Tree Branch to the vertexID.
     */
    protected boolean addRemoteTreeBranch(int nodeID, int vertexID, int neighborID, int TreeNode) {
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
                Message.ACTION_TYPE.MAINTENANCE_ADD_TREE_NODE,
                getHandle(),
                new Object[]{vertexID, neighborID, TreeNode }
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
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_SET_VERTEX_LOCAL, localIndex, vertex, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }

        return true;
    }

    /**
     * addRemoteEdge sends a MASS message to the node associated with the provided node ID
     * to add an edge between the vertexID and neighborID with the provided edge
     * weight.
     */
    protected boolean addRemoteEdge(int nodeID, int vertexID, int neighborID, double weight) {
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
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_SET_VERTEX_LOCAL, localIndex, vertex, MASSBase.getUserName(), sharedPlaceName);
            localMessaging.sendMessage(m);
        }
        
        return true;
    }

    /**
     * removeRemoteEdge sends a MASS message to the node associated with the provided
     * node ID to remove the edge between the provided vertex and neighbor IDs.
     */
    protected boolean removeRemoteEdge(int nodeID, int vertexID, int neighborID) {
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
     * @param vertexID The vertex ID for which the owner is being requested.e vertex ID for which the owner is being requested.
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
    protected void recycleID(int vertexID) {
        // If we're the master node, add the vertex ID to our
        // idQueue to be recycled.
        if (MASS.getMyPid() == 0) {
            idQueue.add(vertexID);
            if (sharedPlaceName != null) {
                // broadcast update locally
                Message m = new Message(Message.ACTION_TYPE.DSG_IDQUEUE_ADD, MASSBase.getUserName(), vertexID, sharedPlaceName);
                localMessaging.sendMessage(m);
            }
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

        MASS.getLogger().debug("Loading DSL File");

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
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_NEXT_VERTEXID_UPDATE, MASSBase.getUserName(), nextVertexID, sharedPlaceName);
            localMessaging.sendMessage(m);
        }
    }

    // loadDSLGraphData is intended to be called by the system and not by 
    // users. It's purpose is to load data from the provided file in a
    // distributed manner, only taking in data for vertices it owns.
    protected int loadDSLGraphData(int vertexOffset, String filePath) throws IOException, FileNotFoundException {
        int myRank = MASS.getMyPid();

        MASS.getLogger().debug("Trying to read DSL Graph file");

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

            // Parse edge list if vertex has neighbors, 
            // and add them to the vertex.
            if (parts.length != 2) { continue; }
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

    // loadSARFile loads graph data concurrently over each available node from an SAR
    // graph file. For more information on SAR files, see Appendix D (MASS Parallel I/O
    // File Format) in Justin Gilroy's Dynamic Graph Construction and Maintenance
    // whitepaper. This can be found here:
    //
    // https://depts.washington.edu/dslab/MASS/reports/JustinGilroy_whitepaper.pdf
    public void loadSARFile(String filePath) throws IOException, FileNotFoundException {
        // Send load request to each node.
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.MAINTENANCE_LOAD_SAR_FILE,
            getHandle(),

            // Send all nodes the current vertex offset and path of file to load.
            new Object[]{Integer.valueOf(nextVertexID), filePath}
        )));

        // Handle local operations
        int vertexCount = loadSARGraphData(nextVertexID, filePath);

        // Wait for all other nodes to complete
        for (MNode node : MASS.getRemoteNodes()) {
            Message msg = node.receiveMessage();
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASS.getLogger().debug("failed to load SAR file");
            }
            vertexCount += msg.getAgentPopulation();
        }

        nextVertexID += vertexCount;
        if (sharedPlaceName != null) {
            // broadcast update locally
            Message m = new Message(Message.ACTION_TYPE.DSG_NEXT_VERTEXID_UPDATE, MASSBase.getUserName(), nextVertexID, sharedPlaceName);
            localMessaging.sendMessage(m);
        }
    }

    // loadSARGraphData is intended to be called by the system and not by 
    // users. It's purpose is to laod data from the provided SAR file in a
    // distributed manner, only taking in data from vertices it owns.
    protected int loadSARGraphData(int vertexOffset, String filePath) throws IOException, FileNotFoundException {
        int myRank = MASS.getMyPid();

        Path path = Paths.get(filePath);
        BufferedReader br = new BufferedReader(new FileReader(path.toString()));
        String line = "";
        int vertexID = -1;
        String[] edges;
        int neighbor;

        // Track the number of vertices added.
        int vertexCount = 0;
        int lineCount = 0;

        // discard first line as we do not care about the number of vertices or their
        // edges.
        br.readLine();

        while((line = br.readLine()) != null) {
            vertexID = lineCount + vertexOffset;
            lineCount++;

            // If we don't own this vertex, skip it
            if (getOwnerID(vertexID) != myRank) { continue; }

            // Attempt to add the vertex
            if (!addVertexOnNode(myRank, vertexID, null)) {
                MASS.getLogger().error("unable to add vertex from SAR file");
                br.close();

                return vertexCount;
            }

            vertexCount++;

            // Parse edge list and add them to the vertex.
            edges = line.split("\t");
            for (String edge : edges) {
                if (edge.isEmpty()) { continue; }

                neighbor = Integer.valueOf(edge);
                if (!addEdgeOnNode(myRank, vertexID, neighbor, DEFAULT_EDGE_WEIGHT)) {
                    MASS.getLogger().error("unable to add edge from SAR file to vertex");
                    br.close();
                    
                    return vertexCount;
                }
            }
        }

        return vertexCount;
    }

    @Override
    public void callAll( int functionId ) {
        this.callAll(functionId, (Object)null);
    }

    @Override
    public void callAll(int functionId, Object argument) {
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.GRAPH_PLACES_CALL_ALL_VOID_OBJECT,
            this.getHandle(),
            functionId,
            argument
        )));

        this.callVertexPlaceMethod(functionId, argument);

        // Synchronize with all secondary processes
        MASSBase.getLogger().debug("Attempting to barrierAllSlaves...");
        MASS.barrierAllSlaves();
        MASSBase.getLogger().debug("barrierAllSlaves completed!");
    }

    protected void callVertexPlaceMethod(int functionId, Object argument) {
        // resume threads? Not sure why this is needed, it's copied from the Places
        // implementation.
        MASSBase.setCurrentPlacesBase(this);
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_CALLALL);
        
        // callAll on all local places objects
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i) == null) { continue; }

            places.get(i).callMethod(functionId, argument);
        }

        // confirm all threads are done with callAll.
        MThread.barrierThreads( 0 );
    }

    @Override
    public Object[] callAll(int functionId, Object argument[]) {
        // return ca_setup( functionId, ( Object )argument,
		// 		 Message.ACTION_TYPE.PLACES_CALL_ALL_RETURN_OBJECT );

        // Allocate space for return values;
        Object[] returnValues = new Object[argument.length];

        // split arguments across available system nodes.
        int numNodes = MASS.getSystemSize();
        ArrayList<ArrayList<Object>> args = new ArrayList<ArrayList<Object>>();
        for (int i = 0; i < numNodes; i++) {
            args.add(new ArrayList<Object>());
        }

        for (int i = 0; i < argument.length; i++) {
            args.get(i % numNodes).add(argument[i]);
        }

        // Send arguments to respective nodes and have them call the appropriate
        // method.
        MASS.getRemoteNodes().forEach(node -> node.sendMessage(new Message(
            Message.ACTION_TYPE.GRAPH_PLACES_CALL_ALL_RETURN_OBJECT,
            this.getHandle(),
            functionId,
            args.get(node.getPid())
        )));

        // callAll locally
        int myRank = MASS.getMyPid();
        Object[] retVals = callVertexPlaceMethod(functionId, args.get(myRank));
        // set appropriate return values
        for (int i = 0; i < retVals.length; i++) {
            returnValues[myRank + numNodes * i] = retVals[i];
        }

        // get the return values from remote nodes
        for (MNode node : MASS.getRemoteNodes()) {
            Message msg = node.receiveMessage();
            if (msg.getAction() != Message.ACTION_TYPE.ACK) {
                MASSBase.getLogger().error("an unknown error occured during callAll. invalid message type received");
                return null;
            }

            int nodeRank = node.getPid();
            retVals = (Object[]) msg.getArgument();
            for (int i = 0; i < retVals.length; i++) {
                returnValues[nodeRank + numNodes * i] = retVals[i];
            }   
        }

        return returnValues;
    }

    protected Object[] callVertexPlaceMethod(int functionId, ArrayList<Object> args) {
        if (args.size() != places.size()) {
            MASS.getLogger().warning("number of callAll args does not match number of places");
        }

        // Allocate memory for return values
        Object[] retVals = new Object[args.size()];
        
        // resume threads? Not sure why this is needed, it's copied from the Places
        // implementation.
        MThread.resumeThreads(MThread.STATUS_TYPE.STATUS_CALLALL);
        
        int numPlaces = places.size();
        for (int i = 0; i < args.size() && i < numPlaces; i++) {
            if (places.get(i) == null) { continue; }

            retVals[i] = places.get(i).callMethod(functionId, args.get(i));
        }

        // confirm all threads are done with callAll.
        MThread.barrierThreads( 0 );

        return retVals;
    }

    @Override
    public void callAll( int functionId, Object argument, int tid ) {
        this.callAll(functionId, argument);
    }

    @Override
    public Object callAll( int functionId, Object[] arguments, int length, int tid ) {
        Object[] retVals = this.callAll(functionId, arguments);

        return retVals;
    }

    @Override
    @Deprecated
    public void exchangeAll(int destinationHandle, int functionId, Vector<int[]> neighbors) {
        MASS.getLogger().error("Parent function deprecated and not implemented by GraphPlaces");
    }

    @Override
    public void exchangeAll( PlacesBase dstPlaces, int functionId, int tid ) { 
        this.exchangeAll(dstPlaces.getHandle(), functionId);
    }

    @Override
    public void exchangeAll( int destinationHandle, int functionId ) {
        MASS.getLogger().debug("executing exchangeAll");
		Message m = new Message( Message.ACTION_TYPE.PLACES_EXCHANGE_ALL, this.getHandle(), destinationHandle, functionId );
		MASS.getRemoteNodes().forEach( node -> node.sendMessage( m ) );
		
		// retrieve the corresponding places
		MASSBase.setCurrentPlacesBase(this);
		MASSBase.setDestinationPlaces( MASSBase.getPlacesMap().get( destinationHandle ) );
		MASSBase.setCurrentFunctionId(functionId);
		
		// reset requestCounter by the main thread
		MASSBase.resetRequestCounter();
		
		// for debug
		MASSBase.showHosts( );
		
		// resume threads
		MThread.resumeThreads( MThread.STATUS_TYPE.STATUS_EXCHANGEALL );
		
		this.exchangeAll(MASSBase.getCurrentFunctionId());

		// confirm all threads are done with exchangeAll.
		MThread.barrierThreads( 0 );
		
		// Synchronized with all slave processes
		MASS.barrierAllSlaves( );

		// transmit outgoing messages
		MASS.getMessagingProvider().flushPlaceMessages();
		  
		// execute methods queued by incoming messages
		MASS.getEventDispatcher().invokeQueuedAsync( OnMessage.class );
        MASS.getLogger().debug("exchangeAll complete");
    }

    /**
     * exchangeAll calls the provided function ID on all the neighbors of each
     * VertexPlace stored in places and aggregates the results within the 
     * source VertexPlace.
     * 
     * @param currentFunctionId The function Id of the function to be called on the 
     * neighboring VertexPlace.
     */
    protected void exchangeAll(int currentFunctionId) {
        int systemSize = MASS.getSystemSize();
        int myRank = MASS.getMyPid();

        // 1. Call function on all local vertices, caching results for retrieval from
        // remote nodes. Also, generate list of data we need from other nodes.
        Object[] callAllResults = new Object[places.size()];
        HashMap<Integer, HashSet<Integer>> resultsNeeded = new HashMap<Integer,HashSet<Integer>>();
        for (int i = 0; i < systemSize; i++) {
            if (i == myRank) { continue; }
            
            resultsNeeded.put(i, new HashSet<Integer>());
        }
        
        int owner;
        int neighbor;
        for (int i = 0; i < places.size(); i++) {
            VertexPlace p = places.get(i);
            if (p == null) { continue; }

            // 1a. Call method and record result.
            callAllResults[i] = p.callMethod(currentFunctionId, null);

            // 1b. group neighbors by owner
            for (Object n : p.neighbors) {
                neighbor = (Integer)n;
                owner = getOwnerID(neighbor);
                if (owner == myRank) { continue; }

                resultsNeeded.get(owner).add(neighbor);
            }
        }

        // 2. Exchange data with other nodes, requesting data from vertices
        // they own and sending them data from vertices we own. Data is 
        // returned in the format of <Owner, <VertexID, callAllResult>>.
        HashMap<Integer, HashMap<Integer, Object>> data = exchangeData(callAllResults, resultsNeeded);
        
        int localIndex;
        // 3. Go back through our places and set neighbor results
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i) == null) { continue; }

            places.get(i).prepareForExchangeAll();
            for (Object n : places.get(i).neighbors) {
                neighbor = (Integer)n;
                owner = getOwnerID(neighbor);

                // 3a. If we own the neighbor, get it from our local results
                if (owner == myRank) {
                    localIndex = neighbor / systemSize;
                    places.get(i).setNeighborResult(n, callAllResults[localIndex]);
                    continue;
                }

                // 3b. Otherwise it comes from a remote node
                places.get(i).setNeighborResult(n, data.get(owner).get(neighbor));
            }
        }
    }

    /**
     * exchangeData requests all the callAll result data this node needs from
     * remote nodes and provide remote nodes with any callAll result data
     * they need.
     * @param myData is the callAll result data from the vertices we own.
     * @param dataNeeded is the list of vertices for which we need call
     * all result data, grouped by the owner.
     * 
     * @return A map<owner, map<vertexID, callAllResult>> of the remote
     * callAll result data, this node needs.
     */
    protected HashMap<Integer, HashMap<Integer, Object>> exchangeData(Object[] myData, HashMap<Integer, HashSet<Integer>> dataNeeded) {
        int myRank = MASS.getMyPid();
        int systemSize = MASS.getSystemSize();
        HashMap<Integer, HashMap<Integer, Object>> requestedData = new HashMap<Integer, HashMap<Integer, Object>>();

        // Figure out who we need data from.
        boolean[] needDataFrom = new boolean[systemSize];
        for (int i = 0; i < systemSize; i++) {
            if (i == myRank) { continue; }

            needDataFrom[i] = !dataNeeded.get(i).isEmpty();
        }

        // ExchangeHandler creates threads for receiving messages from remote nodes and funnels them
        // into a queue from which the main thread can process them.
        ExchangeHandler eh = new ExchangeHandler(MASS.getMyPid(), MASS.getSystemSize(), needDataFrom);

        // Send requests to all remote nodes for the data we need.
        for (int i = 0; i < systemSize; i++) {
            if (i == myRank) { continue; }

            // If we don't need data from the node, send it a finish message
            // so it knows we wont be requesting data from it.
            if (!needDataFrom[i]) {
                MASSBase.getExchange().sendMessage(i, new Message(Message.ACTION_TYPE.FINISH));
                continue;
            }

            // Request the data we need from the remote node.
            MASSBase.getExchange().sendMessage(i, new Message(
                Message.ACTION_TYPE.GRAPH_PLACES_REQUEST_DATA,
                getHandle(),
                dataNeeded.get(i)
            ));
            
            // Once we've sent the request, notify the node that we
            // wont be making any additional requests.
            MASSBase.getExchange().sendMessage(i, new Message(
                Message.ACTION_TYPE.FINISH
            ));
        }

        // Process incoming messages from the ExchangeHandler message queue.
        // Note that we could easily spin up more threads for processing 
        // messages should we want but message complexity should scale with
        // the number of nodes so that may not be necessary. Unless, however, 
        // the size of the messages justified it.
        while (systemSize > 1) {
            // Dequeue message
            EHMessage ehMsg = eh.dequeMsg();

            // If statements used to avoid needing cases for all enum types... Should maybe implement
            // my own types...

            // If a node is requesting data, send it to them.
            if (ehMsg.msg.getAction() == Message.ACTION_TYPE.GRAPH_PLACES_REQUEST_DATA) {
                HashMap<Integer, Object> reqData = getRequestedData(myData, (HashSet<Integer>)ehMsg.msg.getArgument());
                MASSBase.getExchange().sendMessage(ehMsg.src, new Message(
                    Message.ACTION_TYPE.GRAPH_PLACES_SEND_DATA,
                    getHandle(),
                    reqData
                ));
            }

            // If a node has sent us data that we have requested, record it.
            if (ehMsg.msg.getAction() == Message.ACTION_TYPE.GRAPH_PLACES_SEND_DATA) {
                HashMap<Integer, Object> myReqData = (HashMap<Integer, Object>)ehMsg.msg.getArgument();
                requestedData.put(ehMsg.src, myReqData);
            }

            // FINISH acts as our poison pill. We should only ever receive
            // finish once all threads in our ExchangeHandler have returned 
            // and no more messages will be sent into the queue. As such,
            // we can break out of this while loop and return the received
            // data.
            if (ehMsg.msg.getAction() == Message.ACTION_TYPE.FINISH) {
                break; // All threads have completed, we're done with exchange.
            }
        }

        return requestedData;
    }

    /**
     * @param myData is the callAll result data for this node.
     * @param dataRequested are the vertices with which the requesting node needs the callAll
     * result data for.
     *
     * @return hashmap<vertexID, callAllResult> for all vertices contained in `dataRequested`.
     */
    public HashMap<Integer, Object> getRequestedData(Object[] myData, HashSet<Integer> dataRequested) {
        HashMap<Integer, Object> data = new HashMap<Integer, Object>();
        for (Integer vertexID : dataRequested) {
            int localIndex = vertexID / MASS.getSystemSize();
            data.put(vertexID, myData[localIndex]);
        }

        return data;
    }

    /**
     * get the shared place name for this GraphPlaces
     * @return if null then means this GraphPlaces is not shared
     */
    public String getSharedPlaceName() {
        return sharedPlaceName;
    }

    /**
     * Called by LocalMessageReaderThread, Received DSG_INIT_REQUEST type message from other users, help them init by providing my data
     */
    public void helpOtherUsersInit(String receiver) {
        Message m = new Message(Message.ACTION_TYPE.DSG_INIT_RESPONSE, MASSBase.getUserName(), receiver, places, MASSBase.distributed_map, idQueue, nextVertexID, getSharedPlaceName());
        localMessaging.sendMessage(m);
    }

    /**
     * When this GraphPlaces is shared, we need to retreive previous data, use this function to set places
     */
    public void setPlaces(Vector<VertexPlace> places) {
        this.places = places;
    }

    /**
     * When this GraphPlaces is shared, we need to retreive previous data, use this function to set idQueue
     */
    public void setIdQueue(ConcurrentLinkedQueue<Integer> idQueue) {
        this.idQueue = idQueue;
    }

    /**
     * When this GraphPlaces is shared, we need to retreive previous data, use this function to set nextVertexID
     */
    public void setNextVertexID(int nextVertexID) {
        this.nextVertexID = nextVertexID;
    }

    /**
     * get the idQueue for this GraphPlaces
     */
    public ConcurrentLinkedQueue<Integer> getIdQueue() {
        return this.idQueue;
    }

    /**
     * will be called at termination of MASS program
     * only when the GraphPlaces is shared, we terminate the localMessaging and write current data to /dev/shm for backup
     */
    public void finish() {
        if (sharedPlaceName != null) {
            MASSBase.getLogger().debug("Shared graph places, shut down local messaging and write data to /dev/shm");
            localMessaging.shutdown();
            sharedGraph.writeDataToShm(this.places, MASSBase.distributed_map, this.idQueue, this.nextVertexID);
        }
    }

    /**
     * EHMessage is an ExchangeHandler message. It allows us to associate src
     * of the message with the message itself.
     */
    protected static class EHMessage {
        public int src;
        public Message msg;

        public EHMessage(int src, Message msg) {
            this.src = src;
            this.msg = msg;
        }
    }

    /**
     * ExchangeHandler spins up threads for receiving messages from each 
     * remote node. It funnels these messages into a concurrent blocking queue
     * that the caller can use to receive them.
     */
    protected class ExchangeHandler {
        // the receive msg queue
        protected BlockingQueue<EHMessage> msgQueue;
        protected int systemSize;
        protected int myRank;

        // indices with true values indicate a rank
        // from which we need data.
        protected boolean[] needDataFrom;

        // threadCount maintains the number of active threads. When this
        // reaches 0, we send a poison pill into the queue to let the
        // caller know that it will not receive any more messages.
        protected AtomicInteger threadCount = new AtomicInteger(0);

        /**
         * ExchangeHandler instantiates the queue and spins up a thread for each node.
         * 
         * @param myRank the callers system rank
         * @param systemSize the size of the system
         * @param needDataFrom a boolean array of ranks from which this system
         * needs data.
         */
        public ExchangeHandler(int myRank, int systemSize, boolean[] needDataFrom) {
            this.msgQueue = new LinkedBlockingQueue<EHMessage>();
            this.myRank = myRank;
            this.systemSize = systemSize;
            this.needDataFrom = needDataFrom;

            // Kick off threads for each node
            for (int i = 0; i < this.systemSize; i++) {
                // No need to spin up a thread just to talk to ourself.
                if (i == this.myRank) { continue; }

                // Increment thread count and capture rank
                threadCount.incrementAndGet();
                final int rank = i;
                
                // Instantiate a new thread.
                new Thread(() -> {
                    // Assume this rank will request data from us. Requiring them to 
                    // explicitly tell us if they do not.
                    boolean rankNeedsData = true;

                    // Do we need data from this rank
                    boolean iNeedData = this.needDataFrom[rank];

                    // As long as this rank needs data from us, or we need data from it,
                    // continue receiving messages.
                    while(rankNeedsData || iNeedData) {
                        Message msg = MASSBase.getExchange().receiveMessage(rank);

                        // FINISH indicates that this rank will not be requesting
                        // any more data from us.
                        if (msg.getAction() == Message.ACTION_TYPE.FINISH) {
                            rankNeedsData = false;
                            continue;
                        }

                        // SEND_DATA indicates that this rank has responded to our request,
                        // sending us the data that we need. We should no longer need data.
                        if (msg.getAction() == Message.ACTION_TYPE.GRAPH_PLACES_SEND_DATA) {
                            iNeedData = false;
                        }

                        // SEND_DATA and REQUEST_DATA messages both get enqueued.
                        // FINISH does not, as it is reserved for use as our 
                        // poison pill.
                        this.msgQueue.add(new EHMessage(rank, msg));
                    }

                    // This thread is complete, decrement count and check if we're the last
                    // thread. If we are, send poison pill to indicate to the caller that
                    // they will receive no more messages.
                    if (threadCount.decrementAndGet() == 0) {
                        MASS.getLogger().debug("[ExchangeHandler] All threads complete");
                        this.msgQueue.add(new EHMessage(
                            this.myRank, 
                            new Message(Message.ACTION_TYPE.FINISH)
                        ));
                    }
                }).start();
            }
        }

        /**
         * @return a message from the receive queue.
         */
        public EHMessage dequeMsg() {
            EHMessage msg = null;
            while (msg == null) {
                try {
                    msg = this.msgQueue.take();
                } catch (InterruptedException e) {}
            }
            
            return msg;
        }
    }

}
