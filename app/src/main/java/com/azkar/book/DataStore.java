package com.azkar.book;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class DataStore {
    private DataStore() {}

    public static JSONObject load(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("content.json");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        }
    }

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("azkar_prefs", Context.MODE_PRIVATE);
    }

    public static boolean dark(Context c) {
        return prefs(c).getBoolean("dark", false);
    }

    public static int bg(Context c) { return dark(c) ? Color.rgb(22, 29, 27) : Color.rgb(248, 246, 239); }
    public static int card(Context c) { return dark(c) ? Color.rgb(35, 45, 41) : Color.WHITE; }
    public static int text(Context c) { return dark(c) ? Color.rgb(238, 240, 236) : Color.rgb(39, 48, 44); }
    public static int muted(Context c) { return dark(c) ? Color.rgb(176, 185, 180) : Color.rgb(105, 116, 111); }
    public static int green(Context c) { return dark(c) ? Color.rgb(108, 185, 142) : Color.rgb(42, 112, 75); }
    public static int softGreen(Context c) { return dark(c) ? Color.rgb(52, 78, 65) : Color.rgb(232, 243, 235); }
    public static int border(Context c) { return dark(c) ? Color.rgb(73, 92, 83) : Color.rgb(222, 226, 221); }

    public static GradientDrawable rounded(int color, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        if (strokeWidth > 0) g.setStroke(strokeWidth, strokeColor);
        return g;
    }

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static String arabicNumber(int n) {
        String s = String.valueOf(n);
        char[] map = {'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'};
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append(Character.isDigit(ch) ? map[ch-'0'] : ch);
        return b.toString();
    }

    public static void rtl(View v) {
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        v.setTextDirection(View.TEXT_DIRECTION_RTL);
    }
}
