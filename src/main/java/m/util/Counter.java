package m.util;

public class Counter {

    private static int iterateTimes = 0;

    private static int totalVmMigrateTimes = 0;

    private static double totalPower = 0;

    public void addTotalPower(double power) {
        totalPower += power;
    }

    public double getTotalPower() {
        return totalPower;
    }

    public void addMigrateTime(int times) {
        totalVmMigrateTimes += times;
    }

    public int getTotalVmMigratiomTimes() {
        return totalVmMigrateTimes;
    }

    public void addOneIterateTime() {
        iterateTimes++;
    }

    public int getIterateTimes() {
        return iterateTimes;
    }
}
