/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.infra;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.messaging.MASSReqResp;

public class MASSSimpleDistributedMap<key_type, value_type> extends MASSReqResp implements DistributedMap<key_type, value_type> {
    // map is the hash map used to store key/value pairs on this node.
    protected Map<key_type, value_type> map = new ConcurrentHashMap<key_type, value_type>();

    // nodeKeys contains the hash keys in the hash ring for all nodes.
    protected TreeMap<Long, Integer> nodeKeys;

    // md is the MessageDigest used to hash keys to determine their location
    // in the hash ring.
    protected MessageDigest md = null;

    // KEYS_PER_NODE is the number of key entries per node in the consistent
    // hash ring for each node in the cluster.
    public static final int KEYS_PER_NODE = 20;

    // KEY_HASH_ALGORITHM is the algorithm used to hash nodes and keys.
    public static final String KEY_HASH_ALGORITHM = "SHA-256";

    /**
     * MASSSimpleDistributedMap constructs a simple distributed map using
     * consistent hashing to distribute key/value pairs throughout the 
     * MASS cluster.
     */
    public MASSSimpleDistributedMap() {
        // Initialize message digest instance
        try {
            this.md = MessageDigest.getInstance(KEY_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            MASS.getLogger().error("invalid hash algorithm provided to node key hasher: " + KEY_HASH_ALGORITHM);

            return;
        }

        // Initialzie node keys for hash ring
        this.nodeKeys = initializeNodeKeys(MASS.getSystemSize(), KEYS_PER_NODE, this.md);
    }

    public < T extends Serializable, R extends Serializable > Function< T, R > getRequestHandler() {
        return ((T msg) -> {
            // call the appropriate function and return the value if necessary.
            // get, put, etc...
            
            return null;
        });
    }

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
    public value_type get(Object key) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, this.md);
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // if we don't own the key, send it to the appropriate node.
        if (owningNode != MASS.getMyPid()) {
            MASS.getLogger().error("remote node targeted but not supported. getting k/v from local map.");
            
            return this.map.get(key);
        }

        return map.get(key);
    }

    @Override
    public value_type put(key_type key, value_type value) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, this.md);
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // if we don't own the key, send it to the appropriate node.
        if (owningNode != MASS.getMyPid()) {
            MASS.getLogger().error("remote node targeted but not supported. adding k/v to local map.");
            
            return this.map.put(key, value);
        }

        return map.put(key, value);
    }

    @Override
    public value_type remove(Object key) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, this.md);
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // if we don't own the key, send it to the appropriate node.
        if (owningNode != MASS.getMyPid()) {
            MASS.getLogger().error("remote node targeted but not supported. removing k/v from local map.");
            
            return this.map.remove(key);
        }

        return map.remove(key);
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

    @Override
    public key_type reverseLookup(value_type value) {
        Optional<Entry<key_type, value_type>> option =
                this.map.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(value))
                        .findFirst();

        return option.isPresent() ? option.get().getKey() : null;
    }

    /**
     * Retrieves the owning node of the provided key digest.
     * 
     * @param keyDigest The digest of the key that's being looked up.
     * @param nodeKeys A TreeMap of node hashes to their respective owners.
     * 
     * @return The ID of the node that owns the provided hash.
     */
    protected static Integer getOwner(Long keyDigest, TreeMap<Long, Integer> nodeKeys) {
        // get owner key entry
        Entry<Long, Integer> keyEntry = nodeKeys.ceilingEntry(keyDigest);

        // If there isn't a ceiling node entry then we're on the edge.
        // Wrap around to the beginning.
        if (keyEntry == null) {
            keyEntry = nodeKeys.firstEntry();
        }

        return keyEntry.getValue();
    }

    /**
     * hashObject returns a Long representation of the provided objects
     * hash, using the provided MessageDigest.
     * 
     * @param obj The object to be hashed.
     * @param md The MessageDigest used to hash the object.
     * @return The long representation of the provided objects message digest.
     */
    protected static Long hashObject(Object obj, MessageDigest md) {
        // reset provided message digester
        md.reset();

        BigInteger objHashCode = BigInteger.valueOf(obj.hashCode());
        byte[] hash = md.digest(objHashCode.toByteArray());

        // Take first 8 bytes as key value
        ByteBuffer wrapped = ByteBuffer.wrap(hash);

        return wrapped.getLong();
    }

    /**
     * Initializes the node keys assocaited with each MASS node in the hash ring.
     * Each node gets 'keysPerNodes' entries into the ring.
     * 
     * @param numNodes The number of nodes in the system.
     * @param keysPerNode The number of keys allocated to each node in the system.
     * @param md The MessageDigest used to generate entries into the hash ring
     * for each node.
     * 
     * @return A TreeMap mapping each hash to its associated node in the cluster.
     */
    protected static TreeMap<Long, Integer> initializeNodeKeys(int numNodes, int keysPerNode, MessageDigest md) {
        TreeMap<Long, Integer> nodeKeys = new TreeMap<Long, Integer>();

        // Map list of keys to each system in cluster.
        HashMap<Integer, ArrayList<String>> keys = new HashMap<Integer, ArrayList<String>>();
        for (int i = 0; i < numNodes; i++) {
            ArrayList<String> nKeys = new ArrayList<String>();
            String nodePID = Integer.toString(i);

            // Generate 'keysPerNode' keys for each node by simply appending
            // an '@' to the end of them.
            for (int j = 0; j < keysPerNode; j++) {
                StringBuilder sb = new StringBuilder();
                sb.append(nodePID);

                // Append 'j' @ to key
                for (int k = 0; k < j; k++) {
                    sb.append("@");
                }

                nKeys.add(sb.toString());
            }

            keys.put(i, nKeys);
        }

        // For each node key, we need to generate its hashcode but it needs
        // to be uniform across our key space so we can't rely on the default
        // java String hashcode.
        for (Integer node : keys.keySet()) {
            for (String key : keys.get(node)) {
                byte [] hash = md.digest(key.getBytes(StandardCharsets.US_ASCII));
                md.reset();

                // Take first 8 bytes as key value
                ByteBuffer wrapped = ByteBuffer.wrap(hash);
                Long nodeKey = wrapped.getLong();
                nodeKeys.put(nodeKey, node);
            }
        }

        return nodeKeys;
    }
}
