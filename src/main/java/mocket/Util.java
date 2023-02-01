package mocket;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class Util {
    private static InetAddress controllerAddr = null;
    private static int controllerPort = 0;
    private static int nodePort = 0;
    private static Map<Integer, Integer> localPorts = new HashMap<Integer, Integer>();

    public static boolean isState(String desc, String varName) {
        return false;
    }

    public static InetAddress getControllerAddress() {
        return controllerAddr;
    }

    public static int getControllerPort() {
        return controllerPort;
    }

    public static int getLocalPort() {
        return 0;
    }

    public static int getNodePort() { return nodePort; }
}
