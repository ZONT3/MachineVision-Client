package ru.zont.mvc;

import android.content.ReceiverCallNotAllowedException;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;

public class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        ImageView iw1;
        ImageView iw2;
        ImageView iw3;
        ImageView iw4;
        ImageView[] iwList;
        TextView title;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.edit_query_title);
            iw1 = itemView.findViewById(R.id.edit_query_iw1);
            iw2 = itemView.findViewById(R.id.edit_query_iw2);
            iw3 = itemView.findViewById(R.id.edit_query_iw3);
            iw4 = itemView.findViewById(R.id.edit_query_iw4);
            iwList = new ImageView[]{ iw1, iw2, iw3, iw4 };
        }
    }

    private ArrayList<ArtifactObject.Query> dataset;
    private QueryAdapter.OnClickListener listener;
    private WeakReference<RecyclerView> rv;

    QueryAdapter(RecyclerView recyclerView, ArrayList<ArtifactObject.Query> dataset, QueryAdapter.OnClickListener listener) {
        this.dataset = dataset;
        this.listener = listener;
        rv = new WeakReference<>(recyclerView);
    }

    QueryAdapter(RecyclerView recyclerView) {
        dataset = new ArrayList<>();
        rv = new WeakReference<>(recyclerView);
    }

    void setOnClickListener(QueryAdapter.OnClickListener listener) {
        this.listener = listener;
    }

    void add(ArtifactObject.Query q) {
        dataset.add(q);
        notifyItemInserted(dataset.size() - 1);
    }

    void remove(ArtifactObject.Query q) {
        int pos = dataset.indexOf(q);
        if (pos < 0) return;
        dataset.remove(q);
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
        ArtifactObject.Query query = dataset.get(i);
        vh.title.setText(query.title);
        vh.itemView.setOnClickListener(listener);
        for (int j = 0; j < vh.iwList.length; j++) {
            try { if (query.whitelist.get(j) == null) break; }
            catch (IndexOutOfBoundsException ignored) { break; }
            ImageView iw = vh.iwList[j];
            Glide.with(iw)
                    .load(query.whitelist.get(j))
                    .into(iw);
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    abstract static class OnClickListener implements View.OnClickListener {
        private WeakReference<QueryAdapter> adapter;

        OnClickListener(QueryAdapter qa) {
            super();
            adapter = new WeakReference<>(qa);
        }

        @Override
        public void onClick(View v) {
            int position = Objects.requireNonNull(adapter.get().rv.get()
                    .getLayoutManager()).getPosition(v);
            onItemClick(adapter.get().dataset.get(position), position);
        }

        public QueryAdapter getAdapter() {
            return adapter.get();
        }

        public abstract void onItemClick(ArtifactObject.Query item, int pos);
    }


}
