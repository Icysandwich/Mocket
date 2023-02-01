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
            names = "--rootDir",
            description = "The root directory of SUT.",
            required = true
    )
    private String rootDir = "";

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
            names = "--cluster",
            description = "The cluster setting for IP/Port. Format: IP1:Port1,IP2:Port2,...",
            required = true
    )
    private String cluster = "";

    @Parameter(
            names = "--start",
            description = "The script file of start a SUT node.",
            required = true
    )
    private String startScript = "";

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
    private int timeOut = 6000;

    @Parameter(
            names = "--results",
            description = "The storage path of testing results"
    )
    private String results = "./results";
}
