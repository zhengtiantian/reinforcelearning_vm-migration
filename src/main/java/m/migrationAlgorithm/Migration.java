package m.migrationAlgorithm;

import m.po.EnvironmentInfo;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Map;

public interface Migration {
    Map<Vm, Host> processMigration(EnvironmentInfo info);
}
