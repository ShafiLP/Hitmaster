package hitmaster.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private int seconds;

    public Timer(int seconds) {
        this.seconds = seconds;
    }

    public void start(Runnable everySecondAction, Runnable finishedAction) {
        scheduler.scheduleAtFixedRate(() -> {

            if (seconds > 0) {
                everySecondAction.run();
                seconds--;
            } else {
                finishedAction.run();
                scheduler.shutdown();
            }

        }, 0, 1, TimeUnit.SECONDS);
    }

    public int getRemainingSeconds() {
        return seconds;
    }

    public void stop() {
        scheduler.shutdownNow();
    }
}