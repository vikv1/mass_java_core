package edu.uw.bothell.css.dsl.MASS.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import edu.uw.bothell.css.dsl.MASS.AbstractTest;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.MNode;

public class MASSSimpleDistributedMapTest extends AbstractTest {
    @BeforeAll
    public static void beforeAll() {
        resetMASSBase();
        MNode masterNode = new MNode();
        masterNode.setHostName( randomString() );
        masterNode.setMaster( true );
        MASSBase.addNode( masterNode );
        MASSBase.initMASSBase( masterNode );
    }

    @AfterAll
    public static void afterAll() {
        resetMASSBase();
    }

    @Test
    public void TestNew() throws Exception {
        MASSSimpleDistributedMap<String, String> tut = new MASSSimpleDistributedMap<String, String>();
        assertNotNull(tut);
        assertNotNull(tut.map);
        assertNotNull(tut.nodeKeys);
        //assertNotNull(tut.md);
    }

    @Test
    public void TestPut() throws Exception {
        MASSSimpleDistributedMap<String, String> tut = new MASSSimpleDistributedMap<String, String>();
        assertNotNull(tut);
        
        String key = "foo";
        String val = "bar";
        tut.put(key, val);

        assertTrue(tut.map.containsKey(key));
        assertEquals(val, tut.map.get(key));

        tut.close();
    }

    @Test
    public void TestGet() throws Exception {
        MASSSimpleDistributedMap<String, String> tut = new MASSSimpleDistributedMap<String, String>();
        assertNotNull(tut);
        
        String key = "foo";
        String val = "bar";
        tut.map.put(key, val);

        assertEquals(val, tut.get(key));

        tut.close();
    }

    @Test
    public void TestRemove() throws Exception {
        MASSSimpleDistributedMap<String, String> tut = new MASSSimpleDistributedMap<String, String>();
        assertNotNull(tut);

        String key = "foo";
        String val = "bar";
        tut.map.put(key, val);

        String got = tut.remove(key);
        assertEquals(val, got);
        assertFalse(tut.map.containsKey(key));
    }
    
    @ParameterizedTest(name = "{index} => numNodes={0}, numKeys={1}, totalKeys={2}")
    @CsvSource({
        "1, 1, 1",
        "1, 20, 20",
        "4, 1, 4",
        "4, 20, 80"
    })
    public void TestInitializeNodeKeys(int numNodes, int numKeys, int totalKeys) throws Exception{
        MessageDigest md = MessageDigest.getInstance(MASSSimpleDistributedMap.KEY_HASH_ALGORITHM);
        TreeMap<Long, Integer> got = MASSSimpleDistributedMap.initializeNodeKeys(numNodes, numKeys, md);

        assertEquals(got.size(), totalKeys);
    }

    @Test
    public void TestHashObject() throws Exception {
        MessageDigest md = MessageDigest.getInstance(MASSSimpleDistributedMap.KEY_HASH_ALGORITHM);

        Integer tut1 = Integer.valueOf(42);
        Integer tut2 = Integer.valueOf(9000);

        Long got1 = MASSSimpleDistributedMap.hashObject(tut1, md);
        Long got2 = MASSSimpleDistributedMap.hashObject(tut2, md);

        assertNotEquals(got1, got2);
    }

    @ParameterizedTest(name = "{index} => keyDigest={0}, owner={1}")
    @CsvSource({
        // Owned by node 0
        "-13, 0",
        "-10, 0",

        // Owned by node 1
        "-5, 1",
        "0, 1",

        // Owned by node 2
        "4, 2",
        "10, 2",

        // Wrap around to node 0
        "15, 0"
    })
    public void TestGetOwner(Long keyDigest, int owner) throws Exception {
        TreeMap<Long, Integer> nodeKeys = new TreeMap<Long, Integer>();

        nodeKeys.put(Long.valueOf(-10), 0);
        nodeKeys.put(Long.valueOf(0), 1);
        nodeKeys.put(Long.valueOf(10), 2);

        int got = MASSSimpleDistributedMap.getOwner(keyDigest, nodeKeys);
        assertEquals(owner, got);
    }
}
