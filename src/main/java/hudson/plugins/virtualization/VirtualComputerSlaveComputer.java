package hudson.plugins.virtualization;

import hudson.slaves.SlaveComputer;
import hudson.model.Slave;
import net.java.dev.vcc.api.Datacenter;

/**
 * Created by IntelliJ IDEA. User: connollys Date: Aug 13, 2009 Time: 3:01:24 PM To change this template use File |
 * Settings | File Templates.
 */
public class VirtualComputerSlaveComputer extends SlaveComputer {
    /**
     * Cached connection to the virtaul datacenter. Lazily fetched.
     */
    private volatile Datacenter datacenter;

    public VirtualComputerSlaveComputer(Slave slave) {
        super(slave);
    }
}
