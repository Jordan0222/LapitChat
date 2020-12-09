package com.example.lapitchat;

import android.app.Application;
import android.content.Context;

public class GetTimeAgo extends Application {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getTimeAgo(long time, Context ctx) {
        long x = 1000000000000L;

        if (time < x) {
            time *= 1000;
        }
        long now = System.currentTimeMillis();

        if (time>now || time < 0) {
            return null;
        }

        final long diff = now - time;
        if (diff < SECOND_MILLIS) {
            return "just nom";
        } else if (diff < MINUTE_MILLIS * 2){
            return "a minute ago";
        } else if (diff < MINUTE_MILLIS * 30) {
            return "minutes ago";
        } else if (diff < HOUR_MILLIS * 2) {
            return "a hour ago";
        } else if (diff < HOUR_MILLIS * 24) {
            return diff / HOUR_MILLIS + "hours ago";
        } else if (diff < DAY_MILLIS * 2) {
            return "yesterday";
        } else  {
            return "days ago";
        }
    }

}
