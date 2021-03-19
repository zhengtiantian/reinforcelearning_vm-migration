package m.migrationAlgorithm;

import m.po.*;
import m.util.Constant;
import m.util.Counter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;
import java.util.stream.Collectors;


public class GreedyMinMaxHostUtilization extends MigrationTool implements Migration {

    private static Constant constant = new Constant();

    private static Counter counter = new Counter();

    private static List<Host> hostList;

    private static Map<Long, Double> hostCpuMap;

    private static Map<Long, List<Vm>> hostVmsMap;

    private static Map<Vm, Host> vmToHostMap;

    private double MAX_CPU_UTILIZATION_THERSHOLD = 0.8;

    private double MIX_CPU_UTILIZATION_THERSHOLD = 0.4;

    @Override
    public ProcessResult processMigration(EnvironmentInfo info) {
        long startTime = System.nanoTime();
        hostList = new ArrayList<>(info.getDatacenter().getHostList());
        hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        hostVmsMap = hostList.stream().collect(Collectors.toMap(Host::getId, p -> {
            return new ArrayList<>(p.getVmList());
        }));
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
                ExpectedResult result2 = tryMigrateOneVm(host, targetHost);
                if (result2.isCanDo()) {
                    updateHostAndTargetHost(host, hostVmsMap, result.getVmsToHosts());
                    updateMigrationMap(vmToHostMap, result.getVmsToHosts());
                }
            }
            System.out.println();
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

    private ExpectedResult tryMigrateOneVm(Host host, Host targetHost) {

        if ((hostCpuMap.get(targetHost.getId()) + constant.PERCENTAGE_OF_ONE_VM_TO_HOST) >= hostCpuMap.get(host.getId())
                && (hostCpuMap.get(targetHost.getId()) + constant.PERCENTAGE_OF_ONE_VM_TO_HOST) > 1) {
            return createResult(false, null, null);
        }
        List<VmToHost> migList = new ArrayList<>();
        VmToHost vmToHost = new VmToHost();
        vmToHost.setVm(hostVmsMap.get(host.getId()).get(0));
        vmToHost.setHost(targetHost);
        updateHostCpuMap(host, hostCpuMap.get(host.getId()) - constant.PERCENTAGE_OF_ONE_VM_TO_HOST);
        updateHostCpuMap(targetHost, hostCpuMap.get(targetHost.getId()) + constant.PERCENTAGE_OF_ONE_VM_TO_HOST);
        return createResult(true, migList, null);
    }

    private void updateHostCpuMap(Host host, double utilizaiont) {
        hostCpuMap.put(host.getId(), utilizaiont);
    }

}
