package mocket.runtime.testbed;

import mocket.faults.Fault;
import mocket.path.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class FaultController {

    final Logger logger = LoggerFactory.getLogger(mocket.runtime.testbed.FaultController.class);
    Set<Fault> types;

    public FaultController(Set<Fault> types) {
        this.types = types;
    }

    public void loadFaultScripts() {
        if(types.isEmpty()) {
            logger.error("No fault types are chose. Please check fault configuration setting!");
        }

    }

    public void injectFault(Action a) {}

    private void crashNode() {}

    private void restartNode() {}
}
