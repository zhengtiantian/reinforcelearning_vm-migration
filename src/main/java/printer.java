import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.Simulation;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;
import po.EnvironmentInfo;
import util.Conversion;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;

public class printer {

    public void print(EnvironmentInfo info, CloudSim simulation) {
        List<Vm> list = info.getBroker().getVmCreatedList();
        list.forEach(vm ->

                System.out.printf(
                        "\t\tVm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%d). HOST %d %n",
                        vm.getId(), vm.getCpuPercentUtilization() * 100.0, vm.getNumberOfPes(),
                        vm.getCloudletScheduler().getCloudletExecList().size(),
                        vm.getHost().getId())
        );

        for (Host host : info.getDatacenter().getHostList()) {
//            System.out.println(host.getFirstStartTime());
            printHostCpuUtilizationAndPowerConsumption(simulation,host);
        }


        System.out.println();

        System.out.printf("%n%n");
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
            final double watts = getPower(host,utilizationPercent);
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
