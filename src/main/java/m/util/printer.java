package m.util;

import m.envirnment;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.hosts.Host;
import m.util.Conversion;

import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class printer {

    static Map<Long, Map<Integer, Double>> powerMap = new HashMap<>();

    static double totalPower = 0;

    static PowerTool powerTool = new PowerTool();

    public void recordPowerConsumption(envirnment envirnment) {
        CloudSim simulation = envirnment.getSimulation();
        int time = 1;
        initialPowerMap(envirnment.getDatacenter().getHostList());
        while (simulation.isRunning()) {
            List<Host> hostList = envirnment.getDatacenter().getHostList();
            for (Host host : hostList) {
                Map<Integer, Double> hostMap = powerMap.get(host.getId());
                double power = powerTool.getPower(host, host.getCpuPercentUtilization());
                hostMap.put(time, power);
                totalPower += power;
                System.out.println("time:" + time + "host:" + host.getId() + "isactive: " + host.isActive() + "cpu utilization:" + host.getCpuPercentUtilization() + " power:" + power + " * 1s = " + power);
            }
            System.out.println("the total power consumption of the datacenter:" + totalPower);
            time++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void initialPowerMap(List<Host> hostList) {
        for (Host host : hostList) {
            Map<Integer, Double> hostMap = new HashMap<>();
            powerMap.put(host.getId(), hostMap);
        }

    }



}
