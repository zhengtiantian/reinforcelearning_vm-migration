package m;

import m.util.Constant;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyBestFit;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyRoundRobin;
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
import java.util.ArrayList;
import java.util.List;

public class dataCenter {

    private static Constant constant = new Constant();

    public dataCenter() {
    }

    /**
     * getDatacenter
     *
     * @param simulation
     * @return
     */
    public Datacenter getDatacenter(CloudSim simulation) {
        return createDatacenter(simulation);
    }

    /**
     * create data center
     *
     * @param simulation
     * @return
     */
    private Datacenter createDatacenter(CloudSim simulation) {
        final List<Host> hostList = new ArrayList<>(constant.HOSTS);
        for (int i = 0; i < constant.HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }

        DatacenterSimple dc = new DatacenterSimple(simulation, hostList, new VmAllocationPolicyRoundRobin());
        return dc;
    }

    /**
     * create hosts
     *
     * @return
     */
    private Host createHost() {
        final List<Pe> peList = new ArrayList<>(constant.HOSTS);
        for (int i = 0; i < constant.HOST_PES; i++) {
            peList.add(new PeSimple(1000, new PeProvisionerSimple()));
        }

        final long ram = constant.HOST_RAM; //in Megabytes
        final long bw = constant.HOST_BW; //in Megabits/s
        final long storage = constant.HOST_STORAGE; //in Megabytes

        final Host host = new HostSimple(ram, bw, storage, peList, false);
        host.setPowerModel(new PowerModelHostSimple(constant.HOST_MAX_POWER, constant.HOST_STATIC_POWER));
        host.setRamProvisioner(new ResourceProvisionerSimple());
        host.setBwProvisioner(new ResourceProvisionerSimple());
        host.setIdleShutdownDeadline(0.5);
        host.enableStateHistory();
        return host;
    }


}
