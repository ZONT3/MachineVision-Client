package ru.zont.mvc.core;

import android.content.Context;

@SuppressWarnings({"WeakerAccess", "unused"})
public class Dimension {
    public int pxX;
    public int pxY;
    public int dpX;
    public int dpY;

    public static Dimension setDp(int dpX, int dpY, Context context) {
        Dimension result = new Dimension();
        result.dpX = dpX;
        result.dpY = dpY;
        result.pxX = (int) (dpX * context.getResources().getDisplayMetrics().density);
        result.pxY = (int) (dpY * context.getResources().getDisplayMetrics().density);
        return result;
    }

    public static Dimension setPx(int pxX, int pxY, Context context) {
        Dimension result = new Dimension();
        result.pxX = pxX;
        result.pxY = pxY;
        result.dpX = (int) (pxX / context.getResources().getDisplayMetrics().density);
        result.dpY = (int) (pxY / context.getResources().getDisplayMetrics().density);
        return result;
    }

    public static int toDp(int px, Context context) {
        return setPx(px, 0, context).dpX;
    }

    public static int toPx(int dp, Context context) {
        return setDp(dp, 0, context).pxX;
    }

    public static int getDisplayWidthPx(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getDisplayWidthDp(Context context) {
        return toDp(getDisplayWidthPx(context), context);
    }

    public static int getDisplayHeightPx(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getDisplayHeightDp(Context context) {
        return toDp(getDisplayHeightPx(context), context);
    }
}
