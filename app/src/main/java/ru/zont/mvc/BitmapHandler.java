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
    @SuppressWarnings("SameParameterValue")
    static synchronized Bitmap getBitmap(Context context, @NonNull ArtifactObject.ImageItem item,
                                         @Nullable ArrayList<Integer> hide) throws IOException {
        if (!item.link.startsWith("http")) return null;

        return getBitmap(context, item.link, ObjectData.newList(item), hide);
    }

    static Bitmap getBitmap(Context context, String link, ObjectDataList list,
                                    @Nullable ArrayList<Integer> hide) throws IOException {
        Bitmap source;
        try {
            source = Glide.with(context).asBitmap().load(link).submit().get();
        } catch (Throwable e){
            throw new IOException("Cannot load image", e);
        }
        
        Bitmap bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        for (int i = 0; i < list.size(); i++) {
            if (hide != null && hide.contains(i)) continue;
            ObjectData objectData = list.get(i);
            if (objectData == null) continue;

            Canvas canvas = new Canvas(bitmap);
            Paint p = new Paint();
            p.setColor(Color.rgb(0 , 220, 0));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3);
            canvas.drawRect(objectData.box_coord[0], objectData.box_coord[1],
                    objectData.box_coord[2], objectData.box_coord[3], p);
            
            String text = objectData.class_name + (objectData.dec_scores < 0 ? "" 
                    : " ["+objectData.dec_scores+"%]");
            
            Paint p1 = new Paint();
            p1.setColor(Color.BLACK);
            p1.setTextSize(20);
            p1.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(text, objectData.box_coord[0] + 10, objectData.box_coord[1] + 26, p1);
            p1.setColor(Color.WHITE);
            p1.setTextSize(22);
            canvas.drawText(text, objectData.box_coord[0] + 8, objectData.box_coord[1] + 24, p1);
        }
        return bitmap;
    }

    public static class ObjectData {
        float[] box_coord;
        String class_name;
        int dec_scores;
        
        @SuppressWarnings("unused")
        public ObjectData() {}
        
        private ObjectData(Integer[] coord, int i) {
            if (coord.length != 4) return;
            
            box_coord = new float[]{coord[0], coord[1], coord[2], coord[3]};
            class_name = i+"";
            dec_scores = -1;
        }
        
        static ObjectDataList newList(ArtifactObject.ImageItem item) {
            ObjectDataList res = new ObjectDataList();
            for (Integer[] i : item.layout) res.add(new ObjectData(i, item.layout.indexOf(i)));
            return res;
        }
    }
    
    public static class ObjectDataList extends ArrayList<ObjectData> {}
}
