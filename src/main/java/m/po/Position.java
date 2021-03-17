package m.po;

public class Position {
    public int hostId;
    public int vmId;

    public Position(int a, int b){
        hostId = a;
        vmId = b;
    }

    public int getVmId() {
        return vmId;
    }

    public void setVmId(int vmId) {
        this.vmId = vmId;
    }

    public int getHostId() {
        return hostId;
    }

    public void setHostId(int hostId) {
        this.hostId = hostId;
    }
}
