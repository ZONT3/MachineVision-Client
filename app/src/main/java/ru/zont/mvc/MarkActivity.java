package ru.zont.mvc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Objects;

import ru.zont.mvc.core.ArtifactObjectNew;

public class MarkActivity extends AppCompatActivity {
    static final String EXTRA_ITEM = "item";
    static final String EXTRA_QUERY = "query";

    private ArtifactObjectNew.Query query;
    private ArtifactObjectNew.ImageItem item;

    private CropImageView cropImageView;
    private ViewGroup loading;

    private Thread loaderThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark);
        setSupportActionBar(findViewById(R.id.mark_tb));

        cropImageView = findViewById(R.id.mark_cropView);
        loading = findViewById(R.id.mark_loading);

        item = getIntent().getParcelableExtra(EXTRA_ITEM);
        query = getIntent().getParcelableExtra(EXTRA_QUERY);

        if (item == null && query == null) {
            Log.w(MarkActivity.class.getName(), "Item or query not passed");
            finish();
        }

        if (query != null) {
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            nextItem();
        }

        reloadView();
        invalidateOptionsMenu();
    }

    private synchronized void reloadView() { reloadView(null); }

    private synchronized void reloadView(Rect initialPos) {
        if (loaderThread != null && loaderThread.isAlive())
            loaderThread.interrupt();
        Thread oldThread = loaderThread;

        loading.clearAnimation();
        cropImageView.clearAnimation();
        loading.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein));
        cropImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadeout));
        loading.setVisibility(View.VISIBLE);
        cropImageView.setVisibility(View.VISIBLE);
        cropImageView.postOnAnimation(() -> cropImageView.setVisibility(View.GONE));

        loaderThread = new Thread(() -> {
            try {
                while (oldThread != null && oldThread.isAlive())
                    Thread.sleep(10);

                Bitmap bitmap = BitmapHandler.getBitmap(this, item, null);
                if (Thread.currentThread().isInterrupted()) return;

                runOnUiThread(() -> {
                    cropImageView.clearAnimation();
                    loading.clearAnimation();
                    cropImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein));
                    loading.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadeout));
                    cropImageView.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.VISIBLE);
                    loading.postOnAnimation(() -> loading.setVisibility(View.GONE));
                });

                if (bitmap == null) {
                    if (query != null) {
                        Toast.makeText(this, R.string.mark_trouble, Toast.LENGTH_LONG).show();
                        query.whitelist.remove(item);
                        nextItem();
                        reloadView();
                    } else {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                    return;
                }
                cropImageView.setImageBitmap(bitmap);
                if (initialPos != null)
                    cropImageView.setCropRect(initialPos);
            } catch (InterruptedException ignored) { }
        });
        loaderThread.setPriority(Thread.MAX_PRIORITY);
        loaderThread.start();
    }

    private boolean nextItem() {
        ArtifactObjectNew.ImageItem itm = null;
        for (ArtifactObjectNew.ImageItem i : query.whitelist)
            if (i.layout.size() == 0) itm = i;
        if (itm == null) {
            Log.d(MarkActivity.class.getName(), "Unmarked items not found");
            setResult(RESULT_OK, new Intent().putExtra("query", query));
            finish();
            return false;
        }
        item = itm;
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mark, menu);

        if (item != null) {
            menu.findItem(R.id.mark_menu_done).setEnabled(!(query != null && item.layout.size() == 0));
            menu.findItem(R.id.mark_menu_delete).setEnabled(item.layout.size() > 0);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (loading.getVisibility() == View.VISIBLE) return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            default: return super.onOptionsItemSelected(item);
            case R.id.mark_menu_add:
                Rect selection = cropImageView.getCropRect();
                this.item.addLayout(selection);
                reloadView(selection);
                invalidateOptionsMenu();
                return true;
            case R.id.mark_menu_done:
                if (query != null && nextItem()) {
                    reloadView();
                    invalidateOptionsMenu();
                } else {
                    setResult(RESULT_OK, new Intent().putExtra("item", this.item));
                    finish();
                }
                return true;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (query != null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.mark_exit)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.yes, (i1, i2) -> super.onBackPressed())
                    .create().show();
        } else super.onBackPressed();
    }
}
