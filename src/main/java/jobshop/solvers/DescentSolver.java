package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DescentSolver implements Solver {
    @Override
    public Result solve(Instance instance, long deadline) {
        Result s = new GloutonSolver(GloutonSolver.GloutonPriority.LRPT).solve(instance, deadline);
        int best = s.schedule.makespan();
        while (deadline - System.currentTimeMillis() > 1) {
            boolean exit = true;
            ResourceOrder order = new ResourceOrder(s.schedule);
            List<Utils.Block> blocksList = blocksOfCriticalPath(order);
            for (Utils.Block block : blocksList) {
                List<Utils.Swap> swapList = neighbors(block);
                for (Utils.Swap swap : swapList) {
                    ResourceOrder copy = order.copy();
                    swap.applyOn(copy);
                    int makespan = copy.toSchedule().makespan();
                    if (makespan < best) {
                        if (exit) exit = false;
                        order = copy;
                        best = makespan;
                    }
                }
            }
            if (!exit) s = new Result(order.instance, order.toSchedule(), Result.ExitCause.Blocked);
            else return s;
        }
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
