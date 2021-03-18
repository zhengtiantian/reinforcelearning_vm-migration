package m.migrationAlgorithm;

import m.algorithmTool.antColonyOptimization.Ant;
import m.po.EnvironmentInfo;
import m.po.Position;
import m.po.ProcessResult;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AntColonyOptimization extends MigrationTool implements Migration {

    private double MAX_CPU_UTILIZATION_THERSHOLD = 0.8;

    private double MIX_CPU_UTILIZATION_THERSHOLD = 0.2;

    private static List<Host> hostList;

    private static Map<Long, Double> hostCpuMap;

    private static Map<Long, List<Vm>> hostVmsMap;

    private static Map<Long, Host> hostMap;

    private static Map<Long, Vm> vmMap;

    private static Map<Vm, Host> vmToHostMap;

    private static Map<Long, Vm> moveOutVms;

    private static Map<Long, Host> moveInHosts;

    private static Map<Long, Host> allZeroHostMap;

    private int antNum = 5;

    private List<Ant> ants;

    private static int Q = 100;
    private static int maxgen = 10;
    private double[][] pheromone;//pheromone matrix
    private double[][] Delta;//Delta matrix
    public Position[] bestTour;//best solution
    private double bestLength;
    List<Host> moveInHostList;
    List<Vm> moveOutVmList;

    @Override
    public ProcessResult processMigration(EnvironmentInfo info) {
        hostList = new ArrayList<>(info.getDatacenter().getHostList());
        hostMap = hostList.stream().collect(Collectors.toMap(Host::getId, Function.identity()));
        vmMap = getAllVm(hostList);
        hostCpuMap = hostList.stream().collect(Collectors.toMap(Host::getId, Host::getCpuPercentUtilization));
        hostVmsMap = hostList.stream().collect(Collectors.toMap(Host::getId, p -> {
            return new ArrayList<>(p.getVmList());
        }));
        vmToHostMap = new HashMap<>();
        moveOutVms = new HashMap<>();
        moveInHosts = getWithoutZeroHosts(hostList, hostCpuMap);
        allZeroHostMap = getZeroHosts(hostList, hostCpuMap);

        sortHostByIncreasingCpuUtilization(hostList);
        for (int i = 0; i < hostList.size(); i++) {
            Host host = hostList.get(i);
            Double cpuUtilization = hostCpuMap.get(hostList.get(i).getId());
            if (cpuUtilization == 0) {
                continue;
            }
            if (cpuUtilization < MIX_CPU_UTILIZATION_THERSHOLD) {
                tryShotdown(host);
                continue;
            }
            if (cpuUtilization > MAX_CPU_UTILIZATION_THERSHOLD) {
                tryMoveOutVms(host);
            }
        }

        if(moveOutVms.size()>0 && moveInHosts.size()>0){
            init();
            run();
            getResult();
        }

        ProcessResult pr = new ProcessResult();
        pr.setVmToHostMap(vmToHostMap);
        return pr;
    }


    private void tryMoveOutVms(Host host) {
//        calculate how many vms should move out
        int vmsNum = calculateVmsMoveout(hostCpuMap.get(host.getId()), MAX_CPU_UTILIZATION_THERSHOLD);
        if (vmsNum == 0) {
            return;
        }
        Map<Long, Double> hostCpuMapWithoutZero = removeAllZeroHosts(hostCpuMap);
        boolean isEnough = haveEnoughSpace(vmsNum * constant.PERCENTAGE_OF_ONE_VM_TO_HOST, host, hostCpuMapWithoutZero, moveOutVms.size());
        if (isEnough) {
            moveOutVmsFromHost(vmsNum, moveOutVms, host, hostVmsMap);
        } else {
            if (allZeroHostMap.size() > 0) {
                List<Long> removeFromZeroMapHosts = new ArrayList<>();
                for (Map.Entry<Long, Host> e : allZeroHostMap.entrySet()) {
                    hostCpuMapWithoutZero.put(e.getKey(), hostCpuMap.get(e.getKey()));
                    moveInHosts.put(e.getKey(), e.getValue());
                    removeFromZeroMapHosts.add(e.getKey());
                    boolean isEnough2 = haveEnoughSpace(vmsNum * constant.PERCENTAGE_OF_ONE_VM_TO_HOST, host, hostCpuMapWithoutZero, moveOutVms.size());
                    if (isEnough2) {
                        moveOutVmsFromHost(vmsNum, moveOutVms, host, hostVmsMap);
                        updateAllZeroHostMap(removeFromZeroMapHosts, allZeroHostMap);
                        break;
                    }
                }
            }
        }
    }


    private boolean tryShotdown(Host host) {
        Map<Long, Double> hostCpuMapWithoutZero = removeAllZeroHosts(hostCpuMap);
        double rate = calculateAverageDatacenterUsed(hostCpuMapWithoutZero, moveOutVms.size());
        if (rate > 0.6) {
            return false;
        }
        boolean isEnough = haveEnoughSpace(hostCpuMap.get(host.getId()), host, hostCpuMapWithoutZero, moveOutVms.size());
        if (isEnough) {
            List<Vm> vms = hostVmsMap.get(host.getId());
            addAllVms(moveOutVms, vms);
            moveInHosts.remove(host.getId());
            allZeroHostMap.put(host.getId(), host);
            hostCpuMap.put(host.getId(), 0.0);
        }
        return isEnough;
    }


    /**
     * init all ant colony algorithm parameters
     *
     * @param
     */
    public void init() {

        moveInHostList = new ArrayList<>(moveInHosts.values());
        moveOutVmList = new ArrayList<>(moveOutVms.values());
        ants = new ArrayList<Ant>();
        pheromone = new double[moveInHosts.size()][moveOutVms.size()];
        Delta = new double[moveInHosts.size()][moveOutVms.size()];
        bestLength = 1000000;
        for (int i = 0; i < moveInHosts.size(); i++) {
            for (int j = 0; j < moveOutVms.size(); j++) {
                pheromone[i][j] = 0.1;
            }
        }
        bestTour = new Position[moveOutVms.size()];
        for (int i = 0; i < moveOutVms.size(); i++) {
            bestTour[i] = new Position(-1, -1);
        }
        for (int i = 0; i < antNum; i++) {
            ants.add(new Ant());
            ants.get(i).RandomSelectVM(moveInHostList, moveOutVmList);
        }
    }

    /**
     * the process of the ant colony optimization algorithm
     */
    public void run() {
        for (int runTime = 0; runTime < maxgen; runTime++) {
            for (int i = 0; i < antNum; i++) {
                for (int j = 1; j < moveOutVms.size(); j++) {
                    ants.get(i).SelectNextVM(pheromone);
                }
            }
            for (int i = 0; i < antNum; i++) {
                System.out.println("number " + i + " ant");
                ants.get(i).CalTourLength();
                System.out.println("number" + i + "ant's path：" + ants.get(i).tourLength);
                ants.get(i).CalDelta();
                if (ants.get(i).tourLength < bestLength) {
                    //record best path
                    bestLength = ants.get(i).tourLength;
                    System.out.println(runTime + "times iteration," + "number " + i + "ant discovers new solution：" + bestLength);
                    for (int j = 0; j < moveOutVms.size(); j++) {
                        bestTour[j].setHostId(ants.get(i).tour.get(j).getHostId());
                        bestTour[j].setVmId(ants.get(i).tour.get(j).getVmId());
                    }
                    //update pheromone
                    for (int k = 0; k < moveInHosts.size(); k++) {
                        for (int j = 0; j < moveOutVms.size(); j++) {
                            pheromone[k][j] = pheromone[k][j] + Q / bestLength;
                        }
                    }
                }
            }
            UpdatePheromone();//update pheromone

            for (int i = 0; i < antNum; i++) {
                ants.get(i).RandomSelectVM(moveInHostList, moveOutVmList);
            }
        }
    }

    /**
     * updatePheromone
     */
    public void UpdatePheromone() {
        double rou = 0.5;
        for (int k = 0; k < antNum; k++) {
            for (int i = 0; i < moveInHosts.size(); i++) {
                for (int j = 0; j < moveOutVms.size(); j++) {
                    Delta[i][j] += ants.get(k).delta[i][j];
                }
            }
        }

        for (int i = 0; i < moveInHosts.size(); i++) {
            for (int j = 0; j < moveOutVms.size(); j++) {
                pheromone[i][j] = (1 - rou) * pheromone[i][j] + Delta[i][j];
            }
        }
    }

    /**
     * print result
     */
    public void getResult() {
        System.out.println("the length of best path is" + bestLength);
        for (int j = 0; j < moveOutVms.size(); j++) {
            int vmId = bestTour[j].getVmId();
            Vm vm = vmMap.get((long)vmId);
            int hostId = bestTour[j].getHostId();
            Host host = hostMap.get((long)hostId);
            vmToHostMap.put(vm, host);
        }
    }


}
