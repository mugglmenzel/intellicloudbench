package edu.kit.aifb.experiments;

import it.sauronsoftware.cron4j.Scheduler;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by mugglmenzel on 05/07/14.
 */
public class Cron4JExperiment {

    public static void main(String[] params) {

        final SimpleDateFormat df = new SimpleDateFormat();
        df.applyPattern("hh:mm:ss SSS");


        Scheduler s = new Scheduler();
        s.schedule("* * * * *", new Runnable() {
            public void run() {
                long delay = System.currentTimeMillis();
                System.out.println(delay + " -> " + df.format(new Date(delay)));
            }
        });


        s.start();

    }

}
