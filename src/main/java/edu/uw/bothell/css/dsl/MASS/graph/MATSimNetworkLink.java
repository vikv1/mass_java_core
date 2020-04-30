package edu.uw.bothell.css.dsl.MASS.graph;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "link")
public class MATSimNetworkLink {
    @XmlAttribute
    public String id;

    @XmlAttribute
    public long from;

    @XmlAttribute
    public long to;

    @XmlAttribute
    public double length;

    @XmlAttribute
    public double freespeed;

    @XmlAttribute
    public float capacity;

    @XmlAttribute
    public float permlanes;

    @XmlAttribute
    public int oneway;

    @XmlAttribute
    public String modes;
}
