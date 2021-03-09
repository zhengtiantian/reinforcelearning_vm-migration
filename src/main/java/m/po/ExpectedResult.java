package m.po;

import java.util.List;

public class ExpectedResult {

    boolean canDo;

    List<VmToHost> vmsToHosts;

    double reward;

    public boolean isCanDo() {
        return canDo;
    }

    public void setCanDo(boolean canDo) {
        this.canDo = canDo;
    }

    public List<VmToHost> getVmsToHosts() {
        return vmsToHosts;
    }

    public void setVmsToHosts(List<VmToHost> vmsToHosts) {
        this.vmsToHosts = vmsToHosts;
    }

    public double getReward() {
        return reward;
    }

    public void setReward(double reward) {
        this.reward = reward;
    }
}
