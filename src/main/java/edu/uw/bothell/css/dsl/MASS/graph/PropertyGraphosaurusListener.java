/*

 	MASS Java Software License
	© 2012-2025 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2025 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import edu.uw.bothell.css.dsl.MASS.PropertyGraphModel;
import edu.uw.bothell.css.dsl.MASS.PropertyGraphPlaces;
import edu.uw.bothell.css.dsl.MASS.PropertyVertexModel;
import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphosaurusMessage;

/**
 * WebSocket client that sends property graph data (labels, properties, relationships) 
 * to Graphosaurus visualization server.
 * 
 * Extends GraphosaurusListener to add support for:
 * - Node labels and properties
 * - Edge relationship types and properties
 * - Color coding based on labels
 */
public class PropertyGraphosaurusListener extends GraphosaurusListener {

    // Color palette for different node types/labels
    private static final int[] LABEL_COLORS = {
        0xFF6B6B,  // Red
        0x4ECDC4,  // Teal
        0x45B7D1,  // Blue
        0x96CEB4,  // Green
        0xFFA07A,  // Light Salmon
        0x9B59B6,  // Purple
        0xF39C12,  // Orange
        0x1ABC9C,  // Turquoise
        0xE74C3C,  // Crimson
        0x3498DB,  // Sky Blue
        0x2ECC71,  // Emerald
        0xE67E22,  // Carrot
    };

    // Map to track label -> color assignments
    private final Map<String, Integer> labelColorMap = new ConcurrentHashMap<>();
    private final AtomicInteger nextColorIndex = new AtomicInteger(0);

    // Reference to the PropertyGraphPlaces
    private final PropertyGraphPlaces propertyGraphPlaces;

    /**
     * Constructor with default settings
     * 
     * @param propertyGraphPlaces The PropertyGraphPlaces instance to monitor
     */
    public PropertyGraphosaurusListener(PropertyGraphPlaces propertyGraphPlaces) {
        super(propertyGraphPlaces);
        this.propertyGraphPlaces = propertyGraphPlaces;
    }

    /**
     * Constructor with custom settings
     * 
     * @param propertyGraphPlaces The PropertyGraphPlaces instance to monitor
     * @param websocketUrl WebSocket server URL
     * @param pollIntervalMs Polling interval in milliseconds
     */
    public PropertyGraphosaurusListener(PropertyGraphPlaces propertyGraphPlaces, String websocketUrl, long pollIntervalMs) {
        super(propertyGraphPlaces, websocketUrl, pollIntervalMs);
        this.propertyGraphPlaces = propertyGraphPlaces;
    }

    /**
     * Constructor with custom settings and partial loading option
     * 
     * @param propertyGraphPlaces The PropertyGraphPlaces instance to monitor
     * @param websocketUrl WebSocket server URL
     * @param pollIntervalMs Polling interval in milliseconds
     * @param partialLoading If true, skip sending the full graph on connect
     */
    public PropertyGraphosaurusListener(PropertyGraphPlaces propertyGraphPlaces, String websocketUrl, long pollIntervalMs, boolean partialLoading) {
        super(propertyGraphPlaces, websocketUrl, pollIntervalMs, partialLoading);
        this.propertyGraphPlaces = propertyGraphPlaces;
    }

    /**
     * Send the entire property graph structure to Graphosaurus
     */
    @Override
    protected void sendFullGraph() {
        try {
            PropertyGraphModel graphModel = propertyGraphPlaces.getPropertyGraph();
            if (graphModel == null || graphModel.getPropertyVertices() == null) {
                massLogger.debug("PropertyGraphosaurus full graph send skipped: no property graph data");
                // Fall back to parent implementation for non-property graphs
                super.sendFullGraph();
                return;
            }

            List<PropertyVertexModel> vertices = graphModel.getPropertyVertices();
            massLogger.debug("PropertyGraphosaurus sending property graph with " + vertices.size() + " vertices");

            // First pass: send all vertices with their properties
            for (PropertyVertexModel vertex : vertices) {
                if (vertex.nodeName != null) {
                    sendPropertyVertex(vertex);
                    tracker.markVertexAsSent(vertex.nodeName);
                }
            }

            // Second pass: send all edges with their relationship properties
            int edgeCount = 0;
            for (PropertyVertexModel vertex : vertices) {
                if (vertex.nodeName != null && vertex.toRelation != null) {
                    String fromId = vertex.nodeName;
                    for (Map.Entry<Object, Object[]> entry : vertex.toRelation.entrySet()) {
                        String toId = String.valueOf(entry.getKey());
                        if (!tracker.hasEdgeBeenSent(fromId, toId)) {
                            Object[] relationData = entry.getValue();
                            @SuppressWarnings("unchecked")
                            Set<String> relationTypes = relationData[0] != null ? (Set<String>) relationData[0] : null;
                            @SuppressWarnings("unchecked")
                            Map<String, String> relationProperties = relationData[1] != null ? (Map<String, String>) relationData[1] : null;
                            
                            sendPropertyEdge(fromId, toId, relationTypes, relationProperties);
                            tracker.markEdgeAsSent(fromId, toId);
                            edgeCount++;
                        }
                    }
                }
            }

            massLogger.debug("PropertyGraphosaurus sent " + edgeCount + " edges with relationship data");

            if (!vertices.isEmpty()) {
                fullGraphSyncCompleted = true;
            }

        } catch (Exception e) {
            massLogger.error("Error sending property graph", e);
            // Fall back to parent implementation
            super.sendFullGraph();
        }
    }

