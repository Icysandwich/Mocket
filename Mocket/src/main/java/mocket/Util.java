package mocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private final static Logger logger = LoggerFactory.getLogger(mocket.Util.class);

    private static InetAddress controllerAddr = null;
    private static int controllerPort = 0;
    private static int nodePort = 0;

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

    public static String getLocalTime() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return sdf.format(date);
    }

    /**
     * Run a bash shell script file.
     * @param script The path of the script file.
     * @param args Arguments to run the script.
     * @param workspace The path to run the script.
     */
    public static String invokeScript(String script, String args, String... workspace) {
        String result = "";
        try {
            String cmd = "sh " + script + " " + args;
            File dir = null;
            if (workspace[0] != null) {
                dir = new File(workspace[0]);
            }
            String[] evnp = {"val=2", "call=Bash Shell"};
            logger.info("Execute shell command: " + cmd);
            Process process = Runtime.getRuntime().exec(cmd, evnp, dir);
            BufferedReader inReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String readLine = "";
            while ((readLine = inReader.readLine()) != null) {
                logger.debug(readLine);
                result += readLine;
            }
            while ((readLine = errReader.readLine()) != null) {
                logger.error(readLine);
                result += readLine;
            }
            inReader.close();
            errReader.close();
        } catch (IOException e) {
            logger.error("Invoke script: " + script + " failed!");
            e.printStackTrace();
        }
        return result;
    }

    public static int getNodeId(String nid) {
        String regex = "[^0-9]";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(nid);
        return Integer.parseInt(m.replaceAll("").trim());
    }
}
