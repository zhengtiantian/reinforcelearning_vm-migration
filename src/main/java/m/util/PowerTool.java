package m.util;

import org.cloudbus.cloudsim.hosts.Host;

import java.util.List;
import java.util.Map;

public class PowerTool {

    /**
     * calculate the total power of the data center according to the cpu utilization map and hostlist.
     *
     * @param hostList
     * @param hostMap
     * @return
     */
    public double getTotalPower(List<Host> hostList, Map<Long, Double> hostMap) {
        double totalPower = 0.0;
        for (Host host : hostList) {
            totalPower += getPower(host, hostMap.get(host.getId()));
        }
        return totalPower;
    }

    /**
     * @param host
     * @param cpuUtilization
     * @return
     */
    public double getPower(Host host, double cpuUtilization) {
        if (host.isActive() && cpuUtilization >= 0) {
            return host.getPowerModel().getPower(cpuUtilization);
        } else {
            if (cpuUtilization > 0) {
                return host.getPowerModel().getPower(cpuUtilization);
            } else {
                return 0.0;
            }
        }
    }
}
