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

package edu.uw.bothell.css.dsl.MASS.graph;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "network")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class MATSimNetworkModel {
    private List<MATSimNetworkNode> nodes = new ArrayList<>();
    private List<MATSimNetworkLink> links = new ArrayList<>();

    /**
     * Get the collection of network nodes
     * @return The collection of nodes
     */
    @XmlElementWrapper(name="nodes")
    @XmlElement(name = "node", type=MATSimNetworkNode.class)
    public List<MATSimNetworkNode> getNodes() {
        return nodes;
    }

    /**
     * Set the collection of network nodes
     * @param nodes The collection of nodes
     */
    public void setNodes(List<MATSimNetworkNode> nodes) {
        this.nodes = nodes;
    }

    /**
     * Get the collection of network links
     * @return The collection of nodes
     */
    @XmlElementWrapper(name="links")
    @XmlElement(name = "link", type=MATSimNetworkLink.class)
    public List<MATSimNetworkLink> getLinks() {
        return links;
    }

    /**
     * Set the collection of network links
     * @param links The collection of links
     */
    public void setLinks(List<MATSimNetworkLink> links) {
        this.links = links;
    }
}
