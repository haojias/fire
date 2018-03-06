package dog.fire.pangx.fire.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Pangx on 2018/1/18.
 */

public class AnalysisRecord {

    /**
     * 录音参数 1分钟 1小时 一天  1分钟后 一小时后 一天后  15分钟后 10分钟后
     * 半小时后 明天  后天  10号 10号下午  十号下午3点  下午三点半  明天上午三点
     * 下个小时 （默认开始的第一分钟） 下个小时20分钟（）
     * 下个月（默认1号） 下个月一号 下个月20号（最大范围30天）
     */

    // 时间格式化
    private static SimpleDateFormat sdf = new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
    public static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static SimpleDateFormat sdf3 = new SimpleDateFormat("ddHHmm");

    private static final String LANG_TIME = "1";// 月
    private static String regex = "[\u4e00-\u9fa5]";
    private static final String FLAG = "0";
    private static final String PROMPT_TIME_E = "时间段参数错误";

    private static final String FLAG_1 = "1";
    private static final String PROMPT_RECORD_TIME_NULL = "未检测到时间参数";

    private static final String FLAG_2 = "2";
    private static final String PROMPT_TIME_TO_LANG = "超出时间规定最大范围";

    /**
     * 匹配录音里的参数
     *
     * @param text
     * @return
     */
    public static String PredefinedTime(String text) throws ParseException {
        //判断时间大小，最小单位分钟
        boolean a = text.contains("秒");
        boolean bf = text.contains("半分钟");
        if (a || bf) {
            System.out.println("最小时间单位：分钟！");
            return "0";
        }
        //针对转换判断是否存在中文数字，主要判断1，2
        boolean y = text.contains("一");
        boolean l = text.contains("两");
        boolean ban = text.contains("半");
        if (y)
            text = text.replace("一", "1");
        if (l)
            text = text.replace("两", "2");
        if (ban)
            text = text.replaceAll("半", "30");

        boolean ck = ck(text);
        if (!ck) {
            return "0";
        }

        boolean today = text.contains("今天");
        boolean tommorow = text.contains("明天");
        boolean after_t = text.contains("后天");
        boolean after_minute = text.contains("分钟后");
        boolean after_hour = text.contains("小时后");
        boolean point = text.contains("点");

        String time_str = null;
        //处理语音
        if (today || tommorow || after_t || after_minute || after_hour) {
            if (today && point) {//今天
                time_str = returnTime1(text, 0);
                System.out.println(time_str);
            } else if (tommorow && point) {//明天
                time_str = returnTime1(text, 1);
                System.out.println(time_str);
            } else if (after_t && point) {//后天
                time_str = returnTime1(text, 2);
                System.out.println(time_str);
            } else if (after_minute || after_hour) {//分钟.小时后
                time_str = returnTime2(text);
                System.out.println(time_str);
            }
        } else {
            return "0";
        }
        return time_str;
    }

    /**
     * 今天明天后天时间处理
     */
    public static String returnTime1(String str, int i) throws ParseException {
        Date d = null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        str = str.replace("点", ":");
        str = removeToEN(str);
        String[] s = str.split(":");
        System.out.println("length:-----" + s.length + "====" + s[0]);

        if (s.length == 1) {
            if (Integer.parseInt(s[0]) < 10) {
                str = "0" + str + "00";
            } else {
                str = str + "00";
            }
        }
        if (i == 0) {
            d = calendar.getTime();
            String sdf = sdf1.format(d);
            str = sdf + " " + str;
        } else if (i == 1) {
            calendar.add(Calendar.DATE, 1);
            d = calendar.getTime();
            String sdf = sdf1.format(d);
            str = sdf + " " + str;
        } else if (i == 2) {
            calendar.add(Calendar.DATE, 2);
            d = calendar.getTime();
            String sdf = sdf1.format(d);
            str = sdf + " " + str;
        }
        //判断时间是否大于当前时间，如果不大于返回0
        Date d1 = sdf2.parse(str);
        int var1= (int) d1.getTime();
        int var2 = (int) sdf2.parse(sdf2.format(new Date())).getTime();
        int j = var1 - var2;
        if (j < 0) {
            str = "0";
        }
        return str;
    }

    /**
     * 分钟后，小时后处理
     */
    public static String returnTime2(String str) {
        String resutl = null;
        Date date = new Date();
        // 判断分钟和小时  判断 是否制定多长时间之后
        boolean minute = str.contains("分钟");
        boolean hour = str.contains("小时");
        if (minute && !hour) {//分钟
            str = removeToEN(str);
            if (Integer.parseInt(str) > 59 && Integer.parseInt(str) < 1) {
                resutl = "0";//超出规定值
            }
            int n = 1000 * 60 * Integer.parseInt(str);
            Date afterDate = new Date(date.getTime() + n);
            resutl = sdf.format(afterDate);
        } else if (hour && !minute) {//小时
            str = removeToEN(str);
            if (Integer.parseInt(str) > 23 && Integer.parseInt(str) < 1) {
                resutl = "0";//超出规定值
            }
            int n = 1000 * 60 * 60 * Integer.parseInt(str);
            Date afterDate = new Date(date.getTime() + n);
            resutl = sdf.format(afterDate);
        } else if (minute && hour) {//小时分钟
            str = str.replace("小时", ":").replace("分钟", ":00");
            str = removeToEN(str);
            System.out.println(str);
            String[] s = str.split(":");
            int n = 1000 * 60 * 60 * Integer.parseInt(s[0]);
            int n1 = 1000 * 60 * Integer.parseInt(s[1]);
            int ss = n + n1;
            Date afterDate = new Date(date.getTime() + ss);
            resutl = sdf.format(afterDate);
        }
        return resutl;
    }

    /**
     * 判断时间单位
     */
    public static boolean ck(String str) {
        StringBuilder builder = new StringBuilder();
        //判断时间的数字
        List<String> list = new ArrayList<>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
                "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
                "31", "32", "33", "34", "35", "36", "37", "38", "39", "40",
                "41", "42", "43", "44", "45", "46", "47", "48", "49", "50",
                "51", "52", "53", "54", "55", "56", "57", "58", "59"));

        for (int i = 0; i < list.size(); i++) {
            boolean n = str.contains(list.get(i));
            if (n) {
                builder.append(list.get(i) + "、");
            }
        }
        //如果没有数字存在，则判断失败
        if (builder.length() < 1) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 去除中文及字母
     *
     * @param en
     * @return
     */
    public static String removeToEN(String en) {
        //去除中文
        String regex = "[\u4e00-\u9fa5]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(en);
        en = matcher.replaceAll("");
        //去除字母
        en = en.replaceAll("[a-zA-Z]", "");
        en = en.replace(",", "").replace("，","").replace("。", "")
                .replace("?", "").replace("!", "").replace("？","")
                .replace("！", "");
        return en;
    }
}
