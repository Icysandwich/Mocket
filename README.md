# Mocket
A tool demo for testing distributed system implementations guided by model checking.
The goal is to find inconsistencies between the TLA+ specification and
distributed system implementations.

### Prerequisites
JDK 8

Maven 3.6.2

Python 3

### Build Mocket
We enter the directory Mocket, and build the project by
```bash
mvn clean package
```

A jar file "mocket-0.1-SNAPSHOT.jar" is generated in "target/".

### Preprocessing

To import and use some essential interfaces in the implementation, we
add "mocket-0.2-SNAPSHOT.jar" in the project's classpath.
#### Annotate variables and states
We annotate the key variable and state in your Raft implementation as
the following way:
```java
import mocket.annotation.*;

public class RaftNode {
    @MocketVariable(“state”)
    private NodeState state = NodeState.STATE_FOLLOWER;
}
```

```java
import mocket.annotation.*;

@MocketAction(“RequestVote”)
private void requestVote(Peer peer) {
    // Collect parameter values
    mocket.runtime.Message m = mocket.instrument.runtime.Interceptor.getParams(this.NodeId, peer.NodeId);
    // Set message infos
    mocket.instrument.runtime.Interceptor.setMessage(m,
            "RequestVoteRequest",
            "currentTerm",
            raftLog.getLastLogIndex(),
            getLastLogTerm());
}
```

#### Generate testing guidance by model checking results
We add TLC command line parameters "-dump dot,colorize,actionlabels
state" to generate a state.dot file containing the state space graph. 
Then, we traverse the whole graph and generate test cases:
```bash
python path_generator.py END_ACTION /path/to/file.dot /path/to/store/paths [POR]
```

### Testing
First, we add the jar as a java agent in the SUT initialization script to perform the runtime instrumentation.
```bash
$JAVA_HOME/bin/java  -Xbootclasspath/a:mocket-0.1-SNAPSHOT.jar -javaagent:mocket-0.1-SNAPSHOT.jar SUT.main.class
```

Then, we start Mocket's testing server by running the jar independently.
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

Finally, Mocket can automatically test each generated case to find inconsistencies between the TLA+ specification and
implementations.