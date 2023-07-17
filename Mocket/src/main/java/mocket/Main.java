package mocket;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

public class Main {
    public static void main(String[] args) {
        Configuration cfg = new Configuration();
        JCommander jCmd = JCommander.newBuilder().addObject(cfg).build();
        try {
            jCmd.parse(args);
        } catch (ParameterException e) {
            System.out.println("Wrong parameter:" + e.getMessage());
            jCmd.usage();
            return;
        }
        if (cfg.isHelp()) {
            jCmd.usage();
            return;
        }
        Mocket mocket = new Mocket(cfg);
        if(mocket.isInitialized())
            Mocket.nodeNum = cfg.getCluster().split(",").length;
    }
}
