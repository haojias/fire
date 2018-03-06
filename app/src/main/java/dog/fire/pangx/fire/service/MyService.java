package dog.fire.pangx.fire.service;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import dog.fire.pangx.fire.MainActivity;
import dog.fire.pangx.fire.MyApplication;
import dog.fire.pangx.fire.R;
import dog.fire.pangx.fire.TaskFragmentActivity;
import dog.fire.pangx.fire.db.Task;
import dog.fire.pangx.fire.utils.AnalysisRecord;
import dog.fire.pangx.fire.utils.Handle;

public class MyService extends Service {
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Task> taskList= DataSupport.select("recording","remindTime").order("createTime desc").find(Task.class);
                if (taskList.size()>0){
                    for (Task t:taskList){
                        Date date=new Date();
                        String d2=null;
                        String d1= AnalysisRecord.sdf2.format(date);
                        try {
                            Date date1=AnalysisRecord.sdf2.parse(t.getRemindTime());
                            d2=AnalysisRecord.sdf2.format(date1);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if (d1.equals(d2)){
                            Intent intent=new Intent(MyService.this,MainActivity.class);
                            PendingIntent pendingIntent=PendingIntent.getActivity(MyService.this,0,intent,0);
                            NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                            Notification notification=new Notification.Builder(MyService.this).setContentTitle("定时提示")
                                    .setContentText(t.getRecording())
                                    .setWhen(System.currentTimeMillis())
                                    .setSmallIcon(R.drawable.fire)
                                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true)
                                    .setVibrate(new long[] {0,1000,1000,1000,1000,1000,1000,1000,1000,1000})
                                    .setLights(Color.GREEN,1000,1000)
                                    .build();
                            manager.notify(1,notification);
                            DataSupport.delete(Task.class, t.getId());
                            final Intent intent1 = new Intent("ACTION_UPDATEUI");
                            intent.putExtra("msg", "0");
                            sendBroadcast(intent1);
                        }
                    }
                }
            }
        }).start();

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int d_time = 1000 * 60;//每分钟检索一次
        long trigger = SystemClock.elapsedRealtime() + d_time;
        Intent i = new Intent(this, MyService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        boolean s= Handle.isServiceRunning(MyApplication.getContext(),"dog.fire.pangx.fire");
        if (!s){
            Intent intent=new Intent(MyService.this, MyService.class);
            startService(intent);
            System.out.println("启动。。。。");
        }
        super.onDestroy();
    }
}
