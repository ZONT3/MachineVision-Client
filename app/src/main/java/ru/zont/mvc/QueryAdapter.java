package ru.zont.mvc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.Random;

import ru.zont.mvc.core.ArtifactObject;
import ru.zont.mvc.core.RandomString;

class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.VH> {
    class VH extends RecyclerView.ViewHolder {
        private ImageView[] thumbs;
        private boolean[] states;

        private TextView title;
        private ProgressBar pb;
        private TextView nanimo;

        private String id;

        private VH(@NonNull View itemView) {
            super(itemView);
            thumbs = new ImageView[] {
                    itemView.findViewById(R.id.edit_query_iw1),
                    itemView.findViewById(R.id.edit_query_iw2),
                    itemView.findViewById(R.id.edit_query_iw3),
                    itemView.findViewById(R.id.edit_query_iw4)
            };
            title = itemView.findViewById(R.id.edit_query_title);
            pb = itemView.findViewById(R.id.edit_query_pb);
            nanimo = itemView.findViewById(R.id.edit_query_nanimo);
            resetStates();
        }

        private void resetStates() {
            states = new boolean[4];
            for (int i = 0; i < states.length; i++)
                states[i] = false;
        }

        ImageView getIW(int i) {
            if (i>=thumbs.length) return null;
            return thumbs[i];
        }
    }

    private DataSet dataset;
    private OnItemClickListener listener;

    QueryAdapter() { this(new ArrayList<>()); }

    QueryAdapter(ArrayList<ArtifactObject.Query> dataset) {
        this.dataset = new DataSet(dataset);
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    void addQuery(ArtifactObject.Query q) {
        for (DataItem item : dataset) {
            if (item.type == DataItem.TYPE_REGULAR && item.query.title.equals(q.title))
                return;
            else if (item.type == DataItem.TYPE_LOADING && item.query.title.equals(q.title)) {
                int index = dataset.indexOf(item);
                dataset.set(index, new DataItem(q));
                notifyItemChanged(index);
                return;
            }
        }

        dataset.add(new DataItem(q));
        notifyItemInserted(dataset.size() - 1);
    }

    void addLoadingQuery(String title) {
        dataset.add(new DataItem(title));
        notifyItemInserted(dataset.size() - 1);
    }

    void updateQuery(ArtifactObject.Query newQuery) {
        int i = -1;
        for (QueryAdapter.DataItem item : dataset)
            if (item.get() != null && item.get().title.equals(newQuery.title))
                i = dataset.indexOf(item);
        if (i < 0) return;

        dataset.set(i, new DataItem(newQuery));
        notifyItemChanged(i);
    }

    void delete(ArtifactObject.Query q) {
        int pos = -1;
        for (DataItem item : dataset)
            if (item.query != null && item.query.title.equals(q.title))
                pos = dataset.indexOf(item);
        if (pos < 0) return;

        dataset.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, dataset.size());
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_query_thumb, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        DataItem item = dataset.get(i);
        Context context = vh.itemView.getContext();

        for (ImageView iw : vh.thumbs)
            iw.setVisibility(View.INVISIBLE);
        vh.nanimo.setVisibility(View.GONE);
        vh.pb.setVisibility(View.GONE);

        switch (item.type) {
            case DataItem.TYPE_CUSTOM:
                vh.title.setText(R.string.edit_custq);
                vh.nanimo.setVisibility(View.VISIBLE);
                break;
            default:
            case DataItem.TYPE_LOADING:
                vh.pb.setVisibility(View.VISIBLE);
                vh.title.setText(item.get().title);
                break;
            case DataItem.TYPE_REGULAR:
                vh.title.setText(item.get().title);

                vh.resetStates();
                vh.pb.setVisibility(View.VISIBLE);
                vh.id = new RandomString(8, new Random()).nextString();
                assert item.get() != null;
                for (int j=0; j < item.get().whitelist.size() && j < vh.thumbs.length; j++) {
                    final int finalJ = j;
                    final String id = vh.id;
                    Glide.with(context)
                            .load(item.get().whitelist.get(j).link)
                            .addListener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    setReady(vh, finalJ, id);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    setReady(vh, finalJ, id);
                                    return false;
                                }
                            })
                            .into(vh.thumbs[j]);
                }
                break;
        }

        vh.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onItemClick(item, vh);
        });
    }

    private synchronized void setReady(VH holder, int i, String id) {
        if (!id.equals(holder.id)) return;
        boolean b = true;
        for (boolean b1 : holder.states)
            if (!(b = b1)) break;
        if (b) return;

        holder.states[i] = true;

        b = true;
        for (boolean b1 : holder.states)
            if (!(b = b1)) break;
        if (b) {
            holder.pb.startAnimation(AnimationUtils
                    .loadAnimation(holder.itemView.getContext(), R.anim.fadeout));
            holder.pb.postOnAnimation(() -> holder.pb.setVisibility(View.GONE));

            for (ImageView iw : holder.thumbs) {
                iw.startAnimation(AnimationUtils
                        .loadAnimation(holder.itemView.getContext(), R.anim.fadein));
                iw.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    ArrayList<ArtifactObject.Query> getQueries() {
        ArrayList<ArtifactObject.Query> list = new ArrayList<>();
        for (DataItem item : dataset)
            if (item.type == DataItem.TYPE_REGULAR)
                list.add(item.get());
        return list;
    }

    static class DataItem {
        static final int TYPE_CUSTOM = -1;
        static final int TYPE_LOADING = 0;
        static final int TYPE_REGULAR = 1;

        private int type;
        private ArtifactObject.Query query;

        private DataItem() {
            type = TYPE_CUSTOM;
        }

        private DataItem(String title) {
            query = new ArtifactObject.Query(title);
            type = TYPE_LOADING;
        }

        private DataItem(@NonNull ArtifactObject.Query q) {
            query = q;
            type = TYPE_REGULAR;
        }

        ArtifactObject.Query get() {
            return type != TYPE_CUSTOM ? query : null;
        }

        int getType() { return type; }
    }

    private static class DataSet extends ArrayList<DataItem> {
        private DataSet(ArrayList<ArtifactObject.Query> list) {
            super();
            add(new DataItem());
            for (ArtifactObject.Query q : list)
                add(new DataItem(q));
        }

        private int indexOf(ArtifactObject.Query q) {
            if (q == null) return -1;
            for (DataItem item : this)
                if (q.equals(item.query))
                    return indexOf(item);
            return -1;
        }
    }

    interface OnItemClickListener {
        void onItemClick(DataItem item, VH vh);
    }
}
