package m.util;

import m.po.HostViolationRate;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostStateHistoryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Counter {

    private static int iterateTimes = 0;

    private static long totalTime = 0;

    private static AtomicInteger totalVmMigrateRequestTimes = new AtomicInteger(0);

    private static AtomicInteger totalVmMigratefinishedTimes = new AtomicInteger(0);

    private static double totalPower = 0;

    public void addTotalPower(double power) {
        totalPower += power;
    }

    public String getTotalPower() {
        return fourDecimalPlaces(totalPower / 3600);
    }

    public void addMigrateRequestTime(int times) {
        totalVmMigrateRequestTimes.addAndGet(times);
    }

    public AtomicInteger getTotalVmMigratioRequestTimes() {
        return totalVmMigrateRequestTimes;
    }

    public void addMigrateFinishedTime(int times) {
        totalVmMigratefinishedTimes.addAndGet(times);
    }

    public AtomicInteger getTotalVmMigratioFinishedTimes() {
        return totalVmMigratefinishedTimes;
    }

    public void addTime(long mills) {
        totalTime += mills;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public void addOneIterateTime() {
        iterateTimes++;
    }

    public int getIterateTimes() {
        return iterateTimes;
    }

    public String fourDecimalPlaces(double number) {
        return String.format("%.4f", number);
    }

    public String getSlaTimePerActiveHost(List<Host> hosts) {
        double slaViolationTimePerHost = 0;
        double totalTime = 0;

        for (Host host : hosts) {

            double previousTime = -1;
            double previousAllocated = 0;
            double previousRequested = 0;
            boolean previousIsActive = true;

            for (HostStateHistoryEntry entry : host.getStateHistory()) {

                if (previousTime != -1 && previousIsActive) {
                    double timeDiff = entry.getTime() - previousTime;
                    totalTime += timeDiff;
                    if (previousAllocated < previousRequested) {
                        slaViolationTimePerHost += timeDiff;
                    }
                }

                previousAllocated = entry.getAllocatedMips();
                previousRequested = entry.getRequestedMips();
                previousTime = entry.getTime();
                previousIsActive = entry.isActive();
            }
        }

        return fourDecimalPlaces((slaViolationTimePerHost / totalTime) * 100);
    }

    public static List<HostViolationRate> hostViolationRates;

    public List<HostViolationRate> getHostViolationRates() {

        return hostViolationRates;
    }

    public String getAverageSlaViotationRate(List<Host> hosts) {
        hostViolationRates = new ArrayList<>();
        double totalAllocatedMips = 0;
        double totalRequestedMips = 0;

        for (Host host : hosts) {

            double allocatedMips = 0;
            double requestedMips = 0;
            boolean previousIsActive = true;

            for (HostStateHistoryEntry entry : host.getStateHistory()) {

                if (previousIsActive) {
                    allocatedMips += entry.getAllocatedMips();
                    requestedMips += entry.getRequestedMips();
                }

            }

            HostViolationRate hostViolationRate = new HostViolationRate();
            hostViolationRate.setHost(host);
            hostViolationRate.setViolationRate(fourDecimalPlaces(((requestedMips - allocatedMips) / requestedMips) * 100));
            hostViolationRates.add(hostViolationRate);
            totalAllocatedMips += allocatedMips;
            totalRequestedMips += requestedMips;
        }

        return fourDecimalPlaces((((totalRequestedMips - totalAllocatedMips) / totalRequestedMips) / hosts.size()) * 100);

    }

}
