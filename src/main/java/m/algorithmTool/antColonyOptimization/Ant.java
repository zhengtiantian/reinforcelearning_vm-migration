package m.algorithmTool.antColonyOptimization;

import m.po.Position;
import m.util.Constant;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class Ant {

    Constant constant = new Constant();
    public double[][] delta;
    public int Q = 100;
    public List<Position> tour;
    public double tourLength;//
    public double[] host_vm;//
    public List<Integer> tabu;
    private List<Host> hostList;
    private List<Vm> vmList;


    /**
     *
     * @param hosts
     * @param vms
     */
    public void RandomSelectVM(List<Host> hosts, List<Vm> vms) {
        hostList = hosts;
        vmList = vms;

        delta = new double[hostList.size()][vmList.size()];
        host_vm = new double[hostList.size()];
        for (int i = 0; i < hostList.size(); i++) host_vm[i] = 0;
        tabu = new ArrayList<Integer>();
        tour = new ArrayList<Position>();

        int firstHost = (int) (hostList.size() * Math.random());
        Random r = new Random();
        int firstVm = (int) (vmList.get(r.nextInt(vmList.size())).getId());
        tour.add(new Position(firstHost, firstVm));
        tabu.add(new Integer(firstVm));
        host_vm[firstHost] += constant.CLOUDLET_LENGTH;
    }


    public double Dij(int host, int vm) {
        double d;
        double hv = host_vm[host];
        double vmMips = vmList.get(vm).getMips();
        double capacity = hostList.get(host).getBw().getCapacity();
        d = hv / vmMips + (double) constant.CLOUDLET_LENGTH / capacity;
        return d;
    }


    public void SelectNextVM(double[][] pheromone) {
        double[][] p;//Probability of each vm
        p = new double[hostList.size()][vmList.size()];
        double alpha = 1.0;
        double beta = 1.0;
        double sum = 0;//Denominator
        //calculate the Denominator
        for (int i = 0; i < hostList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                if (tabu.contains(j)) {
                    continue;
                }
                double pow = Math.pow(pheromone[i][j], alpha);
                double dij = Dij(i, j);
                double pow1 = Math.pow(1 / dij, beta);
                sum += pow * pow1;
            }
        }
        //calculate the probability
        for (int i = 0; i < hostList.size(); i++) {
            for (int j = 0; j < vmList.size(); j++) {
                p[i][j] = Math.pow(pheromone[i][j], alpha) * Math.pow(1 / Dij(i, j), beta) / sum;
                if (tabu.contains(new Integer(j))) {
                    p[i][j] = 0;
                }
            }
        }
        double selectp = Math.random();
        //choose the vm
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
            System.out.println("no vm has been selected！");
        }
        tabu.add(selectVm);
        tour.add(new Position(selectHost, (int) vmList.get(selectVm).getId()));
        host_vm[selectHost] += constant.CLOUDLET_LENGTH;
    }


    public void CalTourLength() {
        System.out.println();
        double[] max;
        max = new double[hostList.size()];
        for (int i = 0; i < tour.size(); i++) {
            double mips = hostList.get(tour.get(i).getHostId()).getMips();
            max[tour.get(i).getHostId()] += (double) constant.CLOUDLET_LENGTH / mips;
        }
        tourLength = max[0];
        for (int i = 0; i < hostList.size(); i++) {
            if (max[i] > tourLength) tourLength = max[i];
        }
        return;
    }

    /**
     * calculate the delta matrix
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
