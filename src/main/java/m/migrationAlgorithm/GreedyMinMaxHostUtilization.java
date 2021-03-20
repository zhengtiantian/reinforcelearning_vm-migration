package m.migrationAlgorithm;

import m.po.*;
import m.util.Constant;
import m.util.Counter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class GreedyMinMaxHostUtilization extends MigrationTool implements Migration {

    private static Constant constant = new Constant();

    private static Counter counter = new Counter();

    private static List<Host> hostList;

    private static Map<Long, Double> hostCpuMap;

    private static Map<Long, List<Vm>> hostVmsMap;

    private static Map<Vm, Host> vmToHostMap;

    private static Map<Long, Host> hostMap;

    private double MAX_CPU_UTILIZATION_THERSHOLD = 0.6;

    private double MIX_CPU_UTILIZATION_THERSHOLD = 0.4;

    @Override
    public ProcessResult processMigration(EnvironmentInfo info) {
        long startTime = System.nanoTime();
        hostList = new ArrayList<>(info.getDatacenter().getHostList());
        hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        hostVmsMap = hostList.stream().collect(Collectors.toMap(Host::getId, p -> {
            return new ArrayList<>(p.getVmList());
        }));
        hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        vmToHostMap = new HashMap<>();
        sortHostByIncreasingCpuUtilization(hostList);

        for (int i = 0; i < hostList.size(); i++) {
            counter.addOneIterateTime();
            Host host = hostList.get(i);
            Double cpuUtilization = hostCpuMap.get(hostList.get(i).getId());
            if (cpuUtilization == 0 || (cpuUtilization > MIX_CPU_UTILIZATION_THERSHOLD && cpuUtilization < MAX_CPU_UTILIZATION_THERSHOLD)) {
                continue;
            }
            for (int j = hostList.size() - 1; j > i; j--) {
                Host targetHost = hostList.get(j);
                counter.addOneIterateTime();
                ExpectedResult result = tryShutdown(host, targetHost);
                if (result.isCanDo()) {
                    updateHostAndTargetHost(host, hostVmsMap, result.getVmsToHosts());
                    updateMigrationMap(vmToHostMap, result.getVmsToHosts());
                    continue;
                }
                counter.addOneIterateTime();

            }
            ExpectedResult result2 = tryMoveOutVms(host);
            if (result2.isCanDo()) {
                updateHostAndTargetHost(host, hostVmsMap, result2.getVmsToHosts());
                updateMigrationMap(vmToHostMap, result2.getVmsToHosts());
            }
        }
        long endTime = System.nanoTime();
        counter.addTime(endTime - startTime);
        ProcessResult pr = new ProcessResult();
        pr.setVmToHostMap(vmToHostMap);
        return pr;
    }


    private ExpectedResult tryShutdown(Host host, Host targetHost) {

        List<Vm> vms = hostVmsMap.get(host.getId());
        if (hostCpuMap.get(targetHost.getId()) + (vms.size() * constant.PERCENTAGE_OF_ONE_VM_TO_HOST) > 1) {
            return createResult(false, null, null);
        }
        List<VmToHost> migList = new ArrayList<>();
        for (Vm vm : vms) {
            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(vm);
            vmToHost.setHost(targetHost);
            migList.add(vmToHost);
        }
        updateHostCpuMap(host, 0);
        updateHostCpuMap(targetHost, hostCpuMap.get(targetHost.getId()) + (vms.size() * constant.PERCENTAGE_OF_ONE_VM_TO_HOST));
        return createResult(true, migList, null);
    }

    private ExpectedResult tryMoveOutVms(Host host) {
        int vmsNum = calculateVmsMoveout(hostCpuMap.get(host.getId()), MAX_CPU_UTILIZATION_THERSHOLD);
        if (vmsNum <= 0) {
            return createResult(false, null, null);
        }

        Host targetHost = chooseSuitalbeHost(host, vmsNum);
        if (targetHost == null) {
            return createResult(false, null, null);
        }

        List<VmToHost> migList = new ArrayList<>();
        List<Vm> hostVms = hostVmsMap.get(host.getId());
        List<Vm> targetVms = hostVmsMap.get(targetHost.getId());
        for (int i = 0; i < vmsNum; i++) {
            VmToHost vmToHost = new VmToHost();
            Vm vm = hostVms.get(0);
            vmToHost.setVm(vm);
            vmToHost.setHost(targetHost);
            hostVms.remove(0);
            targetVms.add(vm);
            migList.add(vmToHost);
        }
        updateHostCpuMap(host, hostCpuMap.get(host.getId()) - vmsNum * constant.PERCENTAGE_OF_ONE_VM_TO_HOST);
        updateHostCpuMap(targetHost, hostCpuMap.get(targetHost.getId()) + vmsNum * constant.PERCENTAGE_OF_ONE_VM_TO_HOST);
        return createResult(true, migList, null);
    }

    private Host chooseSuitalbeHost(Host host, int vmsNum) {
        long maxHostId = -1;
        double maxHostUti = -1;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            if ((e.getValue() + vmsNum * constant.PERCENTAGE_OF_ONE_VM_TO_HOST) >= hostCpuMap.get(host.getId())
                    || (e.getValue() + vmsNum * constant.PERCENTAGE_OF_ONE_VM_TO_HOST) >= MAX_CPU_UTILIZATION_THERSHOLD) {
                continue;
            }
            if (e.getValue() > maxHostUti) {
                maxHostId = e.getKey();
                maxHostUti = e.getValue();
            }
        }

        if (maxHostId == -1) {
            return null;
        }
        return hostMap.get(maxHostId);
    }

    private void updateHostCpuMap(Host host, double utilizaiont) {
        hostCpuMap.put(host.getId(), utilizaiont);
    }
}
