package ru.zont.mvc;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class EditThumbAdapter extends RecyclerView.Adapter<EditThumbAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        ImageView thumb;
        ImageButton del;
        VH(View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.query_item_thumb);
            del = itemView.findViewById(R.id.query_item_del);
        }
    }

    private ArrayList<String> whitelist;
    private ArrayList<String> blacklist;

    EditThumbAdapter(ArrayList<String> urls) { whitelist = urls; blacklist = new ArrayList<>(); }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_query_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Glide.with(holder.itemView.getContext())
                .load(whitelist.get(position))
                //.apply(new RequestOptions().override(84))
                .into(holder.thumb);
        holder.del.setOnClickListener(new DeleteListener(position));
    }

    public void add(ArrayList<String> list) {
        whitelist.addAll(list);
        notifyDataSetChanged();
        invalidateBlacklist();
    }

    public void add(String url, int position) {
        whitelist.add(position, url);
        notifyItemInserted(position);
        notifyItemRangeChanged(position, whitelist.size());
        invalidateBlacklist();
    }

    public String remove(int position) {
        String res = whitelist.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, whitelist.size());
        return res;
    }

    public String blacklist(int position) {
        String res = remove(position);
        blacklist.add(res);
        return res;
    }

    @Override
    public int getItemCount() {
        return whitelist.size();
    }

    public ArrayList<String> getBlacklist() { return blacklist; }

    private void invalidateBlacklist() {
        for (String s : whitelist)
            blacklist.remove(s);
    }

    private class DeleteListener implements ImageButton.OnClickListener {
        private int position;

        private DeleteListener(int position) { this.position = position; }

        @Override
        public void onClick(View v) {
            final String removed = blacklist(position);
            Snackbar.make(v.getRootView(), R.string.edit_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.edit_del_undo, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { add(removed, position); }
                    }).show();
        }
    }
}
