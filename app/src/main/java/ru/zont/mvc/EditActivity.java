package ru.zont.mvc;

import android.app.ActivityOptions;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import ru.zont.mvc.core.ArtifactObject;
import ru.zont.mvc.core.Client;
import ru.zont.mvc.core.Dimension;
import ru.zont.mvc.core.Request;

import static android.view.inputmethod.EditorInfo.IME_ACTION_DONE;

public class EditActivity extends AppCompatActivity {

    private EditText title;
    private QueryAdapter adapter;
    private ArtifactObject toEdit;
    private Toolbar toolbar;

    @Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
        toolbar = findViewById(R.id.edit_tb);
        setSupportActionBar(toolbar);
		Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);

		title = findViewById(R.id.edit_title);
		title.setImeOptions(IME_ACTION_DONE);
		title.setOnEditorActionListener((v, actionId, event) -> {
		    if (actionId == IME_ACTION_DONE) {
                startSaving();
                return true;
            } else return false;
        });

        if ((toEdit = getIntent().getParcelableExtra("object")) == null)
            adapter = new QueryAdapter();
        else {
            adapter = new QueryAdapter(toEdit.getQueries());
            title.setText(toEdit.getTitle());
        }

        RecyclerView rw = findViewById(R.id.edit_list);
        rw.setAdapter(adapter);
        rw.setLayoutManager(new GridLayoutManager(this,
                Dimension.getDisplayWidthDp(this) / 200));
        adapter.setOnItemClickListener(this::onItemClick);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    private void onItemClick(QueryAdapter.DataItem item, QueryAdapter.VH vh) {
        switch (item.getType()) {
            case QueryAdapter.DataItem.TYPE_REGULAR:
                Pair<View, String> content = new Pair<>(vh.itemView, "CONTENT");
                Pair<View, String> iw1 = new Pair<>(vh.getIW(0), "IW1");
                Pair<View, String> iw2 = new Pair<>(vh.getIW(1), "IW2");
                Pair<View, String> iw3 = new Pair<>(vh.getIW(2), "IW3");
                Pair<View, String> iw4 = new Pair<>(vh.getIW(3), "IW4");
                Pair<View, String> tb = new Pair<>(toolbar, "TB");

                Intent intent = new Intent(this, ResultsActivity.class)
                        .putExtra("query", item.get());
                startActivityForResult(intent, 1337, ActivityOptions
                        .makeSceneTransitionAnimation(this, content, iw1, iw2, iw3, iw4, tb)
                        .toBundle());
                break;
            case QueryAdapter.DataItem.TYPE_CUSTOM:

                break;
            default: break;
        }
    }

    private boolean isEdited() {
        if (toEdit == null)
            return !(title.getText().toString().isEmpty() && adapter.getQueries().size() == 0);
        else return !(toEdit.getTitle().equals(title.getText().toString())
                && toEdit.queriesEquals(adapter.getQueries()));
    }

    public void addQuery(View v) {
        EditText text = new EditText(this);
//        text.setText(R.string.edit_addqdiag_hint);
        text.setSelectAllOnFocus(true);
        text.setImeOptions(IME_ACTION_DONE);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_addqdiag_title)
                .setView(text)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String query = text.getText().toString();
                    if (query.isEmpty()) {
                        Toast.makeText(this, "アホか？", Toast.LENGTH_LONG).show();
                        return;
                    }

                    adapter.addLoadingQuery(query);
                    AsyncGetImages.execute(query, 8, this::onFetchImages);
                })
                .create();

        text.setOnEditorActionListener((v1, actionId, event) -> {
            if (event != null && actionId == IME_ACTION_DONE
                    && alertDialog != null && alertDialog.isShowing()) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .callOnClick();
                return true;
            } else return false;
        });

        alertDialog.setOnShowListener(dialog -> text.requestFocus());
