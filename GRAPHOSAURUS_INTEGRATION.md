# Graphosaurus Integration for MASS Java

## Overview

MASS Java now supports 3D visualization of graph structures using Graphosaurus. This integration uses WebSocket communication to stream updates to a browser-based 3D visualization.

## Architecture

- **MASS Java**: WebSocket client that polls agent states and sends JSON messages
- **Graphosaurus Server**: WebSocket server (`server.js`) that relays messages to browser clients
- **Browser Frontend**: 3D visualization of graph and agent movements

```
[MASS Java] --WebSocket--> [Graphosaurus server.js:8080] <--WebSocket--> [Browser]
     |                              |                                        |
     +-- Polls agent locations      +-- Relays messages            Displays visualization
     +-- Sends graph structure on-demand
```

## Features

- **On-Demand Graph Loading**: Vertices and edges are streamed only when agents visit them (efficient bandwidth usage)
- **Real-Time Agent Tracking**: Automatic detection of agent spawn, movement, and removal events
- **Configurable Polling**: Adjust polling frequency to balance responsiveness and performance
- **Easy Integration**: Simple API to enable/disable visualization

## Setup

### 1. Start Graphosaurus Server

Navigate to the Graphosaurus directory and start the WebSocket server:

```bash
npm install  # (if not already done)
npm run server
```

The server will start on `ws://localhost:8080` by default.

### 2. Open Visualization Frontend

Open `message-demo.html` or `agent-demo.html` in a web browser:

```bash
# Using a browser, open:
file:///C:/Users/lakes/graphosaurus/message-demo.html
```

The frontend will automatically connect to the WebSocket server.

## Remote Cluster Deployment

MASS runs across multiple computing nodes, but the Graphosaurus visualization listener only runs on the **master node** (PID 0). The default WebSocket URL `ws://localhost:8080` assumes the Graphosaurus server is reachable at `localhost` from the master node. When the master node is a remote machine (e.g., a cluster head node), `localhost` on that machine will not reach the Graphosaurus server running on your local workstation.

The simplest fix is **SSH reverse port forwarding**. This tunnels port 8080 on the remote master node back to port 8080 on your local machine, so the default `ws://localhost:8080` URL works without any code or configuration changes.

### Step-by-Step Remote Workflow

