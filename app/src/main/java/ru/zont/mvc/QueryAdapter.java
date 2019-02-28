package ru.zont.mvc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.CircularProgressDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import ru.zont.mvc.core.ArtifactObject;

class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.VH> {
    class VH extends RecyclerView.ViewHolder {
        private ImageView[] thumbs;

        private TextView title;
        private ProgressBar pb;
        private TextView nanimo;

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
        int pos = dataset.indexOf(q);
        if (pos < 0) return;

        dataset.remove(pos);
        notifyItemRemoved(pos);
        notifyItemRangeChanged(pos, dataset.size());
    }

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
                vh.pb.setVisibility(View.GONE);
                assert item.get() != null;
                for (int j=0; j < item.get().whitelist.size() && j < vh.thumbs.length; j++) {
                    CircularProgressDrawable pd = new CircularProgressDrawable(context);
                    pd.setStrokeWidth(5);
                    pd.setCenterRadius(30);
                    pd.start();

                    Glide.with(context)
                            .load(item.get().whitelist.get(j).link)
                            .apply(new RequestOptions().placeholder(pd))
                            .into(vh.thumbs[j]);
                }
                break;
        }

        vh.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onItemClick(item, vh);
        });
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
                if (item.query != null && q.title.equals(item.query.title))
                    return indexOf(item);
            return -1;
        }
    }

    interface OnItemClickListener {
        void onItemClick(DataItem item, VH vh);
    }
}
