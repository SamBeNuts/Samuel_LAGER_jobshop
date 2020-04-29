package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;

public class GloutonSolver implements Solver {
    /*
     * Les différentes règles de priorités
     */
    public enum GloutonPriority {
        SPT,        //donne priorité à la tâche la plus courte
        LPT,        //donne priorité à la tâche la plus longue
        SRPT,       //donne priorité à la tâche appartenant au job ayant la plus petite durée restante
        LRPT,       //donne priorité à la tâche appartenant au job ayant la plus longue durée restante
        EST_SPT,    //idem SPT en traitant uniquement les tâches pouvant commencer au plus tôt
        EST_LPT,    //idem LPT en traitant uniquement les tâches pouvant commencer au plus tôt
        EST_SRPT,   //idem SRPT en traitant uniquement les tâches pouvant commencer au plus tôt
        EST_LRPT    //idem LRPT en traitant uniquement les tâches pouvant commencer au plus tôt
    }

    private GloutonPriority priority;

    public GloutonSolver(GloutonPriority priority) {
        this.priority = priority;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        //ResourceOrder qui représente la solution
        ResourceOrder sol = new ResourceOrder(instance);
        //la liste des tâches qui peuvent et n'ont pas encore été schédulés (au maximum autant que le nombre de jobs)
        ArrayList<Task> taskToScheduled = new ArrayList<>();
        //on initialise la liste avec la première tâche de chaque job
        for (int i = 0; i < instance.numJobs; i++)
            taskToScheduled.add(new Task(i, 0));
        //array pour stocker la durée des tâches restantes pour chaque job
        //cette array sert pour les priorités SRPT, LRPT, EST_SRPT et EST_SRPT
        int[] timeRemaining = new int[instance.numJobs];
        //on initialise l'array s'il est utilisé
        if (priority.name().endsWith("RPT")) {
            for (int i = 0; i < instance.numJobs; i++) {
                for (int j = 0; j < instance.numTasks; j++) {
                    //on ajoute la durée de chaque tâche (j) du job (i)
                    timeRemaining[i] += instance.duration(i, j);
                }
            }
        }
        //array pour stocker à quel moment se termine chaque tâche (si pas encore traitée, alors par défaut 0)
        int[][] endAt = new int[instance.numJobs][instance.numTasks];
        //array pour stocker à quel moment chaque machine est disponible
        int[] releaseTimeOfMachine = new int[instance.numMachines];
        //tant qu'il reste des tâches à traiter on continue
        while (taskToScheduled.size() > 0) {
            Task task;
            //en fonction de la priorité choisie, on récupère la prochaine tâche (task) à sheduler
            switch (priority) {
                case SPT:
                    task = SPT(instance, taskToScheduled);
                    break;
                case LPT:
                    task = LPT(instance, taskToScheduled);
                    break;
                case SRPT:
                    task = SRPT(instance, taskToScheduled, timeRemaining);
                    break;
                case LRPT:
                    task = LRPT(instance, taskToScheduled, timeRemaining);
                    break;
                case EST_SPT:
                    task = EST_SPT(instance, taskToScheduled, endAt, releaseTimeOfMachine);
                    break;
                case EST_LPT:
                    task = EST_LPT(instance, taskToScheduled, endAt, releaseTimeOfMachine);
                    break;
                case EST_SRPT:
                    task = EST_SRPT(instance, taskToScheduled, endAt, releaseTimeOfMachine, timeRemaining);
                    break;
                default:
                    task = EST_LRPT(instance, taskToScheduled, endAt, releaseTimeOfMachine, timeRemaining);
                    break;
            }
            //on récupère la machine qui va traiter la tâche
            int machine = instance.machine(task);
            //on schedule la tâche
            sol.tasksByMachine[machine][sol.nextFreeSlot[machine]++] = task;
            //si la tâche n'est pas la dernière du job, on ajoute la suivante à la liste des tâches à traiter
            if (task.task < instance.numTasks - 1)
                taskToScheduled.add(new Task(task.job, task.task + 1));
        }
        //retourne la solution trouvée
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    /*
     * Retourne une version réduite du tableau tasks, avec seulement les tâches qui peuvent commencer au plus tôt
     */
    private ArrayList<Task> EST(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine) {
        ArrayList<Task> reducer = new ArrayList<>();
        Task t = tasks.get(0);
        reducer.add(t);
        //est a un fonctionnement similaire au est du ResourceOrder
        int est = t.task == 0 ? 0 : endAt[t.job][t.task - 1];
        int best = Math.max(releaseTimeOfMachine[inst.machine(t)], est);
        for (int i = 1; i < tasks.size(); i++) {
            t = tasks.get(i);
            est = t.task == 0 ? 0 : endAt[t.job][t.task - 1];
            int start = Math.max(releaseTimeOfMachine[inst.machine(t)], est);
            //si la tâche (t) commence plus tôt que best, on vide la liste et on y met t
            //si la tâche commence en même temps que best, on l'ajoute à la liste
            if (start < best) {
                reducer.clear();
                reducer.add(t);
                best = start;
            } else if (start == best)
                reducer.add(t);
        }
        return reducer;
    }

    /*
     * Idem que la fonction EST() mais adaptée aux priorités SRPT, LRPT, EST_SRPT et EST_LRPT
     */
    private int[] EST_RPT(Instance inst, ArrayList<Task> tasks, int[] remaining, int[][] endAt, int[] releaseTimeOfMachine) {
        //chaque élément de l'array égal 0 si la tâche ne commence pas au plus tôt
        //sinon sa valeur est égal à l'élément correspondant de l'array remaining
        int[] reducer = new int[remaining.length];
        Task t = tasks.get(0);
        //est a un fonctionnement similaire au est du ResourceOrder
        int est = t.task == 0 ? 0 : endAt[t.job][t.task - 1];
        int best = Math.max(releaseTimeOfMachine[inst.machine(t)], est);
        reducer[t.job] = remaining[t.job];
        for (int i = 1; i < tasks.size(); i++) {
            t = tasks.get(i);
            est = t.task == 0 ? 0 : endAt[t.job][t.task - 1];
            int start = Math.max(releaseTimeOfMachine[inst.machine(t)], est);
            //si la tâche (t) commence plus tôt que best, on crée un nouvel array
            //et l'élément d'indice i prend la valeur correspondante de l'array remaining
            //si la tâche commence en même temps que best, l'élément d'indice i prend la valeur correspondante de l'array remaining
            if (start < best) {
                reducer = new int[remaining.length];
                reducer[tasks.get(i).job] = remaining[tasks.get(i).job];
                best = start;
            } else if (start == best)
                reducer[tasks.get(i).job] = remaining[tasks.get(i).job];
        }
        return reducer;
    }

    /*
     * Retourne la tâche la plus courte
     */
    private Task SPT(Instance inst, ArrayList<Task> tasks) {
        int best = 0;
        int bestDuration = inst.duration(tasks.get(best));
        for (int i = 1; i < tasks.size(); i++) {
            if (bestDuration > inst.duration(tasks.get(i))) {
                best = i;
                bestDuration = inst.duration(tasks.get(best));
            }
        }
        return tasks.remove(best);
    }

    /*
     * Retourne la tâche la plus longue
     */
    private Task LPT(Instance inst, ArrayList<Task> tasks) {
        int best = 0;
        int bestDuration = inst.duration(tasks.get(best));
        for (int i = 1; i < tasks.size(); i++) {
            if (bestDuration < inst.duration(tasks.get(i))) {
                best = i;
                bestDuration = inst.duration(tasks.get(best));
            }
        }
        return tasks.remove(best);
    }

    /*
     * Retourne la tâche appartenant au job ayant la plus petite durée restante
     */
    private Task SRPT(Instance inst, ArrayList<Task> tasks, int[] remaining) {
        int best = 0;
        for (int i = 1; i < remaining.length; i++) {
            //si le job d'indice i est meilleur que best
            //ou si le best = 0 (le job a déjà fini ses tâches), on actualise la meilleure tâche
            //à condition que le job i ne soit pas lui même fini
            if (remaining[i] != 0 && (remaining[best] == 0 || remaining[best] > remaining[i]))
                best = i;
        }
        for (int i = 0; i < tasks.size(); i++) {
            //on actualise le temps restant pour la tâche retournée
            if (tasks.get(i).job == best) {
                remaining[best] -= inst.duration(best, tasks.get(i).task);
                best = i;
                break;
            }
        }
        return tasks.remove(best);
    }

    /*
     * Retourne la tâche appartenant au job ayant la plus longue durée restante
     */
    private Task LRPT(Instance inst, ArrayList<Task> tasks, int[] remaining) {
        int best = 0;
        for (int i = 1; i < remaining.length; i++) {
            //si le job d'indice i est meilleur que best
            if (remaining[best] < remaining[i])
                best = i;
        }
        for (int i = 0; i < tasks.size(); i++) {
            //on actualise le temps restant pour la tâche retournée
            if (tasks.get(i).job == best) {
                remaining[best] -= inst.duration(best, tasks.get(i).task);
                best = i;
                break;
            }
        }
        return tasks.remove(best);
    }

    /*
     * Idem SPT en respectant également la priorité EST
     */
    private Task EST_SPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine) {
        //on réduit la liste pour respecter la priorité EST
        ArrayList<Task> reduce = EST(inst, tasks, endAt, releaseTimeOfMachine);
        Task task = SPT(inst, reduce);
        //on actualise les arrays utilisés par la fonction EST
        EST_actualize(inst, task, endAt, releaseTimeOfMachine);
        tasks.remove(task);
        return task;
    }

