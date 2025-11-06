package vn.tlu.cse.ht2.nhom16.moneymanagementapp;

import android.app.Application;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.workers.BillReminderWorker;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        scheduleBillReminder();
    }

    private void scheduleBillReminder() {
        // Create a periodic work request to run once a day.
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                BillReminderWorker.class, 24, TimeUnit.HOURS)
                .build();

        // Enqueue the work request
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "BillReminderWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest);
    }
}
