package mocket.test;

import mocket.runtime.testbed.ActionScheduler;
import org.junit.Before;

public class ServerTest {
    ActionScheduler as;

    @Before
    public void initActionScheduler() {
        as = new ActionScheduler(null, 9090, null, "test");
        as.start();
    }
}
