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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.ViewHolder> {
    static final int STATUS_ERROR = -99;
    static final int STATUS_ERROR_LEARN = -1;
    static final int STATUS_READY_TL = 0;
    static final int STATUS_DOWNLOADING = 1;
    static final int STATUS_LEARNING = 2;
    static final int STATUS_READY_FU = 3;
    static final int STATUS_OUTDATED = 4;

    private ArrayList<ArtifactObject> mDataset;

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mThumb;
        TextView mTitle;
        TextView mMeta;
        TextView mStatus;
        TextView mLastact;

        ViewHolder(View v) {
            super(v);
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

    ObjectAdapter(ArtifactObject[] myDataset) {
        mDataset = new ArrayList<>();
        Collections.addAll(mDataset, myDataset);
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
        holder.mMeta.setText(String.format("%d Queries%s",
                object.getQueriesSize(),
                object.getTotalBlacklisted() > 0
                        ? ", " + object.getTotalBlacklisted() + " blacklisted"
                        : ""));

        Context context = holder.mThumb.getContext();
        String status = context.getString(R.string.artobj_status_undefined);
        switch (object.getStatus()) {
            case STATUS_READY_TL: status = context.getString(R.string.artobj_status_rtl); break;
            case STATUS_DOWNLOADING: status = context.getString(R.string.artobj_status_dl); break;
            case STATUS_LEARNING: status = context.getString(R.string.artobj_status_lrn); break;
            case STATUS_READY_FU: status = context.getString(R.string.artobj_status_rfu); break;
            case STATUS_OUTDATED: status = context.getString(R.string.artobj_status_odt); break;
            case STATUS_ERROR_LEARN: status = context.getString(R.string.artobj_status_errl); break;
            case STATUS_ERROR: status = context.getString(R.string.artobj_status_err); break;
        }
        holder.mStatus.setText(status);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(object.getModified());
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
        if (calendar.get(Calendar.DAY_OF_YEAR)*calendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)*Calendar.getInstance().get(Calendar.YEAR))
            format = DateFormat.getTimeInstance(DateFormat.SHORT);
        holder.mLastact.setText(context.getString(R.string.artobj_modified, format.format(calendar.getTime())));

        if (object.getThumbnail() != null)
            Glide.with(holder.mThumb)
                    .load(object.getThumbnail())
                    .into(holder.mThumb);
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

}