import java.util.Random;

public class actuator {


    /**
     * 0:do nothing,1:move the vm out
     * 2:shut down the host
     */
    static int[] action = new int[]{0, 1, 2};

    /**
     * state the cpu utilization of the host
     * 0:  0%
     * 1:  0%<utilization<=10%
     * 2: 10%<utilization<=20%
     * 3: 20%<utilization<=30%
     * 4: 30%<utilization<=40%
     * 5: 40%<utilization<=50%
     * 6: 50%<utilization<=60%
     * 7: 60%<utilization<=70%
     * 8: 70%<utilization<=80%
     * 9: 80%<utilization<=90%
     * 10: 90%<utilization<=100%
     */
    //TODO maybe need to adjust
    static int[] state = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    //Q table
    static double[][] qtable;

    static double learningRate = 0.01;

    static double discountFactor = 0.9;

    static double threshold = 0.1;

    public void initQtalbe() {
        qtable = new double[state.length][action.length];
    }

    public void updateQtable(int state, int action, double reward, int nextState) {
        qtable[state][action] += learningRate * (reward + discountFactor * max(qtable[nextState]) - qtable[state][action]);
    }

    private double max(double[] Qs) {
        double max = Double.MIN_VALUE;
        for (double q : Qs) {
            if (q > max) {
                max = q;
            }
        }
        return max;
    }

    //get the valueble action depend on the state
    public int getAction(int state) {
        if (Math.random() < threshold) {
            return action[new Random().nextInt(action.length)];
        } else {
            return getValuebleAct(qtable[state]);
        }
    }

    private int getValuebleAct(double[] doubles) {
        int act = 0;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < doubles.length; i++) {
            if (doubles[i] > max) {
                max = doubles[i];
                act = i;
            }
        }
        return act;
    }

    public int getStateByCpuUtilizition(double utilization) {
        if (utilization == 0.0) {
            return state[0];
        } else if (0.0 < utilization && utilization <= 0.25) {
            return state[1];
        } else if (0.25 < utilization && utilization <= 0.5) {
            return state[2];
        } else if (0.5 < utilization && utilization <= 0.75) {
            return state[3];
        } else {
            return state[4];
        }
    }

    public static int[] getAction() {
        return action;
    }

    public static int[] getState() {
        return state;
    }


}
