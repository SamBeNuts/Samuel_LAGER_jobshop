package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import java.util.List;

public class TabouSolver implements Solver {
    //le nombre maximum d'itération
    private int maxIter;
    //la durée des interdictions
    private int dureeTabou;

    public TabouSolver(int maxIter, int dureeTabou) {
        this.maxIter = maxIter;
        this.dureeTabou = dureeTabou;
    }

    /*
     * Structure de données qui permet de vérifier si une solution a délà été visitée ou non
     */
    static class STabou {
        //matrice carrée de taille nbJob*nbMachine
        //dans chaque cellule on trouve à partir de quelle itération la permutation est autorisée
        final int tab[][];
        //la durée des interdictions
        final int dureeTabou;

        STabou(int nbMachine, int nbJob, int dureeTabou) {
            this.tab = new int[nbJob*nbMachine][nbJob*nbMachine];
            this.dureeTabou = dureeTabou;
        }

        //actualise l'itération à partir duquelle le swap est possible
        public void add(Utils.Swap swap, int k) {
            tab[swap.t1][swap.t2] = k + dureeTabou;
        }

        //vérifie si le swap est possible
        public boolean check(Utils.Swap swap, int k) {
            return k > tab[swap.t1][swap.t2];
        }
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        //on initialise s avec la solution retournée par l'algo Glouton
        Result s = new GloutonSolver(GloutonSolver.GloutonPriority.EST_LRPT).solve(instance, deadline);
        //on s_local = meilleur solution pour l'itération
        Result s_local = s;
        int best = s.schedule.makespan();
        //on crée la structure qui permet de vérifier si une solution a délà été visitée ou non
        STabou sTabou = new STabou(instance.numMachines, instance.numJobs, dureeTabou);
        //k permet de compter les itérations
        int k = 0;
        //tant que le nombre d'itération max n'est pas atteinte et que la deadline n'est pas atteinte
        while (k < maxIter && deadline - System.currentTimeMillis() > 1) {
            k++;
            //l'order qui correspond au meilleur schedule (s)
            ResourceOrder order = new ResourceOrder(s.schedule);
            //l'order qui correspond au meilleur schedule de l'itération (s_local)
            ResourceOrder order_local = new ResourceOrder(s_local.schedule);
            //la liste des Block du chemin critique
            List<Utils.Block> blocksList = Utils.blocksOfCriticalPath(order_local);
            //variables pour stocker les meilleurs résultats locaux
            Utils.Swap bestSwap = null;
            int best_local = -1;
            for (Utils.Block block : blocksList) {
                //la liste des Swap pour le Block
                List<Utils.Swap> swapList = Utils.neighbors(block);
                for (Utils.Swap swap : swapList) {
                    //avant de tester le swap, on vérifie qu'il est autorisé
                    if (sTabou.check(swap, k)) {
                        //on copie l'ordre de s et on applique le swap
                        ResourceOrder copy = order_local.copy();
                        swap.applyOn(copy);
                        int makespan = copy.toSchedule().makespan();
                        //si le swap retourne un meilleur résultat que le résultat local on actualise s_local
                        if (best_local == -1 || makespan < best_local) {
                            bestSwap = swap;
                            best_local = makespan;
                            order_local = copy;
                            //si le swap est également meilleur que s, on actulise s
                            if (makespan < best) {
                                best = makespan;
                                order = copy;
                            }
                        }
                    }
                }
            }
            //si un swap est meilleur que la solution locale on l'ajoute à la structure
            if (bestSwap != null) {
                sTabou.add(bestSwap, k);
            }
            //on actualise s et s_local
            s_local = new Result(order_local.instance, order_local.toSchedule(), Result.ExitCause.Blocked);
            s = new Result(order.instance, order.toSchedule(), Result.ExitCause.Blocked);
        }
        //en fonction de si maxIter a été atteint ou si la deadline a été atteinte
        //on ne retourne pas la même raison de sortie
        if (k == maxIter) return s;
        return new Result(s.instance, s.schedule, Result.ExitCause.Timeout);
    }
}
