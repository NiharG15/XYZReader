package com.example.xyzreader.utils;

import android.os.Build;

/**
 * Created by niharg on 5/28/16 at 11:39 AM.
 */
public class Utils {

    public static boolean isLollipopOrUp() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

}
