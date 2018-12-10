package ru.zont.mvc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

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

    QueryAdapter(ArrayList<ArtifactObject.Query> dataset) {
        this.dataset = dataset;
    }

    QueryAdapter() { dataset = new ArrayList<>(); }

    void add(ArtifactObject.Query q) {
        dataset.add(q);
        notifyItemInserted(dataset.size() - 1);
    }

    void remove(ArtifactObject.Query q) {
        int pos = dataset.indexOf(q);
        if (pos < 0) return;
        dataset.remove(q);
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
        for (int j = 0; j < vh.iwList.length; j++) {
            if (query.whitelist.get(j) == null) break;
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
}
