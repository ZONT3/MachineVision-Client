package ru.zont.mvc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class QueryItemAdapter extends RecyclerView.Adapter<QueryItemAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        ImageButton del;
        VH(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.query_item_thumb);
            del = itemView.findViewById(R.id.query_item_del);
        }
    }

    private ArtifactObject.Query query;
    private View.OnClickListener onClickListener;
    private WeakReference<RecyclerView> rv;

    QueryItemAdapter(RecyclerView rv, ArtifactObject.Query query, View.OnClickListener onClickListener) {
        this.query = query;
        this.onClickListener = onClickListener;
        this.rv = new WeakReference<>(rv);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_query_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        vh.del.setOnClickListener(onClickListener);
        Glide.with(vh.itemView)
                .load(query.whitelist.get(i))
                .apply(new RequestOptions().override(Dimension.toPx(80, vh.itemView.getContext())))
                .into(vh.thumb);
    }

    @Override
    public int getItemCount() {
        return query.whitelist.size();
    }

    static abstract class OnClickListener implements View.OnClickListener {
        private WeakReference<QueryItemAdapter> adapter;

        OnClickListener(QueryItemAdapter qa) {
            super();
            adapter = new WeakReference<>(qa);
        }

        @Override
        public void onClick(View v) {
            onItemClick(adapter.get().query.whitelist.get(
                    Objects.requireNonNull(adapter.get().rv.get()
                            .getLayoutManager()).getPosition(v)));
        }

        public abstract void onItemClick(String item);
    }
}
