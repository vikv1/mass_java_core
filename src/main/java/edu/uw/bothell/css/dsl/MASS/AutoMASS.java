package edu.uw.bothell.css.dsl.MASS;

import java.io.Closeable;
import java.io.IOException;

public class AutoMASS implements Closeable {
    public AutoMASS() {
        MASS.init();
    }

    @Override
    public void close() throws IOException {
        MASS.finish();
    }
}
