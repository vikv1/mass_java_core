package edu.uw.bothell.css.dsl.MASS.infra;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MASSSimpleDistributedMap<key_type, value_type> implements DistributedMap<key_type, value_type> {
    private Map<key_type, value_type> map = new HashMap<>();

    @Override
    public void close() throws IOException {

    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return map.containsValue(o);
    }

    @Override
    public value_type get(Object o) {
        return map.get(o);
    }

    @Override
    public value_type put(key_type key, value_type value) {
        return map.put(key, value);
    }

    @Override
    public value_type remove(Object o) {
        return map.remove(o);
    }

    @Override
    public void putAll(Map<? extends key_type, ? extends value_type> map) {
        this.map.putAll(map);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<key_type> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<value_type> values() {
        return map.values();
    }

    @Override
    public Set<Entry<key_type, value_type>> entrySet() {
        return map.entrySet();
    }
}
