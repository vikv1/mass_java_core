package edu.uw.bothell.css.dsl.MASS.graph;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "node")
public class MATSimNetworkNode {
    @XmlAttribute
    public long id;

    @XmlAttribute
    public double x;

    @XmlAttribute
    public double y;
}
