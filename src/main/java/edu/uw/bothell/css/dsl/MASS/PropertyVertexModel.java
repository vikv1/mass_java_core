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
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class PropertyVertexModel implements Serializable {
	public String nodeName = null;
    public Set<String> labels; // to store node labels
    public Map<String,String> nodeProperties;  // to store node properties
    public Map<Object, Object[]> toRelation; // to store TO relationship 
    public Map<Object, Object[]> fromRelation; // to store FROM relationship 

    public PropertyVertexModel( String nodeName, Set<String> labels, Map<String,String> nodeProperties, Map<Object, Object[]> toRelationship, Map<Object, Object[]> fromRelationship) {
		this.nodeName = nodeName;
		this.labels = labels;
		this.nodeProperties = nodeProperties;
		this.toRelation = toRelationship;
        this.fromRelation = fromRelationship;
    }

	protected String getString(Map<Object, Object[]> relationship) {
		if(relationship == null) return "";

		StringBuilder sb = new StringBuilder();
		
		for(Map.Entry<Object, Object[]> entry: relationship.entrySet()) {
			sb.append("                           ");
			sb.append("ID: " + entry.getKey());
			sb.append(", types: " + entry.getValue()[0].toString());
			sb.append(", propertiess: " + entry.getValue()[1].toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public void print() {
		System.out.println("Vertex ItemID: " +  this.nodeName + ", labels:" + labels);
        System.out.println("              node properties: " + nodeProperties);
		System.out.println("              TO neighbors: \n" + getString(toRelation));
		System.out.println("              FROM neighbors: \n" + getString(fromRelation));
		System.out.println();
	}

}
