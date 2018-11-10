package ru.zont.mvc;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class EditActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
    }

    public void onClickAddQ(View b) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText et = new EditText(this);
        et.setHint(R.string.edit_addqdiag_hint);
        et.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        builder.setTitle(R.string.edit_addqdiag_title)
                .setView(et)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new NewQuery(EditActivity.this, et.getText().toString()).execute();
                    }
                }).create().show();

    }

    private static class NewQuery extends AsyncTask<Void, Void, ArrayList<String>> {
        private WeakReference<EditActivity> ea;
        private String query;

        private IOException e;

        NewQuery(EditActivity ea, String q) {
            this.ea = new WeakReference<>(ea);
            query = q;
        }

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            HashMap<String, String>[] list;
            try {
                list = new Gson().fromJson(Client.sendJsonForResult(
                        "{\"request_code\":\"reqimg\"," +
                        "\"query\":\""+query+"\"," +
                        "\"rtcount\":\"5\"}"
                ), MetadataAnswer.class).metadata;
            } catch (IOException e) {
                e.printStackTrace();
                this.e = e;
                return null;
            }
            ArrayList<String> urls = new ArrayList<>();
            for (HashMap<String, String> ent : list) {
                if (ent.get("image_link")!=null) urls.add(ent.get("image_link"));
            }
            return urls;
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            if (strings == null) {
                ea.get().finish();
                Toast.makeText(ea.get(), e == null ? "Error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            LinearLayout linearLayout = ea.get().findViewById(R.id.edit_list);
            View frag = LayoutInflater.from(ea.get()).inflate(R.layout.fragment_query, linearLayout, false);
            TextView tw = frag.findViewById(R.id.query_title);
            tw.setText(query);

            int spans = Dimension.toDp(ea.get().getResources().getDisplayMetrics().widthPixels, ea.get()) / 100;
            final RecyclerView rw = frag.findViewById(R.id.query_recycler);
            rw.setLayoutManager(new GridLayoutManager(ea.get(), spans){
                @Override
                public boolean canScrollVertically() { return false; }});
            rw.setAdapter(new EditThumbAdapter(strings));

            Button more = frag.findViewById(R.id.query_more);
            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new GetMore(new WeakReference<>(rw), ea, query).execute();
                }
            });

            linearLayout.addView(frag, linearLayout.getChildCount()-1);
        }

        private static class GetMore extends AsyncTask<Void, Void, ArrayList<String>> {
            private WeakReference<RecyclerView> rv;
            private WeakReference<EditActivity> ea;
            private int offset;
            private String query;

            private IOException e;

            private GetMore(WeakReference<RecyclerView> rv, WeakReference<EditActivity> ea, String q) {
                this.rv = rv;
                this.ea = ea;
                query = q;
                offset = rv.get().getAdapter().getItemCount();
            }

            @Override
            protected ArrayList<String> doInBackground(Void... voids) {
                HashMap<String, String>[] list;
                try {
                    list = new Gson().fromJson(Client.sendJsonForResult(
                            "{\"request_code\":\"reqimg\"," +
                            "\"query\":\""+query+"\"," +
                            "\"rtcount\":\"5\","+
                            "\"offset\":\""+offset+"\"}"
                    ), MetadataAnswer.class).metadata;
                } catch (IOException e) {
                    e.printStackTrace();
                    this.e = e;
                    return null;
                }
                ArrayList<String> urls = new ArrayList<>();
                for (HashMap<String, String> ent : list) {
                    if (ent.get("image_link")!=null) urls.add(ent.get("image_link"));
                }
                return urls;
            }

            @Override
            protected void onPostExecute(ArrayList<String> strings) {
                if (strings == null) {
                    ea.get().finish();
                    Toast.makeText(rv.get().getContext(), e == null ? "Error while getting thumbnails" : e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                EditThumbAdapter eta = (EditThumbAdapter) rv.get().getAdapter();
                eta.add(strings);
            }
        }
    }

    private static class MetadataAnswer{
        private String answerCode;
        private HashMap<String, String>[] metadata;
    }

}
