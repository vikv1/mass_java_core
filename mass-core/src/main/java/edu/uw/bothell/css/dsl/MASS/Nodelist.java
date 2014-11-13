package edu.uw.bothell.css.dsl.MASS;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Nodelist is a simple container for MNodes
 * @author msell
 * @since 0.8.0
 */
@XmlRootElement(name = "nodes")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Nodelist {
	
	private List<MNode> nodes = new ArrayList<MNode>();

	/**
	 * Get the collection of MASS Nodes
	 * @return The collection of nodes
	 */
	@XmlElement(name="node")
	public List<MNode> getNodes() {
		return nodes;
	}

	/**
	 * Set the collection of MASS Nodes
	 * @param nodes The collection of nodes
	 */
	public void setNodes(List<MNode> nodes) {
		this.nodes = nodes;
	}

}
