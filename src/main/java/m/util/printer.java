package m.util;

import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.hosts.Host;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class printer {

    static Map<Long, Map<Integer, Double>> powerMap = new HashMap<>();

    static PowerTool powerTool = new PowerTool();

    private static Counter counter = new Counter();

    public void recordPowerConsumption(Datacenter datacenter, int time, int lastTime) {
        initialPowerMap(datacenter.getHostList());
        List<Host> hostList = datacenter.getHostList();
        for (Host host : hostList) {
            Map<Integer, Double> hostMap = powerMap.get(host.getId());
            double power = powerTool.getPower(host, host.getCpuPercentUtilization());
            hostMap.put(time, power);
            counter.addTotalPower(power * lastTime);
            System.out.println("time:" + time + " host:" + host.getId() + " isactive: " + host.isActive() + " cpu utilization:" + host.getCpuPercentUtilization() + " vms:" + host.getVmList().size() + " power:" + power + " * " + lastTime + "s = " + power * lastTime);
        }
        System.out.println("the total power consumption of the datacenter:" + counter.getTotalPower());
        time++;

    }

    public void printIterateTimes() {
        System.out.println("Iterate times is :" + counter.getIterateTimes());
    }

    public void printTotalMigrationTimes() {
        System.out.println("The total number of vms migration is :" + counter.getTotalVmMigratiomTimes());
    }

    private void initialPowerMap(List<Host> hostList) {
        for (Host host : hostList) {
            Map<Integer, Double> hostMap = new HashMap<>();
            powerMap.put(host.getId(), hostMap);
        }

    }


}
