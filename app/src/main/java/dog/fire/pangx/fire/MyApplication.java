package dog.fire.pangx.fire;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePalApplication;

/**
 * Created by Pangx on 2018/1/21.
 */

public class MyApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        context = getApplicationContext();
        LitePalApplication.initialize(context);
        super.onCreate();
    }

    public static Context getContext(){
        return context;
    }
}
