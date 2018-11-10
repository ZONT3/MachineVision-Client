package ru.zont.mvc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.ViewHolder> {
    private String[] mDataset;

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

    ObjectAdapter(String[] myDataset) {
        mDataset = myDataset;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_object, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.mTitle.setText(mDataset[position]);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}