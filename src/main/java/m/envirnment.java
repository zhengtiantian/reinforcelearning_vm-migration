package m;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudsimplus.listeners.EventInfo;


public class envirnment {

    private static CloudSim simulation = new CloudSim();
    private static dataCenter dataCenter = new dataCenter();
    private static dataCenterBroker dataCenterBroker = new dataCenterBroker();
    private static Datacenter datacenter;
    private static DatacenterBroker broker;

    public void start() {
        datacenter = dataCenter.getDatacenter(simulation);
        broker = dataCenterBroker.getBroker(simulation);

        simulation.startSync();

    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public DatacenterBroker getBroker() {
        return broker;
    }

    public CloudSim getSimulation() {
        return simulation;
    }

    public dataCenterBroker getDataCenterBroker() {
        return dataCenterBroker;
    }

    public dataCenter getDataCenter(){
        return dataCenter;
    }


    //01/12/2020
    private void onClockTickListener(EventInfo evt) {
        broker.getVmCreatedList().forEach(vm ->
                System.out.printf(
                        "\t\tTime %6.1f: Vm %d CPU Usage: %6.2f%% (%2d vCPUs. Running Cloudlets: #%d). HOST %d %n",
                        evt.getTime(), vm.getId(), vm.getCpuPercentUtilization() * 100.0, vm.getNumberOfPes(),
                        vm.getCloudletScheduler().getCloudletExecList().size(),
                        vm.getHost().getId())
        );

    }


}
