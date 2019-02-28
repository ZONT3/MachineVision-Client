package ru.zont.mvc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;

import ru.zont.mvc.core.ArtifactObject;

class BitmapHandler {
    static synchronized Bitmap getBitmap(Context context, @NonNull ArtifactObject.ImageItem item,
                                         @Nullable ArrayList<Integer> hide) throws IOException {
        if (!item.link.startsWith("http")) return null;

        Bitmap source;
        try {
            source = Glide.with(context).asBitmap().load(item.link).submit().get();
        } catch (Throwable e){
            throw new IOException("Cannot load image", e);
        }

        Bitmap bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < item.layout.size(); i++) {
            if (hide != null && hide.contains(i)) continue;

            Canvas canvas = new Canvas(bitmap);
            Paint p = new Paint();
            p.setColor(Color.rgb(0 , 220, 0));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            canvas.drawRect(item.getRect(i), p);

            Paint p1 = new Paint();
            p1.setColor(Color.BLACK);
            p1.setTextSize(20);
            p1.setTextAlign(Paint.Align.LEFT);
            canvas.drawText((i+1)+"", item.layout.get(i)[0] + 10, item.layout.get(i)[1] + 26, p1);
            p1.setColor(Color.WHITE);
            p1.setTextSize(22);
            canvas.drawText((i+1)+"", item.layout.get(i)[0] + 8, item.layout.get(i)[1] + 24, p1);
        }
        return bitmap;
    }
}
