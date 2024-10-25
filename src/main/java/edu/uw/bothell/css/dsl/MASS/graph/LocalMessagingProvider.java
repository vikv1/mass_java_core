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
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.CloseHelper;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;

import edu.uw.bothell.css.dsl.MASS.MASSBase;
import edu.uw.bothell.css.dsl.MASS.Message;
import edu.uw.bothell.css.dsl.MASS.VertexPlace;
import edu.uw.bothell.css.dsl.MASS.GraphPlaces;

public class LocalMessagingProvider {
    // aeron related variables
    private static final int MAX_MESSAGE_SIZE_BYTES = 16777216;
    private static final String AERON_FLOW_CONTROL_STRATEGY = "|fc=min";
    private static final String AERON_URL_PREFIX = "aeron:udp?endpoint=";
	private static final String AERON_URL_SUFFIX = "|term-length=128m";
    private String address = "239.239.239.1";
    private String port = "29030";
    private String channel = AERON_URL_PREFIX + address + ":" + port + AERON_URL_SUFFIX;
    private final IdleStrategy idle = new SleepingIdleStrategy();
    private UnsafeBuffer unsafeBuffer = null;
    private MediaDriver mediaDriver = null;
    private Aeron aeron = null;
    private Publication pub = null;
    private Subscription sub = null;
    private int streamID = 1005; // default to 1005

    // the associated GraphPlaces
    private GraphPlaces graph = null;

    // the reader thread
    private LocalMessageReaderThread readerThread = null;

    // the variable for terminating reader thread
    private int[] readerThreadAlive = new int[1];

    public LocalMessagingProvider(GraphPlaces graph) {
        this.graph = graph;
        // connect and create aeron pub and sub
        try {
            mediaDriver = MediaDriver.launch();
        } catch (ActiveDriverException e) {
            MASSBase.getLogger().debug("Another user has already launched media driver in this node!");
        }
        aeron = Aeron.connect();
        // each machine uses different stream
        this.streamID += MASSBase.getMyPid();
        pub = aeron.addPublication(channel + AERON_FLOW_CONTROL_STRATEGY, this.streamID);
        sub = aeron.addSubscription(channel, this.streamID);
        // create reader thread
        readerThreadAlive[0] = 1;
        readerThread = new LocalMessageReaderThread(graph, sub, readerThreadAlive);
        readerThread.start();

        unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(MAX_MESSAGE_SIZE_BYTES));
    }

    public LocalMessagingProvider(GraphPlaces graph, int streamID) {
        this.graph = graph;
        setStreamID(streamID);
        // connect and create aeron pub and sub
        try {
            mediaDriver = MediaDriver.launch();
        } catch (ActiveDriverException e) {
            MASSBase.getLogger().debug("Another user has already launched media driver in this node!");
        }
        aeron = Aeron.connect();
        pub = aeron.addPublication(channel, this.streamID);
        sub = aeron.addSubscription(channel, this.streamID);
        // create reader thread
        readerThreadAlive[0] = 1;
        readerThread = new LocalMessageReaderThread(graph, sub, readerThreadAlive);
        readerThread.start();

        unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(MAX_MESSAGE_SIZE_BYTES));
    }

    public void setStreamID(int streamID) {
        this.streamID = streamID;
    }

    // send message to the local Aeron channel
    public void sendMessage(Message m) {
        byte[] byteArray = SerializationUtils.serialize(m);
        // check if publication is online
        while(!pub.isConnected()) {
            idle.idle();
        }
        // send the message
        unsafeBuffer.putBytes(0, byteArray);
        while (pub.offer(unsafeBuffer, 0, byteArray.length) < 0)
        {
            idle.idle();
        }
    }

    // shutdown the message provider
    public void shutdown() {
        interruptReaderThread();
        pub.close();
        sub.close();
        CloseHelper.quietClose(aeron);
		CloseHelper.quietClose(mediaDriver);
        MASSBase.getLogger().debug("Local Aeron shut down");
    }

    private void interruptReaderThread() {
        readerThreadAlive[0] = 0;
    }
}