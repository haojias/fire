package dog.fire.pangx.fire.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import dog.fire.pangx.fire.db.Record;
import dog.fire.pangx.fire.db.Task;

/**
 * Created by Pangx on 2018/1/18.
 */

public class Handle {


    /**
     * 将返回的JSON数据解析成Record实体类
     */
    public static Record handleRecord(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            return new Gson().fromJson(jsonObject.toString(), Record.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存任务数据
     */
    public static boolean TaskSave(String var1,String time){
        try {
            Task task=new Task();
            task.setFlag("0");
            task.setRecording(var1);
            task.setRemindTime(time);
            task.setCreateTime(new Date());
            task.save();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isServiceRunning(Context context, String ServiceName) {
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(100);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(ServiceName)) {
                return true;
            }
        }
        return false;
    }


}
