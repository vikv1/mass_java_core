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

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hazelcast.config.Config;
import com.hazelcast.config.InterfacesConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.ManagementCenterConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.messaging.hazelcast.HazelcastMessagingProvider;

public class HazelcastDistributedMap implements DistributedMap, Closeable {
    private final IMap<Object, Object> map;
    private final HazelcastInstance instance;

    private HazelcastDistributedMap() {
        
    	// is there an instance of Hazelcast already available from the messaging system?
    	HazelcastInstance existingInstance = Hazelcast.getHazelcastInstanceByName( HazelcastMessagingProvider.HAZELCAST_INSTANCE_NAME );
    	if ( Objects.isNull( existingInstance ) ) {
    	
    		// no other instance of Hazelcast has been init'd
    		
	    	Config config = new Config();
	
	        config.setProperty("hazelcast.logging.type", "log4j2");
	        config.setProperty("hazelcast.logging.level", "ERROR");
	
	        //config.getNetworkConfig().setPort(10101);
	        //config.getNetworkConfig().setReuseAddress(true);
	
	        NetworkConfig netConfig = config.getNetworkConfig();
	
	        netConfig.setPort(10011).setPortCount(100);
	        netConfig.setPortAutoIncrement(true);
	        netConfig.setReuseAddress(true);
	
	        InterfacesConfig ifConfig = netConfig.getInterfaces();
	
	        MASSBase.getLogger().error(MASSBase.getAllNodes().stream().map(n->String.valueOf(n.getPid())).collect(Collectors.joining(",")));
	
	        try {
	            String hostname = MASSBase.getMyHostname();
	            String ipString = InetAddress.getByName(hostname).getHostAddress();
	
	            MASSBase.getLogger().error("This node [host=" + hostname + "; ip=" + ipString + "pid=" + MASSBase.getMyPid() + "]");
	            ifConfig.addInterface(ipString).setEnabled(true);
	        } catch (UnknownHostException e) {
	            MASSBase.getLogger().error("Error retrieving master IP address");
	
	            Arrays.stream(e.getStackTrace()).forEach(st -> MASSBase.getLogger().error(st.toString()));
	        }
	
	        JoinConfig joinConfig = netConfig.getJoin();
	
	        joinConfig.getMulticastConfig().setEnabled(false);
	
	        MASS.getHosts().forEach(host -> joinConfig.getTcpIpConfig().addMember(host));
	
	        joinConfig.getTcpIpConfig().setEnabled(true);
	
	        new ManagementCenterConfig().setEnabled(true).setUrl("http://localhost:11110");
	
	        instance = Hazelcast.newHazelcastInstance(config);

    	}
    	
    	else {
    		
    		// an instance of Hazelcast from the messaging system already exists - use that one
    		instance = existingInstance;
    		
    	}
    	
        this.map = instance.getMap("base_map");
    
    }

    @Override
    public void close() {
        if (instance != null) {
            instance.shutdown();
        }
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

    @Override
    public Object reverseLookup(Object value) {
        Optional<Entry<Object, Object>> option = this.map.entrySet().stream().filter(entry -> entry.getValue().equals(value)).findFirst();

        return option.isPresent() ? option.get().getKey() : null;
    }
}
