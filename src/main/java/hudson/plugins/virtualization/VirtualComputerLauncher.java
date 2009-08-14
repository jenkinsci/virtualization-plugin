package hudson.plugins.virtualization;

import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.model.TaskListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.Extension;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import net.java.dev.vcc.api.Datacenter;
import net.java.dev.vcc.api.Computer;
import net.java.dev.vcc.api.PowerState;
import net.java.dev.vcc.api.commands.StartComputer;
import net.java.dev.vcc.api.commands.SuspendComputer;

/**
 * Created by IntelliJ IDEA. User: connollys Date: Aug 13, 2009 Time: 3:02:47 PM To change this template use File |
 * Settings | File Templates.
 */
public class VirtualComputerLauncher extends ComputerLauncher {
    private final ComputerLauncher delegate;
    private final VirtualComputer virtualComputer;

    @DataBoundConstructor
    public VirtualComputerLauncher(ComputerLauncher delegate, VirtualComputer virtualComputer) {
        this.delegate = delegate;
        this.virtualComputer = virtualComputer;
    }

    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public VirtualComputer getVirtualComputer() {
        return virtualComputer;
    }

    @Override
    public boolean isLaunchSupported() {
        return delegate.isLaunchSupported();
    }

    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener)
            throws IOException, InterruptedException {
        taskListener.getLogger().println("Getting connection to the virtual datacenter");
        try {
            taskListener.getLogger().println("Target virtual computer: " + virtualComputer);
        Datacenter datacenter = virtualComputer.getDatacenter().getConnection();
        taskListener.getLogger().println("Finding the computer");
        for (Computer c : datacenter.getAllComputers()) {
            if (virtualComputer.getName().equals(c.getName())) {
                taskListener.getLogger().println("Found the computer");
                if (!PowerState.RUNNING.equals(c.getState())) {
                    taskListener.getLogger().println("Starting virtual computer");
                    c.execute(new StartComputer());
                } else {
                    taskListener.getLogger().println("Virtual computer is already running");
                }
                taskListener.getLogger().println("Starting stage 2 launcher");
                delegate.launch(slaveComputer, taskListener);
                taskListener.getLogger().println("Stage 2 launcher completed");
                return;
            }
        }
        taskListener.getLogger().println("Could not find the computer");
        throw new IOException("Could not find the computer");
        } catch (IOException e) {
            e.printStackTrace(taskListener.getLogger());
            throw e;
        } catch (Throwable t) {
            t.printStackTrace(taskListener.getLogger());
        }
    }

    @Override
    public void afterDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        taskListener.getLogger().println("Starting stage 2 afterDisconnect");
        delegate.afterDisconnect(slaveComputer, taskListener);
        taskListener.getLogger().println("Getting connection to the virtual datacenter");
        try {
            Datacenter datacenter = virtualComputer.getDatacenter().getConnection();
            taskListener.getLogger().println("Finding the computer");
            for (Computer c : datacenter.getAllComputers()) {
                if (virtualComputer.getName().equals(c.getName())) {
                    taskListener.getLogger().println("Found the computer");
                    if (PowerState.RUNNING.equals(c.getState())) {
                        taskListener.getLogger().println("Suspending virtual computer");
                        c.execute(new SuspendComputer());
                    } else {
                        taskListener.getLogger().println("Virtual computer is already suspended");
                    }
                    return;
                }
            }
            taskListener.getLogger().println("Could not find the computer");
        } catch (Throwable t) {
            taskListener.fatalError(t.getMessage(), t);
        }
    }

    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        delegate.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        return Hudson.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static final Descriptor<ComputerLauncher> DESCRIPTOR = new Descriptor<ComputerLauncher>() {
        public String getDisplayName() {
            return null;
        }

    };

}
