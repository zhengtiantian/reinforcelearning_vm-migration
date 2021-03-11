package m.migrationAlgorithm;

import m.actuator;
import m.envirnment;
import m.po.*;
import m.util.Constant;
import m.util.PowerTool;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ReinforcementLearning implements Migration {
    /**
     * get the implement of m.migrationAlgorithm.actuator
     */
    private static actuator act = new actuator();

    private static PowerTool powerTool = new PowerTool();
    /**
     * get the implement of environment
     */
    private static envirnment envirnment = new envirnment();

    private static Constant constant = new Constant();

    /**
     * The number of times the m.migrationAlgorithm.agent relearns after getting the action from the Q table and cannot execute
     */
    private static final int STUDYTIMESWHENFILLED = 3;

    private static final double DATACENTER_UTILIZATION_THRESHOLD = 0.6;

    private static List<Host> hostList;

    private static Map<Long, Double> hostCpuMap;

    private static Map<Long, List<Vm>> hostVmsMap;

    private static Map<Vm, Host> vmToHostMap;

    private static Map<Long, Host> hostMap;

    private static Map<Long, Host> shutdownHosts;

    private static double totalPower;

    @Override
    public ProcessResult processMigration(EnvironmentInfo info) {

        hostList = new ArrayList<>(info.getDatacenter().getHostList());
        shutdownHosts = new HashMap<>();
        hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        hostVmsMap = hostList.stream().collect(Collectors.toMap(Host::getId, p -> {
            return new ArrayList<>(p.getVmList());
        }));
        hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        vmToHostMap = new HashMap<>();
        hostList.sort(new Comparator<Host>() {
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

        totalPower = powerTool.getTotalPower(hostList, hostCpuMap);
        for (Host host : hostList) {
            if (!host.isActive()) {
                continue;
            }
            int state = act.getStateByCpuUtilizition(host.getCpuPercentUtilization());
            int action = act.getAction(state);
            ExpectedResult result = canDo(action, host);
            int nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
            if (result.isCanDo()) {
                updateHostAndTargetHost(host, result.getVmsToHosts());
                updateMigrationMap(result.getVmsToHosts());
                if (action == 2) {
                    shutdownHosts.put(host.getId(), host);
                }
                act.updateQtable(state, action, result.getReward(), nextState);
            } else {
                int iterateTimes = 0;
                act.updateQtable(state, action, result.getReward(), nextState);
                while (iterateTimes < STUDYTIMESWHENFILLED) {
                    action = act.getAction(state);
                    ExpectedResult result1 = canDo(action, host);
                    nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
                    act.updateQtable(state, action, result1.getReward(), nextState);
                    if (result1.isCanDo()) {
                        updateHostAndTargetHost(host, result1.getVmsToHosts());
                        updateMigrationMap(result1.getVmsToHosts());
                        if (action == 2) {
                            shutdownHosts.put(host.getId(), host);
                        }
                        break;
                    }
                    iterateTimes++;
                }

            }
            totalPower = powerTool.getTotalPower(hostList, hostCpuMap);
        }

        ProcessResult pr = new ProcessResult();
        pr.setShutdownHosts(shutdownHosts);
        pr.setVmToHostMap(vmToHostMap);
        return pr;
    }

    private void updateMigrationMap(List<VmToHost> vmsToHosts) {
        if (vmsToHosts != null && vmsToHosts.size() > 0) {
            for (VmToHost vth : vmsToHosts) {
                vmToHostMap.put(vth.getVm(), vth.getHost());
            }
        }
    }

    private void updateHostAndTargetHost(Host host, List<VmToHost> vmsToHosts) {
        if (vmsToHosts != null && vmsToHosts.size() > 0) {
            for (VmToHost vmsToHost : vmsToHosts) {
                hostVmsMap.get(host.getId()).remove(vmsToHost.getVm());
                hostVmsMap.get(vmsToHost.getHost().getId()).add(vmsToHost.getVm());
            }

        }
    }

    /**
     * 1.get the hosts information
     * 2.calculate the total power consumption of the datacenter
     * 3.iterate the hosts
     * 3.1. get the state by host's cpu utilization
     * 3.2. get the estimate action from q-table.
     * 3.3. Determine whether can do the actions in the action array
     * 3.3.1 if can do then migrate all the vms
     * 3.3.2 update the q-table
     * 3.3.3 if can not do relearn the action.the upper limit is 3 times
     * 3.3.4 update the q-table
     * 3.4. recalculate the total power of the data center
     *
     * @param
     */


    private double dataCenterUtilization(Map<Long, Double> hostCpuMap) {
        double totalUtilization = 0.0;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            totalUtilization += e.getValue();
        }
        return totalUtilization / (hostCpuMap.size() * 100.0);
    }


    /**
     * Determine whether the given action from the Q table is the best action
     *
     * @param action
     * @param host
     * @return ExpectedResult
     */
    private ExpectedResult canDo(int action, Host host) {
        if (action == 0) {
            ExpectedResult result = canDoAction2(host);
            if (result.isCanDo()) {
                return createResult(false, null, -result.getReward());
            }
            ExpectedResult result1 = canDoAction1(host);
            if (result1.isCanDo()) {
                return createResult(false, null, -result1.getReward());
            }
            return createResult(true, null, 0.0);
        }
        if (action == 1) {
            ExpectedResult result = canDoAction2(host);
            if (result.isCanDo()) {
                return createResult(false, null, -result.getReward());
            }
            return canDoAction1(host);
        }
        if (action == 2) {
            return canDoAction2(host);
        }
        return createResult(false, null, 0.0);
    }

    private boolean istheleastUsedHost(Host host, Map<Long, Double> hostMap) {
        double utilization = host.getCpuPercentUtilization();
        for (Map.Entry<Long, Double> e : hostMap.entrySet()) {
            if (utilization > e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine whether can do action1(migrate one vm to other host)
     * the mainly purpose is to reduce the risk of SLA(Service Level Agreement)
     *
     * @param host
     * @return ExpectedResult
     */
    private ExpectedResult canDoAction1(Host host) {
        boolean theLeastUsed = istheleastUsedHost(host, hostCpuMap);
        if (theLeastUsed) {
            return createResult(true, null, 0.0);
        }
        double totalPowerAfterMigrate = totalPower;
        Map.Entry<Long, Double> mostSaving = getMostSavingTargatHost(host);
        totalPowerAfterMigrate -= (powerTool.getPower(host, hostCpuMap.get(host.getId())) - powerTool.getPower(host, hostCpuMap.get(host.getId()) - (constant.VM_PES / constant.HOST_PES)));
        totalPowerAfterMigrate += (powerTool.getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES)) - powerTool.getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue()));
        if ((mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES)) <= 100
                && (mostSaving.getValue() + (constant.VM_PES / constant.HOST_PES)) < hostCpuMap.get(host.getId())
                && ((totalPowerAfterMigrate <= totalPower) || hostCpuMap.get(host.getId()) > 80)) {
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

    private Map.Entry<Long, Double> getMostSavingTargatHost(Host host) {
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
    private ExpectedResult canDoAction2(Host host) {

        double rate = dataCenterUtilization(hostCpuMap);
        if (rate >= DATACENTER_UTILIZATION_THRESHOLD) {
            return createResult(true, null, 0.0);
        }
        double utilization = hostCpuMap.get(host.getId());
        hostCpuMap.remove(host.getId());
        double sumFreeUt = 0.0;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
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
            updateHostMap(host, minHeap);
        } else {
            hostCpuMap.put(host.getId(), utilization);
        }
        double reward = totalPower - totalPowerAfterMigrate;
        totalPower = totalPowerAfterMigrate;

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
            HostAndCpuUtilization h = new HostAndCpuUtilization();
            h.setHostId(entry.getKey());
            h.setCpuUtilization(entry.getValue());
            minHeap.add(h);
        }
        return minHeap;
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


    /**
     * recalculate the cpu utilization map
     *
     * @param host
     * @param minHeap
     */
    private void updateHostMap(Host host, PriorityQueue<HostAndCpuUtilization> minHeap) {
        hostCpuMap.put(host.getId(), 0.0);
        for (HostAndCpuUtilization hostAndCpuUtilization : minHeap) {
            hostCpuMap.put(hostAndCpuUtilization.getHostId(), hostAndCpuUtilization.getCpuUtilization());
        }
    }


}
