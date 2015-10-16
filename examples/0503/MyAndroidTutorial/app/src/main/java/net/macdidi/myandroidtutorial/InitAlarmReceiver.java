package net.macdidi.myandroidtutorial;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

public class InitAlarmReceiver extends BroadcastReceiver {
    public InitAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ItemDAO itemDAO = new ItemDAO(context.getApplicationContext());
        List<Item> items = itemDAO.getAll();

        long current = Calendar.getInstance().getTimeInMillis();

        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        for (Item item : items) {
            long alarm = item.getAlarmDatetime();

            if (alarm == 0 || alarm <= current) {
                continue;
            }

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            // 移除原來的記事標題
            //alarmIntent.putExtra("title", item.getTitle());

            // 加入記事編號
            intent.putExtra("id", item.getId());

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, (int)item.getId(),
                    alarmIntent, PendingIntent.FLAG_ONE_SHOT);
            am.set(AlarmManager.RTC_WAKEUP, item.getAlarmDatetime(), pi);
        }
    }
}
