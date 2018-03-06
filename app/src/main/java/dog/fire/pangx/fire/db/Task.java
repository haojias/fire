package dog.fire.pangx.fire.db;

import org.litepal.crud.DataSupport;

import java.util.Date;

/**
 * Created by Pangx on 2018/1/18.
 */

public class Task extends DataSupport{

    private int id;
    //录音内容
    private String recording;
    //提醒时间
    private String remindTime;
    //是否完成
    private String flag;

    private Date createTime;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRecording() {
        return recording;
    }

    public void setRecording(String recording) {
        this.recording = recording;
    }


    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRemindTime() {
        return remindTime;
    }

    public void setRemindTime(String remindTime) {
        this.remindTime = remindTime;
    }
}
