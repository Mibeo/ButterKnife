package com.zbj.butterknife;

import android.app.Activity;

/**
 * Created by bingjia.zheng on 2019/8/7.
 */

public class ButterKnife {
    public static void bind(Activity activity) {
        String name = activity.getClass().getName() + "_ViewBinding";
        try {
            Class<?> aClass = Class.forName(name);
            IBinder iBinder = (IBinder) aClass.newInstance();
            iBinder.bind(activity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
