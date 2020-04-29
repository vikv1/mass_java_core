package edu.uw.bothell.css.dsl.MASS.graph;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

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
