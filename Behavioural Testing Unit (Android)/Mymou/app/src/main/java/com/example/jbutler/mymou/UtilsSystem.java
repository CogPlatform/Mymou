package com.example.jbutler.mymou;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.util.StringTokenizer;

public class UtilsSystem {
   // Debug
    public static String TAG = "MymouUtilsSystem";

    public static void setOnClickListenerLoop(Button[] buttons, View.OnClickListener view) {
        for (Button button : buttons) {
            button.setOnClickListener(view);
        }
    }

    public static String convertIntArrayToString(int[] list) {
//        String out = Arrays.toString(list);
        StringBuilder str = new StringBuilder();
        for (int s : list) {
            str.append(s).append(",");
        }
        String out = str.toString();
        return out;
    }

    public static int[] loadIntArray(String tag, SharedPreferences prefs, Context context) {
        Log.d(TAG, tag+"  "+getDefaultArr(tag, context));
        String savedString = prefs.getString(tag, getDefaultArr(tag, context));
        Log.d(TAG, "Loaded "+savedString+"from "+tag);
        StringTokenizer st = new StringTokenizer(savedString, ",");
        int n = st.countTokens();
        int[] savedList = new int[n];
        for (int i = 0; i < n; i++) {
            savedList[i] = Integer.parseInt(st.nextToken());
        }
        return savedList;
    }

    // Get default colour values for ColourPicker (as specified in Strings.xml)
    public static String getDefaultArr(String tag, Context context) {
        if (tag == context.getResources().getString(R.string.preftag_gocuecolors)) {
            return context.getResources().getString(R.string.default_gocue_colours);
        } else if (tag == context.getResources().getString(R.string.preftag_task_objdisc_corr)) {
            return context.getResources().getString(R.string.default_objdis_corr_colours);
        } else if (tag == context.getResources().getString(R.string.preftag_task_objdisc_incorr)) {
            return context.getResources().getString(R.string.default_objdis_incorr_colours);
        }
        new Exception("Invalid tag specified");
        return "";
    }


}



