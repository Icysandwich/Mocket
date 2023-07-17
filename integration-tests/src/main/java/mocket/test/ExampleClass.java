package mocket.test;

import mocket.annotation.Action;
import mocket.annotation.Variable;
import mocket.instrument.runtime.Interceptor;

public class ExampleClass {

    @Variable("StateA")
    private int stateA;

    @Action("ActionA")
    public void ActionA(int a) {
        Interceptor.collectParams("A", String.valueOf(a));
        stateA = a;
    }

}
