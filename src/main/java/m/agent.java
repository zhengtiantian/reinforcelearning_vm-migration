package m;

import ch.qos.logback.classic.Level;
import m.migrationAlgorithm.Migration;
import m.migrationAlgorithm.ReinforcementLearning;
import m.po.VmToHost;
import m.util.printer;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.util.Log;
import m.po.EnvironmentInfo;
import sun.misc.VM;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
     * get m.migrationAlgorithm.printer
     */
    private static m.util.printer printer = new printer();

    /**
     * Indicates the interval of historical power consumption of the host
     */
    private static double INTERVAL = 5;

    /**
     * How long does it take to get information from the queue
     */
    private static int INTERVAL_INT = 3000;

    private static boolean allHostsHaveNoVms = true;

    private static Migration migrate = new ReinforcementLearning();


    /**
     * main function create two threads
     * the first illustrate that strat the simulation - produce infomation of hosts and vms.
     * the second one simulate the m.migrationAlgorithm.agent, witch get information of hosts and vms from queue.
     *
     * @param args
     */
    public static void main(String[] args) {
        Log.setLevel(Level.DEBUG);
        ExecutorService ex = Executors.newFixedThreadPool(3);

        agent agent = new agent();

        // create a thread to start the m.migrationAlgorithm.envirnment
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

            ex.submit(new Runnable() {
                @Override
                public void run() {
                    printer.recordPowerConsumption(envirnment);
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
            actuator.iniQtable();
            waitSomeMillis(60 * 1000);
            while (simulation.isRunning()) {
                EnvironmentInfo info = new EnvironmentInfo();
                info.setDatacenter(envirnment.getDatacenter());
                info.setBroker(envirnment.getBroker());
                queue.offer(info);
                simulation.runFor(INTERVAL);
                waitSomeMillis(5 * 1000);
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
                    if (allHostsHaveNoVms) {
                        checkAllHosts(info.getDatacenter().getHostList());
                        if (allHostsHaveNoVms) {
                            continue;
                        }
                    }


                    processInfo(info);
                } catch (Exception e) {
                    System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" +
                            "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                    e.printStackTrace();
                }

            }
            waitSomeMillis(5 * 1000);
        }

    }

    private void processInfo(EnvironmentInfo info) {
        Map<Vm, Host> vmHostMap = migrate.processMigration(info);
        migrateVms(vmHostMap);
    }

    private void migrateVms(Map<Vm, Host> vmHostMap) {
        if (vmHostMap != null && vmHostMap.size() > 0) {
            for (Map.Entry<Vm, Host> vm : vmHostMap.entrySet()) {
                envirnment.getDatacenter().requestVmMigration(vm.getKey(),vm.getValue());
            }
        }



    }

    private void checkAllHosts(List<Host> hostList) {
        for (Host host : hostList) {
            if (host.getVmList() != null && host.getVmList().size() > 0) {
                allHostsHaveNoVms = false;
            }
        }

    }

    private static void waitSomeMillis(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
