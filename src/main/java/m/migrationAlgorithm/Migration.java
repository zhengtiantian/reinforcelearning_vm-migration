package m.migrationAlgorithm;

import m.po.EnvironmentInfo;
import m.po.ProcessResult;

public interface Migration {
    ProcessResult processMigration(EnvironmentInfo info);
}
