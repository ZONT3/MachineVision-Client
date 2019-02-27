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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import ru.zont.mvc.core.ArtifactObject;

class BitmapHandler {
    static synchronized Bitmap getBitmap(Context context, @NonNull ArtifactObject.ImageItem item,
                                         @Nullable ArrayList<Integer> hide) throws IOException {
        if (!item.link.startsWith("http")) return null;

        File temp = new File(context.getCacheDir(), "BHtemp");
        temp.delete();

        try (BufferedInputStream in = new BufferedInputStream(new URL(item.link).openStream());
             FileOutputStream fos = new FileOutputStream(temp)) {
            byte buff[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buff, 0, 1024)) != -1)
                fos.write(buff, 0, bytesRead);
        }

        Bitmap source = BitmapFactory.decodeFile(temp.getAbsolutePath());
        if (source == null) throw new IOException("Cannot load image");

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
