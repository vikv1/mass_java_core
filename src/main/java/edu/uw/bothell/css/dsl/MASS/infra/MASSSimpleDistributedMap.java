package edu.uw.bothell.css.dsl.MASS.infra;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MASSSimpleDistributedMap<key_type, value_type> implements DistributedMap<key_type, value_type> {
    @Override
    public void close() throws IOException {

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object o) {
        return false;
    }

    @Override
    public boolean containsValue(Object o) {
        return false;
    }

    @Override
    public value_type get(Object o) {
        return null;
    }

    @Override
    public value_type put(key_type key_type, value_type value_type) {
        return null;
    }

    @Override
    public value_type remove(Object o) {
        return null;
    }

    @Override
    public void putAll(Map<? extends key_type, ? extends value_type> map) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<key_type> keySet() {
        return null;
    }

    @Override
    public Collection<value_type> values() {
        return null;
    }

    @Override
    public Set<Entry<key_type, value_type>> entrySet() {
        return null;
    }
}
