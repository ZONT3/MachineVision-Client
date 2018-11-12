package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;

public class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.ViewHolder> {
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
        ArrayList<ArtifactObject> added = new ArrayList<>();
        ArrayList<ArtifactObject> removed = new ArrayList<>();
        for (ArtifactObject obj : tempDataset) {
            if (!mDataset.contains(obj)) {
                mDataset.add(obj);
                notifyItemInserted(mDataset.indexOf(obj));
            }
        }
        for (ArtifactObject obj : mDataset) {
            if (!tempDataset.contains(obj)) {
                int pos = mDataset.indexOf(obj);
                mDataset.remove(obj);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(mDataset.indexOf(obj), pos);
            }
        }
    }

}