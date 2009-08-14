package hudson.plugins.virtualization;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA. User: connollys Date: Aug 13, 2009 Time: 4:05:21 PM To change this template use File |
 * Settings | File Templates.
 */
public class VirtualComputer implements Serializable, Comparable<VirtualComputer> {

    private final String name;

    private final VirtualDatacenter datacenter;

    @DataBoundConstructor
    public VirtualComputer(VirtualDatacenter datacenter, String name) {
        this.datacenter = datacenter;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public VirtualDatacenter getDatacenter() {
        return datacenter;
    }

    public String getComputerName() {
        return name;
    }

    public String getDatacenterUri() {
        return datacenter.getDatacenterUri();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VirtualComputer)) {
            return false;
        }

        VirtualComputer that = (VirtualComputer) o;

        if (datacenter != null ? !datacenter.equals(that.datacenter) : that.datacenter != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (datacenter != null ? datacenter.hashCode() : 0);
        return result;
    }

    public String getDisplayName() {
        return name + "@" + datacenter.getDatacenterUri();
    }

    public int compareTo(VirtualComputer o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("VirtualComputer");
        sb.append("{name='").append(name).append('\'');
        sb.append(", datacenter=").append(datacenter);
        sb.append('}');
        return sb.toString();
    }
}
