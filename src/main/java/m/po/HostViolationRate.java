package m.po;

import org.cloudbus.cloudsim.hosts.Host;

public class HostViolationRate {

    private Host host;

    private String violationRate;

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public String getViolationRate() {
        return violationRate;
    }

    public void setViolationRate(String violationRate) {
        this.violationRate = violationRate;
    }
}