    /**
     * Send a property vertex to Graphosaurus with labels and properties
     * 
     * @param vertex The property vertex model to send
     */
    protected void sendPropertyVertex(PropertyVertexModel vertex) {
        // Generate random position for the vertex (Graphosaurus will layout in 3D)
        double[] position = new double[3];
        position[0] = Math.random() * 8 - 4;  // Random x between -4 and 4
        position[1] = Math.random() * 8 - 4;  // Random y between -4 and 4
        position[2] = Math.random() * 8 - 4;  // Random z between -4 and 4

        // Determine color based on the first label
        int color = getColorForLabels(vertex.labels);

        GraphosaurusMessage.AddNodeMessage message = 
            new GraphosaurusMessage.AddNodeMessage(
                vertex.nodeName, 
                color, 
                position
            );
        
        // Add labels to data
        if (vertex.labels != null && !vertex.labels.isEmpty()) {
            message.addData("labels", new ArrayList<>(vertex.labels));
            message.addData("labelString", String.join(", ", vertex.labels));
        }
        
        // Add node properties to data
        if (vertex.nodeProperties != null && !vertex.nodeProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : vertex.nodeProperties.entrySet()) {
                message.addData(entry.getKey(), entry.getValue());
            }
        }
        
        // Add vertex name
        message.addData("name", vertex.nodeName);
        message.addData("type", "PropertyVertex");

