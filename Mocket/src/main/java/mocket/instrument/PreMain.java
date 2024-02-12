package mocket.instrument;

import mocket.instrument.runtime.Variables;
import mocket.instrument.runtime.Interceptor;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;

public class PreMain {

    public static boolean DEBUG = true;
    public static Variables vars;

    static{
        vars = new Variables();
    }

    public static void premain(String args, Instrumentation inst) throws Exception {
        HashMap<String, String> arguments = parseArgs(args);
        if(!arguments.isEmpty()) {
            setOptions(arguments);
        }
        inst.addTransformer(new MocketTransformer(), true);
    }

    private static HashMap<String, String> parseArgs(String argString) {
        String[] args = argString.split(",");
        HashMap<String, String> result = new HashMap<String, String>();
        for(String arg : args) {
            int split = arg.indexOf('=');
            if(split == -1) {
                result.put(arg, "true");
            } else {
                String option = arg.substring(0, split);
                String value = arg.substring(split + 1);
                result.put(option, value);
            }
        }
        return result;
    }

    private static void setOptions(HashMap<String, String> args) {
        try {
            for (String opt : args.keySet()) {
                switch(opt) {
                    case "SUT":
                        Interceptor.SUT = args.get(opt);
                        break;
                    case "Host":
                        Interceptor.myHost = args.get(opt);
                        break;
                    case "MocketHost":
                        Interceptor.hostAddress = args.get(opt);
                        break;
                    case "Port":
                        Interceptor.port = Integer.parseInt(args.get(opt));
                        break;
                    case "sid":
                        Interceptor.sid = Integer.parseInt(args.get(opt));
                        break;
                    case "MethodVariable":// TODO: Add method variable handling
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Parameter handling error: " + e.getMessage());
        }
    }
}
