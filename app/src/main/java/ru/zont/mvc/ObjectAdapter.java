package ru.zont.mvc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

import ru.zont.mvc.core.ArtifactObject;

class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.VH> {
    class VH extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView meta;
        private TextView status;
        private TextView act;
        private ImageView thumb;

        private VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.main_item_title);
            meta = itemView.findViewById(R.id.main_item_meta);
            status = itemView.findViewById(R.id.main_item_status);
            act = itemView.findViewById(R.id.main_item_lastact);
            thumb = itemView.findViewById(R.id.main_item_thumb);
        }
    }

    private ArrayList<ArtifactObject> dataset;
    private OnItemClickListener listener;

    ObjectAdapter() {
        this(new ArrayList<>());
    }

    ObjectAdapter(ArrayList<ArtifactObject> dataset) {
        this.dataset = dataset;
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_object, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        ArtifactObject object = dataset.get(i);
        Context context = vh.itemView.getContext();

        vh.title.setText(object.getTitle());
        vh.meta.setText(context.getString(R.string.main_meta, object.getTotal()));
        vh.status.setText(MainActivity.getStatusString(object, context));

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(object.getLastAct());
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        if (calendar.get(Calendar.DAY_OF_YEAR) * calendar.get(Calendar.YEAR) ==
                Calendar.getInstance().get(Calendar.DAY_OF_YEAR) * Calendar.getInstance().get(Calendar.YEAR))
            format = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        vh.act.setText(String.format(MainActivity.getActionString(object, context),
                format.format(calendar.getTime())));

        Glide.with(context)
                .load(object.getThumbnail())
                .into(vh.thumb);

        vh.itemView.setOnClickListener(v -> {
            if (listener == null) return;
            listener.onItemClick(object);
        });
    }

    void updateDataset(ArtifactObject[] newDataset) {
        ArrayList<ArtifactObject> list = new ArrayList<>();
        Collections.addAll(list, newDataset);
        updateDataset(list);
    }

    void updateDataset(ArrayList<ArtifactObject> newDataset) {
        for (ArtifactObject object : newDataset) {
            if (!dataset.contains(object)) {
                dataset.add(object);
                notifyItemInserted(dataset.indexOf(object));
                notifyItemRangeChanged(dataset.indexOf(object), dataset.size());
            }
        }
        for (ArtifactObject object : dataset) {
            if (!dataset.contains(object)) {
                int pos = dataset.indexOf(object);
                dataset.remove(object);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, dataset.size());
            }
        }
        for (ArtifactObject obj : dataset) {
            if (!obj.dataEquals(newDataset.get(newDataset.indexOf(obj)))) {
                dataset.set(dataset.indexOf(obj), newDataset.get(newDataset.indexOf(obj)));
                notifyItemChanged(dataset.indexOf(obj));
            }
        }
    }

    void clear() {
        if (dataset.size() == 0) return;
        int size = dataset.size();
        dataset = new ArrayList<>();
        notifyItemRangeRemoved(0, size);
        notifyItemRangeChanged(0, size);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    interface OnItemClickListener {
        void onItemClick(ArtifactObject item);
    }
}
