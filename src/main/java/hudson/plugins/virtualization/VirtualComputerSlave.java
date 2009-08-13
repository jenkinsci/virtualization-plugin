package hudson.plugins.virtualization;

import hudson.model.Slave;
import hudson.model.Node;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.RetentionStrategy;
import hudson.slaves.NodeProperty;
import hudson.slaves.Cloud;
import hudson.Util;
import hudson.Extension;
import hudson.Functions;

import java.util.List;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Set;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.apache.commons.collections.list.TreeList;
import net.java.dev.vcc.api.Computer;

/**
 * Created by IntelliJ IDEA. User: connollys Date: Aug 13, 2009 Time: 2:55:43 PM To change this template use File |
 * Settings | File Templates.
 */
public class VirtualComputerSlave extends Slave {
    @DataBoundConstructor
    public VirtualComputerSlave(String name, String nodeDescription, String remoteFS, String numExecutors,
                                Mode mode, String labelString, ComputerLauncher launcher,
                                RetentionStrategy retentionStrategy, List<? extends NodeProperty<?>> nodeProperties,
                                VirtualComputer virtualComputer)
            throws
            Descriptor.FormException, IOException {
        super(name, nodeDescription, remoteFS, Util.tryParseNumber(numExecutors, 1).intValue(), mode, labelString,
                new VirtualComputerLauncher(launcher, virtualComputer), retentionStrategy, nodeProperties);
    }

    @Extension
    public static final class DescriptorImpl extends SlaveDescriptor {
        public String getDisplayName() {
            return "Slave virtual computer running on a virtualization platform (via vcc-api)";
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }

        public Set<VirtualComputer> getVirtualComputers() {
            SortedSet<VirtualComputer> result = new TreeSet<VirtualComputer>();
            for (Cloud cloud: Hudson.getInstance().clouds) {
                if (cloud instanceof VirtualDatacenter) {
                    VirtualDatacenter datacenter = VirtualDatacenter.class.cast(cloud);
                    for (Computer b: datacenter.getComputers().values()) {
                        result.add(new VirtualComputer(datacenter, b.getName()));
                    }
                }
            }
            return result;
        }

        public List<VirtualDatacenter> getDatacenters() {
            List<VirtualDatacenter> result = new ArrayList<VirtualDatacenter>();
            for (Cloud cloud: Hudson.getInstance().clouds) {
                if (cloud instanceof VirtualDatacenter) {
                    result.add((VirtualDatacenter) cloud);
                }
            }
            return result;
        }

        public List<Descriptor<ComputerLauncher>> getComputerLauncherDescriptors() {
            List<Descriptor<ComputerLauncher>> result = new ArrayList<Descriptor<ComputerLauncher>>();
            for (Descriptor<ComputerLauncher> launcher: Functions.getComputerLauncherDescriptors()) {
                if (!VirtualComputerLauncher.DESCRIPTOR.getClass().isAssignableFrom(launcher.getClass())) {
                    result.add(launcher);
                }
            }
            return result;
        }

    }


}
