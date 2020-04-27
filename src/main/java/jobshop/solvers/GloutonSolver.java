package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;

public class GloutonSolver implements Solver {
  public enum GloutonPriority {
    SPT, LPT, SRPT, LRPT, EST_SPT, EST_LPT, EST_SRPT, EST_LRPT
  }

  private GloutonPriority priority;

  public GloutonSolver(GloutonPriority priority) {
    this.priority = priority;
  }

  @Override
  public Result solve(Instance instance, long deadline) {
    ResourceOrder sol = new ResourceOrder(instance);
    ArrayList<Task> taskToScheduled = new ArrayList<>();
    for (int i = 0; i < instance.numJobs; i++)
      taskToScheduled.add(new Task(i, 0));
    int[] timeRemaining = new int[instance.numJobs];
    if (priority.name().endsWith("RPT")) {
      for (int i = 0; i < instance.numJobs; i++) {
        for (int j = 0; j < instance.numTasks; j++) {
          timeRemaining[i] += instance.duration(i, j);
        }
      }
    }
    int[][] endAt = new int[instance.numJobs][instance.numTasks];
    int[] releaseTimeOfMachine = new int[instance.numMachines];
    while (taskToScheduled.size() > 0) {
      Task task;
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
      int machine = instance.machine(task.job, task.task);
      sol.tasksByMachine[machine][sol.nextFreeSlot[machine]++] = task;
      if (task.task < instance.numTasks - 1)
        taskToScheduled.add(new Task(task.job, task.task + 1));
    }
    return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
  }

  private ArrayList<Task> EST(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine) {
    ArrayList<Task> reducer = new ArrayList<>();
    Task t = tasks.get(0);
    reducer.add(t);
    int est = t.task == 0 ? 0 : endAt[t.job][t.task-1];
    int best = Math.max(releaseTimeOfMachine[inst.machine(t.job, t.task)], est);
    for (int i = 1; i < tasks.size(); i++) {
      t = tasks.get(i);
      est = t.task == 0 ? 0 : endAt[t.job][t.task-1];
      int start = Math.max(releaseTimeOfMachine[inst.machine(t.job, t.task)], est);
      if (start < best) {
        reducer.clear();
        reducer.add(t);
        best = start;
      } else if (start == best)
        reducer.add(t);
    }
    return reducer;
  }

  private int[] EST_RPT(Instance inst, ArrayList<Task> tasks, int[] remaining, int[][] endAt, int[] releaseTimeOfMachine) {
    int[] reducer = new int[remaining.length];
    Task t = tasks.get(0);
    int est = t.task == 0 ? 0 : endAt[t.job][t.task-1];
    int best = Math.max(releaseTimeOfMachine[inst.machine(t.job, t.task)], est);
    reducer[t.job] = remaining[t.job];
    for (int i = 1; i < tasks.size(); i++) {
      t = tasks.get(i);
      est = t.task == 0 ? 0 : endAt[t.job][t.task-1];
      int start = Math.max(releaseTimeOfMachine[inst.machine(t.job, t.task)], est);
      if (start < best) {
        reducer = new int[remaining.length];
        reducer[tasks.get(i).job] = remaining[tasks.get(i).job];
        best = start;
      } else if (start == best)
        reducer[tasks.get(i).job] = remaining[tasks.get(i).job];
    }
    return reducer;
  }

  private Task SPT(Instance inst, ArrayList<Task> tasks) {
    int best = 0;
    int bestDuration = inst.duration(tasks.get(best).job, tasks.get(best).task);
    for (int i = 1; i < tasks.size(); i++) {
      if (bestDuration > inst.duration(tasks.get(i).job, tasks.get(i).task)) {
        best = i;
        bestDuration = inst.duration(tasks.get(best).job, tasks.get(best).task);
      }
    }
    return tasks.remove(best);
  }

  private Task LPT(Instance inst, ArrayList<Task> tasks) {
    int best = 0;
    int bestDuration = inst.duration(tasks.get(best).job, tasks.get(best).task);
    for (int i = 1; i < tasks.size(); i++) {
      if (bestDuration < inst.duration(tasks.get(i).job, tasks.get(i).task)) {
        best = i;
        bestDuration = inst.duration(tasks.get(best).job, tasks.get(best).task);
      }
    }
    return tasks.remove(best);
  }

  private Task SRPT(Instance inst, ArrayList<Task> tasks, int[] remaining) {
    int best = 0;
    for (int i = 1; i < remaining.length; i++) {
      if (remaining[i] != 0 && (remaining[best] == 0 || remaining[best] > remaining[i]))
        best = i;
    }
    for (int i = 0; i < tasks.size(); i++) {
      if (tasks.get(i).job == best) {
        remaining[best] -= inst.duration(best, tasks.get(i).task);
        best = i;
        break;
      }
    }
    return tasks.remove(best);
  }

  private Task LRPT(Instance inst, ArrayList<Task> tasks, int[] remaining) {
    int best = 0;
    for (int i = 1; i < remaining.length; i++) {
      if (remaining[best] < remaining[i])
        best = i;
    }
    for (int i = 0; i < tasks.size(); i++) {
      if (tasks.get(i).job == best) {
        remaining[best] -= inst.duration(best, tasks.get(i).task);
        best = i;
        break;
      }
    }
    return tasks.remove(best);
  }

  private Task EST_SPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine) {
    ArrayList<Task> reduce = EST(inst, tasks, endAt, releaseTimeOfMachine);
    Task task = SPT(inst, reduce);
    EST_actualize(inst, task, endAt, releaseTimeOfMachine);
    tasks.remove(task);
    return task;
  }

  private Task EST_LPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine) {
    ArrayList<Task> reduce = EST(inst, tasks, endAt, releaseTimeOfMachine);
    Task task = LPT(inst, reduce);
    EST_actualize(inst, task, endAt, releaseTimeOfMachine);
    tasks.remove(task);
    return task;
  }

  private Task EST_SRPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine, int[] remaining) {
    int[] reduce = EST_RPT(inst, tasks, remaining, endAt, releaseTimeOfMachine);
    Task task = SRPT(inst, tasks, reduce);
    EST_actualize(inst, task, endAt, releaseTimeOfMachine);
    remaining[task.job] = reduce[task.job];
    return task;
  }

  private Task EST_LRPT(Instance inst, ArrayList<Task> tasks, int[][] endAt, int[] releaseTimeOfMachine, int[] remaining) {
    int[] reduce = EST_RPT(inst, tasks, remaining, endAt, releaseTimeOfMachine);
    Task task = LRPT(inst, tasks, reduce);
    EST_actualize(inst, task, endAt, releaseTimeOfMachine);
    remaining[task.job] = reduce[task.job];
    return task;
  }

  private void EST_actualize(Instance inst, Task task, int[][] endAt, int[] releaseTimeOfMachine) {
    int machine = inst.machine(task.job, task.task);
    int duration =  + inst.duration(task.job, task.task);
    int est = task.task == 0 ? 0 : endAt[task.job][task.task - 1];
    endAt[task.job][task.task] = Math.max(releaseTimeOfMachine[machine], est) + duration;
    releaseTimeOfMachine[machine] = endAt[task.job][task.task];
  }
}
