package m.migrationAlgorithm;

import m.po.ExpectedResult;
import m.po.VmToHost;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public abstract class MigrationTool {

    public void sortHostByIncreasingCpuUtilization(List<Host> hosts) {
        hosts.sort(new Comparator<Host>() {
            @Override
            public int compare(Host o1, Host o2) {
                if (o1.getCpuPercentUtilization() > o2.getCpuPercentUtilization()) {
                    return 1;
                } else if (o1.getCpuPercentUtilization() == o2.getCpuPercentUtilization()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
    }

    /**
     * Assembly parameters
     *
     * @param canDo
     * @param vmsToHosts
     * @param reward
     * @return
     */
    public ExpectedResult createResult(boolean canDo, List<VmToHost> vmsToHosts, Double reward) {
        ExpectedResult result = new ExpectedResult();
        result.setCanDo(canDo);
        result.setVmsToHosts(vmsToHosts);
        if (reward != null) {
            result.setReward(reward);
        }
        return result;
    }

    public void updateMigrationMap(Map<Vm, Host> vmToHostMap, List<VmToHost> vmsToHosts) {
        if (vmsToHosts != null && vmsToHosts.size() > 0) {
            for (VmToHost vth : vmsToHosts) {
                vmToHostMap.put(vth.getVm(), vth.getHost());
            }
        }
    }

    public void updateHostAndTargetHost(Host host, Map<Long, List<Vm>> hostVmsMap, List<VmToHost> vmsToHosts) {
        if (vmsToHosts != null && vmsToHosts.size() > 0) {
            for (VmToHost vmsToHost : vmsToHosts) {
                hostVmsMap.get(host.getId()).remove(vmsToHost.getVm());
                hostVmsMap.get(vmsToHost.getHost().getId()).add(vmsToHost.getVm());
            }

        }
    }
}
