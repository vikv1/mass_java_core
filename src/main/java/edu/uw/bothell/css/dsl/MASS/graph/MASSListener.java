package edu.uw.bothell.css.dsl.MASS.graph;

public interface MASSListener {
    /**
     * Wait for completion before closing
     */
    void finish();

    void registerProcessor(String key, GraphRequest requestProcessor);
}
