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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Collection;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.concurrent.TimeUnit;
import java.io.Serializable;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.annotations.OnMessage;
import edu.uw.bothell.css.dsl.MASS.messaging.NodeToNodeMessenger;
import edu.uw.bothell.css.dsl.MASS.messaging.NodeToNodeMessage;

public class MASSSimpleDistributedMap<key_type, value_type> extends NodeToNodeMessenger<MASSMapMsg> implements Serializable, DistributedMap<key_type, value_type> {
    // map is the hash map used to store key/value pairs on this node.
    protected Map<key_type, value_type> map = new ConcurrentHashMap<key_type, value_type>();

    // nodeKeys contains the hash keys in the hash ring for all nodes.
    protected TreeMap<Long, Integer> nodeKeys;

    // md is the MessageDigest used to hash keys to determine their location
    // in the hash ring.
    // protected MessageDigest md = null;

    // KEYS_PER_NODE is the number of key entries per node in the consistent
    // hash ring for each node in the cluster.
    public static final int KEYS_PER_NODE = 20;

    // KEY_HASH_ALGORITHM is the algorithm used to hash nodes and keys.
    public static final String KEY_HASH_ALGORITHM = "SHA-256";

    // RequestTimeoutInSeconds is the number of seconds the distributed map will
    // wait for a reply from a remote node before concluding that the underlying
    // request has failed.
    private int RequestTimeoutInSeconds = 10;

    /**
     * MASSSimpleDistributedMap constructs a simple distributed map using
     * consistent hashing to distribute key/value pairs throughout the 
     * MASS cluster.
     */
    public MASSSimpleDistributedMap() {
        super();

        // Initialize message digest instance
        /*
        try {
            this.md = MessageDigest.getInstance(KEY_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            MASS.getLogger().error("invalid hash algorithm provided to node key hasher: " + KEY_HASH_ALGORITHM);

            return;
        }
        */

        // Initialzie node keys for hash ring
        this.nodeKeys = initializeNodeKeys(MASS.getSystemSize(), KEYS_PER_NODE, getMessageDigest());
    }

    /**
     * Constructs a new instance of MASSSimpleDistributedMap and sets the request
     * timeout to the value provided.
     * 
     * @param timeout The request timeout in seconds. The distributed map will wait
     * at most <timeout> seconds for a request to a remote node to complete before
     * deeming it a failure.
     */
    public MASSSimpleDistributedMap(int timeout) {
        this();

        this.RequestTimeoutInSeconds = timeout;
    }

    @Override
    public void clear() {
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();

        // send requests to all nodes, asking them to clear their maps.
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.CLEAR
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Overlap communication with clearing our own map.
        this.map.clear();

        // Wait for response from each node
        Boolean success = true;
        for (Integer id : nodes) {
            try {
                success &= resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).success;
            } catch (InterruptedException e) {
                MASS.getLogger().error("clear request timed out waiting for reply from other node.");
            }
        }

        if (!success) {
            MASS.getLogger().error("Not all nodes successfully cleared their map.");
        }

