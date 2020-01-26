package edu.uw.bothell.css.dsl.MASS.infra;

import java.io.Closeable;
import java.util.Map;

public interface DistributedMap<key_type, value_type> extends Map<key_type, value_type>, Closeable {

}
