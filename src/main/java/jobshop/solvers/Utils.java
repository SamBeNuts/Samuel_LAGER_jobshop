package jobshop.solvers;

import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {
            Task t = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t2] = t;
        }
    }

    /** Returns a list of all blocks of the critical path. */
    static List<Block> blocksOfCriticalPath(ResourceOrder order) {
        //on récupère le chemin critique
        List<Task> criticalPath = order.toSchedule().criticalPath();
        //la liste des Block du chemin critique
        List<Block> blocksList = new ArrayList<>();
        Task t = criticalPath.get(0);
        int machine = order.instance.machine(t);
        //on récupère la position dans l'ordre d'exécution de la machine
        int firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(t);
        int lastTask = firstTask;
        for (int i = 1; i < criticalPath.size(); i++) {
            t = criticalPath.get(i);
            //on vérifie que les deux tâches sont effectuées sur la même machine
            if (machine == order.instance.machine(t)) {
                lastTask++;
            } else {
                //on vérifie si un Block existe
                if (firstTask != lastTask) {
                    blocksList.add(new Block(machine, firstTask, lastTask));
                }
                //on reset les variables
                machine = order.instance.machine(t);
                firstTask = Arrays.asList(order.tasksByMachine[machine]).indexOf(t);
                lastTask = firstTask;
            }
        }
        //on vérifie si un Block existe à la fin du chemin
        if (firstTask != lastTask) {
            blocksList.add(new Block(machine, firstTask, lastTask));
        }
        return blocksList;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    static List<Swap> neighbors(Block block) {
        List<Swap> swapList = new ArrayList<>();
        swapList.add(new Swap(block.machine, block.firstTask, block.firstTask+1));
        //si la différence entre 2 indices est supérieure à 1, alors la taille du Block est supérieure à 2
        //il y a donc un deuxième Swap possible entre les deux dernières tâches
        if (block.firstTask != block.lastTask+1) swapList.add(new Swap(block.machine, block.lastTask-1, block.lastTask));
        return swapList;
    }
}
