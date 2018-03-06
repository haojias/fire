package dog.fire.pangx.fire;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.idst.nls.NlsClient;
import com.alibaba.idst.nls.NlsListener;
import com.alibaba.idst.nls.StageListener;
import com.alibaba.idst.nls.internal.protocol.NlsRequest;
import com.alibaba.idst.nls.internal.protocol.NlsRequestProto;

import org.litepal.crud.DataSupport;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import dog.fire.pangx.fire.db.Record;
import dog.fire.pangx.fire.db.Task;
import dog.fire.pangx.fire.service.MyService;
import dog.fire.pangx.fire.utils.AnalysisRecord;
import dog.fire.pangx.fire.utils.DateUtils;
import dog.fire.pangx.fire.utils.Handle;

import static org.litepal.LitePalApplication.getContext;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String ID = "LTAICYGOqA9Ju29H";
    private static final String SECRET = "RUkCbnAnCLGa0fFuRVkHAdZvQTTsxh";

    private BroadcastMain receiver;

    private Context context;
    private Button startButton;
    private Button stopButton;
    private ProgressDialog dialog;
    private ProgressBar progressBar;

//    private static int i;//每次执行的大小
//    private static int start_n;//初始化大小

    private TextView time;
    private TextView content;
    private TextView status;
    private LinearLayout have;
    private LinearLayout no;

    private List<Task> taskList;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();


    private LayoutInflater inflater;
    private ViewGroup container;
    /**
     * 阿里云是别语音jar
     */
    private NlsClient mNlsClient;
    private NlsRequest mNlsRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        startButton = (Button) findViewById(R.id.start_r);
        stopButton = (Button) findViewById(R.id.stop_r);
        time = (TextView) findViewById(R.id.m_time);
        content = (TextView) findViewById(R.id.m_content);
        status = (TextView) findViewById(R.id.m_status);
        have = (LinearLayout) findViewById(R.id.m_have);
        no = (LinearLayout) findViewById(R.id.m_no);
        have.setVisibility(View.GONE);
        no.setVisibility(View.GONE);
        progressBar=(ProgressBar)findViewById(R.id.p_bar);

        //阿里云语音场景 app_key
        String app_key = "nls-service";
        //语音格式
        String opu = "opu";
        mNlsRequest = initNlsRequest();
        mNlsRequest.setApp_key(app_key);
        mNlsRequest.setAsr_sc(opu);

        NlsClient.openLog(true);
        //全局实例化
        NlsClient.configure(getApplicationContext());
        //实例化 NlsClient
        mNlsClient = NlsClient.newInstance(MainActivity.this, mRecognizeListener, mStageListener, mNlsRequest);
        mNlsClient.setMaxRecordTime(600000);// 最长1分钟
        mNlsClient.setMaxStallTime(1000);//最短语音 1秒
        mNlsClient.setMinRecordTime(500);//最大录音中断时间
        mNlsClient.setRecordAutoStop(false);//自动停止录音
        mNlsClient.setMinVoiceValueInterval(200);//音量回掉时长

        boolean s = Handle.isServiceRunning(context, "dog.fire.pangx.fire");
        if (!s) {
            Intent intent = new Intent(MainActivity.this, MyService.class);
            startService(intent);
            System.out.println("启动。。。。");
        }
        //去除已超时的任务
        ckTimer();
        //查询最近待处理的内容
        queryTimer();
        //开始录音
        initStartRecognizing();
        //结束录音
        initStopRecognizing();
    }

    boolean start=false;

    /**
     * 开始录音
     */
    public void initStartRecognizing() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    mNlsRequest.authorize(ID, SECRET);
                    mNlsClient.start();
                    startButton.setText("录音中...");
                    start=true;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mNlsRequest.authorize(ID, SECRET);
                    mNlsClient.start();
                    startButton.setText("录音中...");
                    start=true;
                }
                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    /**
     * 结束录音
     */
    public void initStopRecognizing() {
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (start)
                    showPD();
                mNlsClient.stop();
                startButton.setText("开始");
            }
        });
    }

    private NlsRequest initNlsRequest() {
        NlsRequestProto proto = new NlsRequestProto(context);
        //proto.setApp_user_id(""); //设置在应用中的用户名，可选
        return new NlsRequest(proto);
    }

    private NlsListener mRecognizeListener = new NlsListener() {
        @Override
        public void onRecognizingResult(int status, RecognizedResult result) {
            switch (status) {
                case NlsClient.ErrorCode.SUCCESS:
                    //result实例化bean
                    Record record = Handle.handleRecord(result.asr_out);
                    if (record.getResult().equals("") || record.getResult().equals(":") || record.getResult() == null) {
                        dialog.cancel();
                        Toast.makeText(MainActivity.this, "未能正常解析录音，请重试！", Toast.LENGTH_LONG).show();
                        return;
                    }
                    //解析录音里时间参数，返回具体的时间。未匹配到的参数提示解析失败
                    String date = null;
                    try {
                        String str = AnalysisRecord.PredefinedTime(record.getResult().toString());
                        if (str.equals("0") || str == null) {
                            dialog.cancel();
                            Toast.makeText(MainActivity.this, "提取语音时间失败，请点击右上角菜单，关于！", Toast.LENGTH_LONG).show();
                            return;
                        }
                        date = str;
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    boolean r = Handle.TaskSave(record.getResult().toString(), date);
                    if (r) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TaskFragmentActivity activity = (TaskFragmentActivity) getSupportFragmentManager().findFragmentById(R.id.list_view_task);
                                activity.queryTaskList();
                                dialog.cancel();
                                queryTimer();
                            }
                        });
                    }else {
                        dialog.cancel();
                    }
                    break;
                case NlsClient.ErrorCode.RECOGNIZE_ERROR:
                    dialog.cancel();
                    Toast.makeText(MainActivity.this, "识别失败！", Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.RECORDING_ERROR:
                    dialog.cancel();
                    Toast.makeText(MainActivity.this, "录音失败!", Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.NOTHING:
                    dialog.cancel();
                    Toast.makeText(MainActivity.this, "语音服务异常，请重试！", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    private StageListener mStageListener = new StageListener() {
        @Override
        public void onStartRecognizing(NlsClient recognizer) {
            super.onStartRecognizing(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStopRecognizing(NlsClient recognizer) {
            super.onStopRecognizing(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStartRecording(NlsClient recognizer) {
            super.onStartRecording(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStopRecording(NlsClient recognizer) {
            super.onStopRecording(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onVoiceVolume(int volume) {
            super.onVoiceVolume(volume);
        }

    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 提示框
     */
    private void showPD() {
        dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//转盘
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage("正在处理，请稍后……");
        dialog.show();
    }

    /**
     * 初始页面数据
     */
    public void queryTimer() {
        final List<Task> task = DataSupport.select("recording", "remindTime", "flag","createTime").order("remindTime asc").limit(1).find(Task.class);
        if (task.size() == 0) {
            no.setVisibility(View.VISIBLE);
            if (have.getVisibility()==View.VISIBLE)
                have.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            if (progressBar.getVisibility()==View.VISIBLE)
                progressBar.setVisibility(View.GONE);
        } else if (task.size() > 0) {
            if (no.getVisibility() == View.VISIBLE)
                no.setVisibility(View.GONE);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Task task1 = task.get(0);
                    if (task1.getRecording().length() > 15)
                        content.setText(task1.getRecording().substring(0, 15) + "...");
                    else
                        content.setText(task1.getRecording());
                    if (task1.getFlag().equals("0"))
                        status.setText("状态：待处理");
                    try {
                        Date date = AnalysisRecord.sdf2.parse(task1.getRemindTime());
                        String s = AnalysisRecord.sdf2.format(date);
                        time.setText(s);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    have.setVisibility(View.VISIBLE);
                    //加载progressbar
//                    try {
//                        //求创建时间和目标时间毫秒数
//                        int c_time=(int)AnalysisRecord.sdf2.parse(AnalysisRecord.sdf2.format(task1.getCreateTime())).getTime();
//                        int r_time=(int)AnalysisRecord.sdf2.parse(task1.getRemindTime()).getTime();
//                        calculation(c_time,r_time);
//                    } catch (ParseException e) {
//                        e.printStackTrace();
//                    }
//                    //每两秒刷新一次那个progressBar
//                    Timer timer = new Timer(true);
//                    timer.schedule(new MyTask(), 0, 2000);
                }
            });
        }
    }

    /**
     * 加载 ProgressBar
     */
//    private  class  MyTask extends TimerTask{
//        @Override
//        public void run() {
//            int progress=progressBar.getProgress();
//            progress=progress+i;
//            progressBar.setProgress(progress);
//        }
//    }

    /**
     * 接收广播处理提示任务
     */
    class BroadcastMain extends BroadcastReceiver {
        //必须要重载的方法，用来监听是否有广播发送
        @Override
        public void onReceive(Context context, Intent intent) {
            queryTimer();
            TaskFragmentActivity activity = (TaskFragmentActivity) getSupportFragmentManager().findFragmentById(R.id.list_view_task);
            activity.queryTaskList();
        }
    }

    private boolean r=false;

    @Override
    protected void onResume() {
        super.onResume();
        //监听广播
        r=true;
        receiver = new BroadcastMain();
        //注册广播接收程序
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_UPDATEUI");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        if (r){
            r=false;
            unregisterReceiver(receiver);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 因为杀掉进程可能存在错过的任务，需要检索所有任务，如果提示时间已经错过，要删除，确保程序正常使用。
     */
    public void ckTimer(){
        final List<Task> task = DataSupport.select( "id","remindTime").find(Task.class);
        if (task.size()>0){
            for (Task t:task){
                try{
                    int d= (int) AnalysisRecord.sdf2.parse(t.getRemindTime()).getTime();
                    int str= (int) AnalysisRecord.sdf2.parse(AnalysisRecord.sdf2.format(new Date())).getTime();
                    int i=d-str;
                    System.out.println("----------> iiii="+i);
                    if (i<0){
                        DataSupport.delete(Task.class, t.getId());
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 计算时间大小，进度和时间同百分比
     */
//    private void calculation(int c_time,int r_time){
//        try{
//            int ss1=(r_time-c_time)/1000;//总间隔秒数
//            int j_time=(int) AnalysisRecord.sdf2.parse(AnalysisRecord.sdf2.format(new Date())).getTime();
//            int ss2=(r_time-j_time)/1000;//中间间隔秒数
//            start_n=100-ss2/ss1*100;//已消耗进度值百分比
//            int b=ss2/ss1*100;//剩余间隔进度百分比
//            int c=ss2/2;//中间秒数执行完剩余次数
//            i=b/c;
//            progressBar.setProgress(start_n);//初始化progress的进度
//            progressBar.setVisibility(View.VISIBLE);//显示
//        }catch (Exception e){
//
//        }
//    }


}
