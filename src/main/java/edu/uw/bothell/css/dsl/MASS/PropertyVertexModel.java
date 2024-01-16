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

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import edu.uw.bothell.css.dsl.MASS.graph.transport.VertexModel;

public class PropertyVertexModel extends VertexModel {
	public String nodeName = null;
    public Set<String> labels = new HashSet<String>();
    public Map<String,String> nodeProperties = new HashMap<>();  // to store node properties
    // public List<Object> neighbors = new Vector<>(); // declared in VerterModel.java
    public Map<Object, Set<String>> relationTypes = new HashMap<Object, Set<String>>();
    public Map<Object, Map<String,String>> relationProperties = new HashMap<Object, Map<String,String>>(); // to store relationship properties

    public PropertyVertexModel(Object id, String nodeName, Set<String> labels, Map<String,String> nodeProperties, List<Object> neighbors, Map<Object, Set<String>> relationTypes,  Map<Object,Map<String,String>> relationProperties) {
        super(id,neighbors);
		
		this.nodeName = nodeName;
		this.labels = labels;
		this.nodeProperties = nodeProperties;
		this.relationTypes = relationTypes;
        this.relationProperties = relationProperties;
    }

	public void print() {
		System.out.println("Vertex ID: " +  this.id + ", labels:" + labels);
        System.out.println("              node properties: " + nodeProperties);
		System.out.println("              list of neighbors: " + neighbors);
		System.out.println("              relation types: " + relationTypes);
		System.out.println("              relation properties: " + relationProperties);
	}

}
