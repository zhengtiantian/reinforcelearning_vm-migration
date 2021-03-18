package m.algorithmTool.antColonyOptimization;

import m.po.Position;
import m.util.Constant;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 蚂蚁类
 *
 * @author Gavrila
 */
public class Ant {

    Constant constant = new Constant();
    public double[][] delta;//每个节点增加的信息素
    public int Q = 100;
    public List<Position> tour;//蚂蚁获得的路径（解，任务分配给虚拟机的分法）
    public double tourLength;//蚂蚁获得的路径长度（分配好后，总的花费时间）
    public double[] host_vm;//每个主机的虚拟机数
    public List<Integer> tabu;//禁忌表
    private List<Host> hostList;
    private List<Vm> vmList;

    /**
     * 随机分配蚂蚁到某个节点中，同时完成蚂蚁包含字段的初试化工作
     */
    public void RandomSelectVM(List<Host> hosts, List<Vm> vms) {
        hostList = hosts;
        vmList = vms;

        delta = new double[hostList.size()][vmList.size()];
        host_vm = new double[hostList.size()];
        for (int i = 0; i < hostList.size(); i++) host_vm[i] = 0;
        tabu = new ArrayList<Integer>();
        tour = new ArrayList<Position>();

        //随机选择蚂蚁的位置
        int firstHost = (int) (hostList.size() * Math.random());
        Random r = new Random();
        int firstVm = (int) (vmList.get(r.nextInt(vmList.size())).getId());
        tour.add(new Position(firstHost, firstVm));
        tabu.add(new Integer(firstVm));
        host_vm[firstHost] += constant.CLOUDLET_LENGTH;
    }

    /**
     *
     */
    public double Dij(int host, int vm) {
        double d;
        double hv = host_vm[host];
        double vmMips = vmList.get(vm).getMips();
        double capacity = hostList.get(host).getBw().getCapacity();
        d = hv / vmMips + (double)constant.CLOUDLET_LENGTH / capacity;
        return d;
    }

    /**
     * 选择下一个节点
     *
     * @param pheromone 全局的信息素信息
     */
    public void SelectNextVM(double[][] pheromone) {
        double[][] p;//每个节点被选中的概率
        p = new double[hostList.size()][vmList.size()];
        double alpha = 1.0;
        double beta = 1.0;
        double sum = 0;//分母
        //计算公式中的分母部分
        for (int i = 0; i < hostList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                if (tabu.contains(new Integer(j))) {
                    continue;
                }
                double pow = Math.pow(pheromone[i][j], alpha);
                double dij = Dij(i, j);
                double pow1 = Math.pow(1 / dij, beta);
                sum +=  pow * pow1;
            }
        }
        //计算每个节点被选的概率
        for (int i = 0; i < hostList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                p[i][j] = Math.pow(pheromone[i][j], alpha) * Math.pow(1 / Dij(i, j), beta) / sum;
                if (tabu.contains(new Integer(j))) {
                    p[i][j] = 0;
                }
            }
        }
        double selectp = Math.random();
        //轮盘赌选择一个VM
        double sumselect = 0;
        int selectHost = -1;
        int selectVm = -1;
        boolean flag = true;
        for (int i = 0; i < hostList.size() && flag == true; i++) {
            for (int j = 0; j < vmList.size(); j++) {
                sumselect += p[i][j];
                if (sumselect >= selectp) {
                    selectHost = i;
                    selectVm = j;
                    flag = false;
                    break;
                }
            }
        }
        if (selectHost == -1 | selectVm == -1) {
            System.out.println("选择下一个虚拟机没有成功！");
        }
        tabu.add(new Integer(selectVm));
        tour.add(new Position(selectHost, (int)vmList.get(selectVm).getId()));
        host_vm[selectHost] += constant.CLOUDLET_LENGTH;
    }


    public void CalTourLength() {
        System.out.println();
        double[] max;
        max = new double[hostList.size()];
        for (int i = 0; i < tour.size(); i++) {
            double mips = hostList.get(tour.get(i).getHostId()).getMips();
            max[tour.get(i).getHostId()] += (double)constant.CLOUDLET_LENGTH / mips;
        }
        tourLength = max[0];
        for (int i = 0; i < hostList.size(); i++) {
            if (max[i] > tourLength) tourLength = max[i];
            System.out.println("第" + i + "台虚拟机的执行时间：" + max[i]);
        }
        return;
    }

    /**
     * 计算信息素增量矩阵
     */
    public void CalDelta() {
        for (int i = 0; i < hostList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                if (i == tour.get(j).getHostId() && tour.get(j).getVmId() == j) {
                    delta[i][j] = Q / tourLength;
                } else {
                    delta[i][j] = 0;
                }
            }
        }
    }
}
