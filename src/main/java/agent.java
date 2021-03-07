import ch.qos.logback.classic.Level;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.util.Log;
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

/**
 * @author zhengxiangyu
 */
public class agent {

    /**
     * get the information from the environment
     */
    private static ConcurrentLinkedQueue<EnvironmentInfo> queue = new ConcurrentLinkedQueue<>();

    /**
     * get the implement of environment
     */
    private static envirnment envirnment = new envirnment();

    /**
     * get the simulation
     */
    private static final CloudSim simulation = envirnment.getSimulation();

    /**
     * get the implement of actuator
     */
    private static actuator act = new actuator();

    /**
     * get printer
     */
    private static printer printer = new printer();

    /**
     * Indicates the interval of historical power consumption of the host
     */
    private static double INTERVAL = 3.0;

    /**
     * How long does it take to get information from the queue
     */
    private static int INTERVAL_INT = 3000;

    private static Cloudlet cloudlet;

    /**
     * each host cores
     */
    private static double HOST_PES = envirnment.getDataCenter().getHostPes();

    /**
     * each vm cores
     */
    private static double VM_PES = envirnment.getDataCenterBroker().getVmPes();

    /**
     * The number of times the agent relearns after getting the action from the Q table and cannot execute
     */
    private static final int STUDYTIMESWHENFILLED = 3;

    private static boolean allHostsHaveNoVms = true;

    private static final double DATACENTER_UTILIZATION_THRESHOLD = 0.6;


    /**
     * main function create two threads
     * the first illustrate that strat the simulation - produce infomation of hosts and vms.
     * the second one simulate the agent, witch get information of hosts and vms from queue.
     *
     * @param args
     */
    public static void main(String[] args) {
        Log.setLevel(Level.DEBUG);
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
            waitSomeMillis(60 * 1000);
            // create a thread to get cpu and ram from HOST and VM
            ex.submit(new Runnable() {
                @Override
                public void run() {
                    agent.getInfo();
                }
            });
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

    }


    /**
     * start simulation and get the information from the environment each second then put the information into the queue.
     */
    public void startSimulation() {
        try {
            envirnment.start();
            iniQtable();
            waitSomeMillis(60 * 1000);
            while (simulation.isRunning()) {
                EnvironmentInfo info = new EnvironmentInfo();
                info.setDatacenter(envirnment.getDatacenter());
                info.setBroker(envirnment.getBroker());
                queue.offer(info);
                simulation.runFor(INTERVAL);
                waitSomeMillis(10 * 1000);
            }
        } catch (Exception e) {
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" +
                    "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            e.printStackTrace();
        }

    }

    /**
     * get the information from the queue then sent the assigment of create vms and vm migration to the environment
     */
    public void getInfo() {

        for (int i = 0; i < 100000; i++) {
            if (!queue.isEmpty()) {
                EnvironmentInfo info = queue.poll();
                try {
                    //print the cpu and ram usage of HOST and the cpu usage of VM
                    printer.print(info, simulation);
                    //22/01/2021
                    listenBroker();
                    //25/01/2021
                    if (allHostsHaveNoVms) {
                        checkAllHosts(info.getDatacenter().getHostList());
                        if (allHostsHaveNoVms) {
                            continue;
                        }
                    }
                    migrateVms(info);
                } catch (Exception e) {
                    System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" +
                            "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                    e.printStackTrace();
                }

            }
            waitSomeMillis(10 * 1000);
        }
//        while (simulation.isRunning()) {
//
//        }
    }

