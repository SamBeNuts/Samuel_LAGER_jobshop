package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabouSolver implements Solver {
    private int maxIter;
    private int dureeTabou;

    public TabouSolver(int maxIter, int dureeTabou) {
        this.maxIter = maxIter;
        this.dureeTabou = dureeTabou;
    }

    static class STabou {
        final int tab[][];
        final int dureeTabou;

        STabou(int nbMachine, int nbJob, int dureeTabou) {
            this.tab = new int[nbJob*nbMachine][nbJob*nbMachine];
            this.dureeTabou = dureeTabou;
        }

        public void add(Utils.Swap swap, int k) {
            tab[swap.t1][swap.t2] = k + dureeTabou;
        }

        public boolean check(Utils.Swap swap, int k) {
            return k > tab[swap.t1][swap.t2];
        }
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        Result s = new GloutonSolver(GloutonSolver.GloutonPriority.EST_LRPT).solve(instance, deadline);
        Result s_local = s;
        int best = s.schedule.makespan();
        STabou sTabou = new STabou(instance.numMachines, instance.numJobs, dureeTabou);
        int k = 0;
        while (k < maxIter && deadline - System.currentTimeMillis() > 1) {
            k++;
            ResourceOrder order = new ResourceOrder(s.schedule);
            ResourceOrder order_local = new ResourceOrder(s_local.schedule);
            List<Utils.Block> blocksList = blocksOfCriticalPath(order_local);
            Utils.Swap bestSwap = null;
            int best_local = -1;
            for (Utils.Block block : blocksList) {
                List<Utils.Swap> swapList = neighbors(block);
                for (Utils.Swap swap : swapList) {
                    if (sTabou.check(swap, k)) {
                        ResourceOrder copy = order_local.copy();
                        swap.applyOn(copy);
                        int makespan = copy.toSchedule().makespan();
                        if (best_local == -1 || makespan < best_local) {
                            bestSwap = swap;
                            best_local = makespan;
                            order_local = copy;
                            if (makespan < best) {
                                best = makespan;
                                order = copy;
                            }
                        }
                    }
                }
            }
            if (bestSwap != null) {
                sTabou.add(bestSwap, k);
            }
            s_local = new Result(order_local.instance, order_local.toSchedule(), Result.ExitCause.Blocked);
            s = new Result(order.instance, order.toSchedule(), Result.ExitCause.Blocked);
        }
        if (k == maxIter) return s;
        return new Result(s.instance, s.schedule, Result.ExitCause.Timeout);
    }

    /** Returns a list of all blocks of the critical path. */
    List<Utils.Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        List<Utils.Block> blocksList = new ArrayList<>();
        Task t = criticalPath.get(0);
        int machine = order.instance.machine(criticalPath.get(0));
        int firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(t);
        int lastTask = firstTask;
        for (int i = 1; i < criticalPath.size(); i++) {
            t = criticalPath.get(i);
            if (machine == order.instance.machine(t)) {
                lastTask++;
            } else {
                if (firstTask != lastTask) {
                    blocksList.add(new Utils.Block(machine, firstTask, lastTask));
                }
                machine = order.instance.machine(t);
                firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(t);
                lastTask = firstTask;
            }
        }
        return blocksList;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Utils.Swap> neighbors(Utils.Block block) {
        List<Utils.Swap> swapList = new ArrayList<>();
        swapList.add(new Utils.Swap(block.machine, block.firstTask, block.firstTask+1));
        if (block.firstTask != block.lastTask+1) swapList.add(new Utils.Swap(block.machine, block.lastTask-1, block.lastTask));
        return swapList;
    }
}
