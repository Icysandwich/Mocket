# Mocket
A tool demo for testing distributed systems guided by model checking.
It is the artifact for EuroSys '23 paper "Model Checking Guided Testing
for Distributed Systems".

### Prerequisites
JDK 8

Maven 3.6.2

Python 3

### Build Mocket
We enter the main directory, and build the project by
```bash
mvn clean package
```

A jar file "mocket-0.1-SNAPSHOT.jar" is generated in "target/".

### Preprocessing

To import and use some essential interfaces in the implementation, we
add "mocket-0.1-SNAPSHOT.jar" in the project's classpath.
#### Annotate variables and states
We annotate the key variables and actions in your Raft implementation as
the following way:
```java
import mocket.annotation.*;

public class RaftNode {
    @Variable(“state”)
    private NodeState state = NodeState.STATE_FOLLOWER;
}
```

```java
import mocket.annotation.*;

@Action(“RequestVote”)
private void requestVote(Peer peer) {
    // Collect parameter values
    mocket.runtime.Message m = mocket.instrument.runtime.Interceptor.getParams(this.NodeId, peer.NodeId);
    // Set message infos
    mocket.instrument.runtime.Interceptor.setMessage(m,
            "RequestVoteRequest",
            "currentTerm",
            raftLog.getLastLogIndex(),
            getLastLogTerm());
```

#### Generate testing guidance by model checking results
We add TLC command line parameters "-dump dot,colorize,actionlabels
state" to generate a state.dot file containing the state space graph. 
Then, you must find the root state `ROOT` and use
```python
py PathGenerator.py $ROOT$ state.dot $OUTPUT_PATH$
```
to traverse the whole graph. In the directory `OUTPUT_PATH`, you can
find two files, i.e., `ep.node` storing all nodes with an ID and state
values, and `ep.edge` storing all paths consisting of edges.

### Testing
First, we start Mocket's testbed by running the jar independently.
```bash
java -jar mocket-0.1-SNAPSHOT.jar -rootDir=$ROOT_DIR$       #The root directory of SUT.
                                  -port=$PORT$              #The port used by Mocket.
                                  -cluster=$IP1$:$PORT1$    #The cluster setting.
                                           $IP2$:$PORT2$,
                                           ...
                                  -guidance=$GUIDENCE_DIR$     #The directory to store guidance files
                                  -nodeStarter=$NODE_DIR$      #The node start script
                                  -faults=$FAULT_TYPE$         #The fault types to be injected.
                                  -clientRequests=$LL$:$FILE$, #The client requests and corresponding script file.
                                                  $LC$:$FILE$
```

Then, to perform the runtime instrumentation, we add the jar as a java agent in the SUT initialization script.
```bash
$JAVA_HOME/bin/java  -Xbootclasspath/a:mocket-0.1-SNAPSHOT.jar -javaagent:mocket-0.1-SNAPSHOT.jar SUT.main.class
```

Finally, the testing can be automatically performed to find inconsistencies between the TLA+ specification and
Raft implementations.