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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    @Override
    public key_type reverseLookup(value_type value) {
        Optional<Entry<key_type, value_type>> option =
                this.map.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(value))
                        .findFirst();

        return option.isPresent() ? option.get().getKey() : null;
    }
}
