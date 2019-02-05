package ru.zont.mvc;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class ObjectAdapter extends RecyclerView.Adapter<ObjectAdapter.VH> {
    class VH extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView meta;
        private TextView status;
        private TextView act;
        private ImageView thumb;

        private VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.main_item_title);
            meta = itemView.findViewById(R.id.main_item_meta);
            status = itemView.findViewById(R.id.main_item_status);
            act = itemView.findViewById(R.id.main_item_lastact);
            thumb = itemView.findViewById(R.id.main_item_thumb);
        }
    }

    private ArrayList<ArtifactObject> dataset;

    ObjectAdapter(ArrayList<ArtifactObject> dataset) {
        this.dataset = dataset;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new VH(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_object, viewGroup));
    }

    @Override
    public void onBindViewHolder(@NonNull VH vh, int i) {
        ArtifactObject object = dataset.get(i);
        Context context = vh.itemView.getContext();

        vh.title.setText(object.getTitle());
        vh.meta.setText(context.getString(R.string.main_meta, object.getTotal()));
        vh.status.setText(MainActivity.getStatusString(object, context));
//TODO        vh.act.setText();
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    interface OnItemClickListener {
        void onItemClick(ArtifactObject item, int pos);
    }
}
