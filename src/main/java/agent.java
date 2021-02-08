import javafx.util.Pair;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.resources.Ram;
import org.cloudbus.cloudsim.vms.Vm;
import po.Info;
import util.Conversion;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class agent {

    private static ConcurrentLinkedQueue<Info> queue = new ConcurrentLinkedQueue<>();

    private static envirnment envirnment = new envirnment();

    private static final CloudSim simulation = envirnment.getSimulation();

    private static actuator act = new actuator();

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
            Info info = new Info();
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
                Info info = queue.poll();
                //print the cpu and ram usage of HOST and the cpu usage of VM
                print(info);
                //TODO 1. Use algorithms to migrate VMs in some HOSTs with high CPU usage to HOSTs with low CPU usage
                //22/01/2021
                listenBroker();
                //25/01/2021
                migrateVms(info);
                //25/01/2021
            }

            waitSomeMillis();
        }
    }

    private void migrateVms(Info info) {
        List<Host> hostList = info.getDatacenter().getHostList();
        Map<Long, Double> hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        double totalPower = getTotalPower(hostList, hostMap);


        for (Host host : hostList) {
            int state = act.getStateByCpuUtilizition(host.getCpuPercentUtilization());

            int action = act.getAction(state);

            Pair<Boolean, List<Pair<Vm, Host>>> pair = canDo(action, host, hostList, hostMap, totalPower);
            if (pair.getKey()) {
                migrate(action,pair.getValue());
            } else {
                boolean canDo = false;
                int iterateTimes = 0;
                while (canDo && iterateTimes < STUDYTIMESWHENFILLED) {
                    if (action == 1) {
                        int nextState = act.getStateByCpuUtilizition(hostMap.get(host.getId()));
                        act.updateQtable(state, action, -10, nextState);
                    }
                    if (action == 2) {
                        act.updateQtable(state, action, -10, 0);
                    }
                    Pair<Boolean, List<Pair<Vm, Host>>> pair1 = canDo(action, host, hostList, hostMap, totalPower);
                    if (pair1.getKey()) {
                        migrate(action,pair1.getValue());
                        break;
                    }
                }

            }

            totalPower = getTotalPower(hostList, hostMap);
        }


    }

    public void migrate(int action,List<Pair<Vm, Host>> vmHostList){
        if (action == 1) {
            Pair<Vm, Host> vmHostPair = vmHostList.get(0);
            envirnment.getDatacenter().requestVmMigration(vmHostPair.getKey(), vmHostPair.getValue());
        }
        if (action == 2) {
            for (Pair<Vm, Host> vmHostPair : vmHostList) {
                envirnment.getDatacenter().requestVmMigration(vmHostPair.getKey(), vmHostPair.getValue());
            }

        }
    }


    private Pair<Boolean, List<Pair<Vm, Host>>> canDo(int action, Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        if (action == 0) {
            Pair<Boolean, List<Pair<Vm, Host>>> pair = canDoAction2(host, hostList, hostMap, totalPower);
            if (pair.getKey()) {
                return new Pair<>(false, new ArrayList<>());
            }
            Pair<Boolean, List<Pair<Vm, Host>>> pair1 = canDoAction1(host, hostList, hostMap, totalPower);
            if (pair1.getKey()) {
                return new Pair<>(false, new ArrayList<>());
            }
            return new Pair<>(true, new ArrayList<>());
        }
        if (action == 1) {
            Pair<Boolean, List<Pair<Vm, Host>>> pair = canDoAction2(host, hostList, hostMap, totalPower);
            if (pair.getKey()) {
                return new Pair<>(false, new ArrayList<>());
            }
            return canDoAction1(host, hostList, hostMap, totalPower);
        }
        if (action == 2) {
            return canDoAction2(host, hostList, hostMap, totalPower);
        }
        return new Pair<>(false, new ArrayList<>());
    }

    private Pair<Boolean, List<Pair<Vm, Host>>> canDoAction1(Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        //TODO finish it
        return new Pair<>(false, new ArrayList<>());
    }

    private Pair<Boolean, List<Pair<Vm, Host>>> canDoAction2(Host host, List<Host> hostList, Map<Long, Double> hostMap, double totalPower) {
        double utilization = host.getCpuPercentUtilization();
        double sumFreeUt = 0.0;
        for (Host host1 : hostList) {
            sumFreeUt += (100 - host1.getCpuPercentUtilization());
        }
        if (utilization > sumFreeUt) {
            return new Pair<>(false, null);
        }

        //TODO Polling strategy or minimum allocation strategy?
        //TODO update HostMap
        return new Pair<>(true, new ArrayList<>());
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
                    int reward = getReward(state, action, nextState);
                    act.updateQtable(state, action, reward, nextState);
                }
            }
        }
    }

    /**
     * reward range is 0-100
     * the action will get more scores when save more power
     *
     * @param state
     * @param action
     * @param nextState
     * @return
     */
    public int getReward(int state, int action, int nextState) {
        if (action == 2) {
            return state * 10;
        } else if (action == 1 && state > nextState) {
            return (state - nextState) * 10;
        } else {
            return 0;
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


    private void print(Info info) {

        info.getBroker().getVmCreatedList().forEach(vm ->

                System.out.printf(
                        "\t\tVm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%d). HOST %d %n",
                        vm.getId(), vm.getCpuPercentUtilization() * 100.0, vm.getNumberOfPes(),
                        vm.getCloudletScheduler().getCloudletExecList().size(),
                        vm.getHost().getId())
        );

        for (Host host : info.getDatacenter().getHostList()) {
            printHostCpuUtilizationAndPowerConsumption(host);
        }


        System.out.println();

        System.out.printf("%n%n");
    }

    private void printHostCpuUtilizationAndPowerConsumption(Host host) {
        System.out.printf("Host %d CPU utilization and power consumption%n", host.getId());
        final Map<Double, DoubleSummaryStatistics> utilizationPercentHistory = host.getUtilizationHistory();
        double totalWattsSec = 0;
        double prevUtilizationPercent = -1, prevWattsSec = -1;
        //time difference from the current to the previous line in the history
        double utilizationHistoryTimeInterval;
        double prevTime = 0;
        for (Map.Entry<Double, DoubleSummaryStatistics> entry : utilizationPercentHistory.entrySet()) {
            utilizationHistoryTimeInterval = entry.getKey() - prevTime;
            //The total Host's CPU utilization for the time specified by the map key
            final double utilizationPercent = entry.getValue().getSum();
            final double watts = host.getPowerModel().getPower(utilizationPercent);
            //Energy consumption in the time interval
            final double wattsSec = watts * utilizationHistoryTimeInterval;
            //Energy consumption in the entire simulation time
            totalWattsSec += wattsSec;
            //only prints when the next utilization is different from the previous one, or it's the first one
            System.out.printf(
                    "\tTime %8.2f | CPU Utilization %6.2f%% | Power Consumption: %8.2f W * %.2f s = %.2f Ws%n",
                    entry.getKey(), utilizationPercent * 100, watts, utilizationHistoryTimeInterval, wattsSec);
            prevUtilizationPercent = utilizationPercent;
            prevWattsSec = wattsSec;
            prevTime = entry.getKey();
        }

        System.out.printf(
                "Total Host %d Power Consumption in %.0f s: %.0f Ws (%.5f kWh)%n",
                host.getId(), simulation.clock(), totalWattsSec, Conversion.wattSecondsToKWattHours(totalWattsSec));
        final double powerWattsSecMean = totalWattsSec / simulation.clock();
        System.out.printf(
                "Mean %.2f Ws for %d usage samples (%.5f kWh)%n",
                powerWattsSecMean, utilizationPercentHistory.size(), Conversion.wattSecondsToKWattHours(powerWattsSecMean));
    }


    private void waitSomeMillis() {
        try {
            Thread.sleep(INTERVAL_INT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
