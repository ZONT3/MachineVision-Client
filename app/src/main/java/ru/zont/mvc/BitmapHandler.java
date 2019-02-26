package ru.zont.mvc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;

import ru.zont.mvc.core.ArtifactObjectNew;

public class BitmapHandler {
    static Bitmap getBitmap(Context context) {
        ArtifactObjectNew.ImageItem item = new ArtifactObjectNew.ImageItem("https://ya-webdesign.com/images/star-platinum-png-4.png");
        item.addLayout(new Integer[]{313, 51, 484, 247});
        item.addLayout(new Integer[]{464, 103, 589, 294});

        return getBitmap(context, item, null);
    }

    static synchronized Bitmap getBitmap(Context context, @NonNull ArtifactObjectNew.ImageItem item, @Nullable ArrayList<Integer> hide) {
        if (!item.link.startsWith("http")) return null;

        File temp = new File(context.getCacheDir(), "BHtemp");
        temp.delete();

        try (BufferedInputStream in = new BufferedInputStream(new URL(item.link).openStream());
             FileOutputStream fos = new FileOutputStream(temp)) {
            byte buff[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buff, 0, 1024)) != -1)
                fos.write(buff, 0, bytesRead);
        } catch (Exception e) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeFile(temp.getAbsolutePath());
        for (int i = 0; i < item.layout.size(); i++) {
            if (hide != null && hide.contains(i)) continue;

            Canvas canvas = new Canvas(bitmap);
            Paint p = new Paint();
            p.setColor(Color.rgb(0 , 200, 0));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(10);
            canvas.drawRect(item.getRect(i), p);
            p.setColor(Color.rgb(0, 220, 0));
            p.setStrokeWidth(8);
            canvas.drawRect(item.getRect(i).left + 1f, item.getRect(i).top + 1f,
                    item.getRect(i).right - 1f, item.getRect(i).bottom - 1f, p);

            Paint p1 = new Paint();
            p1.setColor(Color.BLACK);
            p1.setTextSize(13);
            p1.setTextAlign(Paint.Align.LEFT);
            canvas.drawText((i+1)+"", item.layout.get(i)[0] + 14, item.layout.get(i)[1] + 14, p1);
            p1.setColor(Color.WHITE);
            p1.setTextSize(14);
            canvas.drawText((i+1)+"", item.layout.get(i)[0] + 13, item.layout.get(i)[1] + 13, p1);
        }
        return bitmap;
    }
}
