package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

@SuppressWarnings("CanBeFinal")
public class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.ViewHolder> {
    private ArrayList<ArtifactObject> mDataset;
    private OnItemClick onItemClick;

    static class ViewHolder extends RecyclerView.ViewHolder {
        View root;
        ImageView mThumb;
        TextView mTitle;
        TextView mMeta;
        TextView mStatus;
        TextView mLastact;

        ViewHolder(View v) {
            super(v);
            root = v;
            mThumb = v.findViewById(R.id.main_item_thumb);
            mTitle = v.findViewById(R.id.main_item_title);
            mMeta = v.findViewById(R.id.main_item_meta);
            mStatus = v.findViewById(R.id.main_item_status);
            mLastact = v.findViewById(R.id.main_item_lastact);
        }
    }

//    ObjectAdapter(ArrayList<ArtifactObject> myDataset) {
//        mDataset = myDataset;
//    }

    ObjectAdapter(ArtifactObject[] myDataset, OnItemClick onItemClick) {
        mDataset = new ArrayList<>();
        Collections.addAll(mDataset, myDataset);
        this.onItemClick = onItemClick;
        this.onItemClick.wr = new WeakReference<>(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_object, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArtifactObject object = mDataset.get(position);
        holder.mTitle.setText(object.getTitle());
        holder.mMeta.setText(String.format(holder.mThumb.getContext().getString(R.string.main_obj_meta),
                object.getQueriesSize(),
                object.getTotalBlacklisted() > 0
                        ? ", " + holder.mThumb.getContext().getString(R.string.main_obj_bl, object.getTotalBlacklisted())
                        : ""));

        Context context = holder.mThumb.getContext();
        holder.mStatus.setText(MainActivity.getStatusString(object, context));

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(object.getLastAct());
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        if (calendar.get(Calendar.DAY_OF_YEAR)*calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)*Calendar.getInstance().get(Calendar.YEAR))
            format = DateFormat.getTimeInstance(DateFormat.SHORT);
        int act = R.string.blank;
        switch (object.getLastActType()) {
            case ArtifactObject.ACTION.CREATED: act = R.string.artobj_created; break;
            case ArtifactObject.ACTION.EDITED: act = R.string.artobj_edited; break;
            case ArtifactObject.ACTION.LEARNED: act = R.string.artobj_learned; break;
        }
        holder.mLastact.setText(context.getString(act, format.format(calendar.getTime())));

        if (object.getThumbnail() != null)
            Glide.with(holder.mThumb)
                    .load(object.getThumbnail())
                    .into(holder.mThumb);

        holder.root.setTag(object.getId());
        holder.root.setOnClickListener(onItemClick);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    void updateDataset(ArtifactObject[] newDataset) {
        ArrayList<ArtifactObject> tempDataset = new ArrayList<>();
        Collections.addAll(tempDataset, newDataset);
        for (ArtifactObject obj : tempDataset) {
            if (!mDataset.contains(obj)) {
                mDataset.add(obj);
                notifyItemInserted(mDataset.indexOf(obj));
                notifyItemRangeChanged(mDataset.indexOf(obj), mDataset.size());
            }
        }
        for (ArtifactObject obj : mDataset) {
            if (!tempDataset.contains(obj)) {
                int pos = mDataset.indexOf(obj);
                mDataset.remove(obj);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, mDataset.size());
            }
        }
        for (ArtifactObject obj : mDataset) {
            if (!obj.dataEquals(tempDataset.get(tempDataset.indexOf(obj)))) {
                mDataset.set(mDataset.indexOf(obj), tempDataset.get(tempDataset.indexOf(obj)));
                notifyItemChanged(mDataset.indexOf(obj));
            }
        }
    }

    void clear() {
        if (mDataset.size() == 0) return;
        int size = mDataset.size();
        mDataset = new ArrayList<>();
        notifyItemRangeRemoved(0, size);
        notifyItemRangeChanged(0, size);
    }

    static abstract class OnItemClick implements View.OnClickListener {
        private WeakReference<ObjectAdapter> wr;

        @Override
        public void onClick(View v) {
            if (wr == null) return;
            for (ArtifactObject object : wr.get().mDataset) {
                if (object.getId().equals(v.getTag())) {
                    onItemClick(object);
                    return;
                }
            }
        }

        abstract void onItemClick(ArtifactObject object);
    }

}