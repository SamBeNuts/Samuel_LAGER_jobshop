package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import java.util.List;

public class DescentSolver implements Solver {
    @Override
    public Result solve(Instance instance, long deadline) {
        //on initialise s avec la solution retournée par l'algo Glouton
        Result s = new GloutonSolver(GloutonSolver.GloutonPriority.LRPT).solve(instance, deadline);
        int best = s.schedule.makespan();
        //tant que la deadline n'est pas atteinte
        while (deadline - System.currentTimeMillis() > 1) {
            //par défaut on sort (on part du principe qu'aucun meilleur order ne sera trouvé)
            boolean exit = true;
            //l'order qui correspond au meilleur schedule (s)
            ResourceOrder order = new ResourceOrder(s.schedule);
            //la liste des Block du chemin critique
            List<Utils.Block> blocksList = Utils.blocksOfCriticalPath(order);
            for (Utils.Block block : blocksList) {
                //la liste des Swap pour le Block
                List<Utils.Swap> swapList = Utils.neighbors(block);
                for (Utils.Swap swap : swapList) {
                    //on copie l'ordre de s et on applique le swap
                    ResourceOrder copy = order.copy();
                    swap.applyOn(copy);
                    int makespan = copy.toSchedule().makespan();
                    //si le swap retourne un meilleur résultat on actualise s
                    if (makespan < best) {
                        if (exit) exit = false;
                        order = copy;
                        best = makespan;
                    }
                }
            }
            //si aucun meilleur order n'a été trouvé on sort
            if (!exit) s = new Result(order.instance, order.toSchedule(), Result.ExitCause.Blocked);
            else return s;
        }
        return new Result(s.instance, s.schedule, Result.ExitCause.Timeout);
    }
}
