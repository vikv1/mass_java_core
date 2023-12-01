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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PropertyGraphPlaces extends GraphPlaces {

    public PropertyGraphPlaces() {
        super();
    }


    public PropertyGraphPlaces(int handle, String className) {
        super();
    }

    /**
     * this addVertexOnNode creates a new PropertyVertexPlace at the node with
     * the provided nodeID and setIndex for the PropertyVertexPlace.
     * 
     * @param nodeID The node ID of the node with which to add the vertex.
     * @param vertexInitParams The init paramters for the VertexPlace.
     * @param vertexID The ID of the vertex.
     * @return true if successful, false otherwise.
     */
    @Override
    public boolean addVertexOnNode(int nodeID, int vertexID, Object vertexInitParams) {
        
        if (nodeID < 0 || nodeID > MASS.getSystemSize()) { 
            return false; 
        }
        
        // If another node owns this vertex, send it a message to add it.
        if (nodeID != MASS.getMyPid()) {
            return addRemoteVertex(nodeID, vertexID, vertexInitParams);
        }
        // Get local index and size of places array.
        int localIndex = vertexID / MASS.getSystemSize();
        int localSize = places.size();

        // If the ID is associated with an index that doesn't exist,
        // return false.
        if (localIndex > localSize) { 
            return false; 
        }

        // Create new VertexPlace
        PropertyVertexPlace vertexPlace;
        try {
            vertexPlace = new PropertyVertexPlace();
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

    // set properties for PropertyVertexPlace with id
    public boolean setProperties(Object id, Map<Object,Object> properties){
        PropertyVertexPlace place = (PropertyVertexPlace) this.getVertex(id);
        if(place == null) {
            System.out.println("place is null");
            return false;
        }
        place.setProperties(properties);
        return true;
        
    }

    // get properties for PropertyVertexPlace with id
    public Map<Object,Object> getProperties(Object id){
        PropertyVertexPlace place = (PropertyVertexPlace) this.getVertex(id);
        if(place == null) {
            System.out.println("place is null");
            return null;
        }
        return place.getProperties();
    }

    // set relation properties for edge between FromID to ToID
    public boolean setRelationEdge(Object FromID, Object ToID,  Map<Object,Object> relationProperties) {
        PropertyVertexPlace placeFrom = (PropertyVertexPlace) this.getVertex(FromID);
        if(placeFrom == null) {
            System.out.println("FromID is invalid");
            return false;
        }
        if(this.getVertex(ToID) == null) {
            System.out.println("ToID is invalid");
            return false;
        }
        placeFrom.setNeighborProperties(ToID, relationProperties);
        return true;

    }

    // get relation properties for edge between FromID to ToID
    public Map<Object,Object> getRelationEdge(Object FromID, Object ToID) {
        PropertyVertexPlace placeFrom = (PropertyVertexPlace) this.getVertex(FromID);
        if(placeFrom == null) {
            System.out.println("FromID is invalid");
            return null;
        }
        if(this.getVertex(ToID) == null) {
            System.out.println("ToID is invalid");
            return null;
        }
        return placeFrom.getNeighborProperties(ToID);
    }

    // get all neighbor and relation properties for all edges link to FromID
    public Map<Object,Map<Object,Object>> getAllRelationEdges(Object FromID) {
        PropertyVertexPlace placeFrom = (PropertyVertexPlace) this.getVertex(FromID);
        if(placeFrom == null) {
            System.out.println("FromID is invalid");
            return null;
        }
        return placeFrom.getAllNeighborProperties();
    }

    // get PropertyGraphModel, for printGraph purpose.
    public PropertyGraphModel getPropertyGraph() {
        PropertyGraphModel graph = new PropertyGraphModel();

        if (getPlaces() != null && getPlaces().length > 0 && !(getPlaces()[0] instanceof PropertyVertexPlace)) {
            MASSBase.getLogger().warning("Requested map to graph but places are {"
                    + getPlaces()[0].getClass().getName() + "} not PropertyVertexPlaces.");

            return graph;
        }

        // Places on master node
        if (getPlaces() != null) {
            for (Place place : Arrays.stream(getPlaces()).filter(p -> p != null).collect(Collectors.toList())) {

                PropertyVertexPlace vPlace = (PropertyVertexPlace) place;

                Object attribute = MASSBase.distributed_map.reverseLookup(place.getIndex()[0]);

                if (attribute == null) {
                    attribute = place.getIndex()[0];
                }
                
                graph.addPropertyVertex(attribute, vPlace.neighbors, vPlace.getProperties(), vPlace.getAllNeighborProperties());
            }
        }

        merge(graph, getRemotePropertyGraphs());

        return graph;
    }

    // getRemoteGraphs sends messages to all worker nodes requesting the graph
    // models containing their respective vertices.
    private PropertyGraphModel getRemotePropertyGraphs() {
        PropertyGraphModel graph = new PropertyGraphModel();

        for (MNode node : MASSBase.getRemoteNodes()) {
            node.sendMessage(new Message(Message.ACTION_TYPE.MAINTENANCE_GET_PLACES, getHandle(), null));

            Message m = node.receiveMessage();

            if (m.getAction() != Message.ACTION_TYPE.MAINTENANCE_GET_PLACES_RESPONSE) {
                throw new RuntimeException("Received incorrect response from node");
            } else {
                PropertyGraphModel model = (PropertyGraphModel) m.getArgument();

                merge(graph, model);
            }
        }

        return graph;
    }

    private void merge(PropertyGraphModel source, PropertyGraphModel remoteGraphs) {
        source.getPropertyVertices().addAll(remoteGraphs.getPropertyVertices());
    }

    public void printGraph() {        
        List<PropertyVertexModel> vertices = this.getPropertyGraph().getPropertyVertices();

		for (PropertyVertexModel vertex: vertices) {
			System.out.println("Printing the graph with node properties and relationship information: ============");
			System.out.println("Vertex " + Integer.toString((int) vertex.id) + " Properties: " + vertex.nodeProperties);
			System.out.println("       " + Integer.toString((int) vertex.id) + " Neighbor relationships: " + vertex.relationProperties);
		}
    }
}
