package ru.zont.mvc;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class EditThumbAdapter extends RecyclerView.Adapter<EditThumbAdapter.VH> {
    static class VH extends RecyclerView.ViewHolder {
        View root;
        ImageView thumb;
        ImageButton del;
        VH(View itemView) {
            super(itemView);
            root = itemView;
            thumb = itemView.findViewById(R.id.query_item_thumb);
            del = itemView.findViewById(R.id.query_item_del);
        }
    }

    private ArrayList<String> whitelist;
    private ArrayList<String> blacklist;
    private String thumbUrl;

    private Runnable onEdit;
    private WeakReference<EditActivity> wr;

    int mOffset = 0;

    EditThumbAdapter(ArrayList<String> urls, Runnable onEdit, EditActivity activity) {
        whitelist = urls;
        blacklist = new ArrayList<>();
        this.onEdit = onEdit;
        wr = new WeakReference<>(activity);

        if (urls.size() > 0) thumbUrl = urls.get(0);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_query_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final String url = whitelist.get(position);
        Glide.with(holder.itemView.getContext())
                .load(url)
                //.apply(new RequestOptions().override(84))
                .into(holder.thumb);
        holder.del.setOnClickListener(new DeleteListener(position));
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wr.get().startActivity(
                        new Intent(Intent.ACTION_VIEW)
                                .setDataAndType(Uri.parse(url), "image/*"));
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                wr.get().setThumbnail(url);
                Toast.makeText(v.getContext(), R.string.edit_thumb_set, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    public void add(ArrayList<String> list) {
        whitelist.addAll(list);
        notifyDataSetChanged();
        onEdit.run();
        invalidateBlacklist();
    }

    public void add(String url, int position) {
        whitelist.add(position, url);
        notifyItemInserted(position);
        notifyItemRangeChanged(position, whitelist.size());
        onEdit.run();
        invalidateBlacklist();
    }

    String remove(int position) {
        String res = whitelist.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, whitelist.size());
        onEdit.run();
        return res;
    }

    String blacklist(int position) {
        String res = remove(position);
        blacklist.add(res);
        return res;
    }

    @Override
    public int getItemCount() {
        return whitelist.size();
    }

    ArrayList<String> getBlacklist() { return blacklist; }

    String getThumbUrl() {
        if (whitelist.size() > 0) return whitelist.get(0);
        else return thumbUrl;
    }

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
