/*

 	MASS Java Software License
	© 2012-2024 University of Washington

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

package edu.uw.bothell.css.dsl.MASS.graph;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.driver.exceptions.*;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;
import org.apache.commons.lang3.SerializationUtils;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.Message;
import edu.uw.bothell.css.dsl.MASS.VertexPlace;
import edu.uw.bothell.css.dsl.MASS.GraphPlaces;


public class LocalMessageReaderThread extends Thread {
    private static final int FRAGMENT_COUNT_LIMIT = 10;
    private GraphPlaces graph = null;
    private Subscription sub = null;
    private final IdleStrategy idle = new SleepingIdleStrategy();
    private int[] alive;
    private boolean initialized = false;
    
    public LocalMessageReaderThread(GraphPlaces graph, Subscription sub, int[] alive) {
        this.graph = graph;
        this.sub = sub;
        this.alive = alive;
    }

    @Override
    public void run() {
        // create handler for the sub
        FragmentHandler handler = (buffer, offset, length, header) -> {
            // get the message object
            byte[] byteArray = new byte[length];
            buffer.getBytes(offset, byteArray);
            Message m = (Message) SerializationUtils.deserialize(byteArray);
            if (m.getSharedPlaceName().equals(graph.getSharedPlaceName())) {
                // this message is sent for my shared place
                if (!m.getUserName().equals(MASSBase.getUserName())) {
                    // this message is not sent by myself
                    if (m.getAction() == Message.ACTION_TYPE.DSG_SET_VERTEX_LOCAL) {
                        graph.getGraphPlaces().set(m.getLocalIndex(), m.getVertex());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_ADD_VERTEX_LOCAL) {
                        graph.getGraphPlaces().add(m.getVertex());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_REMOVE_VERTEX_LOCAL) {
                        graph.getGraphPlaces().set(m.getLocalIndex(), null);
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_REMOVE_NEIGHBOR_LOCAL) {
                        Vector<VertexPlace> places = graph.getGraphPlaces();
                        int neighborID = m.getLocalIndex();
                        for (VertexPlace place : places) {
                            if (place == null) {
                                continue;
                            }
                            place.removeNeighborSafely(neighborID);
                        }
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_INIT_REQUEST) {
                        // someone sends a request, need to response to it
                        graph.helpOtherUsersInit(m.getUserName());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_INIT_RESPONSE && m.getReceiver().equals(MASSBase.getUserName()) && !initialized) {
                        // my request got an response, update the local data
                        initialized = true;
                        Vector<VertexPlace> places = m.getPlaces();
                        for (VertexPlace vp : places) {
                            vp.reinitialize();
                        }
                        graph.setPlaces(places);
                        MASSBase.setDistributedMap(m.getDistributedMap());
                        graph.setIdQueue(m.getIdQueue());
                        graph.setNextVertexID(m.getNextVertexID());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_DISTRIBUTED_MAP_PUT) {
                        MASSBase.distributed_map.put(m.getObjVertexID(), m.getIntVertexID());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_DISTRIBUTED_MAP_REMOVE) {
                        MASSBase.distributed_map.remove(m.getObjVertexID());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_IDQUEUE_REMOVE) {
                        graph.getIdQueue().remove();
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_IDQUEUE_ADD) {
                        graph.getIdQueue().add(m.getIntVertexID());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_NEXT_VERTEXID_UPDATE) {
                        graph.setNextVertexID(m.getIntVertexID());
                    }
                    else if (m.getAction() == Message.ACTION_TYPE.DSG_REINITIALIZE) {
                        graph.reinitializeLocal();
                    }
                }
            }
        };

        // assembler's job is to take fragmented messages (ones too large for a single packet)
        // and build a single message from it
        final FragmentAssembler assembler = new FragmentAssembler( handler );

        MASSBase.getLogger().debug("Local reader thread started, waiting for message");
        try {
            while(alive[0] == 1) {
                final int fragmentsRead = sub.poll(assembler, FRAGMENT_COUNT_LIMIT);
                idle.idle(fragmentsRead);
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                MASSBase.getLogger().debug("Thread interrupted!");
            } else {
                MASSBase.getLogger().debug("Unusual exception occured!" + e);
            }
        }
        MASSBase.getLogger().debug("Local reader thread terminated");
    }
}