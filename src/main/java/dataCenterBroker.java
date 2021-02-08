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

    private static final int VM_PES = 2;
    private static final int RAM = 2048;
    private static final int BW = 1000;
    private static final int SIZE = 10000;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10000;

    private int createsVms;

    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private static DatacenterBroker broker;


    DatacenterBroker getBroker(CloudSim simulation) {

        broker = new DatacenterBrokerSimple(simulation);
        vmList = new ArrayList<>(2);
        cloudletList = new ArrayList<>(4);
        createVmsAndCloudlet(2, 1);
        return broker;
    }

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

    public Vm createVm() {
        final int id = createsVms++;
        Vm vm = new VmSimple(id, 1000, VM_PES)
                .setRam(RAM).setBw(BW).setSize(SIZE)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
        vm.getUtilizationHistory().enable();
        return vm;
    }

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
