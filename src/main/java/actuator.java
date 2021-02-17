import java.util.Random;

/**
 * @author zxy18
 */
public class actuator {


    /**
     * 0:do nothing,1:move the vm out
     * 2:shut down the host
     */
    static int[] action = new int[]{0, 1, 2};

    /**
     * State the cpu utilization of the host
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
    static int[] state = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    /**
     * Q-table
     */
    static double[][] qtable;

    /**
     * Learning rate
     */
    static double learningRate = 0.01;

    /**
     * Determine the value of reward to the future
     */
    static double discountFactor = 0.9;

    /**
     * exploration rate which indicate the exploitation rate is 1-discount
     */
    static double exploration = 0.1;

    /**
     * Initial Q-table
     */
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

    /**
     * select the best action from q-table according to state
     *
     * @param state
     * @return best action
     */
    public int getAction(int state) {
        if (Math.random() < exploration) {
            return action[new Random().nextInt(action.length)];
        } else {
            return getValuebleAct(qtable[state]);
        }
    }

    /**
     * @param doubles given array
     * @return most valueble action
     */
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

    /**
     * get the state range accroding to cpu utilization
     *
     * @param utilization
     * @return state
     */
    public int getStateByCpuUtilizition(double utilization) {
        if (utilization == 0.0) {
            return state[0];
        } else if (0.0 < utilization && utilization <= 0.1) {
            return state[1];
        } else if (0.1 < utilization && utilization <= 0.2) {
            return state[2];
        } else if (0.2 < utilization && utilization <= 0.3) {
            return state[3];
        } else if (0.3 < utilization && utilization <= 0.4) {
            return state[4];
        } else if (0.4 < utilization && utilization <= 0.5) {
            return state[5];
        } else if (0.5 < utilization && utilization <= 0.6) {
            return state[6];
        } else if (0.6 < utilization && utilization <= 0.7) {
            return state[7];
        } else if (0.7 < utilization && utilization <= 0.8) {
            return state[8];
        } else if (0.8 < utilization && utilization <= 0.9) {
            return state[9];
        } else {
            return state[10];
        }
    }

    /**
     *
     * @return action array
     */
    public static int[] getAction() {
        return action;
    }

    /**
     *
     * @return state array
     */
    public static int[] getState() {
        return state;
    }

    /**
     * reward range is 0-100
     * the action will get more scores when save more power
     *
     * @param state
     * @param action
     * @param nextState
     * @return
     */
    public int getReward(int state, int action, int nextState) {
        if (action != 0) {
            return state * 10;
        } else if (action == 1 && state > nextState) {
            return (state - nextState) * 10;
        } else {
            return 0;
        }
    }

}
