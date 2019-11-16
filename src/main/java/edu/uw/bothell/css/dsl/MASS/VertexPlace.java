package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.Parallel_IO.InvalidNumberOfNodesException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.InvalidNumberOfPlacesException;
import edu.uw.bothell.css.dsl.MASS.Parallel_IO.UnsupportedFileTypeException;
import ucar.ma2.InvalidRangeException;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;

public class VertexPlace extends Place implements Serializable {
    private Object [] graphArguments;
    public Vector<Integer> neighbors = new Vector<>();
    public Vector<Integer> weights = new Vector<>();

    public int [] getNeighbors() {
        int [] result = new int[neighbors.size()];

        for (int i = 0; i < result.length; i++) {
            result[i] = neighbors.get(i);
        }

        return result;
    }

    public int [] getWeights() {
        int [] result = new int[weights.size()];

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
        
        Object [] arguments = (Object [])args;
        
        graphArguments = Arrays.copyOfRange(arguments, 0, 3);

        // TODO: Parallel IO requires the index to be set
        //  We might consider refactoring the neighbors to come after the constructor
        this.setIndex(new int[]{ (int)graphArguments[2] });

        init(args);

        MASSBase.getLogger().debug(String.format("VertexPlace constructed with args: { id: %d, neighbors: [%s], weights: [%s] }\n",
                graphArguments[2],
                this.neighbors.stream()
                        .map(n -> n.toString())
                        .collect(Collectors.joining(", ")),
                this.weights.stream()
                        .map(w -> w.toString())
                        .collect(Collectors.joining(", "))));
    }

    private void init(Object args) {
        Object [] arguments = (Object[])args;

        graphArguments = Arrays.copyOfRange(arguments, 0, 3);

        init_neighbors((String)graphArguments[0], (int)graphArguments[2]);
    }

    private void init_neighbors(String neighborFilePath, int index) {
        if (neighborFilePath == null) return;

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

                throw new IOException();
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
}
