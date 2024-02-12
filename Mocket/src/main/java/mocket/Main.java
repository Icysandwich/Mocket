package mocket;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Please enter parameters.");
            System.exit(0);
        }
        Mocket mocket;
        if (args[0].contains("config")) { // Use configuration file
            String configFile;
            if(args[0].contains("="))
                configFile = args[0].substring(args[0].indexOf("=") + 1);
            else if(args.length > 1)
                configFile = args[1];
            else {
                System.err.println("Cannot parse config parameter!");
                return;
            }
            mocket = new Mocket(configFile);
        } else { // Use cmd configuration
            Configuration cfg = new Configuration();
            JCommander jCmd = JCommander.newBuilder().addObject(cfg).build();
            try {
                jCmd.parse(args);
            } catch (ParameterException e) {
                System.err.println("Wrong parameter:" + e.getMessage());
                jCmd.usage();
                return;
            }
            if (cfg.isHelp()) {
                jCmd.usage();
                return;
            }
            mocket = new Mocket(cfg);

        }
        mocket.init();
        if(mocket.isInitialized())
            mocket.start();
    }
}
