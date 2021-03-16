package m;

import ch.qos.logback.classic.Level;
import m.migrationAlgorithm.GreedyMinMaxHostUtilization;
import m.migrationAlgorithm.Migration;
import m.migrationAlgorithm.ReinforcementLearning;
import m.po.ProcessResult;
import m.util.Constant;
import m.util.Counter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudsimplus.util.Log;
import m.po.EnvironmentInfo;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author zhengxiangyu
 */
public class agent {


    /**
     * get the implement of environment
     */
    private static envirnment envirnment = new envirnment();

    /**
     * get the simulation
     */
    private static final CloudSim simulation = envirnment.getSimulation();

    private static Constant constant = new Constant();

    private static Counter counter = new Counter();

    private static boolean allHostsHaveNoVms = true;

    private static Migration migrate = new ReinforcementLearning();
//    private static Migration migrate = new GreedyMinMaxHostUtilization();

    private static ConcurrentLinkedQueue<EnvironmentInfo> queue = envirnment.getQueue();

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
            waitSomeMillis((long)constant.SIMULATION_RUNNING_INTERVAL*1000);
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
            actuator.iniQtable();
            while (simulation.isRunning()) {
                simulation.runFor(constant.SIMULATION_RUNNING_INTERVAL);
                waitSomeMillis((long)constant.SIMULATION_RUNNING_INTERVAL*1000);
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
        while (simulation.isRunning()) {
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
            waitSomeMillis(1000);
        }

    }

    private void processInfo(EnvironmentInfo info) {
        ProcessResult pr = migrate.processMigration(info);
        migrateVms(pr.getVmToHostMap());
    }



    private void migrateVms(Map<Vm, Host> vmHostMap) {
        if (vmHostMap != null && vmHostMap.size() > 0) {
            counter.addMigrateTime(vmHostMap.size());
            for (Map.Entry<Vm, Host> vm : vmHostMap.entrySet()) {
                Host host = vm.getValue();
                if (!host.isActive()) {
                    host.setActive(true);
                }
                envirnment.getDatacenter().requestVmMigration(vm.getKey(), vm.getValue());
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
