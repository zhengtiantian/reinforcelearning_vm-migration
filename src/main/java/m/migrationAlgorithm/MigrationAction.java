package m.migrationAlgorithm;

import m.po.ExpectedResult;
import m.po.HostAndCpuUtilization;
import m.po.VmToHost;
import m.util.Constant;
import m.util.PowerTool;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;

public abstract class MigrationAction {

    private static PowerTool powerTool = new PowerTool();

    private static Constant constant = new Constant();

    /**
     * Determine whether can do action1(migrate one vm to other host)
     * the mainly purpose is to reduce the risk of SLA(Service Level Agreement)
     *
     * @param host
     * @return ExpectedResult
     */
    ExpectedResult canDoAction1(Host host, Map<Long, Double> hostCpuMap, Map<Long, Host> hostMap, double totalPower) {
        boolean theLeastUsed = istheleastUsedHost(host, hostCpuMap);
        if (theLeastUsed && hostCpuMap.get(host.getId()) < 70) {
            return createResult(true, null, 0.0);
        }
        double totalPowerAfterMigrate = totalPower;
        Map.Entry<Long, Double> mostSaving = getMostSavingTargatHost(host, hostCpuMap);
        totalPowerAfterMigrate -= (powerTool.getPower(host, hostCpuMap.get(host.getId())) - powerTool.getPower(host, hostCpuMap.get(host.getId()) - (constant.VM_PES / constant.HOST_PES)));
        totalPowerAfterMigrate += (powerTool.getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES)) - powerTool.getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue()));
        if ((mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES)) <= 100
                && (mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES)) < hostCpuMap.get(host.getId())
                && ((totalPowerAfterMigrate <= totalPower) || hostCpuMap.get(host.getId()) > 70)) {
            List<VmToHost> pairList = new ArrayList<>();
            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(host.getVmList().get(new Random().nextInt(host.getVmList().size())));
            vmToHost.setHost(hostMap.get(mostSaving.getKey()));
            pairList.add(vmToHost);
            hostCpuMap.put(host.getId(), hostCpuMap.get(host.getId()) - (constant.VM_PES / constant.HOST_PES));
            hostCpuMap.put(mostSaving.getKey(), mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES));
            return createResult(true, pairList, 50.0);
        }
        return createResult(false, null, -20.0);
    }

    private Map.Entry<Long, Double> getMostSavingTargatHost(Host host, Map<Long, Double> hostCpuMap) {
        Map.Entry<Long, Double> mostSaving = null;
        Map.Entry<Long, Double> mostSavingMoreThanZero = null;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            if (e.getKey() == host.getId()) {
                continue;
            }
            if (mostSaving == null) {
                mostSaving = e;
            }
            if (mostSavingMoreThanZero == null && e.getValue() > 0) {
                mostSavingMoreThanZero = e;
            }

            if (mostSaving.getValue() > e.getValue()) {
                mostSaving = e;
            }

            if (mostSavingMoreThanZero != null && mostSavingMoreThanZero.getValue() > 0 && mostSavingMoreThanZero.getValue() > e.getValue()) {
                mostSavingMoreThanZero = e;
            }
        }
        return mostSavingMoreThanZero == null ? mostSaving : mostSavingMoreThanZero;
    }

    /**
     * Determine whether can do action1(migrate all of the vms from the host to other hosts)
     *
     * @param host
     * @return ExpectedResult
     */
    ExpectedResult canDoAction2(Host host, Map<Long, Double> hostCpuMap, Map<Long, List<Vm>> hostVmsMap, Map<Long, Host> hostMap, double totalPower) {

        double rate = dataCenterUtilization(hostCpuMap);
        if (rate >= constant.DATACENTER_UTILIZATION_THRESHOLD) {
            return createResult(true, null, 0.0);
        }
        double utilization = hostCpuMap.get(host.getId());
        hostCpuMap.remove(host.getId());
        double sumFreeUt = 0.0;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            if (e.getValue() == 0) {
                continue;
            }
            sumFreeUt += (100 - e.getValue());
        }
        if (utilization > sumFreeUt) {
            hostCpuMap.put(host.getId(), host.getCpuPercentUtilization());
            return createResult(false, null, -10.0);
        }

        //create min Heap
        PriorityQueue<HostAndCpuUtilization> minHeap = createPriorityQueue(hostCpuMap);

        List<VmToHost> migList = new ArrayList<>();
        double totalPowerAfterMigrate = totalPower;
        totalPowerAfterMigrate -= powerTool.getPower(host, utilization);
        for (Vm vm : hostVmsMap.get(host.getId())) {

            HostAndCpuUtilization head = minHeap.peek();

            if (head == null || head.getCpuUtilization() >= 100 || head.getCpuUtilization() + (constant.VM_PES / constant.HOST_PES) > 100) {
                hostCpuMap.put(host.getId(), utilization);
                return createResult(false, null, -10.0);
            }

            Host targetHost = hostMap.get(head.getHostId());
            totalPowerAfterMigrate -= powerTool.getPower(targetHost, head.getCpuUtilization());
            totalPowerAfterMigrate += powerTool.getPower(targetHost, head.getCpuUtilization() + (constant.VM_PES / constant.HOST_PES));

            if (totalPowerAfterMigrate >= totalPower) {
                hostCpuMap.put(host.getId(), utilization);
                return createResult(false, null, totalPower - totalPowerAfterMigrate);
            }

            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(vm);
            vmToHost.setHost(targetHost);
            migList.add(vmToHost);
            minHeap.poll();
            head.setCpuUtilization(head.getCpuUtilization() + (constant.VM_PES / constant.HOST_PES));
            minHeap.add(head);
        }

        // update HostMap
        if (migList.size() > 0) {
            updateHostMap(host, minHeap, hostCpuMap);
        } else {
            hostCpuMap.put(host.getId(), utilization);
        }
        double reward = totalPower - totalPowerAfterMigrate;

        return createResult(true, migList, reward);

    }


    private PriorityQueue<HostAndCpuUtilization> createPriorityQueue(Map<Long, Double> hostCpuMap) {
        PriorityQueue<HostAndCpuUtilization> minHeap = new PriorityQueue<>(hostCpuMap.entrySet().size(), new Comparator<HostAndCpuUtilization>() {
            @Override
            public int compare(HostAndCpuUtilization o1, HostAndCpuUtilization o2) {
                if (o1.getCpuUtilization() > o2.getCpuUtilization()) {
                    return 1;
                } else if (o1.getCpuUtilization() == o2.getCpuUtilization()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
        for (Map.Entry<Long, Double> entry : hostCpuMap.entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }
            HostAndCpuUtilization h = new HostAndCpuUtilization();
            h.setHostId(entry.getKey());
            h.setCpuUtilization(entry.getValue());
            minHeap.add(h);
        }
        return minHeap;
    }

    private boolean istheleastUsedHost(Host host, Map<Long, Double> hostMap) {
        double utilization = host.getCpuPercentUtilization();
        for (Map.Entry<Long, Double> e : hostMap.entrySet()) {
            if (e.getValue() == 0) {
                continue;
            }
            if (utilization > e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Assembly parameters
     *
     * @param canDo
     * @param vmsToHosts
     * @param reward
     * @return
     */
    private ExpectedResult createResult(boolean canDo, List<VmToHost> vmsToHosts, Double reward) {
        ExpectedResult result = new ExpectedResult();
        result.setCanDo(canDo);
        result.setVmsToHosts(vmsToHosts);
        result.setReward(reward);
        return result;
    }

    private double dataCenterUtilization(Map<Long, Double> hostCpuMap) {
        int size = hostCpuMap.size();
        double totalUtilization = 0.0;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            if (e.getValue() == 0) {
                size--;
                continue;
            }
            totalUtilization += e.getValue();
        }
        return totalUtilization / (size * 100.0);
    }


    /**
     * recalculate the cpu utilization map
     *
     * @param host
     * @param minHeap
     */
    private void updateHostMap(Host host, PriorityQueue<HostAndCpuUtilization> minHeap, Map<Long, Double> hostCpuMap) {
        hostCpuMap.put(host.getId(), 0.0);
        for (HostAndCpuUtilization hostAndCpuUtilization : minHeap) {
            hostCpuMap.put(hostAndCpuUtilization.getHostId(), hostAndCpuUtilization.getCpuUtilization());
        }
    }

}
