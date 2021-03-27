package m.util;

import m.environment;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostStateHistoryEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class printer {

    static Map<Long, Map<Integer, Double>> powerMap = new HashMap<>();

    static PowerTool powerTool = new PowerTool();

    private static Counter counter = new Counter();

    private static environment environment = new environment();


    public void recordPowerConsumption(Datacenter datacenter, int time, int lastTime) {
        initialPowerMap(datacenter.getHostList());
        List<Host> hostList = datacenter.getHostList();
        for (Host host : hostList) {
            for (HostStateHistoryEntry entry : host.getStateHistory()) {
                entry.getAllocatedMips();
                entry.getRequestedMips();
            }

            Map<Integer, Double> hostMap = powerMap.get(host.getId());
            double power = powerTool.getPower(host, host.getCpuPercentUtilization());
            hostMap.put(time, power);
            double powerConsumption = power * lastTime;
            counter.addTotalPower(powerConsumption);
            System.out.println("time:" + time + " host:" + host.getId() + " isactive: " + host.isActive() + " cpu utilization:" + host.getCpuPercentUtilization() + " vms:" + host.getVmList().size() + " power:" + power + "watts * " + lastTime + "s = " + counter.fourDecimalPlaces(powerConsumption / 3600) + "W·h");
        }
        System.out.println("the total power consumption of the datacenter:" + counter.getTotalPower() + "W·h");
        time++;

    }

    public void printIterateTimes() {
        System.out.println("Iterate times is :" + counter.getIterateTimes());
    }

    public void printTotalMigrationFinishedTimes() {
        System.out.println("The total number of finished migration vms migration is :" + counter.getTotalVmMigratioFinishedTimes());
    }

    public void printTotalMigrationRequestTimes() {
        System.out.println("The total number of vms migration is :" + counter.getTotalVmMigratioRequestTimes());
    }

    private void initialPowerMap(List<Host> hostList) {
        for (Host host : hostList) {
            Map<Integer, Double> hostMap = new HashMap<>();
            powerMap.put(host.getId(), hostMap);
        }

    }

    public void printHostsSlaViolation() {
        List<Host> hostList = environment.getDatacenter().getHostList();
        System.out.println("sla violation rate accroding to time is:" + counter.getSlaTimePerActiveHost(hostList) + "%");
        System.out.println("averate sla violation rate is :" + counter.getAverageSlaViotationRate(hostList) + "%");
//        for (HostViolationRate h : counter.getHostViolationRates()) {
//            System.out.println("host:" + h.getHost().getId() + " sla violation rate is " + h.getViolationRate() + "%");
//        }
    }

    public void printTotalTime() {
        System.out.println("the total time consumed by the algorithm is " + counter.getTotalTime() + " mills");
    }


}
