import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import po.EnvironmentInfo;
import util.Conversion;

import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class printer {

    static Map<Long, Map<Integer, Double>> powerMap = new HashMap<>();

    static double totalPower = 0;

    public void recordPowerConsumption(envirnment envirnment) {
        CloudSim simulation = envirnment.getSimulation();
        int time = 1;
        initialPowerMap(envirnment.getDatacenter().getHostList());
        while (simulation.isRunning()) {
            List<Host> hostList = envirnment.getDatacenter().getHostList();
            for (Host host : hostList) {
                Map<Integer, Double> hostMap = powerMap.get(host.getId());
                double power = getPower(host, host.getCpuPercentUtilization());
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

    private void printHostCpuUtilizationAndPowerConsumption(CloudSim simulation, Host host) {
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
            final double watts = getPower(host, utilizationPercent);
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

    private double getPower(Host host, double cpuUtilization) {
        if (host.isActive()) {
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
