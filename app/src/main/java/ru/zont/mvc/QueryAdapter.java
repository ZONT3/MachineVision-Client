package ru.zont.mvc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class QueryAdapter extends RecyclerView.Adapter<QueryAdapter.VH> {
    public static class VH extends RecyclerView.ViewHolder {
        ImageView iw1;
        ImageView iw2;
        ImageView iw3;
        ImageView iw4;
        TextView title;

        public VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.edit_query_title);
            iw1 = itemView.findViewById(R.id.edit_query_iw1);
            iw2 = itemView.findViewById(R.id.edit_query_iw2);
            iw3 = itemView.findViewById(R.id.edit_query_iw3);
            iw4 = itemView.findViewById(R.id.edit_query_iw4);
        }
    }

    private ArrayList<String> dataset;

    QueryAdapter(ArrayList<String> dataset) {
        this.dataset = dataset;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_query_thumb, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        // TODO доделать
    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
