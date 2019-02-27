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
        if (count >= 0)
            notifyItemRangeInserted(was, getItemCount());
        else {
            notifyItemRangeRemoved(was + count, was);
            notifyItemRangeChanged(was + count, was);
        }
    }

    void addImages(String[] urls, int expectedCount) {
        int startIntentPos = query.whitelist.size();
        int oldSize = getItemCount();

        intentions -= expectedCount;
        if (intentions < 0 ) intentions = 0;

        query.addNewImages(urls);
        notifyItemRangeChanged(startIntentPos, oldSize);
    }

    void modifyItem(ArtifactObject.ImageItem newInstance) {
        for (ArtifactObject.ImageItem item : query.whitelist)
            if (item.link.equals(newInstance.link))
                query.whitelist.set(query.whitelist.indexOf(item), newInstance);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    void setOnItemLongClickListener(OnItemLongClickListener listener) {
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

        ArtifactObject.ImageItem imageItem = query.whitelist.get(i);
        String url = imageItem.link;
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
                listener.onItemClick(imageItem);
        });
        vh.itemView.setOnLongClickListener(v -> {
            if (longListener != null)
                longListener.onItemLongClick(imageItem);
            return true;
        });
    }

    private void onLoadFailed(String reqID, VH vh, String url) {
//        if (!reqID.equals(vh.id)) return;

        int i = -1;
        for (ArtifactObject.ImageItem item : query.whitelist)
            if (item.link.equals(url))
                i = query.whitelist.indexOf(item);
        if (i < 0) return;

        query.whitelist.remove(i);
        notifyItemRemoved(i);
        notifyItemRangeChanged(i, query.whitelist.size());
    }

    private void onResourceReady(String reqID, VH vh) {
        if (!reqID.equals(vh.id)) return;
        vh.pb.setVisibility(View.GONE);
        vh.thumbnail.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return query.whitelist.size() + intentions;
    }

    interface OnItemClickListener {
        void onItemClick(ArtifactObject.ImageItem item);
    }

    interface OnItemLongClickListener {
        void onItemLongClick(ArtifactObject.ImageItem item);
    }


}