//        alertDialog.create();
        alertDialog.show();
    }

    private void onFetchImages(@NonNull String query, @Nullable String[] urls, @Nullable Exception e) {
        if (urls == null) {
            if (e != null) {
                Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                return;
            } else {
                Toast.makeText(this, "Unexpected error", Toast.LENGTH_LONG).show();
                return;
            }
        }
        if (urls.length == 0) {
            Toast.makeText(this, R.string.edit_nores, Toast.LENGTH_LONG).show();
            return;
        }

        ArtifactObject.Query queryInstance = new ArtifactObject.Query(query);
        Collections.addAll(queryInstance.whitelist, urls);
        adapter.addQuery(queryInstance);
    }

    @Override
    public void onBackPressed() {
        if (!isEdited()) super.onBackPressed();
        else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.edit_save)
                    .setPositiveButton(R.string.yes, this::startSaving)
                    .setNegativeButton(R.string.no, (i1, i2) -> finish())
                    .setNeutralButton(android.R.string.cancel, null)
                    .create().show();
        }
    }

    private void startSaving(DialogInterface di, int w) { startSaving(); }

    private boolean canSave() {
        if (title.getText().toString().isEmpty()) {
            Toast.makeText(this, R.string.edit_err_title_void, Toast.LENGTH_LONG).show();
            return false;
        }
        if (adapter.getQueries().size() <= 0) {
            Toast.makeText(this, R.string.edit_err_noquer, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private void startSaving() {
        if (!canSave()) return;

        title.setEnabled(false);
        View content = findViewById(R.id.edit_content);
        View loading = findViewById(R.id.edit_loading);
        content.startAnimation(AnimationUtils
                .loadAnimation(this, R.anim.fadeout));
        loading.startAnimation(AnimationUtils
                .loadAnimation(this, R.anim.fadein));
        content.postOnAnimation(() -> content.setVisibility(View.GONE));

        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) { e.printStackTrace(); return; }
            if (save()) finish();
            else runOnUiThread(() -> {
                Toast.makeText(this, "Error on saving", Toast.LENGTH_SHORT)
                        .show();

            });
        }).start();
    }

    private boolean save() {
        ArtifactObject toSend;
        if (toEdit != null) {
            toEdit.edit(title.getText().toString(), adapter.getQueries());
            toSend = toEdit;
        } else toSend = new ArtifactObject(title.getText().toString(), adapter.getQueries());

        try {
            Client.sendJsonForResult(
                    Request.create("new_object")
                            .put("artifact_object", toSend)
                            .toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_menu_save: startSaving(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
	    return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1337 && resultCode == RESULT_OK && data != null) {
            if (data.hasExtra("delete"))
                adapter.delete(data.getParcelableExtra("delete"));
            else adapter.updateQuery(data.getParcelableExtra("query"));
        }
    }

    @SuppressWarnings({"UnnecessarySemicolon", "unused"})
    private void typaLogo() {
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;                     ;;;;;;;;;;;;                      ;;;;;;;;;;;;;;;;                ;;;;;;;;    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;               ;;;;;;;;;;;;;;;;;;;;;;;;;               ;;;;;;;;;;;;;;;;                ;;;;;;;;    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;        ;;;;;;;;;;;               ;;;;;;;;;;;;;        ;;;;;;;;;;;;;;;;                ;;;;;;;;    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                                      ;;;;;;;;;    ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;        ;;;;;;;;        ;;;;;;;;                    ;;;;;;;;
                                  ;;;;;;;;;        ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;        ;;;;;;;;        ;;;;;;;;                    ;;;;;;;;
                              ;;;;;;;;;            ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;        ;;;;;;;;        ;;;;;;;;                    ;;;;;;;;
                          ;;;;;;;;;                ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;        ;;;;;;;;        ;;;;;;;;                    ;;;;;;;;
                      ;;;;;;;;;                    ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;        ;;;;;;;;        ;;;;;;;;                    ;;;;;;;;
                  ;;;;;;;;;                        ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;                ;;;;;;;;;;;;;;;;                    ;;;;;;;;
              ;;;;;;;;;                            ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;                ;;;;;;;;;;;;;;;;                    ;;;;;;;;
          ;;;;;;;;;                                ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;                ;;;;;;;;;;;;;;;;                    ;;;;;;;;
        ;;;;;;;;;                                  ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;                ;;;;;;;;;;;;;;;;                    ;;;;;;;;
        ;;;;;;;;                                   ;;;;;;;;;;;                         ;;;;;;;;;;;    ;;;;;;;;                ;;;;;;;;;;;;;;;;                    ;;;;;;;;
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;        ;;;;;;;;;;;               ;;;;;;;;;;;;;        ;;;;;;;;                        ;;;;;;;;                    ;;;;;;;;
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;               ;;;;;;;;;;;;;;;;;;;;;;;;;               ;;;;;;;;                        ;;;;;;;;                    ;;;;;;;;
        ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;                     ;;;;;;;;;;;;                      ;;;;;;;;                        ;;;;;;;;                    ;;;;;;;;
    }
}
