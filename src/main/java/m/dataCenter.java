package m;

import m.util.Constant;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.distributions.ContinuousDistribution;
import org.cloudbus.cloudsim.distributions.UniformDistr;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class dataCenter {

    private static Constant constant = new Constant();

    private static ContinuousDistribution random = new UniformDistr();

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
        VmAllocationPolicySimple vmAllocationPolicy = new VmAllocationPolicySimple();
//        VmAllocationPolicyRoundRobin vmAllocationPolicy = new VmAllocationPolicyRoundRobin();
        vmAllocationPolicy.setFindHostForVmFunction(this::findRandomSuitableHostForVm);
        DatacenterSimple dc = new DatacenterSimple(simulation, hostList, vmAllocationPolicy);

        dc.setSchedulingInterval(constant.SCHEDULING_INTERVAL);
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
//        host.setPowerModel(new PowerModelHostSpec(getCPUlist()));
        host.setRamProvisioner(new ResourceProvisionerSimple());
        host.setBwProvisioner(new ResourceProvisionerSimple());
        host.setVmScheduler(new VmSchedulerTimeShared());
        host.setIdleShutdownDeadline(1.0);
        return host;
    }



    private Optional<Host> findRandomSuitableHostForVm(final VmAllocationPolicy vmAllocationPolicy, final Vm vm) {
        final List<Host> hostList = vmAllocationPolicy.getHostList();
        for (int i = 0; i < hostList.size(); i++) {
            final int randomIndex = (int) (random.sample() * hostList.size());
            final Host host = hostList.get(randomIndex);
            if (host.isSuitableForVm(vm)) {
                return Optional.of(host);
            }
        }
        return Optional.empty();
    }


}
