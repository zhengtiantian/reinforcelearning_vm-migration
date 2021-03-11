package m.po;

import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Map;

public class ProcessResult {

    Map<Vm, Host> vmToHostMap;

    Map<Long, Host> shutdownHosts;

    public Map<Vm, Host> getVmToHostMap() {
        return vmToHostMap;
    }

    public void setVmToHostMap(Map<Vm, Host> vmToHostMap) {
        this.vmToHostMap = vmToHostMap;
    }

    public Map<Long, Host> getShutdownHosts() {
        return shutdownHosts;
    }

    public void setShutdownHosts(Map<Long, Host> shutdownHosts) {
        this.shutdownHosts = shutdownHosts;
    }
}
