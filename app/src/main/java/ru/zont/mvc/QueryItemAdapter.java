package ru.zont.mvc;

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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class QueryItemAdapter extends RecyclerView.Adapter<QueryItemAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        ImageButton del;
        ProgressBar pb;
        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.query_item_thumb);
            del = itemView.findViewById(R.id.query_item_del);
            pb = itemView.findViewById(R.id.query_pb);
        }
    }

    private ArtifactObject.Query query;
    private int offset;
    private QueryItemAdapter.OnLongClickListener onLongClickListener;
    private QueryItemAdapter.OnClickListener onClickListener;
    private WeakReference<RecyclerView> rv;
    private int firstLoaded = 0;
    private int firstNeeded;

    QueryItemAdapter(RecyclerView rv, ArtifactObject.Query query,
                     OnClickListener onClickListener, OnLongClickListener onLongClickListener) {
        this.query = query;
        this.rv = new WeakReference<>(rv);
        offset = this.query.whitelist.size()+1;
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
        this.onClickListener.setAdapter(this);
        this.onLongClickListener.setAdapter(this);
        firstNeeded = query.whitelist.size();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_query_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        String url = query.whitelist.get(i);
        vh.del.setOnClickListener(null);
        vh.itemView.setOnClickListener(null);

        vh.pb.setVisibility(View.VISIBLE);
        vh.thumb.setImageDrawable(null);
        if (url != null) {
            vh.del.setOnClickListener(v -> remove(query.whitelist.get(i)));
            vh.itemView.setOnClickListener(onClickListener);
            vh.itemView.setOnLongClickListener(onLongClickListener);
            Glide.with(vh.itemView)
                    .load(url)
                    .apply(new RequestOptions().override(Dimension.toPx(80, vh.itemView.getContext())))
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Toast.makeText(vh.itemView.getContext(), R.string.edit_err_item_load, Toast.LENGTH_LONG).show();
                            vh.pb.setVisibility(View.GONE);
                            remove(url);
                            firstLoaded++;
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            vh.pb.setVisibility(View.GONE);
                            firstLoaded++;
                            return false;
                        }
                    })
                    .into(vh.thumb);
        }
    }

    @Override
    public int getItemCount() {
        return query.whitelist.size();
    }

    private void remove(String url) {
        int pos = query.whitelist.indexOf(url);
        if (pos < 0 ) return;

        query.whitelist.remove(url);
        query.blacklist.add(url);

        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, query.whitelist.size());
    }

    private static ArrayList<Long> queue = new ArrayList<>();
    void loadMore(EditActivity activity, int count) {
        int pos = query.whitelist.size();
        for (int i = 0; i < count; i++)
            query.whitelist.add(null);
        notifyItemRangeInserted(pos, count);

        new Thread(() -> {
            long id = new Random().nextLong();
            queue.add(id);
            while (!(queue.size() <= 0) || queue.get(0).equals(id))
                try { Thread.sleep(300); } catch (InterruptedException ignored) { return; }

            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            while (firstLoaded < firstNeeded) {
                try { Thread.sleep(500); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
            new EditActivity.ImageGetter(activity, new EditActivity.ImageGetter.ImageGetterPostExec() {
                @Override
                void postExec(WeakReference<EditActivity> wr, String[] result) {
                    if (result == null) {
                        offset -= count;
                        for (int i = count-1; i >= 0; i--)
                            query.whitelist.remove(i + pos);
                        Toast.makeText(activity, R.string.edit_svresperr, Toast.LENGTH_LONG).show();
                        notifyItemRangeRemoved(pos, count);
                        return;
                    }

                    ArrayList<Integer> removed = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        if (result.length > i)
                            query.whitelist.set(i + pos, result[i]);
                        else {
                            query.whitelist.remove(i + pos);
                            removed.add(i + pos);
                        }
                    }

                    for (Integer pos : removed) notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, count);
                    if (queue.size() > 0) queue.remove(0);
                }
            }, query.title, count, offset);
            offset += count;
        }).start();
    }

    static abstract class OnClickListener implements View.OnClickListener {
        private WeakReference<QueryItemAdapter> adapter;

        OnClickListener() {
            super();
        }

        private void setAdapter(QueryItemAdapter adapter) {
            this.adapter = new WeakReference<>(adapter);
        }

        @Override
        public void onClick(View v) {
            onItemClick(adapter.get().query.whitelist.get(
                    Objects.requireNonNull(adapter.get().rv.get()
                            .getLayoutManager()).getPosition(v)));
        }

        public abstract void onItemClick(String item);
    }

    abstract static class OnLongClickListener implements View.OnLongClickListener {
        private WeakReference<QueryItemAdapter> adapter;

        OnLongClickListener() {
            super();
        }

        private void setAdapter(QueryItemAdapter adapter) {
            this.adapter = new WeakReference<>(adapter);
        }

        @Override
        public boolean onLongClick(View v) {
            onItemLongClick(adapter.get().query.whitelist.get(
                    Objects.requireNonNull(adapter.get().rv.get()
                            .getLayoutManager()).getPosition(v)));
            return true;
        }

        public abstract void onItemLongClick(String item);
    }
}