    private void checkAllHosts(List<Host> hostList) {
        for (Host host : hostList) {
            if (host.getVmList() != null && host.getVmList().size() > 0) {
                allHostsHaveNoVms = false;
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
     * @param info
     */
    private void migrateVms(EnvironmentInfo info) {
        List<Host> hostList = info.getDatacenter().getHostList();
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
        Map<Long, Double> hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        double totalPower = getTotalPower(hostList, hostCpuMap);
        for (Host host : hostList) {

            int state = act.getStateByCpuUtilizition(host.getCpuPercentUtilization());
            int action = act.getAction(state);
            ExpectedResult result = canDo(action, host, hostList, hostCpuMap, totalPower);
            int nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
            if (result.isCanDo()) {
                migrate(action, host, result.getVmsToHosts());
                act.updateQtable(state, action, result.getReward(), nextState);
            } else {
                int iterateTimes = 0;
                act.updateQtable(state, action, result.getReward(), nextState);
                while (iterateTimes < STUDYTIMESWHENFILLED) {
                    action = act.getAction(state);
                    ExpectedResult result1 = canDo(action, host, hostList, hostCpuMap, totalPower);
                    nextState = act.getStateByCpuUtilizition(hostCpuMap.get(host.getId()));
                    act.updateQtable(state, action, result1.getReward(), nextState);
                    if (result1.isCanDo()) {
                        migrate(action, host, result1.getVmsToHosts());
                        break;
                    }
                    iterateTimes++;
                }

            }
            totalPower = getTotalPower(hostList, hostCpuMap);
        }


    }

    private double dataCenterUtilization(Map<Long, Double> hostCpuMap) {
        double totalUtilization = 0.0;
        for (Map.Entry<Long, Double> e : hostCpuMap.entrySet()) {
            totalUtilization += e.getValue();
        }
        return totalUtilization / (hostCpuMap.size() * 100.0);
    }


    /**
     * sent the assignment of vm migration to the environment
     *
     * @param action
     * @param host
     * @param vmHostList
     */
    public void migrate(int action, Host host, List<VmToHost> vmHostList) {
        if (vmHostList != null && vmHostList.size() > 0) {
            for (VmToHost vmToHost : vmHostList) {
                envirnment.getDatacenter().requestVmMigration(vmToHost.getVm(), vmToHost.getHost());
            }
            if (action == 2) {
                host.setActive(false);
            }
        }
    }


    /**
     * Determine whether the given action from the Q table is the best action
     *
     * @param action
     * @param host
     * @param hostList
     * @param hostMap
     * @param totalPower
     * @return ExpectedResult
     */
    private ExpectedResult canDo(int action, Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        if (action == 0) {
            ExpectedResult result = canDoAction2(host, hostList, hostMap, totalPower);
            if (result.isCanDo()) {
                return createResult(false, null, -result.getReward());
            }
            ExpectedResult result1 = canDoAction1(host, hostList, hostMap, totalPower);
            if (result1.isCanDo()) {
                return createResult(false, null, -result1.getReward());
            }
            return createResult(true, null, 0.0);
        }
        if (action == 1) {
            ExpectedResult result = canDoAction2(host, hostList, hostMap, totalPower);
            if (result.isCanDo()) {
                return createResult(false, null, -result.getReward());
            }
            return canDoAction1(host, hostList, hostMap, totalPower);
        }
        if (action == 2) {
            return canDoAction2(host, hostList, hostMap, totalPower);
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
     * @param hostList
     * @param hostCpuMap
     * @param totalPower
     * @return ExpectedResult
     */
    private ExpectedResult canDoAction1(Host host, List<Host> hostList, Map<Long, Double> hostCpuMap, double totalPower) {
        boolean theLeastUsed = istheleastUsedHost(host, hostCpuMap);
        if (theLeastUsed) {
            return createResult(true, null, 0.0);
        }
        Map<Long, Host> hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        double totalPowerAfterMigrate = totalPower;
        Map.Entry<Long, Double> mostSaving = getMostSavingTargatHost(hostCpuMap, host);
        totalPowerAfterMigrate -= getPower(host, hostCpuMap.get(host.getId()) - (VM_PES / HOST_PES));
        totalPowerAfterMigrate += getPower(hostMap.get(mostSaving.getKey()), mostSaving.getValue() + (VM_PES / HOST_PES));
        if ((mostSaving.getValue() + (VM_PES / HOST_PES)) <= 100
                && (mostSaving.getValue() + (VM_PES / HOST_PES)) < hostCpuMap.get(host.getId())
                && ((totalPowerAfterMigrate <= totalPower) || hostCpuMap.get(host.getId()) > 80)) {
            List<VmToHost> pairList = new ArrayList<>();
            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(host.getVmList().get(new Random().nextInt(host.getVmList().size())));
            vmToHost.setHost(hostMap.get(mostSaving.getKey()));
            pairList.add(vmToHost);
            hostCpuMap.put(host.getId(), hostCpuMap.get(host.getId()) - (VM_PES / HOST_PES));
            hostCpuMap.put(mostSaving.getKey(), mostSaving.getValue() + (VM_PES / HOST_PES));
            return createResult(true, pairList, 50.0);
        }

        return createResult(false, null, -20.0);
    }

    private Map.Entry<Long, Double> getMostSavingTargatHost(Map<Long, Double> hostCpuMap, Host host) {
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
     * @param hostList
     * @param hostMap
     * @param totalPower
     * @return ExpectedResult
     */
    private ExpectedResult canDoAction2(Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {

        //Polling strategy or lowest usage fisrt allocation strategy
        //return pollingAllocateVms(host,hostList,hostMap,totalPower);
        return lowestUsageFirst(host, hostList, hostMap, totalPower);

    }

    /**
     * Strategy for allocating virtual machines, which user PriorityQueue pull the host with lowest power consumption first
     *
     * @param host
     * @param hostList
     * @param hostCpuMap
     * @param totalPower
     * @return
     */
    private ExpectedResult lowestUsageFirst(Host host, List<Host> hostList, Map<Long, Double> hostCpuMap, double totalPower) {
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

        Map<Long, Host> hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        List<VmToHost> migList = new ArrayList<>();
        double totalPowerAfterMigrate = totalPower;
        totalPowerAfterMigrate -= getPower(host, utilization);
        for (Vm vm : host.getVmList()) {

            HostAndCpuUtilization head = minHeap.peek();

            if (head == null || head.getCpuUtilization() >= 100) {
                updateHostMap(host, hostCpuMap, minHeap);
                return createResult(false, null, -10.0);
            }

            if (head.getCpuUtilization() + (VM_PES / HOST_PES) > 100) {
                updateHostMap(host, hostCpuMap, minHeap);
                continue;
            }
            Host targetHost = hostMap.get(head.getHostId());
            totalPowerAfterMigrate -= getPower(targetHost, head.getCpuUtilization());
            totalPowerAfterMigrate += getPower(targetHost, head.getCpuUtilization() + (VM_PES / HOST_PES));

            if (totalPowerAfterMigrate >= totalPower) {
                updateHostMap(host, hostCpuMap, minHeap);
                return createResult(false, null, totalPower - totalPowerAfterMigrate);
            }
            VmToHost vmToHost = new VmToHost();
            vmToHost.setVm(vm);
            vmToHost.setHost(targetHost);
            migList.add(vmToHost);
            minHeap.poll();
            head.setCpuUtilization(head.getCpuUtilization() + (VM_PES / HOST_PES));
            minHeap.add(head);
        }

        // update HostMap
        updateHostMap(host, hostCpuMap, minHeap);
        return createResult(true, migList, totalPower - totalPowerAfterMigrate);
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
     * recalculate the cpu utilization map
     *
     * @param host
     * @param hostMap
     * @param minHeap
     */
    private void updateHostMap(Host host, Map<Long, Double> hostMap, PriorityQueue<HostAndCpuUtilization> minHeap) {
        hostMap.put(host.getId(), 0.0);
        for (HostAndCpuUtilization hostAndCpuUtilization : minHeap) {
            hostMap.put(hostAndCpuUtilization.getHostId(), hostAndCpuUtilization.getCpuUtilization());
        }

    }

    /**
     * calculate the total power of the data center according to the cpu utilization map and hostlist.
     *
     * @param hostList
     * @param hostMap
     * @return
     */
    private double getTotalPower(List<Host> hostList, Map<Long, Double> hostMap) {
        double totalPower = 0.0;
        for (Host host : hostList) {
            totalPower += getPower(host, hostMap.get(host.getId()));
        }
        return totalPower;
    }


    /**
     * inicial q-table
     */
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

    /**
     * monite the last cloudlet in current cloudlet list, when the cloudlet finished then create more cloudlets and vms.
     */
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

    private static void waitSomeMillis(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
     * @param host
     * @param cpuUtilization
     * @return
     */
    private double getPower(Host host, double cpuUtilization) {
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
