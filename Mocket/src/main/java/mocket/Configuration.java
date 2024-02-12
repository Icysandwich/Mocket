package mocket;

import lombok.Data;
import com.beust.jcommander.Parameter;
import org.objectweb.asm.Opcodes;

@Data
public class Configuration {

    public static final int ASM_VERSION = Opcodes.ASM7;

    public boolean isHelp(){
        return help;
    }

    @Parameter(names = {"--help", "-h"}, help = true)
    private boolean help = false;

    @Parameter(
            names = "--config",
            description = "The configuration file path.",
            required = false
    )
    private String config = "./config/config.properties";

    @Parameter(
            names = "--workspace",
            description = "The workspace directory.",
            required = true
    )
    private String workspace = "";

    @Parameter(
            names = "--SUT",
            description = "System under test. Support Raft and ZooKeeper for now.",
            required = true
    )
    private String sut = "";

    @Parameter(
            names = "--port",
            description = "The port of Mocket.",
            required = true
    )
    private int port = 0;

    @Parameter(
            names = "--SUTPort",
            description = "The communication port of SUT. 9090 in default.",
            required = false
    )
    private int sutport = 0;

    @Parameter(
            names = "--cluster",
            description = "The cluster setting for IP/Port. Format: IP1:Port1,IP2:Port2,...",
            required = true
    )
    private String cluster = "";

    @Parameter(
            names = "--startCluster",
            description = "The script file of starting the SUT cluster.",
            required = true
    )
    private String startCluster = "";

    @Parameter(
            names = "--stopCluster",
            description = "The script file of stopping the SUT cluster.",
            required = true
    )
    private String stopCluster = "";

    @Parameter(
            names = "--inputs",
            description = "The directory storing path files."
    )
    private String inputs = "./outputs";

    @Parameter(
            names = "--faults",
            description = "The fault types to inject."
    )
    private String faults = "";

    @Parameter(
            names = "--clientRequests",
            description = "The client requests."
    )
    private String clientRequests = "";

    @Parameter(
            names = "--timeout",
            description = "The maximum time for Mocket waiting a SUT cluster communication."
    )
    private long timeout = 6000;

    @Parameter(
            names = "--results",
            description = "The storage path of testing results"
    )
    private String results = "./results";
}
