package edu.uw.bothell.css.dsl.MASS.infra;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class HazelcastDistributedMap implements DistributedMap {
    private final IMap<Object, Object> map;

    private HazelcastDistributedMap() {
        Config config = new Config();

        config.setProperty("hazelcast.logging.type", "log4j2");
        config.getNetworkConfig().setPort(10101);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        this.map = instance.getMap("base_map");
    }

    public static HazelcastDistributedMap getInstance() {
        return new HazelcastDistributedMap();
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
    public Object get(Object o) {
        return map.get(o);
    }

    @Override
    public Object put(Object o, Object o2) {
        return map.put(o, o2);
    }

    @Override
    public Object remove(Object o) {
        return map.remove(o);
    }

    @Override
    public void putAll(Map map) {
        this.map.putAll(map);
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return this.map.entrySet();
    }
}
