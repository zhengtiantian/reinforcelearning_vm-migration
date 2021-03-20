package m;

import m.util.Constant;
import m.util.Counter;
import m.util.printer;
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
import org.cloudsimplus.listeners.VmHostEventInfo;

import java.util.ArrayList;
import java.util.List;

public class dataCenterBroker {

    private static Constant constant = new Constant();

    private static Counter counter = new Counter();

    private int createsVms;
    private List<Cloudlet> cloudletList;
    private List<Vm> vmList;
    private static DatacenterBroker broker;
    private static double CREATEVMSINTERVAL = (constant.CLOUDLET_LENGTH / constant.VM_MIPS) / 1.5;

    static double[] createVmsRate = new double[]{0.066, 0.15, 0.066, 0.15, 0.066, 0.1, 0.066, 0.1, 0.15, 0.066};

    /**
     * get data center broker
     *
     * @param simulation
     * @return
     */
    DatacenterBroker getBroker(CloudSim simulation) {
        broker = new DatacenterBrokerSimple(simulation);
        vmList = new ArrayList<>(2);
        cloudletList = new ArrayList<>(4);
        int totalVms = constant.HOSTS * 60;
        for (int i = 0; i < 10; i++) {
            createVmsAndCloudlet((int) (totalVms * createVmsRate[i % 10]), 1, i * CREATEVMSINTERVAL);
        }

        return broker;
    }

    /**
     * create vms and cloudlets
     *
     * @param createsVms
     * @param createCloudlet
     */
    public void createVmsAndCloudlet(int createsVms, int createCloudlet, double submissionDelay) {
        List<Vm> newVmList = new ArrayList<>(createsVms);
        List<Cloudlet> newCloudletList = new ArrayList<>(createCloudlet);
        for (int i = 0; i < createsVms; i++) {
            Vm vm = createVm(submissionDelay);
            newVmList.add(vm);
            for (int j = 0; j < createCloudlet; j++) {
                Cloudlet cloudlet = createCloudlet(vm);
                cloudlet.addOnFinishListener(this::cloudletProcessingUpdateListener);
                newCloudletList.add(cloudlet);
            }
        }
        this.vmList.addAll(newVmList);
        this.cloudletList.addAll(newCloudletList);
        broker.submitVmList(newVmList);
        broker.submitCloudletList(newCloudletList);
    }

    private void cloudletProcessingUpdateListener(CloudletVmEventInfo info) {
        Cloudlet cloudlet = info.getCloudlet();
        Vm vm = cloudlet.getVm();
        vm.getHost().destroyVm(vm);
    }

    /**
     * create vms
     *
     * @return
     */
    public Vm createVm(double submissionDelay) {
        final int id = createsVms++;
        Vm vm = new VmSimple(id, constant.VM_MIPS, constant.VM_PES)
                .setRam(constant.VM_RAM).setBw(constant.VM_BW).setSize(constant.VM_SIZE).setCloudletScheduler(new CloudletSchedulerTimeShared());
        vm.getUtilizationHistory().enable();
        vm.setSubmissionDelay(submissionDelay);
        vm.addOnMigrationFinishListener(this::migrationFinish);
        vm.addOnMigrationStartListener(this::migrationStart);
        return vm;
    }


    /**
     * create cloudlets
     *
     * @param vm
     * @return
     */
    public Cloudlet createCloudlet(Vm vm) {
        UtilizationModel utilizationModel = new UtilizationModelFull();
        Cloudlet cloudlet = new CloudletSimple(constant.CLOUDLET_LENGTH, constant.CLOUDLET_PES)
                .setFileSize(1024)
                .setOutputSize(1024)
                .setUtilizationModel(utilizationModel)
                .setVm(vm);
        return cloudlet;
    }

    private void migrationFinish(VmHostEventInfo vmHostEventInfo) {
        counter.addMigrateFinishedTime(1);
    }

    private void migrationStart(VmHostEventInfo vmHostEventInfo) {
        counter.addMigrateRequestTime(1);
    }


}
