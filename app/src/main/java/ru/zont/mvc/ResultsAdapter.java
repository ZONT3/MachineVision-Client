package ru.zont.mvc;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.Collections;
import java.util.Random;

import ru.zont.mvc.core.ArtifactObject;
import ru.zont.mvc.core.RandomString;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        private String id;

        private ImageView thumbnail;
        private ImageButton del;
        private ProgressBar pb;
        VH(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.query_item_thumb);
            del = itemView.findViewById(R.id.query_item_del);
            pb = itemView.findViewById(R.id.query_item_pb);
        }
    }

    private ArtifactObject.Query query;
    private OnItemClickListener listener;
    private OnItemLongClickListener longListener;
    private int intentions;
    private int offset;

    ResultsAdapter(ArtifactObject.Query query) {
        this.query = query;
        intentions = 0;
        offset = query.whitelist.size();
    }

    void addImageIntentions(int count) {
        int was = getItemCount();
        intentions += count;
        offset += count;
        notifyItemRangeInserted(was, getItemCount());
    }

    void addImages(String[] urls, int intentionWas) {
        int startIntentPos = query.whitelist.size();
        int oldSize = getItemCount();

        intentions -= intentionWas;
        if (intentions < 0 ) intentions = 0;

        Collections.addAll(query.whitelist, urls);
        notifyItemRangeChanged(startIntentPos, oldSize);
    }

    private void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private void setOnItemLongClickListener(OnItemLongClickListener listener) {
        longListener = listener;
    }

    int getOffset() { return offset; }

    ArtifactObject.Query getQuery() { return query; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_query_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        vh.pb.setVisibility(View.VISIBLE);
        vh.thumbnail.setVisibility(View.INVISIBLE);
        if (i >= query.whitelist.size()) return;

        if (i < 4) vh.thumbnail.setTransitionName("IW" + (i+1));
        else vh.thumbnail.setTransitionName("NULL");

        String url = query.whitelist.get(i);
        Context context = vh.itemView.getContext();

        final String id = new RandomString(8, new Random()).nextString();
        vh.id = id;
        Glide.with(context)
                .load(url)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        ResultsAdapter.this.onLoadFailed(id, vh, url);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        ResultsAdapter.this.onResourceReady(id, vh);
                        return false;
                    }
                })
                .into(vh.thumbnail);
        vh.del.setOnClickListener(v -> {
            query.whitelist.remove(i);
            notifyItemRemoved(i);
            notifyItemRangeChanged(i, query.whitelist.size());
        });

        vh.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onItemClick(url);
        });
        vh.itemView.setOnLongClickListener(v -> {
            if (longListener != null)
                longListener.onItemLongClick(url);
            return true;
        });
    }

    private synchronized void onLoadFailed(String reqID, VH vh, String url) {
        if (!reqID.equals(vh.id)) return;

        int i = query.whitelist.indexOf(url);
        if (i < 0) return;

        query.whitelist.remove(i);
        notifyItemRemoved(i);
        notifyItemRangeChanged(i, query.whitelist.size());
    }

    private synchronized void onResourceReady(String reqID, VH vh) {
        if (!reqID.equals(vh.id)) return;
        vh.pb.setVisibility(View.GONE);
        vh.thumbnail.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return query.whitelist.size() + intentions;
    }

    interface OnItemClickListener {
        void onItemClick(String url);
    }

    interface OnItemLongClickListener {
        void onItemLongClick(String url);
    }


}