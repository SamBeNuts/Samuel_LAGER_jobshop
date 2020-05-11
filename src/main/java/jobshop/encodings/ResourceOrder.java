package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;
import jobshop.solvers.BruteForceSolver;

import java.util.Arrays;

public class ResourceOrder extends Encoding {

    // for each machine m, taskByMachine[m] is an array of tasks to be
    // executed on this machine in the same order
    public final Task[][] tasksByMachine;

    // for each machine, indicate on many tasks have been initialized
    public final int[] nextFreeSlot;

    /** Creates a new empty resource order. */
    public ResourceOrder(Instance instance) {
        super(instance);

        // matrix of null elements (null is the default value of objects)
        tasksByMachine = new Task[instance.numMachines][instance.numJobs];

        // no task scheduled on any machine (0 is the default value)
        nextFreeSlot = new int[instance.numMachines];
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule) {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.tasksByMachine = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        int machine;
        Task best[] = new Task[instance.numMachines];
        while (nextFreeSlot[0] < instance.numJobs) {
            Arrays.fill(best, null);
            for (int i=0; i < instance.numJobs; i++) {
                for (int j=0; j < instance.numTasks; j++) {
                    machine = instance.machine(i, j);
                    if ((nextFreeSlot[machine] == 0 || schedule.startTime(tasksByMachine[machine][nextFreeSlot[machine]-1].job, tasksByMachine[machine][nextFreeSlot[machine]-1].task) < schedule.startTime(i, j)) && (best[machine] == null || schedule.startTime(best[machine].job, best[machine].task) > schedule.startTime(i, j))) best[machine] = new Task(i, j);
                }
            }
            for (int i=0; i < instance.numMachines; i++) {
                tasksByMachine[i][nextFreeSlot[i]++] = best[i];
            }
        }
    }

    @Override
    public Schedule toSchedule() {
        int[] nextFreeTimeResource = new int[instance.numMachines];
        int[] nextTaskJob = new int[instance.numJobs];
        int[] nextTaskMachine = new int[instance.numMachines];
        int[][] startTimes = new int[instance.numJobs][instance.numTasks];
        int nbNotScheduled = 0;
        for (int nbTask: nextFreeSlot) nbNotScheduled += nbTask;
        while (nbNotScheduled > 0) {
            for (int i = 0; i < instance.numMachines; i++) {
                if (nextTaskMachine[i] < instance.numJobs) {
                    Task t = tasksByMachine[i][nextTaskMachine[i]];
                    if (t.task == nextTaskJob[t.job]) {
                        int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                        est = Math.max(est, nextFreeTimeResource[i]);
                        startTimes[t.job][t.task] = est;
                        nextFreeTimeResource[i] = est + instance.duration(t.job, t.task);
                        nextTaskJob[t.job]++;
                        nextTaskMachine[i]++;
                        nbNotScheduled--;
                    }
                }
            }
        }
        return new Schedule(instance, startTimes);
    }

    /** Creates an exact copy of this resource order. */
    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    public void printSolutions() { new BruteForceSolver(false, true).solve(instance, 0); }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i=0; i < instance.numMachines; i++) {
            str.append(Arrays.toString(Arrays.copyOfRange(tasksByMachine[i], 0, nextFreeSlot[i]))).append('\n');
        }
        return str.toString();
    }

}