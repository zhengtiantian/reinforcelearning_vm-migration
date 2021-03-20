package m.migrationAlgorithm;

import m.algorithmTool.reinforcementLearning.QTable;
import m.po.*;
import m.util.Constant;
import m.util.Counter;
import m.util.PowerTool;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ReinforcementLearning extends MigrationTool implements Migration {
    /**
     * get the implement of m.migrationAlgorithm.actuator
     */
    private static QTable act = new QTable();

    private static PowerTool powerTool = new PowerTool();

    private static Counter counter = new Counter();

    private static Constant constant = new Constant();
    /**
     * The number of times the m.migrationAlgorithm.agent relearns after getting the action from the Q table and cannot execute
     */
    private static List<Host> hostList;

    private static Map<Long, Double> hostCpuMap;

    private static Map<Long, List<Vm>> hostVmsMap;

    private static Map<Vm, Host> vmToHostMap;

    private static Map<Long, Host> hostMap;

    private static Map<Long, Host> shutdownHosts;

    private static double totalPower;

    private double MAX_CPU_UTILIZATION_THERSHOLD = 0.8;

    public static final double DATACENTER_UTILIZATION_THRESHOLD = 0.7;

    @Override
    public ProcessResult processMigration(EnvironmentInfo info) {
        long startTime = System.nanoTime();
        hostList = new ArrayList<>(info.getDatacenter().getHostList());
        shutdownHosts = new HashMap<>();
        hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        hostVmsMap = hostList.stream().collect(Collectors.toMap(Host::getId, p -> {
            return new ArrayList<>(p.getVmList());
        }));
        hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        vmToHostMap = new HashMap<>();
        sortHostByIncreasingCpuUtilization(hostList);

        totalPower = powerTool.getTotalPower(hostList, hostCpuMap);
        for (Host host : hostList) {
            if (host.getCpuPercentUtilization() == 0) {
                continue;
            }
            int state = act.getStateByCpuUtilizition(host.getCpuPercentUtilization());
            int action = act.getAction(state);
            ExpectedResult result = canDo(action, host);
            int nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
            counter.addOneIterateTime();
            if (result.isCanDo()) {
                updateHostAndTargetHost(host, hostVmsMap, result.getVmsToHosts());
                updateMigrationMap(vmToHostMap, result.getVmsToHosts());
                if (action == 2) {
                    shutdownHosts.put(host.getId(), host);
                }
                act.updateQtable(state, action, result.getReward(), nextState);
            } else {
                int iterateTimes = 0;
                act.updateQtable(state, action, result.getReward(), nextState);
                while (iterateTimes < act.STUDYTIMESWHENFILLED) {
                    action = act.getAction(state);
                    ExpectedResult result1 = canDo(action, host);
                    nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
                    act.updateQtable(state, action, result1.getReward(), nextState);
                    counter.addOneIterateTime();
                    if (result1.isCanDo()) {
                        updateHostAndTargetHost(host, hostVmsMap, result1.getVmsToHosts());
                        updateMigrationMap(vmToHostMap, result1.getVmsToHosts());
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

        long endTime = System.nanoTime();
        long diff = endTime - startTime;
        counter.addTime(diff);
        ProcessResult pr = new ProcessResult();
        pr.setShutdownHosts(shutdownHosts);
        pr.setVmToHostMap(vmToHostMap);
        return pr;
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
            ExpectedResult result = canDoAction2(host, hostCpuMap, hostVmsMap, hostMap, totalPower);
            if (result.isCanDo()) {
                return createResult(false, null, -result.getReward());
            }
            ExpectedResult result1 = canDoAction1(host, hostCpuMap, hostMap, totalPower);
            if (result1.isCanDo()) {
                return createResult(false, null, -result1.getReward());
            }
            return createResult(true, null, 0.0);
        }
        if (action == 1) {
            ExpectedResult result = canDoAction2(host, hostCpuMap, hostVmsMap, hostMap, totalPower);
            if (result.isCanDo()) {
                return createResult(false, null, -result.getReward());
            }
            return canDoAction1(host, hostCpuMap, hostMap, totalPower);
        }
        if (action == 2) {
            return canDoAction2(host, hostCpuMap, hostVmsMap, hostMap, totalPower);
        }
        return createResult(false, null, 0.0);
    }


    /**
     * Determine whether can do action1(migrate one vm to other host)
     * the mainly purpose is to reduce the risk of SLA(Service Level Agreement)
     *
     * @param host
     * @return ExpectedResult
     */
    ExpectedResult canDoAction1(Host host, Map<Long, Double> hostCpuMap, Map<Long, Host> hostMap, double totalPower) {
        boolean theLeastUsed = istheleastUsedHost(host, hostCpuMap);
        if (theLeastUsed && hostCpuMap.get(host.getId()) < MAX_CPU_UTILIZATION_THERSHOLD) {
            return createResult(true, null, 0.0);
        }
        double totalPowerAfterMigrate = totalPower;
        Map.Entry<Long, Double> mostSaving = getMostSavingTargatHost(host, hostCpuMap);
        totalPowerAfterMigrate -= (powerTool.getPower(host, hostCpuMap.get(host.getId())) - powerTool.getPower(host, hostCpuMap.get(host.getId()) - (constant.PERCENTAGE_OF_ONE_VM_TO_HOST)));
        totalPowerAfterMigrate += (powerTool.getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue() + (constant.PERCENTAGE_OF_ONE_VM_TO_HOST)) - powerTool.getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue()));
        if ((mostSaving.getValue() + (constant.PERCENTAGE_OF_ONE_VM_TO_HOST)) <= 1
                && (mostSaving.getValue() + ((double) constant.VM_PES / (double) constant.HOST_PES)) < hostCpuMap.get(host.getId())
                && ((totalPowerAfterMigrate <= totalPower) || hostCpuMap.get(host.getId()) > MAX_CPU_UTILIZATION_THERSHOLD)) {
            List<VmToHost> pairList = new ArrayList<>();
            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(host.getVmList().get(new Random().nextInt(host.getVmList().size())));
            vmToHost.setHost(hostMap.get(mostSaving.getKey()));
            pairList.add(vmToHost);
            hostCpuMap.put(host.getId(), hostCpuMap.get(host.getId()) - (constant.PERCENTAGE_OF_ONE_VM_TO_HOST));
            hostCpuMap.put(mostSaving.getKey(), mostSaving.getValue() + (constant.PERCENTAGE_OF_ONE_VM_TO_HOST));
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
        if (rate >= DATACENTER_UTILIZATION_THRESHOLD) {
            return createResult(true, null, 0.0);
        }
        double utilization = hostCpuMap.get(host.getId());
        hostCpuMap.remove(host.getId());
        Map<Long, Double> hostCpuMapWithoutZero = removeAllZeroHosts(hostCpuMap);
        boolean isEnough = haveEnoughSpace(utilization, host, hostCpuMapWithoutZero, 0);
        if (!isEnough) {
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

            if (head == null || head.getCpuUtilization() + (constant.PERCENTAGE_OF_ONE_VM_TO_HOST) > 1) {
                hostCpuMap.put(host.getId(), utilization);
                return createResult(false, null, -10.0);
            }

            Host targetHost = hostMap.get(head.getHostId());
            totalPowerAfterMigrate -= powerTool.getPower(targetHost, head.getCpuUtilization());
            totalPowerAfterMigrate += powerTool.getPower(targetHost, head.getCpuUtilization() + (constant.PERCENTAGE_OF_ONE_VM_TO_HOST));

            if (totalPowerAfterMigrate >= totalPower) {
                hostCpuMap.put(host.getId(), utilization);
                return createResult(false, null, totalPower - totalPowerAfterMigrate);
            }

            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(vm);
            vmToHost.setHost(targetHost);
            migList.add(vmToHost);
            minHeap.poll();
            double newUtilization = head.getCpuUtilization() + (constant.PERCENTAGE_OF_ONE_VM_TO_HOST);
            head.setCpuUtilization(newUtilization);

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
