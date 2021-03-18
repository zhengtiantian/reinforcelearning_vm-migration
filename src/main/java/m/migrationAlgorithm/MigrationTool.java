package m.migrationAlgorithm;

import m.po.ExpectedResult;
import m.po.VmToHost;
import m.util.Constant;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;

public abstract class MigrationTool {

    Constant constant = new Constant();

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


    public boolean haveEnoughSpace(Double utilization, Host host, Map<Long, Double> hostCpuMap, int unassignmentVms) {
        double sumFreeUt = unassignmentVms * constant.PERCENTAGE_OF_ONE_VM_TO_HOST;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            if (e.getKey() == host.getId()) {
                continue;
            }
            sumFreeUt += (100 - e.getValue());
        }
        if (utilization > sumFreeUt) {
            return false;
        }
        return true;
    }

    public Map<Long, Double> removeAllZeroHosts(Map<Long, Double> hostCpuMap) {
        Map<Long, Double> hostCpuMapWithoutZero = new HashMap<>();
        if (hostCpuMap != null && hostCpuMap.size() > 0) {
            for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
                if (e.getValue() == 0) {
                    continue;
                }
                hostCpuMapWithoutZero.put(e.getKey(), e.getValue());
            }
        }
        return hostCpuMapWithoutZero;
    }

    public void addAllVms(Map<Long, Vm> moveOutVms, List<Vm> vms) {
        if (vms != null && vms.size() > 0) {
            for (Vm vm : vms) {
                moveOutVms.put(vm.getId(), vm);
            }
        }
    }

    public int calculateVmsMoveout(Double cpuUtilization, Double cpuUtilizationThershold) {
        return (int) Math.ceil((cpuUtilization - cpuUtilizationThershold) / constant.PERCENTAGE_OF_ONE_VM_TO_HOST);
    }

    public Map<Long, Host> getWithoutZeroHosts(List<Host> hostList, Map<Long, Double> hostCpuMap) {
        Map<Long, Host> hostMapWithoutZero = new HashMap<>();
        if (hostList != null && hostList.size() > 0) {
            for (Host host : hostList) {
                if (hostCpuMap.get(host.getId()) == 0) {
                    continue;
                }
                hostMapWithoutZero.put(host.getId(), host);
            }

        }
        return hostMapWithoutZero;
    }

    public Map<Long, Host> getZeroHosts(List<Host> hostList, Map<Long, Double> hostCpuMap) {
        Map<Long, Host> hostZeroMap = new HashMap<>();
        if (hostList != null && hostList.size() > 0) {
            for (Host host : hostList) {
                if (hostCpuMap.get(host.getId()) == 0) {
                    hostZeroMap.put(host.getId(), host);
                }

            }

        }
        return hostZeroMap;
    }

    public void moveOutVmsFromHost(int vmsNum, Map<Long, Vm> moveOutVms, Host host, Map<Long, List<Vm>> hostVmsMap) {
        List<Vm> vms = hostVmsMap.get(host.getId());
        for (int i = 0; i < vmsNum; i++) {
            moveOutVms.put(vms.get(0).getId(), vms.get(0));
            vms.remove(0);
        }
    }

    public void updateAllZeroHostMap(List<Long> removeFromZeroMapHosts, Map<Long, Host> allZeroHostMap) {
        if (removeFromZeroMapHosts != null && removeFromZeroMapHosts.size() > 0) {
            for (Long hostId : removeFromZeroMapHosts) {
                allZeroHostMap.remove(hostId);
            }
        }
    }

    public Map<Long, Vm> getAllVm(List<Host> hostList) {
        Map<Long, Vm> vmMap = new HashMap<>();
        if (hostList != null & hostList.size() > 0) {
            for (Host host : hostList) {
                List<Vm> vmList = host.getVmList();
                if (vmList != null && vmList.size() > 0) {
                    for (Vm vm : vmList) {
                        vmMap.put(vm.getId(), vm);
                    }
                }
            }
        }
        return vmMap;
    }

    public double calculateAverageDatacenterUsed(Map<Long, Double> hostCpuMapWithoutZero, int unassignmentVms) {
        double rate = unassignmentVms * constant.PERCENTAGE_OF_ONE_VM_TO_HOST;
        if (hostCpuMapWithoutZero != null && hostCpuMapWithoutZero.size() > 0) {
            for (Map.Entry<Long, Double> e : hostCpuMapWithoutZero.entrySet()) {
                rate += e.getValue();
            }

        }
        return rate / hostCpuMapWithoutZero.size();
    }
}
