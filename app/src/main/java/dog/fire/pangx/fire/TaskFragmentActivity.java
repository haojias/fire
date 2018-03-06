package dog.fire.pangx.fire;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.litepal.crud.DataSupport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dog.fire.pangx.fire.db.Task;

public class TaskFragmentActivity extends Fragment {

    /**
     * 任务列表
     */
    private List<Task> taskList;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private SimpleAdapter simpleAdapter;
    private List<String> dataList = new ArrayList<>();

    List<Map<String, Object>> data = new ArrayList<>();



    private SwipeRefreshLayout swipeRefresh;

    private ProgressDialog progressDialog;

    /**
     * 显示进度对话框
     */
    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    public void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_task_fragment, container, false);
        swipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryTaskList();
            }
        });
        listView = (ListView) view.findViewById(R.id.list_view);
        simpleAdapter = new SimpleAdapter(getContext(), data, R.layout.list_text,
                new String[] {"r_id", "r_content"}, new int[] {R.id.r_id, R.id.r_content});
        listView.setAdapter(simpleAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        queryTaskList();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String id=data.get(i).get("r_id").toString();
                deleteDialog(id);
            }
        });
    }

    /**
     * 删除
     */
    private void deleteDialog(final String id){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("取消当前任务？");
        builder.setPositiveButton("是的", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DataSupport.delete(Task.class, Integer.parseInt(id));
                queryTaskList();
                MainActivity activity= (MainActivity) getActivity();
                activity.queryTimer();
            }
        });
        builder.setNegativeButton("不是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        builder.show();
    }

    /**
     * 从数据库查询任务列表
     */
    public void queryTaskList(){
        taskList= DataSupport.select("recording").order("createTime desc").find(Task.class);
        if (taskList.size()>0){
            data.clear();
            for (Task t:taskList){
                Map<String, Object> temp = new LinkedHashMap<>();
                temp.put("r_id", t.getId());
                temp.put("r_content", t.getRecording());
                data.add(temp);
                listView.invalidateViews(); //listView刷新
            }
            listView.setSelection(0);
            swipeRefresh.setRefreshing(false);
        }else {
            data.clear();
            listView.invalidateViews();
            swipeRefresh.setRefreshing(false);
        }
    }

}
