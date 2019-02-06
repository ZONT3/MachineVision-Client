package ru.zont.mvc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    }

    private DataSet dataset;
    private OnItemClickListener listener;

    QueryAdapter(ArrayList<ArtifactObject.Query> dataset) { this.dataset = new DataSet(dataset); }

    void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_query_item, viewGroup));
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
                break;
            case DataItem.TYPE_REGULAR:
                assert item.get() != null;
                for (int j=0; j < item.get().whitelist.size() && j < vh.thumbs.length; j++) {
                    //TODO
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    private static class DataItem {
        private static final int TYPE_CUSTOM = -1;
        private static final int TYPE_LOADING = 0;
        private static final int TYPE_REGULAR = 1;

        private int type;
        private ArtifactObject.Query query;

        private DataItem(int type) {
            this.type = type;
        }

        private DataItem(@NonNull ArtifactObject.Query q) {
            query = q;
            type = TYPE_REGULAR;
        }

        private ArtifactObject.Query get() {
            return type == TYPE_REGULAR ? query : null;
        }
    }

    private static class DataSet extends ArrayList<DataItem> {
        private DataSet(ArrayList<ArtifactObject.Query> list) {
            super();
            add(new DataItem(DataItem.TYPE_CUSTOM));
            for (ArtifactObject.Query q : list)
                add(new DataItem(q));
        }
    }

    interface OnItemClickListener {
        void onItemClick(ArtifactObject item);
    }
}