        sendMessage(message);
    }

    /**
     * Send a property edge to Graphosaurus with relationship types and properties
     * 
     * @param fromId Source vertex ID
     * @param toId Target vertex ID
     * @param relationTypes Set of relationship type labels
     * @param relationProperties Map of relationship properties
     */
    protected void sendPropertyEdge(String fromId, String toId, Set<String> relationTypes, Map<String, String> relationProperties) {
        // Determine edge color based on relationship type
        int edgeColor = getColorForRelationType(relationTypes);
        
        GraphosaurusMessage.AddEdgeMessage message = 
            new GraphosaurusMessage.AddEdgeMessage(fromId, toId, edgeColor);
        
        // Add relationship types to data
        if (relationTypes != null && !relationTypes.isEmpty()) {
            message.addData("relationshipTypes", new ArrayList<>(relationTypes));
            message.addData("relationship", String.join(", ", relationTypes));
        }
        
        // Add relationship properties to data
        if (relationProperties != null && !relationProperties.isEmpty()) {
            for (Map.Entry<String, String> entry : relationProperties.entrySet()) {
                message.addData(entry.getKey(), entry.getValue());
            }
        }
        
        message.addData("type", "PropertyEdge");
        
        sendMessage(message);
    }

    /**
     * Get a consistent color for a set of labels
     * 
     * @param labels Set of labels
     * @return Color as integer
     */
    private int getColorForLabels(Set<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return 0x888888;  // Default gray
        }
        
        // Use the first label to determine color
        String primaryLabel = labels.iterator().next().toLowerCase();
        
        if (!labelColorMap.containsKey(primaryLabel)) {
            int index = nextColorIndex.getAndIncrement();
            labelColorMap.put(primaryLabel, LABEL_COLORS[index % LABEL_COLORS.length]);
        }
        
        return labelColorMap.get(primaryLabel);
    }

    /**
     * Get a color for relationship types
     * 
     * @param relationTypes Set of relationship types
     * @return Color as integer
     */
    private int getColorForRelationType(Set<String> relationTypes) {
        if (relationTypes == null || relationTypes.isEmpty()) {
            return 0xCCCCCC;  // Default light gray
        }
        
        // Use a darker shade based on relationship type
        String primaryType = relationTypes.iterator().next().toLowerCase();
        
        if (!labelColorMap.containsKey("rel_" + primaryType)) {
            // Use darker colors for edges
            int index = nextColorIndex.getAndIncrement();
            int baseColor = LABEL_COLORS[index % LABEL_COLORS.length];
            // Darken the color by reducing RGB values
            int r = ((baseColor >> 16) & 0xFF) * 3 / 4;
            int g = ((baseColor >> 8) & 0xFF) * 3 / 4;
            int b = (baseColor & 0xFF) * 3 / 4;
            int darkColor = (r << 16) | (g << 8) | b;
            labelColorMap.put("rel_" + primaryType, darkColor);
        }
        
        return labelColorMap.get("rel_" + primaryType);
    }

    /**
     * Send graph structure for a single vertex using the PropertyGraphModel,
     * which contains the actual edge/relationship data that setRelationEdge stores.
     * The base class version uses GraphModel.neighbors which is empty for property graphs.
     */
    @Override
    protected void sendGraphStructureForVertex(Object vertexId) {
        try {
            PropertyGraphModel graphModel = propertyGraphPlaces.getPropertyGraph();
            if (graphModel == null || graphModel.getPropertyVertices() == null) {
                return;
            }

            List<PropertyVertexModel> allVertices = graphModel.getPropertyVertices();
            String vertexIdStr = String.valueOf(vertexId);

            for (PropertyVertexModel vertex : allVertices) {
                if (vertex.nodeName == null || !vertex.nodeName.equals(vertexIdStr)) {
                    continue;
                }

                // 1) Send this vertex
                if (!tracker.hasVertexBeenSent(vertexIdStr)) {
                    sendPropertyVertex(vertex);
                    tracker.markVertexAsSent(vertexIdStr);
                }

                // Collect neighbor IDs from both TO and FROM relationships
                Map<String, Object[]> neighborsToSendEdges = new java.util.LinkedHashMap<>();

                if (vertex.toRelation != null) {
                    for (Map.Entry<Object, Object[]> entry : vertex.toRelation.entrySet()) {
                        String neighborId = String.valueOf(entry.getKey());
                        neighborsToSendEdges.put(neighborId, entry.getValue());
                    }
                }

                // 2) Send all neighbor vertices first
                for (String neighborId : neighborsToSendEdges.keySet()) {
                    if (!tracker.hasVertexBeenSent(neighborId)) {
                        for (PropertyVertexModel neighbor : allVertices) {
                            if (neighbor.nodeName != null && neighbor.nodeName.equals(neighborId)) {
                                sendPropertyVertex(neighbor);
                                tracker.markVertexAsSent(neighborId);
                                break;
                            }
                        }
                    }
                }

                // 3) Send all edges (both endpoints now exist on the frontend)
                for (Map.Entry<String, Object[]> entry : neighborsToSendEdges.entrySet()) {
                    String neighborId = entry.getKey();
                    if (!tracker.hasEdgeBeenSent(vertexIdStr, neighborId)) {
                        Object[] relationData = entry.getValue();
                        @SuppressWarnings("unchecked")
                        Set<String> relationTypes = relationData[0] != null ? (Set<String>) relationData[0] : null;
                        @SuppressWarnings("unchecked")
                        Map<String, String> relationProperties = relationData[1] != null ? (Map<String, String>) relationData[1] : null;

                        sendPropertyEdge(vertexIdStr, neighborId, relationTypes, relationProperties);
                        tracker.markEdgeAsSent(vertexIdStr, neighborId);
                    }
                }

                break;
            }
        } catch (Exception e) {
            massLogger.error("Error sending property graph structure for vertex: " + vertexId, e);
        }
    }

    /**
     * Manually send a property vertex by ID
     * Useful for on-demand graph loading
     * 
     * @param vertexId The vertex ID to send
     */
    public void sendPropertyVertexById(String vertexId) {
        try {
            PropertyGraphModel graphModel = propertyGraphPlaces.getPropertyGraph();
            if (graphModel == null || graphModel.getPropertyVertices() == null) {
                return;
            }

            for (PropertyVertexModel vertex : graphModel.getPropertyVertices()) {
                if (vertex.nodeName != null && vertex.nodeName.equals(vertexId)) {
                    if (!tracker.hasVertexBeenSent(vertexId)) {
                        sendPropertyVertex(vertex);
                        tracker.markVertexAsSent(vertexId);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            massLogger.error("Error sending property vertex by ID: " + vertexId, e);
        }
    }
}
