package edu.uw.bothell.css.dsl.mass.apps.ShortestPath;
import java.io.*;

public class Args2Agents implements Serializable {
    public Args2Agents( int src, int dest, int node, int time ) {
	source = src;
	destination = dest;
	nextNode = node;
	deptTime = time;
    }
    public int source = -1;
    public int destination = -1;
    public int nextNode = -1;
    public int deptTime = -1;
}