        return;
    }

    @Override
    public boolean containsKey(Object key) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, getMessageDigest());
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // If we own the key, there's no need to send requests to other nodes.
        if (owningNode == MASS.getMyPid()) { return this.map.containsKey(key); }

        // Otherwise, ask the node that should own the key, whether its local map
        // contains it.
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        this.sendRequest(
            owningNode, 
            new MASSMapMsg(
                MASSMapMsg.Function.CONTAINS_KEY,
                key
            ), 
            (msg) -> {
                resultQueue.add(msg);
            }
        );

        try {
            return (Boolean) resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
        } catch (InterruptedException e) {
            MASS.getLogger().error("containsKey request timed out waiting for reply from other node.");
        }
        
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        // If the local node contains the value, we can short-circuit and
        // return true.
        if ( this.map.containsValue(value) ) { return true; }
        
        // Otherwise, ask other nodes if their local map contains the value.
        boolean containsVal = false;
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.CONTAINS_VALUE,
                    value
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Wait for response from each node
        for (Integer id : nodes) {
            try {
                containsVal |= (Boolean)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
            } catch (InterruptedException e) {
                MASS.getLogger().error("containsValue request timed out waiting for reply from other node.");
            }
        }

        return containsVal;
    }

    @Override
    public Set<Entry<key_type, value_type>> entrySet() {
        Set<Entry<key_type, value_type>> entries = new HashSet<Entry<key_type, value_type>>();

        // Issue keyset requests to all nodes
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.ENTRY_SET
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Overlap communication with the proccessing of our local map entryset.
        entries.addAll(this.map.entrySet());

        // Wait for response from each node. Note that the entire map is needed because
        // Map.Entry is not serializable.
        for (Integer id : nodes) {
            ConcurrentHashMap<key_type, value_type> remoteMap = null;
            try {
                remoteMap = (ConcurrentHashMap<key_type, value_type>)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
            } catch (InterruptedException e) {
                MASS.getLogger().error("entrySet request timed out waiting for reply from other nodes.");

                return null;
            }

            entries.addAll(remoteMap.entrySet());
        }
        
        return entries;
    }

    @Override
    public value_type get(Object key) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, getMessageDigest());
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // If we own the key, return the value
        if (owningNode == MASS.getMyPid()) { return this.map.get(key); }

        // Otherwise, ask the node that should own the key for its value.
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        this.sendRequest(
            owningNode, 
            new MASSMapMsg(
                MASSMapMsg.Function.GET,
                key
            ), 
            (msg) -> {
                resultQueue.add(msg);
            }
        );

        try {
            return (value_type)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
        } catch (InterruptedException e) {
            MASS.getLogger().error("get request timed out waiting for reply from other node.");
        }
        
        return null;
    }

    @Override
    public boolean isEmpty() {
        boolean empty = this.map.isEmpty();

        // If our local map isn't empty, we can short circuit and return false.
        if (!empty) { return false; }

        // Otherwise, ask other nodes if their local maps are empty.
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.IS_EMPTY
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Wait for response from each node
        for (Integer id : nodes) {
            try {
                empty &= (Boolean)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
            } catch (InterruptedException e) {
                MASS.getLogger().error("isEmpty request timed out waiting for reply from other nodes.");
            }
        }

        return empty;
    }

    @Override
    public Set<key_type> keySet() {
        Set<key_type> keys = new HashSet<key_type>();
        
        // Request keysets from all remote nodes.
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.KEY_SET
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Overlap communication with processing our local keySet.
        keys.addAll(this.map.keySet());

        // Wait for keyset from each node
        for (Integer id : nodes) {
            Set<key_type> remoteKeys = null;
            try {
                remoteKeys = (Set<key_type>)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
            } catch (InterruptedException e) {
                MASS.getLogger().error("keySet request timed out waiting for reply from other nodes.");

                return null;
            }

            remoteKeys.forEach((element) -> {
                keys.add(element);
            });
        }
        
        return keys;
    }

    @Override
    public value_type put(key_type key, value_type value) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, getMessageDigest());
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // If we own the key, put the k/v pair into our local map.
        if (owningNode == MASS.getMyPid()) { return this.map.put(key, value); }
            
        // Otherwise, ask the node that should own the key to place it into its local map.
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        this.sendRequest(
            owningNode, 
            new MASSMapMsg(
                MASSMapMsg.Function.PUT,
                key,
                value
            ), 
            (msg) -> {
                resultQueue.add(msg);
            }
        );

        try {
            return (value_type)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
        } catch (InterruptedException e) {
            MASS.getLogger().error("put request timed out waiting for reply from other node.");
        }
        
        return null;
    }

    @Override
    public void putAll(Map<? extends key_type, ? extends value_type> map) {
        HashMap<Integer, Map<key_type, value_type>> mapToNodes = new HashMap<Integer, Map<key_type, value_type>>();
        int systemSize = MASS.getSystemSize();
        
        // Create maps to distribute values across the nodes that own their respective keys.
        for (int i = 0; i < systemSize; i++) {
            mapToNodes.put(i, new HashMap<key_type, value_type>());
        }

        // group KV pairs by owning node.
        Long keyDigest;
        Integer owningNode;
        for (Map.Entry<? extends key_type, ? extends value_type> entry : map.entrySet()) {
            keyDigest = hashObject(entry.getKey(), getMessageDigest());
            owningNode = getOwner(keyDigest, this.nodeKeys);

            mapToNodes.get(owningNode).put(entry.getKey(), entry.getValue());
        }

        // Send each node its map of KV pairs.
        ArrayList<Integer> nodeReqs = new ArrayList<Integer>();
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            // If this node doesn't have any KV pairs, skip it. No need for
            // unnecessary communication.
            if (mapToNodes.get(id).isEmpty()) { continue; }

            // Record which nodes we send to so we know how many responses we should get.
            nodeReqs.add(id);
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.PUT_ALL,
                    mapToNodes.get(id)
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Overlap communication with the processing of our local KV pairs.
        if (!mapToNodes.get(MASS.getMyPid()).isEmpty()) {
            this.map.putAll(mapToNodes.get(MASS.getMyPid()));
        }

        // Wait for all nodes to complete the putAll function.
        Boolean success = true;
        for (Integer id : nodeReqs) {
            try {
                success &= resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).success;
            } catch (InterruptedException e) {
                MASS.getLogger().error("putAll request timed out waiting for reply from other node.");
            }
        }

        if (!success) {
            MASS.getLogger().error("Not all nodes completed the putAll function successfully.");
        }

        return;
    }

    @Override
    public value_type remove(Object key) {
        // get owning node of provided key
        Long keyDigest = hashObject(key, getMessageDigest());
        Integer owningNode = getOwner(keyDigest, this.nodeKeys);

        // If we own the key, remove it from our local map.
        if (owningNode == MASS.getMyPid()) { return this.map.remove(key); }

        // Otherwise, ask the node that should own it to remove it.
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        this.sendRequest(
            owningNode, 
            new MASSMapMsg(
                MASSMapMsg.Function.REMOVE,
                key
            ), 
            (msg) -> {
                resultQueue.add(msg);
            }
        );

        try {
            return (value_type)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
        } catch (InterruptedException e) {
            MASS.getLogger().error("remove request timed out waiting for reply from other node.");
        }
        
        return null;
    }

    @Override
    public key_type reverseLookup(value_type value) {
        // Do a reverse lookup on our local map first and immediately return the 
        // key if our map contains it.
        key_type key = this.reverseMapLookup(value);
        if (key != null) { return key; }
        
        // Otherwise, ask other nodes for the matching key
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.REVERSE_LOOKUP,
                    value
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Wait for the remote nodes to return their reverseLookup results.
        key_type remoteKey = null;
        for (Integer id : nodes) {
            try {
                key_type retVal = (key_type)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
                if (retVal != null) { remoteKey = retVal; }
            } catch (InterruptedException e) {}
        }

        return remoteKey;
    }

    @Override
    public int size() {
        int size = this.map.size();

        // Ask each node for the size of its local map.
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.SIZE
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Wait for response from each node
        for (Integer id : nodes) {
            MASSMapMsg res = null;
            try {
                res = resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                MASS.getLogger().error("size request timed out waiting for reply from other nodes.");
                return -1;
            }

            size += (Integer)res.returnValue;
        }

        return size;
    }

    @Override
    public Collection<value_type> values() {
        HashSet<value_type> valueCollection = new HashSet<value_type>();
        
        // Issue values requests to all nodes
        ArrayList<Integer> nodes = getRemoteNodeIDs();
        LinkedBlockingQueue<MASSMapMsg> resultQueue = new LinkedBlockingQueue<MASSMapMsg>();
        for (Integer id : nodes) {
            this.sendRequest(
                id, 
                new MASSMapMsg(
                    MASSMapMsg.Function.VALUES
                ), 
                (msg) -> {
                    resultQueue.add(msg);
                }
            );
        }

        // Overlap communication with processing our local map values.
        valueCollection.addAll(this.map.values());

        // Wait for response from each node. Note that the entire map is needed because
        // Map.Entry is not serializable.
        for (Integer id : nodes) {
            HashSet<value_type> remoteValues = null;
            try {
                remoteValues = (HashSet<value_type>)resultQueue.poll(this.RequestTimeoutInSeconds, TimeUnit.SECONDS).returnValue;
            } catch (InterruptedException e) {
                MASS.getLogger().error("keySet request timed out waiting for reply from other nodes.");

                return null;
            }

            valueCollection.addAll(remoteValues);
        }
        
        return valueCollection;
    }

    

    

    // OnMessage simply calls the OnMessage function of our super class.
    // The distmap shouldn't touch this. It's only needed because the
    // OnMessage annotation is unable to identify/call annotated functions
    // from parent classes, so we need to propagate the request.
    @OnMessage
    public void OnMessage(NodeToNodeMessage<MASSMapMsg> msg) {
        super.OnMessage(msg);
    }

    // getRequestHandler returns a function that is capable of handling 
    // different DistributedMap request messages based on their function
    // type.
    @Override
    public Function<MASSMapMsg, MASSMapMsg> getRequestHandler() {
        return ((MASSMapMsg msg) -> {
            // call the appropriate function and return the value if necessary.
            // get, put, etc...
            switch (msg.func) {
                case CLEAR:
                    this.map.clear();
                    msg.success = true;

                    return msg;

                case CONTAINS_KEY:
                    msg.returnValue = Boolean.valueOf(this.map.containsKey(msg.arg1));
                    msg.success = true;

                    return msg;

                case CONTAINS_VALUE:
                    msg.returnValue = Boolean.valueOf(this.map.containsValue(msg.arg1));
                    msg.success = true;

                    return msg;
                
                case ENTRY_SET:
                    msg.returnValue = this.map;
                    msg.success = true;

                    return msg;

                case GET:
                    msg.returnValue = this.map.get(msg.arg1);
                    msg.success = true;

                    return msg;

                case IS_EMPTY:
                    msg.returnValue = Boolean.valueOf(this.map.isEmpty());
                    msg.success = false;

                    return msg;

                case KEY_SET:
                    HashSet<key_type> keys = new HashSet<key_type>();
                    keys.addAll(this.map.keySet());
                    msg.returnValue = keys;
                    msg.success = true;

                    return msg;

                case PUT:
                    msg.returnValue = this.map.put((key_type)msg.arg1, (value_type)msg.arg2);
                    msg.success = true;

                    return msg;

                case PUT_ALL:
                    Map<key_type, value_type> map = (Map<key_type, value_type>) msg.arg1;
                    this.map.putAll(map);
                    msg.success = true;

                    return msg;

                case REMOVE:
                    msg.returnValue = this.map.remove(msg.arg1);
                    msg.success = true;

                    return msg;

                case REVERSE_LOOKUP:
                    msg.returnValue = this.reverseMapLookup((value_type)msg.arg1);
                    msg.success = true;

                    return msg;
                
                case SIZE:
                    msg.returnValue = Integer.valueOf(this.map.size());
                    msg.success = true;

                    return msg;

                case VALUES:
                    HashSet<value_type> vals = new HashSet<value_type>();
                    vals.addAll(this.map.values());
                    msg.returnValue = vals;
                    msg.success = true;
                    
                    return msg;
            }

            msg.success = false;
            msg.errormsg = "unknown function";

            return msg;
        });
    }

    /**
     * getRemoteNodeIDs returns an array of remote node IDs.
     */
    private ArrayList<Integer> getRemoteNodeIDs() {
        ArrayList<Integer> remoteNodeIDs = new ArrayList<Integer>();
        int myID = MASS.getMyPid();
        int systemSize = MASS.getSystemSize();

        for (int i = 0; i < systemSize; i++) {
            if (i == myID) { continue; }

            remoteNodeIDs.add(i);
        }

        return remoteNodeIDs;
    }

    /**
     * reverseMapLookup performs a linear reverse map lookup on the calling
     * nodes local map.
     * 
     * @param value The value for which the caller would like the key to.
     *
     * @return The key associated with the provided value, or null if the value
     * cannot be found.
     */
    private key_type reverseMapLookup(value_type value) {
        for (Entry<key_type, value_type> entry : this.map.entrySet()) {
            if (entry.getValue() == value) {
                return entry.getKey();
            }
        }

        return null;
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

    protected MessageDigest getMessageDigest() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(KEY_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            MASS.getLogger().error("invalid hash algorithm provided to node key hasher: " + KEY_HASH_ALGORITHM);

            return md;
        }
        return md;
    }
}