    /*
     * Idem LPT en respectant également la priorité EST
     */
    private Task EST_LPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine) {
        //on réduit la liste pour respecter la priorité EST
        ArrayList<Task> reduce = EST(inst, tasks, endAt, releaseTimeOfMachine);
        Task task = LPT(inst, reduce);
        //on actualise les arrays utilisés par la fonction EST
        EST_actualize(inst, task, endAt, releaseTimeOfMachine);
        tasks.remove(task);
        return task;
    }

    /*
     * Idem SRPT en respectant également la priorité EST
     */
    private Task EST_SRPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine, int[] remaining) {
        //on réduit la liste pour respecter la priorité EST
        int[] reduce = EST_RPT(inst, tasks, remaining, endAt, releaseTimeOfMachine);
        Task task = SRPT(inst, tasks, reduce);
        //on actualise les arrays utilisés par la fonction EST_RPT
        EST_actualize(inst, task, endAt, releaseTimeOfMachine);
        remaining[task.job] = reduce[task.job];
        return task;
    }

    /*
     * Idem LRPT en respectant également la priorité EST
     */
    private Task EST_LRPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine, int[] remaining) {
        //on réduit la liste pour respecter la priorité EST
        int[] reduce = EST_RPT(inst, tasks, remaining, endAt, releaseTimeOfMachine);
        Task task = LRPT(inst, tasks, reduce);
        //on actualise les arrays utilisés par la fonction EST_RPT
        EST_actualize(inst, task, endAt, releaseTimeOfMachine);
        remaining[task.job] = reduce[task.job];
        return task;
    }

    /*
     * actualise les arrays utilisés par les fonctions EST et EST_RPT en fonction de la tâche qui vient d'être schedulé
     */
    private void EST_actualize(Instance inst, Task task, int[][] endAt, int[] releaseTimeOfMachine) {
        int machine = inst.machine(task);
        int duration = +inst.duration(task);
        int est = task.task == 0 ? 0 : endAt[task.job][task.task - 1];
        endAt[task.job][task.task] = Math.max(releaseTimeOfMachine[machine], est) + duration;
        releaseTimeOfMachine[machine] = endAt[task.job][task.task];
    }
}
