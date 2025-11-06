package vn.tlu.cse.ht2.nhom16.moneymanagementapp.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import vn.tlu.cse.ht2.nhom16.moneymanagementapp.R;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.activities.MainActivity;
import vn.tlu.cse.ht2.nhom16.moneymanagementapp.models.Bill;

public class BillReminderWorker extends Worker {

    private static final String TAG = "BillReminderWorker";
    private static final String CHANNEL_ID = "BILL_REMINDER_CHANNEL";

    public BillReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker is running...");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in, stopping worker.");
            return Result.success();
        }

        String userId = currentUser.getUid();
        final CountDownLatch latch = new CountDownLatch(1);

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 3); // Check for bills due in the next 3 days
        Date threeDaysFromNow = calendar.getTime();

        FirebaseFirestore.getInstance().collection("users").document(userId).collection("bills")
                .whereEqualTo("paid", false)
                .whereGreaterThanOrEqualTo("dueDate", today)
                .whereLessThanOrEqualTo("dueDate", threeDaysFromNow)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Bill bill = document.toObject(Bill.class);
                            Log.d(TAG, "Found upcoming bill: " + bill.getName());
                            sendNotification(bill);
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch bills.", task.getException());
                    }
                    latch.countDown();
                });

        try {
            latch.await(); // Wait for Firestore query to complete
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted", e);
            return Result.failure();
        }

        Log.d(TAG, "Worker finished.");
        return Result.success();
    }

    private void sendNotification(Bill bill) {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở Hóa đơn",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Thông báo cho các hóa đơn sắp đến hạn");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String dueDateString = sdf.format(bill.getDueDate());

        String contentText = String.format(Locale.getDefault(),
                "Hóa đơn '%s' với số tiền %,.0f VND sắp đến hạn vào ngày %s.",
                bill.getName(), bill.getAmount(), dueDateString);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_receipt) // Ensure you have this drawable
                .setContentTitle("Nhắc nhở thanh toán hóa đơn")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Use a unique ID for each notification to show multiple notifications
        int notificationId = (int) (bill.getDueDate().getTime() / 1000);
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Sent notification for bill: " + bill.getName());
    }
}
