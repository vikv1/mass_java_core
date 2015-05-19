package edu.uw.bothell.css.dsl.MASS.motif;

// GraphCrawler.java
//
// by Matt Kipps
// 12/12/14

import java.io.Serializable;

import edu.uw.bothell.css.dsl.MASS.*;

public class GraphCrawler extends Agent {

    public static final int update_ = 0;
    public Object callMethod(int functionID, Object args) {
        switch(functionID) {
            case update_:
                update();
                break;
            default:
                break;
        }
        return null;
    }

    private int migrateTo;
    private Subgraph subgraph;
    private AdjacencyList extension;

    public GraphCrawler(Object rawArgs) {
        super();

        Constructor constructor = (Constructor)rawArgs;
        this.migrateTo = constructor.getMigrateTo();
        this.subgraph  = constructor.getSubgraph();
        this.extension = constructor.getExtension();
    }

    public void update() {
        if (migrateTo != -1) {
            migrateAsync(migrateTo);
            migrateTo = -1;
            return;
        }

        // 'v' is equal to the subgraph root, unless it is empty, then
        // 'v' is equal to 'w' ('w' is the current node).
        GraphNode node = getNode();
        int w = node.getFirstIndex();
        int v;
        if (subgraph.size() == 0) {
            v = w;
        } else {
            v = subgraph.root();
        }

        // add the current node to the subgraph
        if (subgraph.size() == subgraph.order() - 1) {
            subgraph.add(w, node.getAdjacencyList());
            node.addToSubgraphs(subgraph);
            killAsync();
            return;
        }

        // examine each node 'u' from the set of nodes adjacent to 'w',
        // and add it to the next extension if it is exclusive to the
        // subgraph, and greater than 'v'
        CompactHashSet.Iter uIter = node.getAdjacencyList().iterator();
        while (uIter.hasNext()) {
            int u = uIter.next();
            if (u > v) {
                if (subgraph.excludes(u)) {
                    extension.add(u);
                }
            }
        }

        if (extension.isEmpty()) {
            killAsync();
            return;
        }

        // add the current node to the subgraph
        subgraph.add(w, node.getAdjacencyList());

        // extend the subgraph
        CompactHashSet.Iter iter = extension.iterator();
        if (extension.size() > 1) {
            Object[] spawnParams = new Object[extension.size() - 1];
            for (int i = 0; i < spawnParams.length; i++) {
                int spawnAt = iter.next();
                iter.remove();
                spawnParams[i] = (Object)(new GraphCrawler.Constructor(
                    spawnAt,
                    subgraph.copy(),
                    (subgraph.size() < subgraph.order() - 1) ? extension.copy() : null));
            }

            spawnAsync(spawnParams.length, spawnParams, null);
        }

        // pick the destination for this crawler
        int destination = iter.next();
        iter.remove();

        // migrate to the new destination
        migrateAsync(destination);
    }

    private GraphNode getNode() {
        return (GraphNode)getPlace();
    }

    // the nested Constructor class simplifies the instantiation of GraphCrawler
    // objects through MASS library calls
    public static class Constructor implements Serializable {
        private int migrateTo;
        private int motifSize;
        private Subgraph subgraph;
        private AdjacencyList extension;

        public Constructor(int motifSize) {
            this(-1, motifSize, null, null);
        }

        public Constructor(
            int migrateTo,
            Subgraph subgraph,
            AdjacencyList extension) {
            this(migrateTo, subgraph.order(), subgraph, extension);
        }

        private Constructor(
            int migrateTo,
            int motifSize,
            Subgraph subgraph,
            AdjacencyList extension) {
            this.migrateTo = migrateTo;
            this.motifSize = motifSize;
            this.subgraph  = subgraph;
            this.extension = extension;
        }


        public int getMigrateTo() {
            return migrateTo;
        }

        public Subgraph getSubgraph() {
            if (subgraph == null) {
                return new Subgraph(motifSize);
            } else {
                return subgraph;
            }
        }

        public AdjacencyList getExtension() {
            if (extension == null) {
                return new AdjacencyList();
            } else {
                return extension;
            }
        }
    }
}
