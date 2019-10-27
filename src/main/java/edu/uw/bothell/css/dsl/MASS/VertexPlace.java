package edu.uw.bothell.css.dsl.MASS;

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

        int firstIndex = 0;

        Path filePath = Paths.get(MASSBase.getWorkingDirectory(), neighborFilePath);

        MASSBase.getLogger().debug(String.format("VertexPlace::init_neighbors - filePath: %s", filePath));

        try (BufferedReader br = new BufferedReader(new FileReader(filePath.toString()))) {
            String line = br.readLine();

            // TODO: fix this duplicated code between neighbors and weights
            while (line != null && !line.isEmpty()) {
                String [] parts = line.split(",\\s*"); // remove comma and trailing whitespace

                if (parts[0].equals(Integer.toString(index))) {
                    for (int i = 1; i < parts.length; i += 2) {
                        neighbors.add(Integer.parseInt(parts[i]));
                        weights.add(Integer.parseInt(parts[i + 1]));
                    }
                }

                line = br.readLine();
            }
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
        }
    }
}
