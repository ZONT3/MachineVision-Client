package ru.zont.mvc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        ImageView iw1;
        ImageView iw2;
        ImageView iw3;
        ImageView iw4;
        ImageView[] iwList;
        TextView title;
        ProgressBar pb;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.edit_query_title);
            iw1 = itemView.findViewById(R.id.edit_query_iw1);
            iw2 = itemView.findViewById(R.id.edit_query_iw2);
            iw3 = itemView.findViewById(R.id.edit_query_iw3);
            iw4 = itemView.findViewById(R.id.edit_query_iw4);
            iwList = new ImageView[]{ iw1, iw2, iw3, iw4 };
            pb = itemView.findViewById(R.id.edit_query_pb);
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

    void addQuery(String query, EditActivity activity) {
        int thisIndex = dataset.size();
        ArtifactObject.Query nQuery = new ArtifactObject.Query(query);
        add(nQuery);
        new EditActivity.ImageGetter(activity, new EditActivity.ImageGetter.ImageGetterPostExec() {
            @Override
            void postExec(WeakReference<EditActivity> wr, String[] result) {
                if (result == null) {
                    Toast.makeText(activity, "Error", Toast.LENGTH_SHORT).show();
                    remove(nQuery);
                    return;
                }
                nQuery.whitelist.addAll(Arrays.asList(result));
                dataset.set(thisIndex, nQuery);
                notifyItemChanged(thisIndex);
            }
        }, query, 4);
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
        for (ImageView iw : vh.iwList) iw.setImageDrawable(null);

        if (query.whitelist == null || query.whitelist.size() != 0) {
            vh.pb.setVisibility(View.GONE);
            vh.itemView.setOnClickListener(listener);
            for (int j = 0; j < vh.iwList.length; j++) {
                ImageView iw = vh.iwList[j];
                try {
                    if (query.whitelist.get(j) == null) break;
                } catch (IndexOutOfBoundsException ignored) { break; }
                Glide.with(iw)
                        .load(query.whitelist.get(j))
                        .into(iw);
            }
        } else vh.pb.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    ArrayList<ArtifactObject.Query> getDataset() {
        return dataset;
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

        QueryAdapter getAdapter() {
            return adapter.get();
        }

        public abstract void onItemClick(ArtifactObject.Query item, int pos);
    }

}
