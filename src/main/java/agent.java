import javafx.util.Pair;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import po.EnvironmentInfo;
import po.ExpectedResult;
import po.HostAndCpuUtilization;
import po.VmToHost;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class agent {

    private static ConcurrentLinkedQueue<EnvironmentInfo> queue = new ConcurrentLinkedQueue<>();

    private static envirnment envirnment = new envirnment();

    private static final CloudSim simulation = envirnment.getSimulation();

    private static actuator act = new actuator();

    private static printer printer = new printer();

    private static double INTERVAL = 3.0;

    private static int INTERVAL_INT = 1000;

    private static Cloudlet cloudlet;

    private static double HOST_PES = envirnment.getDataCenter().getHostPes();

    private static double VM_PES = envirnment.getDataCenterBroker().getVmPes();

    private static int STUDYTIMESWHENFILLED = 3;


    public static void main(String[] args) {

        ExecutorService ex = Executors.newFixedThreadPool(2);

        agent agent = new agent();

        // create a thread to start the envirnment
        try {
            ex.submit(new Runnable() {
                @Override
                public void run() {
                    agent.startSimulation();
                }
            });

            // create a thread to get cpu and ram from HOST and VM
            ex.submit(new Runnable() {
                @Override
                public void run() {
                    agent.getInfo();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void startSimulation() {
        envirnment.start();
        iniQtable();
        for (int i = 0; i < 10000; i++) {
            EnvironmentInfo info = new EnvironmentInfo();
            info.setDatacenter(envirnment.getDatacenter());
            info.setBroker(envirnment.getBroker());
            queue.offer(info);
            simulation.runFor(INTERVAL);
            waitSomeMillis();
        }

    }

    public void getInfo() {
        for (int i = 0; i < 10000; i++) {

            if (!queue.isEmpty()) {
                EnvironmentInfo info = queue.poll();
                //print the cpu and ram usage of HOST and the cpu usage of VM
                printer.print(info, simulation);
                //TODO 1. Use algorithms to migrate VMs in some HOSTs with high CPU usage to HOSTs with low CPU usage
                //22/01/2021
                listenBroker();
                //25/01/2021
                migrateVms(info);
            }

            waitSomeMillis();
        }
    }

    private void migrateVms(EnvironmentInfo info) {
        List<Host> hostList = info.getDatacenter().getHostList();
        Map<Long, Double> hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));

        double totalPower = getTotalPower(hostList, hostCpuMap);


        for (Host host : hostList) {
            int state = act.getStateByCpuUtilizition(host.getCpuPercentUtilization());

            int action = act.getAction(state);

            ExpectedResult result = canDo(action, host, hostList, hostCpuMap, totalPower);
            int nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
            if (result.isCanDo()) {
                migrate(result.getVmsToHosts());

                act.updateQtable(state, action, result.getReward(), nextState);
            } else {
                boolean canDo = false;
                int iterateTimes = 0;
                while (canDo && iterateTimes < STUDYTIMESWHENFILLED) {

                    ExpectedResult result1 = canDo(action, host, hostList, hostCpuMap, totalPower);
                    nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
                    act.updateQtable(state, action, result1.getReward(), nextState);
                    if (result1.isCanDo()) {
                        migrate(result1.getVmsToHosts());
                        act.updateQtable(state, action, result.getReward(), nextState);
                        break;
                    }
                }

            }

            totalPower = getTotalPower(hostList, hostCpuMap);
        }


    }

    public void migrate(List<VmToHost> vmHostList) {
        for (VmToHost vmToHost : vmHostList) {
            envirnment.getDatacenter().requestVmMigration(vmToHost.getVm(), vmToHost.getHost());
        }
    }


    private ExpectedResult canDo(int action, Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        if (action == 0) {
            ExpectedResult result = canDoAction2(host, hostList, hostMap, totalPower);
            if (result.isCanDo()) {
                return createResult(false, null, result.getReward());
            }
            ExpectedResult result1 = canDoAction1(host, hostList, hostMap, totalPower);
            if (result1.isCanDo()) {
                return createResult(false, null, result1.getReward());
            }
            return createResult(true, null, 0.0);
        }
        if (action == 1) {
            ExpectedResult result = canDoAction2(host, hostList, hostMap, totalPower);
            if (result.isCanDo()) {
                return createResult(false, null, result.getReward());
            }
            return canDoAction1(host, hostList, hostMap, totalPower);
        }
        if (action == 2) {
            return canDoAction2(host, hostList, hostMap, totalPower);
        }
        return createResult(false, null, 0.0);
    }

    private ExpectedResult canDoAction1(Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        double totalPowerAfterMigrate = totalPower;
        for (Host host1 : hostList) {

            totalPowerAfterMigrate -= host.getPowerModel().getPower(host.getCpuPercentUtilization() - (VM_PES / HOST_PES));
            totalPowerAfterMigrate += host1.getPowerModel().getPower(host1.getCpuPercentUtilization() + (VM_PES / HOST_PES));
            if (totalPowerAfterMigrate < totalPower && host1.getCpuPercentUtilization() < (100 - (VM_PES / HOST_PES))) {
                List<VmToHost> pairList = new ArrayList<>();
                VmToHost vmToHost = new VmToHost();
                vmToHost.setVm(host.getVmList().get(new Random().nextInt(host.getVmList().size())));
                vmToHost.setHost(host1);
                pairList.add(vmToHost);
                hostMap.put(host.getId(), hostMap.get(host.getId()) - (VM_PES / HOST_PES));
                hostMap.put(host1.getId(), hostMap.get(host1.getId()) + (VM_PES / HOST_PES));
                return createResult(true, pairList, totalPower - totalPowerAfterMigrate);
            }
        }

        return createResult(false, null, totalPower - totalPowerAfterMigrate);
    }

    private ExpectedResult canDoAction2(Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        double utilization = host.getCpuPercentUtilization();
        double sumFreeUt = 0.0;
        for (Host host1 : hostList) {
            sumFreeUt += (100 - host1.getCpuPercentUtilization());
        }
        if (utilization > sumFreeUt) {
            return createResult(false, null, -10.0);
        }

        //Polling strategy or lowest usage fisrt allocation strategy
        //return pollingAllocateVms(host,hostList,hostMap,totalPower);
        return lowestUsageFirst(host, hostList, hostMap, totalPower);

    }

    private ExpectedResult lowestUsageFirst(Host host, List<Host> hostList, Map<Long, Double> hostCpuMap, double totalPower) {
        hostCpuMap.remove(host.getId());
        //create min Heap
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
        Map<Long, Host> hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        List<VmToHost> migList = new ArrayList<>();
        double totalPowerAfterMigrate = totalPower;
        totalPowerAfterMigrate -= host.getPowerModel().getPower(host.getCpuPercentUtilization());
        for (Vm vm : host.getVmList()) {

            HostAndCpuUtilization head = minHeap.peek();

            if (head == null || head.getCpuUtilization() >= 100) {
                return createResult(false, null, -10.0);
            }

            if (head.getCpuUtilization() + (VM_PES / HOST_PES) > 100) {
                continue;
            }
            Host targetHost = hostMap.get(head.getHostId());
            totalPowerAfterMigrate -= targetHost.getPowerModel().getPower(head.getCpuUtilization());
            totalPowerAfterMigrate += targetHost.getPowerModel().getPower(head.getCpuUtilization() + (VM_PES / HOST_PES));

            if (totalPowerAfterMigrate >= totalPower) {
                return createResult(false, null, totalPower - totalPowerAfterMigrate);
            }
            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(vm);
            vmToHost.setHost(targetHost);
            migList.add(vmToHost);
            minHeap.poll();
            head.setCpuUtilization(head.getCpuUtilization() + head.getCpuUtilization() + (VM_PES / HOST_PES));
            minHeap.add(head);
        }

        // update HostMap
        updateHostMap(host, hostCpuMap, minHeap);
        return createResult(true, migList, totalPower - totalPowerAfterMigrate);
    }

    private void updateHostMap(Host host, Map<Long, Double> hostMap, PriorityQueue<HostAndCpuUtilization> minHeap) {
        hostMap = new HashMap<>();
        hostMap.put(host.getId(), 0.0);
        for (HostAndCpuUtilization hostAndCpuUtilization : minHeap) {
            hostMap.put(hostAndCpuUtilization.getHostId(), hostAndCpuUtilization.getCpuUtilization());
        }

    }


    private double getTotalPower(List<Host> hostList, Map<Long, Double> hostMap) {
        double totalPower = 0.0;
        for (Host host : hostList) {
            totalPower += host.getPowerModel().getPower(hostMap.get(host.getId()));
        }
        return totalPower;
    }


    private void iniQtable() {
        act.initQtalbe();
        int[] actions = act.getAction();
        int[] states = act.getState();
        for (int state : states) {
            for (int action : actions) {
                for (int nextState : states) {
                    int reward = act.getReward(state, action, nextState);
                    act.updateQtable(state, action, reward, nextState);
                }
            }
        }
    }

    private void listenBroker() {
        for (Cloudlet cloudlet1 : envirnment.getBroker().getCloudletCreatedList()) {
            System.out.println(cloudlet1.getId() + "------------" + cloudlet1.getStatus());
        }
        Cloudlet LastCloudlet = envirnment.getDataCenterBroker().getLastCloudletFromList();
        if (cloudlet == null || cloudlet.getId() != LastCloudlet.getId()) {
            cloudlet = LastCloudlet;
            System.out.println("--------------------------------------------------------------" + cloudlet.getId());
            cloudlet.addOnFinishListener(envirnment.getDataCenterBroker()::createVmsAndCloudlet);
        }
    }

    private void waitSomeMillis() {
        try {
            Thread.sleep(INTERVAL_INT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private ExpectedResult createResult(boolean canDo, List<VmToHost> vmsToHosts, Double reward) {
        ExpectedResult result = new ExpectedResult();
        result.setCanDo(canDo);
        result.setVmsToHosts(vmsToHosts);
        result.setReward(reward);
        return result;
    }

}
