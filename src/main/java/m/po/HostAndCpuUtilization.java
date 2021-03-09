package m.po;

import org.cloudbus.cloudsim.hosts.Host;

public class HostAndCpuUtilization {

    private long hostId;

    private double cpuUtilization;

    private Host host;

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    public double getCpuUtilization() {
        return cpuUtilization;
    }

    public void setCpuUtilization(double cpuUtilization) {
        this.cpuUtilization = cpuUtilization;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }
}
