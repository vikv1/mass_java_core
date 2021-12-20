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

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public class VertexPlace extends Place implements Serializable, Cloneable {

    // serialVersionUID set to resemble semantic versioning for
    // VertexPlace (v1.0.0 == 01 00 00)
    static final long serialVersionUID = 010000L;
    
	protected Map<Object, Object> neighborResults;
    private Object [] graphArguments;
    public Vector<Object> neighbors = new Vector<>();
    public Vector<Object> weights = new Vector<>();

    // clone returns a deep copy of the VertexPlace with the exception that it's
    // possible for the objects within the neighbors, weights, graphArguments, 
    // and neighborResults containers to contain objects that themselves have
    // shared references.
    public Object clone() throws CloneNotSupportedException {
        VertexPlace vertexClone = (VertexPlace)super.clone();
        
        // Clone neighbors container...
        vertexClone.neighbors = new Vector<Object>(this.neighbors.size());
        vertexClone.neighbors.addAll(this.neighbors);
        
        // Clone weights container...
        vertexClone.weights = new Vector<Object>(this.weights.size());
        vertexClone.weights.addAll(this.weights);

        // Clone graphArguments container...
        if (this.graphArguments != null) {
            vertexClone.graphArguments = this.graphArguments.clone();    
        }

        // Clone neighborResults container...
        if (this.neighborResults != null) {
            vertexClone.neighborResults = new HashMap<>(this.neighborResults.size());
            vertexClone.neighborResults.putAll(this.neighborResults);
        }

        return vertexClone;
    }

    public void prepareForExchangeAll() {
        neighborResults = new HashMap<>(neighbors.size());
    }

    public void addNeighbor(Object neighborId, double weight) throws IllegalArgumentException {
        if (neighbors.contains(neighborId)) {
            return;
        }

        neighbors.add(neighborId);
        weights.add((int) Math.round(weight));
    }

    public void removeNeighbor(Object neighborId) throws IllegalArgumentException {
        if (!neighbors.contains(neighborId)) {
            throw new IllegalArgumentException("Invalid neighbor for remove: " + neighborId);
        }

        int index = neighbors.indexOf(neighborId);

        neighbors.remove(neighborId);
        weights.remove(index);
    }

    public void setNeighborResult(Object neighbor, Object result) {
        int neighborIndex = neighbors.indexOf(neighbor);

        neighborResults.put(neighborIndex, result);
    }

    public void removeNeighborSafely(Object neighborVertexId) {
        if (neighbors != null && neighbors.contains(neighborVertexId)) {
            removeNeighbor(neighborVertexId);
        }
    }

    public static class Tuple {
        public Tuple(Object i, double w) {
            index = i;
            weight = w;
        }

        public Object index;
        public double weight;
    }

    public Object [] getNeighbors() {
        Object [] result = new Object[neighbors.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = neighbors.get(i);
        }

        return result;
    }

    public Object [] getWeights() {
        Object [] result = new Object[weights.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = weights.get(i);
        }

        return result;
    }

    public VertexPlace() {
        super();

        MASSBase.getLogger().debug("VertexPlace constructed.");
    }

    public VertexPlace(Object args) {
        super();

        try {
            if (args != null) {
                Object[] arguments = (Object[]) args;

                graphArguments = Arrays.copyOfRange(arguments, 0, 3);

                // TODO: Parallel IO requires the index to be set
                //  We might consider refactoring the neighbors to come after the constructor
                this.setIndex(new int[]{(int) graphArguments[2]});

                init(args);

                MASSBase.getLogger().trace(String.format("VertexPlace constructed with args: { id: %d, neighbors: [%s], weights: [%s] }\n",
                        graphArguments[2],
                        this.neighbors.stream()
                                .map(n -> n.toString())
                                .collect(Collectors.joining(", ")),
                        this.weights.stream()
                                .map(w -> w.toString())
                                .collect(Collectors.joining(", "))));
            }
        } catch (Exception e) {
            MASSBase.getLogger().error("Exception in VertexPlace constructor", e);
        }
    }

    private void init(Object args) {
        Object [] arguments = (Object[])args;

        graphArguments = Arrays.copyOfRange(arguments, 0, 3);
    }

    public Object getAttribute() {
        return MASSBase.distributed_map.get(getIndex()[0]);
    }
}
