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

    /**
     * total hosts
     */
    private static final int HOSTS = 2;
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
    private static final double MAX_POWER = 1000;

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
        final List<Host> hostList = new ArrayList<>(HOSTS);
        for (int i = 0; i < HOSTS; i++) {
            Host host = createHost();
            hostList.add(host);
        }
        VmAllocationPolicySimple vmAllocationPolicy = new VmAllocationPolicySimple();
//        vmAllocationPolicy.setFindHostForVmFunction(this::findRandomSuitableHostForVm);
        DatacenterSimple dc = new DatacenterSimple(simulation, hostList, vmAllocationPolicy);

        dc.setSchedulingInterval(SCHEDULING_INTERVAL);
        return dc;
    }

    /**
     * create hosts
     *
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

        final Host host = new HostSimple(ram, bw, storage, peList, false);
        host.setPowerModel(new PowerModelHostSimple(MAX_POWER, STATIC_POWER));
//        host.setPowerModel(new PowerModelHostSpec(getCPUlist()));
        host.setRamProvisioner(new ResourceProvisionerSimple());
        host.setBwProvisioner(new ResourceProvisionerSimple());
        host.setVmScheduler(new VmSchedulerTimeShared());
        host.setIdleShutdownDeadline(1.0);
        return host;
    }

    public int getHostPes() {
        return HOST_PES;
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

    public double getStaticPower() {
        return STATIC_POWER;
    }

    public double getMaxPower() {
        return MAX_POWER;
    }
}
