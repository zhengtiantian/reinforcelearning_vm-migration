import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyFirstFit;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;

import java.util.ArrayList;
import java.util.List;

public class dataCenter {

    /**
     * total hosts
     */
    private static final int HOSTS = 5;
    /**
     * total core of single host
     */
    private static final int HOST_PES = 64;
    /**
     * total memory of single host
     */
    private static final long RAM = 131072; //in Megabytes
    /**
     * total internet bandwidth of single host
     */
    private static final long BW = 10240; //in Megabits/s
    /**
     * total storage of single host
     */
    private static final long STORAGE = 10240000; //in Megabytes
    /**
     * schedul interval
     */
    private static final int SCHEDULING_INTERVAL = 2;
    /**
     * Defines the power a Host uses, even if it's idle (in Watts).
     */
    private static final double STATIC_POWER = 200;

    /**
     * The max power a Host uses (in Watts).
     */
    private static final int MAX_POWER = 1000;

    /**
     * getDatacenter
     * @param simulation
     * @return
     */
    public Datacenter getDatacenter(CloudSim simulation){
        return createDatacenter(simulation);
    }

    /**
     * create data center
     * @param simulation
     * @return
     */
    private Datacenter createDatacenter(CloudSim simulation) {
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        DatacenterSimple dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicyFirstFit());
        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    /**
     * create hosts
     * @return
     */
    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOST_PES; i++) {
            peList.add(new PeSimple(1000, new PeProvisionerSimple()));
        }

        final long ram = RAM; //in Megabytes
        final long bw = BW; //in Megabits/s
        final long storage = STORAGE; //in Megabytes

        final Host host = new HostSimple(ram, bw, storage, peList);
        host.setPowerModel(new PowerModelHostSimple(MAX_POWER, STATIC_POWER));
        host.setRamProvisioner(new ResourceProvisionerSimple());
        host.setBwProvisioner(new ResourceProvisionerSimple());
        host.setVmScheduler(new VmSchedulerTimeShared());
        return host;
    }

    public int getHostPes(){
        return HOST_PES;
    }

}