**1. Start the Graphosaurus server on your local machine:**
Link to the frontend repo can be found [here].(https://github.com/vikv1/mass-graphosaurus)
```bash
npm install  # (if not already done)
npm run server
```

The server listens on `ws://localhost:8080`.

**2. SSH into the master node with reverse port forwarding:**

```bash
ssh -R 8080:localhost:8080 user@master-node
```

The `-R 8080:localhost:8080` flag binds port 8080 on the remote master node and forwards any connections to it back through the SSH tunnel to port 8080 on your local machine.

**3. Run MASS application on the master node with the following flag:**

```bash
-Dgraphosaurus.enabled=true
```

The `GraphosaurusListener` on the master node connects to `ws://localhost:8080`, which the SSH tunnel routes to the Graphosaurus server on your local machine.

**4. Open the visualization in your local browser:**

```
viewer.html
```

The browser connects directly to `ws://localhost:8080` on your local machine -- no tunnel needed on this side.

### How It Works

```
Your Local Machine                 SSH Tunnel              Master Node (PID 0)
+--------------------------+                           +-------------------------+
| Graphosaurus server.js   |<---- ssh -R 8080 --------| GraphosaurusListener    |
|   listening on :8080     |      (reverse tunnel)     |   connects to           |
|                          |                           |   ws://localhost:8080   |
| Browser                  |                           +-------------------------+
|   connects to            |                                     |
|   ws://localhost:8080    |                                     | SSH (JSch)
+--------------------------+                                     v
                                                       Worker Nodes (PID 1, 2, ...)
                                                       +-------------------------+
                                                       | MProcess instances      |
                                                       +-------------------------+
```

### Alternative: Custom WebSocket URL

If you prefer to run the Graphosaurus server on a host that is directly reachable from the master node (without tunneling), you can override the URL via system property:

```bash
java -Dgraphosaurus.enabled=true \
     -Dgraphosaurus.websocket.url=ws://your-server-host:8080 \
     -jar your-mass-application.jar
```

Or programmatically:

```java
graph.enableGraphosaurusVisualization("ws://your-server-host:8080", 500);
```

## Usage

### Option 1: Programmatic API

Enable Graphosaurus visualization in your MASS application:

```java
// Create a GraphPlaces instance
GraphPlaces graph = new GraphPlaces(handle, className);

// Enable visualization with default settings (ws://localhost:8080, 500ms polling)
graph.enableGraphosaurusVisualization();

// Or use custom settings
graph.enableGraphosaurusVisualization("ws://localhost:8080", 1000);

// Your simulation code here...
// Add vertices, spawn agents, etc.

// Disable when done (optional - automatically cleaned up)
graph.disableGraphosaurusVisualization();
```

### Option 2: System Properties Configuration

Enable Graphosaurus automatically using system properties when running your MASS application:

```bash
java -Dgraphosaurus.enabled=true \
     -Dgraphosaurus.websocket.url=ws://localhost:8080 \
     -Dgraphosaurus.poll.interval=500 \
     -jar your-mass-application.jar
```

**Configuration Properties:**
- `graphosaurus.enabled`: Set to `true` to enable (default: `false`)
- `graphosaurus.websocket.url`: WebSocket server URL (default: `ws://localhost:8080`)
- `graphosaurus.poll.interval`: Polling interval in milliseconds (default: `500`)

### Option 3: Maven Configuration

Add to your Maven command:

```bash
mvn exec:java -Dexec.mainClass="your.MainClass" \
              -Dgraphosaurus.enabled=true
```

## How It Works

### Agent Tracking

The `AgentLocationTracker` class monitors agents on `VertexPlace` objects and detects:
- **Spawned**: New agent appears on a vertex
- **Moved**: Agent migrates to a different vertex
- **Removed**: Agent no longer exists in the system

### Message Types

The integration sends JSON messages matching the Graphosaurus MESSAGE_API:

**1. Spawn Agent**
```json
{
  "type": "spawn_agent",
  "nodeId": "vertex-123",
  "id": "agent-456",
  "color": 16711680,
  "shape": "sphere",
  "data": {"agentId": 456}
}
```

**2. Move Agent**
```json
{
  "type": "move_agent",
  "agentId": "agent-456",
  "targetNodeId": "vertex-789",
  "speed": 1.0
}
```

**3. Remove Agent**
```json
{
  "type": "remove_agent",
  "agentId": "agent-456"
}
```

**4. Add Node** (sent on-demand when agent visits)
```json
{
  "type": "add_node",
  "id": "vertex-123",
  "color": 8947848,
  "position": [0.5, -1.2, 0.8],
  "data": {"vertexId": 123}
}
```

**5. Add Edge** (sent on-demand with neighbors)
```json
{
  "type": "add_edge",
  "fromNodeId": "vertex-123",
  "toNodeId": "vertex-456",
  "color": 13421772
}
```

### On-Demand Graph Loading Strategy

To optimize bandwidth and improve performance:
1. Graph structure (vertices and edges) is NOT sent all at once
2. When an agent spawns or moves to a vertex:
   - The vertex is sent (if not already sent)
   - All neighboring vertices are sent (if not already sent)
   - Edges between the vertex and its neighbors are sent (if not already sent)
3. This creates a "revealed" subgraph around active agents

## Performance Tuning

### Polling Interval

- **Lower values (100-300ms)**: More responsive visualization, higher CPU/network usage
- **Higher values (1000-2000ms)**: Less responsive, lower resource usage
- **Recommended**: 500ms for most applications

### Network Considerations

- Each agent spawn/move sends 1-3 messages (agent + optional graph structure)
- Large graphs with many agents may generate significant network traffic
- Consider increasing polling interval for large-scale simulations

## Troubleshooting

### Connection Refused

**Problem**: `Failed to initialize Graphosaurus connection`

**Solution**: Ensure Graphosaurus server is running:
```bash
cd C:\Users\lakes\graphosaurus
npm run server
```

### No Agents Visible

**Problem**: Graph shows but no agents appear

**Possible causes**:
1. Agents are not on `VertexPlace` objects (only graph agents are visualized)
2. Polling hasn't detected agents yet (wait 500ms)
3. WebSocket connection lost (check console logs)

### Graph Not Appearing

**Problem**: Agents appear but no graph structure

**Solution**: This is normal - graph is revealed on-demand as agents explore it. If the first agent spawns on a vertex, that vertex and its neighbors will appear.

## File Structure

New files added to MASS Java:

```
src/main/java/edu/uw/bothell/css/dsl/MASS/
├── GraphPlaces.java (modified)
│   └── Added enableGraphosaurusVisualization() methods
│
└── graph/
    ├── AgentLocationTracker.java (new)
    │   └── Tracks agent state changes
    │
    ├── GraphosaurusListener.java (new)
    │   └── WebSocket client and polling logic
    │
    └── transport/
        └── GraphosaurusMessage.java (new)
            └── Message POJOs for JSON serialization
```

## Dependencies Added

The following dependencies were added to `pom.xml`:

```xml
<!-- WebSocket client -->
<dependency>
    <groupId>org.java-websocket</groupId>
    <artifactId>Java-WebSocket</artifactId>
    <version>1.5.3</version>
</dependency>

<!-- JSON serialization (already present) -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.8.6</version>
</dependency>
```

## Example Application

```java
import edu.uw.bothell.css.dsl.MASS.*;

public class GraphVisualizationExample {
    public static void main(String[] args) {
        // Initialize MASS
        MASS.init();
        
        // Create graph
        GraphPlaces graph = new GraphPlaces(1, "MyVertexPlace");
        
        // Enable Graphosaurus visualization
        graph.enableGraphosaurusVisualization();
        
        // Add vertices and edges
        int v1 = graph.addVertex("vertex-1");
        int v2 = graph.addVertex("vertex-2");
        int v3 = graph.addVertex("vertex-3");
        graph.addEdge("vertex-1", "vertex-2");
        graph.addEdge("vertex-2", "vertex-3");
        
        // Create agents that will move through the graph
        Agents agents = new Agents(2, "MyAgent", null, graph, 10);
        
        // Run simulation
        for (int i = 0; i < 100; i++) {
            agents.manageAll();
            MASS.sleep(1000); // Sleep 1 second between steps
        }
        
        // Cleanup
        graph.disableGraphosaurusVisualization();
        MASS.finish();
    }
}
```

## Limitations

- **Master Node Only**: Visualization is enabled only on the master node (PID 0)
- **Graph Agents Only**: Only agents on `VertexPlace` objects are visualized
- **Static Graph Structure**: Graph changes (add/remove vertices/edges) after initialization are not sent
- **Single Server**: Currently supports one Graphosaurus server connection per GraphPlaces instance

## Future Enhancements

Potential improvements for future versions:
- Configurable agent colors and shapes based on agent properties
- Performance metrics overlay (agents/sec, message rate, etc.)
- Recording and playback of simulations

## Support

For issues or questions:
1. Check Graphosaurus documentation
2. Enable debug logging: Set MASS log level to DEBUG
3. Check WebSocket server logs: Look at console output where `npm run server` is running
4. Verify network connectivity: Ensure no firewall blocking port 8080

## References

- Graphosaurus GitHub: https://github.com/frewsxcv/graphosaurus
- Graphosaurus Message API: `C:\Users\lakes\graphosaurus\MESSAGE_API.md`
- MASS Java Documentation: http://depts.washington.edu/dslab/MASS/

