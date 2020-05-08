package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {
    public static void main(String[] args) {
        try {
            // load the aaa1 instance
            Instance instance = Instance.fromFile(Paths.get("instances/aaa1"));
            JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            ResourceOrder enc2 = new ResourceOrder(enc.toSchedule());
            System.out.println("\nJobNumbers:\n" + enc);
            System.out.println("Schedule:" + enc.toSchedule());
            System.out.println("\nResourceOrder:\n" + enc2);
            System.out.println("Schedule:" + enc2.toSchedule());

            //Schedule sched = enc.toSchedule();
            //System.out.println("SCHEDULE: " + sched);
            //System.out.println("VALID: " + sched.isValid());
            //System.out.println("MAKESPAN: " + sched.makespan());

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
