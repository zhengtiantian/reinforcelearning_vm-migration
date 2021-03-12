package m.migrationAlgorithm;

import m.actuator;
import m.po.*;
import m.util.Counter;
import m.util.PowerTool;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ReinforcementLearning extends MigrationAction implements Migration {
    /**
     * get the implement of m.migrationAlgorithm.actuator
     */
    private static actuator act = new actuator();

    private static PowerTool powerTool = new PowerTool();


    private static Counter counter = new Counter();
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
            if (host.getCpuPercentUtilization() == 0) {
                continue;
            }
            int state = act.getStateByCpuUtilizition(host.getCpuPercentUtilization());
            int action = act.getAction(state);
            ExpectedResult result = canDo(action, host);
            int nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
            counter.addOneIterateTime();
            if (result.isCanDo()) {
                updateHostAndTargetHost(host, result.getVmsToHosts());
                updateMigrationMap(result.getVmsToHosts());
                System.out.println("choose action" + action);
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


}
