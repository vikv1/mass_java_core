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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import edu.uw.bothell.css.dsl.MASS.Parallel_IO.InvalidNumberOfNodesException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.InvalidNumberOfPlacesException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.UnsupportedFileTypeException;
import edu.uw.bothell.css.dsl.MASS.graph.HIPPIETABEdge;
import edu.uw.bothell.css.dsl.MASS.graph.HIPPIETABFormatLineParts;
import ucar.ma2.InvalidRangeException;

public class VertexPlace extends Place implements Serializable {

    static final long serialVersionUID = 12345L;
    
	private Map<Object, Object> neighborResults;
    private Object [] graphArguments;
    public Vector<Object> neighbors = new Vector<>();
    public Vector<Object> weights = new Vector<>();

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

        System.err.println("VertexPlace constructed");

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

        init_neighbors((String)graphArguments[0], (int)graphArguments[2]);
    }

    private void init_neighbors(String networkFilename, int index) {
        if (networkFilename == null) return;

        if (networkFilename.contains(".xml")) {
            init_neighbors_matsim(networkFilename, index);
        } else if (networkFilename.contains(".tsv")) {
            init_neighbors_hippie(networkFilename, index);
        } else if (networkFilename.endsWith(".sar")) {
            init_neighbors_sar(networkFilename, index);
        } else {
            init_neighbors_parallel(networkFilename, index);
        }
    }

    private void init_neighbors_sar(String networkFilename, int index) {
        Path filePath = Paths.get(MASSBase.getWorkingDirectory(), networkFilename);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toFile()))) {
            String onSet = br.readLine();

            List<Integer> counts = Arrays.stream(onSet.split(","))
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());

            int predecessors = counts.stream().limit(index).reduce(0, Integer::sum);

            int offset = predecessors * 10 + index + predecessors;

            br.skip(offset);

            String neighborLine = br.readLine();

            List<Integer> neighbors = Arrays.stream(neighborLine.split("\t"))
                    .map(s -> s.trim())
                    .map(Integer::valueOf)
                    .collect(Collectors.toList());

            this.neighbors.addAll(neighbors);
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            MASSBase.getLogger().error(sw.toString());
        }
    }

    private void init_neighbors_hippie(String networkFilename, int index) {
        Set<Map.Entry<Object, Integer>> entries = MASSBase.distributed_map.entrySet();

        String key = "";

        try {
            for (Map.Entry<Object, Integer> entry : entries) {
                String entryKey = (String) entry.getKey();
                Integer globalIndex = entry.getValue();

                if (globalIndex == index) {
                    key = entryKey;

                    break;
                }
            }
        } catch (Exception e) {
            MASSBase.getLogger().error("Exception in reverse map lookup " + index, e);
        }


        if (key.equals("")) {
            MASSBase.getLogger().error("Requested index not in map: " + index);
        }

        List<Tuple> neighbors = getHippieNeighbors(networkFilename, key);

        for (Tuple neighbor : neighbors) {
            this.neighbors.add(neighbor.index);

            // TODO: Refactor weights to double
            // this.weights.add(neighbor.weight);

            int weight = 1;

            try {
                weight = (int)Math.round(neighbor.weight);
            } catch (Exception e) {
                MASSBase.getLogger().warning("Exception parsing double to integer: " + e.getMessage());
            }

            this.weights.add(weight);
        }
    }

    public static List<Tuple> getHippieNeighbors(String networkFilename, String key) {
        List<Tuple> neighbors = new ArrayList<>();

        Path filePath = Paths.get(MASSBase.getWorkingDirectory(), networkFilename);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
            String line;

            while ((line = br.readLine()) != null) {
                String [] parts = line.split("\t");

                String lineKey = HIPPIETABFormatLineParts.getPart(parts, HIPPIETABFormatLineParts.PROTEIN_KEY);

                if (lineKey.equals(key)) {
                    HIPPIETABEdge edge = HIPPIETABEdge.fromParts(parts);

//                    int globalIndexForKey = MASSBase.getGlobalIndexForKey(edge.getInteractionKey());
//
//                    int neighborId = globalIndexForKey;

                    //Tuple neighbor = new Tuple(neighborId, edge.getInteractionAttribute());
                    Tuple neighbor = new Tuple(edge.getInteractionKey(), edge.getInteractionAttribute());

                    neighbors.add(neighbor);
                }
            }
        } catch (FileNotFoundException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            MASSBase.getLogger().error(sw.toString());
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            MASSBase.getLogger().error(sw.toString());
        }

        return neighbors;
    }

        public static List<Tuple> getNeighbors(String xmlFilename, int index) {
        List<Tuple> neighbors = new ArrayList<>();

        XPathFactory factory = XPathFactory.newInstance();

        XPath path = factory.newXPath();

        XPathExpression expression = null;

        try {
            expression = path.compile("/network/links/link[@from='" + (index + 1) + "']");

            NodeList nodeList = (NodeList) expression.evaluate(new InputSource(xmlFilename),
                    XPathConstants.NODESET);

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                NamedNodeMap attributes = node.getAttributes();

                String toString = attributes.getNamedItem("to").getNodeValue();
                String weightString = attributes.getNamedItem("length").getNodeValue();

//                Tuple neighbor = new Tuple(Integer.parseInt(toString),
//                        Double.parseDouble(weightString));
                
                Tuple neighbor = new Tuple(toString,
                        Double.parseDouble(weightString));

                neighbors.add(neighbor);
            }
        } catch (XPathExpressionException e) {
            MASSBase.getLogger().error("Exception parsing network xml: " + e.getMessage());
        }

        return neighbors;
    }

    private void init_neighbors_matsim(String networkFilename, int index) {
        Path filePath = Paths.get(MASSBase.getWorkingDirectory(), networkFilename);

        MASSBase.getLogger().debug(String.format("VertexPlace::init_neighbors_matsim - filePath: %s", filePath));

        List<Tuple> neighbors = getNeighbors(networkFilename, index);

        for (Tuple neighbor : neighbors) {
            // Network file is 1 based. Shift to 0 based.
            //this.neighbors.add((Integer) neighbor.index - 1);
            this.neighbors.add(neighbor.index);

            // TODO: Refactor weights to double
            // this.weights.add(neighbor.weight);

            int weight = 1;

            try {
                weight = (int)Math.round(neighbor.weight);
            } catch (Exception e) {
                MASSBase.getLogger().warning("Exception parsing double to integer: " + e.getMessage());
            }

            this.weights.add(weight);
        }
    }

    private void init_neighbors_parallel(String neighborFilePath, int index) {
        Path filePath = Paths.get(MASSBase.getWorkingDirectory(), neighborFilePath);

        MASSBase.getLogger().debug(String.format("VertexPlace::init_neighbors - filePath: %s", filePath));

        // try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
        try {
            int fd = open(filePath.toString(), 0);

            // Trim the input to avoid number format exception on last element
            String line = new String(read(fd)).trim();

            String[] parts = line.split(",\\s*"); // remove comma and trailing whitespace

            if (parts[0].trim().equals(Integer.toString(index))) {
                for (int i = 1; i < parts.length; i += 2) {
                    neighbors.add(Integer.parseInt(parts[i]));
                    weights.add(Integer.parseInt(parts[i + 1]));
                }
            } else {
                String message = String.format("Place received incorrect input: { place: %d, line: %s }", getIndex()[0], line);

                MASSBase.getLogger().error(message);

                throw new IOException(message);
            }
        } catch (NumberFormatException nfe) {
            StringWriter sw = new StringWriter();
            nfe.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        } catch (FileNotFoundException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
            MASSBase.getLogger().error(" -- IOException: " + e.getMessage());
        } catch (UnsupportedFileTypeException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        } catch (InterruptedException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        } catch (InvalidRangeException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        } catch (InvalidNumberOfNodesException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        } catch (InvalidNumberOfPlacesException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();

            MASSBase.getLogger().error("Init_neighbors error: " + exceptionAsString);
        }
    }

    public Object getAttribute() {
        return MASSBase.distributed_map.get(getIndex()[0]);
    }
}
