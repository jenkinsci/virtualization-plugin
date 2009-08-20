package hudson.plugins.virtualization;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Resource;
import hudson.model.ResourceActivity;
import hudson.model.ResourceList;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.tasks.BuildWrapper;
import net.java.dev.vcc.api.Command;
import net.java.dev.vcc.api.Computer;
import net.java.dev.vcc.api.Datacenter;
import net.java.dev.vcc.api.PowerState;
import net.java.dev.vcc.api.commands.StartComputer;
import net.java.dev.vcc.api.commands.SuspendComputer;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

/**
 * A virtual computer that is used as a build resource.
 */
public class VirtualComputerBuildWrapper extends BuildWrapper implements ResourceActivity {
    private final List<VirtualComputerResource> resources;

    @DataBoundConstructor
    public VirtualComputerBuildWrapper(VirtualComputerResource[] resources)
            throws
            Descriptor.FormException, IOException {
        this.resources = Collections.unmodifiableList(new ArrayList<VirtualComputerResource>(Arrays.asList(resources)));
    }

    public VirtualComputerResource[] getResources() {
        return resources.toArray(new VirtualComputerResource[resources.size()]);
    }

    @Override
    public Environment setUp(AbstractBuild abstractBuild, Launcher launcher, BuildListener buildListener)
            throws IOException, InterruptedException {
        class EnvironmentImpl extends Environment {
            @Override
            public boolean tearDown(AbstractBuild abstractBuild, BuildListener buildListener)
                    throws IOException, InterruptedException {
                boolean failed = false;
                Map<String, Command> pendingOperations = new TreeMap<String, Command>();
                for (VirtualComputerResource resource : resources) {
                    VirtualComputer virtualComputer = resource.getVirtualComputer();
                    if (virtualComputer == null) {
                        continue;
                    }
                    String name = virtualComputer.getName();
                    Datacenter datacenter = virtualComputer.getDatacenter().getConnection();
                    for (Computer c : datacenter.getAllComputers()) {
                        if (virtualComputer.getName().equals(c.getName())) {
                            if (PowerState.RUNNING.equals(c.getState())) {
                                buildListener.getLogger()
                                        .println("[virtualization] Suspending virtual computer " + name);
                                pendingOperations.put(name, c.execute(new SuspendComputer()));
                            } else {
                                buildListener.getLogger()
                                        .println("[virtualization] Virtual computer " + name + " is already suspended");
                            }
                        }
                    }
                }
                while (!pendingOperations.isEmpty()) {
                    Iterator<Map.Entry<String, Command>> i = pendingOperations.entrySet().iterator();
                    while (i.hasNext()) {
                        Map.Entry<String, Command> entry = i.next();
                        Command pendingOperation = entry.getValue();
                        try {
                            pendingOperation.get();
                            i.remove();
                            buildListener.getLogger()
                                    .println("[virtualization] Virtual computer " + entry.getKey() + " suspended");
                        } catch (ExecutionException e) {
                            buildListener
                                    .error("[virtualization] Could not suspend virtual computer {0}", entry.getKey());
                            e.printStackTrace(buildListener.getLogger());
                            i.remove();
                            failed = true;
                        }
                    }
                }
                return !failed;
            }
        }
        boolean failed = false;
        Map<String, Command> pendingOperations = new TreeMap<String, Command>();
        for (VirtualComputerResource resource : resources) {
            VirtualComputer virtualComputer = resource.getVirtualComputer();
            if (virtualComputer == null) {
                continue;
            }
            Datacenter datacenter = virtualComputer.getDatacenter().getConnection();
            boolean found = false;
            String name = virtualComputer.getName();
            for (Computer c : datacenter.getAllComputers()) {
                if (name.equals(c.getName())) {
                    found = true;
                    if (!PowerState.RUNNING.equals(c.getState())) {
                        buildListener.getLogger().println("[virtualization] Starting virtual computer " + name);
                        pendingOperations.put(name, c.execute(new StartComputer()));
                    } else {
                        buildListener.getLogger()
                                .println("[virtualization] Virtual computer " + name + " is already started");
                    }
                    break;
                }
            }
            if (!found) {
                failed = true;
                buildListener.getLogger().println("[virtualization] Could not find virtual computer " + name);
                break;
            }
        }
        while (!pendingOperations.isEmpty()) {
            Iterator<Map.Entry<String, Command>> i = pendingOperations.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry<String, Command> entry = i.next();
                Command pendingOperation = entry.getValue();
                try {
                    pendingOperation.get();
                    i.remove();
                    buildListener.getLogger()
                            .println("[virtualization] Virtual computer " + entry.getKey() + " started");
                } catch (ExecutionException e) {
                    buildListener.fatalError("[virtualization] Could not start virtual computer {0}", entry.getKey());
                    e.printStackTrace(buildListener.getLogger());
                    failed = true;
                    i.remove();
                }
            }
        }
        if (failed) {
            new EnvironmentImpl().tearDown(abstractBuild, buildListener);
            return null;
        }
        return new EnvironmentImpl();
    }

    public ResourceList getResourceList() {
        ResourceList result = new ResourceList();
        for (VirtualComputerResource r : resources) {
            result.w(r.getResource());
        }
        return result;
    }

    public String getDisplayName() {
        return getDescriptor().getDisplayName();
    }

    private static VirtualComputer findVirtualComputer(String datacenterUri, String computerName) {
        computerName.getClass();
        for (VirtualComputer c : ((DescriptorImpl) Hudson.getInstance()
                .getDescriptor(VirtualComputerBuildWrapper.class))
                .getVirtualComputers()) {
            if ((datacenterUri == null || datacenterUri.equals(c.getDatacenterUri())) && computerName
                    .equals(c.getComputerName())) {
                return c;
            }
        }
        return null;
    }

    public static final class VirtualComputerResource implements Serializable {
        private final String datacenterUri;
        private final String computerName;
        private transient VirtualComputer virtualComputer = null;
        private transient Resource resource = null;

        @DataBoundConstructor
        public VirtualComputerResource(String datacenterUri, String computerName) throws IOException {
            datacenterUri.getClass(); // throw NPE if null
            computerName.getClass(); // throw NPE if null
            this.datacenterUri = datacenterUri;
            this.computerName = computerName;
        }

        public String getDatacenterUri() {
            return datacenterUri;
        }

        public String getComputerName() {
            return computerName;
        }

        public synchronized VirtualComputer getVirtualComputer() {
            if (virtualComputer == null) {
                virtualComputer = findVirtualComputer(datacenterUri, computerName);
            }
            return virtualComputer;
        }

        public synchronized Resource getResource() {
            if (resource == null) {
                resource = new Resource(new Resource(null, datacenterUri), computerName);
            }
            return resource;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            VirtualComputerResource that = (VirtualComputerResource) o;

            if (!computerName.equals(that.computerName)) {
                return false;
            }
            if (!datacenterUri.equals(that.datacenterUri)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = datacenterUri.hashCode();
            result = 31 * result + computerName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("VirtualComputerResource");
            sb.append("{datacenterUri='").append(datacenterUri).append('\'');
            sb.append(", computerName='").append(computerName).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        public DescriptorImpl() {
            super(VirtualComputerBuildWrapper.class);
        }

        public String getDisplayName() {
            return "Slave virtual computer running on a virtualization platform (via vcc-api)";
        }

        public Set<VirtualComputer> getVirtualComputers() {
            SortedSet<VirtualComputer> result = new TreeSet<VirtualComputer>();
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof VirtualDatacenter) {
                    VirtualDatacenter datacenter = VirtualDatacenter.class.cast(cloud);
                    result.addAll(datacenter.getVirtualComputers().values());
                }
            }
            return result;
        }

        public List<VirtualDatacenter> getDatacenters() {
            List<VirtualDatacenter> result = new ArrayList<VirtualDatacenter>();
            for (Cloud cloud : Hudson.getInstance().clouds) {
                if (cloud instanceof VirtualDatacenter) {
                    result.add((VirtualDatacenter) cloud);
                }
            }
            return result;
        }

        public List<Descriptor<ComputerLauncher>> getComputerLauncherDescriptors() {
            List<Descriptor<ComputerLauncher>> result = new ArrayList<Descriptor<ComputerLauncher>>();
            for (Descriptor<ComputerLauncher> launcher : Functions.getComputerLauncherDescriptors()) {
                if (!VirtualComputerLauncher.DESCRIPTOR.getClass().isAssignableFrom(launcher.getClass())) {
                    result.add(launcher);
                }
            }
            return result;
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest staplerRequest, JSONObject jsonObject)
                throws FormException {
            System.out.println(jsonObject);
            return super.newInstance(staplerRequest,
                    jsonObject);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }


}
