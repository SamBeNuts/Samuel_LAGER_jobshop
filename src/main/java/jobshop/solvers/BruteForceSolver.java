package jobshop.solvers;

import jobshop.*;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.Arrays;

public class BruteForceSolver implements Solver {
    private Schedule schedule = null;
    private int best = 0;
    private boolean useJobNumbers;
    private boolean justPrint = false;

    public BruteForceSolver(boolean useJobNumbers) {
        this.useJobNumbers = useJobNumbers;
    }
    public BruteForceSolver(boolean useJobNumbers, boolean justPrint) {
        this.useJobNumbers = useJobNumbers;
        this.justPrint = justPrint;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        int[] nextTaskByJobs = new int[instance.numJobs];
        if (useJobNumbers) scheduleJobs(instance, new JobNumbers(instance), 0, nextTaskByJobs);
        else scheduleJobs(instance, new ResourceOrder(instance), 0, nextTaskByJobs);
        return new Result(instance, schedule, Result.ExitCause.ProvedOptimal);
    }

    private void scheduleJobs(Instance instance, JobNumbers sol, int index, int[] nextTaskByJobs) {
        for (int i = 0; i < instance.numJobs; i++) {
            if (nextTaskByJobs[i] < instance.numTasks) {
                sol.jobs[index] = i;
                int[] clone = nextTaskByJobs.clone();
                clone[i]++;
                if (index + 1 < instance.numTasks * instance.numJobs) scheduleJobs(instance, sol, index + 1, clone);
                else if (justPrint) System.out.println(Arrays.toString(sol.jobs));
                else {
                    Schedule sched = sol.toSchedule();
                    if (schedule == null || sched.makespan() < best) {
                        best = sched.makespan();
                        schedule = sched;
                    }
                }
            }
        }
    }

    private void scheduleJobs(Instance instance, ResourceOrder sol, int index, int[] nextTaskByJobs) {
        for (int i = 0; i < instance.numJobs; i++) {
            int machine = index / instance.numJobs;
            if (nextTaskByJobs[i] == machine) {
                int task = index % instance.numJobs;
                sol.tasksByMachine[machine][task] = new Task(i, instance.task_with_machine(i, machine));
                int[] clone = nextTaskByJobs.clone();
                clone[i]++;
                if (index + 1 < instance.numTasks * instance.numJobs) scheduleJobs(instance, sol, index + 1, clone);
                else if (justPrint) System.out.println(Arrays.deepToString(sol.tasksByMachine));
                else {
                    Schedule sched = sol.toSchedule();
                    if (schedule == null || sched.makespan() < best) {
                        best = sched.makespan();
                        schedule = sched;
                    }
                }
            }
        }
    }
}
