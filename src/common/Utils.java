package common;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static int now() {
        return (int) (new Date().getTime() / 1000);
    }

    public static String stampToString(int stamp) {
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dt.format(new Date((long) stamp * 1000));
    }
}
