package po;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

public class VmToHost {

    private Vm vm;
    private Host host;

    public Vm getVm() {
        return vm;
    }

    public void setVm(Vm vm) {
        this.vm = vm;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }
}
