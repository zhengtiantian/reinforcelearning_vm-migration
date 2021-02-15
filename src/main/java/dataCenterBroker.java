import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModel;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.listeners.CloudletVmEventInfo;

import java.util.ArrayList;
import java.util.List;

public class dataCenterBroker {
    /**
     * total cores of single vm
     */
    private static final int VM_PES = 2;
    /**
     * total memory of single vm
     */
    private static final int RAM = 2048;
    /**
     * total internet bandwidth of single vm
     */
    private static final int BW = 1000;
    /**
     * total storage of single vm
     */
    private static final int SIZE = 10000;
    /**
     * total cores of single cloudlet needed
     */
    private static final int CLOUDLET_PES = 2;
    /**
     * Each cloudlet execution time
     */
    private static final int CLOUDLET_LENGTH = 10000;

    private int createsVms;
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private static DatacenterBroker broker;


    /**
     * get data center broker
     * @param simulation
     * @return
     */
    DatacenterBroker getBroker(CloudSim simulation) {
        broker = new DatacenterBrokerSimple(simulation);
        vmList = new ArrayList<>(2);
        cloudletList = new ArrayList<>(4);
        createVmsAndCloudlet(2, 1);
        return broker;
    }

    /**
     * create vms and cloudlets
     * @param createsVms
     * @param createCloudlet
     */
    public void createVmsAndCloudlet(int createsVms, int createCloudlet) {
        List<Vm> newVmList = new ArrayList<>(createsVms);
        List<Cloudlet> newCloudletList = new ArrayList<>(createCloudlet);
        for (int i = 0; i < createsVms; i++) {
            Vm vm = createVm();
            newVmList.add(vm);
            for (int j = 0; j < createCloudlet; j++) {
                Cloudlet cloudlet = createCloudlet(vm);
                newCloudletList.add(cloudlet);
            }
        }
        this.vmList.addAll(newVmList);
        this.cloudletList.addAll(newCloudletList);
        broker.submitVmList(newVmList);
        broker.submitCloudletList(newCloudletList);
    }

    /**
     * create vms
     * @return
     */
    public Vm createVm() {
        final int id = createsVms++;
        Vm vm = new VmSimple(id, 1000, VM_PES)
                .setRam(RAM).setBw(BW).setSize(SIZE)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
        vm.getUtilizationHistory().enable();
        return vm;
    }

    /**
     * create cloudlets
     * @param vm
     * @return
     */
    public Cloudlet createCloudlet(Vm vm) {
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet cloudlet = new CloudletSimple(CLOUDLET_LENGTH, CLOUDLET_PES)
                .setFileSize(1024)
                .setOutputSize(1024)
                .setUtilizationModel(utilizationModel)
                .setVm(vm);
        return cloudlet;
    }

    public Cloudlet getLastCloudletFromList() {
        return cloudletList.get(cloudletList.size() - 1);
    }

    public void createVmsAndCloudlet(CloudletVmEventInfo cloudletVmEventInfo) {
        createVmsAndCloudlet(4, 1);
    }

    public int getVmPes(){
        return VM_PES;
    }
}
