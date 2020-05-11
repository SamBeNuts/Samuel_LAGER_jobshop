package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

public class BruteForceSolver implements Solver {
    private Schedule schedule = null;
    private int best = 0;
    private boolean useJobNumbers;

    public BruteForceSolver(boolean useJobNumbers) {
        this.useJobNumbers = useJobNumbers;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        int[] nextTaskByJobs = new int[instance.numJobs];
        if (useJobNumbers) scheduleJobs(instance, new JobNumbers(instance), 0, nextTaskByJobs);
        else {
            int[] nextTaskByMachines = new int[instance.numMachines];
            scheduleJobs(instance, new ResourceOrder(instance), 0, nextTaskByJobs, nextTaskByMachines);
        }
        return new Result(instance, schedule, Result.ExitCause.ProvedOptimal);
    }

    private void scheduleJobs(Instance instance, JobNumbers sol, int index, int[] nextTaskByJobs) {
        for (int i = 0; i < instance.numJobs; i++) {
            if (nextTaskByJobs[i] < instance.numTasks) {
                int[] clone = nextTaskByJobs.clone();
                sol.jobs[index] = i;
                clone[i]++;
                if (index + 1 < instance.numTasks * instance.numJobs) scheduleJobs(instance, sol, index + 1, clone);
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

    private void scheduleJobs(Instance instance, ResourceOrder sol, int index, int[] nextTaskByJobs, int[] nextTaskByMachines) {
        for (int i = 0; i < instance.numJobs; i++) {
            if (nextTaskByJobs[i] < instance.numTasks) {
                int[] cloneJob = nextTaskByJobs.clone();
                int[] cloneMachine = nextTaskByMachines.clone();
                int machine = instance.task_with_machine(i, nextTaskByJobs[i]);
                sol.tasksByMachine[machine][cloneMachine[machine]] = new Task(i, nextTaskByJobs[i]);
                cloneJob[i]++;
                cloneMachine[machine]++;
                if (index + 1 < instance.numTasks * instance.numJobs) scheduleJobs(instance, sol, index + 1, cloneJob, cloneMachine);
                else {
                    Schedule sched = sol.toSchedule();
                    if (sched != null && (schedule == null || sched.makespan() < best)) {
                        best = sched.makespan();
                        schedule = sched;
                    }
                }
            }
        }
    }
}
