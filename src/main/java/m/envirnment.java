package m;

import m.po.EnvironmentInfo;
import m.util.Constant;
import m.util.printer;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudsimplus.listeners.EventInfo;

import java.util.concurrent.ConcurrentLinkedQueue;


public class envirnment {

    private static Constant constant = new Constant();
    private static CloudSim simulation = new CloudSim();
    private static dataCenter dataCenter = new dataCenter();
    private static dataCenterBroker dataCenterBroker = new dataCenterBroker();
    private static Datacenter datacenter;
    private static DatacenterBroker broker;
    private static double previousClock = 0.0;
    private static printer printer = new printer();

    /**
     * get the information from the environment
     */
    private static ConcurrentLinkedQueue<EnvironmentInfo> queue = new ConcurrentLinkedQueue<>();

    public void start() {
        datacenter = dataCenter.getDatacenter(simulation);
        broker = dataCenterBroker.getBroker(simulation);
        simulation.addOnClockTickListener(this::onClockTickListener);
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

    public dataCenter getDataCenter() {
        return dataCenter;
    }

    public ConcurrentLinkedQueue<EnvironmentInfo> getQueue() {
        return queue;
    }

    private static int previousPrintTime = 0;

    private static int PreviousSendMessageTime = 0;

    //01/12/2020
    private void onClockTickListener(EventInfo evt) {


        if (simulation.clock() == previousClock || broker.getVmExecList().isEmpty()) {
            return;
        }
        previousClock = simulation.clock();

        int simulationTime = (int)Math.round(simulation.clock());

        if (simulationTime - previousPrintTime >= constant.PRINT_INVERVAL) {
            printer.recordPowerConsumption(datacenter, simulationTime,simulationTime - previousPrintTime);
            printer.printIterateTimes();
            printer.printTotalMigrationTimes();
            previousPrintTime = simulationTime;
        }

        if(simulationTime - PreviousSendMessageTime >= constant.SEND_MESSAGE_INVERVAL){
            PreviousSendMessageTime = simulationTime;
            EnvironmentInfo info = new EnvironmentInfo();
            info.setDatacenter(datacenter);
            info.setBroker(broker);
            queue.offer(info);
        }

    }


}
