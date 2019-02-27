package ru.zont.mvc;

import android.annotation.SuppressLint;
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

import java.io.IOException;
import java.util.Objects;

import ru.zont.mvc.core.ArtifactObject;

public class MarkActivity extends AppCompatActivity {
    static final String EXTRA_ITEM = "item";
    static final String EXTRA_QUERY = "query";

    private ArtifactObject.Query query;
    private ArtifactObject.ImageItem item;

    private CropImageView cropImageView;
    private ViewGroup loading;

    private Thread loaderThread;
    private boolean isLoading = false;

    private Intent resultData = new Intent();

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
            if (!nextItem()) return;
        }

        reloadView();
        invalidateOptionsMenu();
    }

    private synchronized void reloadView() { reloadView(null); }

    @SuppressLint("DefaultLocale")
    private synchronized void reloadView(Rect initialPos) {
        if (query != null)
            Objects.requireNonNull(getSupportActionBar())
                    .setSubtitle(String.format("%s (%d/%d)", query.title,
                            query.whitelist.indexOf(item) + 1, query.whitelist.size()));

        if (loaderThread != null && loaderThread.isAlive())
            loaderThread.interrupt();
        Thread oldThread = loaderThread;

        isLoading = true;
        loading.clearAnimation();
        cropImageView.clearAnimation();
        loading.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein));
        cropImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadeout));
        loading.setVisibility(View.VISIBLE);
        cropImageView.setVisibility(View.VISIBLE);
        cropImageView.postOnAnimation(() -> {
            if (isLoading)
                cropImageView.setVisibility(View.GONE);
        });

        loaderThread = new Thread(() -> {
            try {
                while (oldThread != null && oldThread.isAlive())
                    Thread.sleep(10);

                Bitmap bitmap = BitmapHandler.getBitmap(this, item, null);
                if (Thread.currentThread().isInterrupted()) return;

                runOnUiThread(() -> {
                    cropImageView.setImageBitmap(bitmap);
                    if (initialPos != null)
                        cropImageView.setCropRect(initialPos);
                });
            } catch (InterruptedException ignored) {
            } catch (IOException e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    if (query != null) {
                        Toast.makeText(this, R.string.mark_troubleRm, Toast.LENGTH_LONG).show();
                        query.whitelist.remove(item);
                        if (nextItem()) reloadView();
                    } else {
                        Toast.makeText(this, R.string.mark_trouble, Toast.LENGTH_LONG).show();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                });
            } finally {
                runOnUiThread(() -> {
                    isLoading = false;
                    cropImageView.clearAnimation();
                    loading.clearAnimation();
                    cropImageView.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadein));
                    loading.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fadeout));
                    cropImageView.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.VISIBLE);
                    loading.postOnAnimation(() -> {
                        if (!isLoading)
                            loading.setVisibility(View.GONE);
                    });
                });
            }
        });
        loaderThread.setPriority(Thread.MAX_PRIORITY);
        loaderThread.start();
    }

    private boolean nextItem() {
        ArtifactObject.ImageItem itm = ArtifactObject.nextItem(query);
        if (itm == null) {
            Log.d(MarkActivity.class.getName(), "Unmarked items not found");
            setResult(RESULT_OK, resultData.putExtra(EXTRA_QUERY, query));
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
            menu.findItem(R.id.mark_menu_done).setVisible(!(query != null && item.layout.size() == 0));
            menu.findItem(R.id.mark_menu_delete).setVisible(query != null);

            MenuItem del = menu.findItem(R.id.mark_menu_remove);
            del.setVisible(item.layout.size() > 0);
            if (item.layout.size() > 1) {
                del.getSubMenu().add(R.string.mark_deleteall)
                        .setOnMenuItemClickListener(this::onDeleteItemClick);
                for (int i = 0; i < item.layout.size(); i++) {
                    MenuItem newitem = del.getSubMenu().add((i+1)+"");
                    newitem.setOnMenuItemClickListener(this::onDeleteItemClick);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    private boolean onDeleteItemClick(MenuItem menuItem) {
        String title = menuItem.getTitle().toString();
        if (title.matches("\\d+")) {
            try {
                Integer[] rect = this.item.layout.get(Integer.valueOf(title) - 1);
                this.item.layout.remove(Integer.valueOf(title) - 1);
                reloadView(new Rect(rect[0], rect[1], rect[2], rect[3]));
                invalidateOptionsMenu();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (title.equals(getString(R.string.mark_deleteall))) {
            if (item.layout.size() > 0)
                item.layout.subList(0, item.layout.size()).clear();

            reloadView();
            invalidateOptionsMenu();
            return true;
        } else return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (loading.getVisibility() == View.VISIBLE || this.item == null)
            return super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
            case R.id.mark_menu_add:
                Rect selection = cropImageView.getCropRect();
                this.item.addLayout(selection);
                reloadView(selection);
                invalidateOptionsMenu();
                return true;
            case R.id.mark_menu_done:
                if (query != null) {
                    if (nextItem()) {
                        reloadView();
                        invalidateOptionsMenu();
                    } else {
                        setResult(RESULT_OK, resultData.putExtra(EXTRA_QUERY, this.query));
                        finish();
                    }
                } else onBackPressed();
                return true;
            case R.id.mark_menu_remove:
                if (this.item.layout.size() == 1) {
                    Integer[] rect = this.item.layout.get(0);
                    this.item.layout.remove(0);
                    reloadView(new Rect(rect[0], rect[1], rect[2], rect[3]));
                    invalidateOptionsMenu();
                } else return false;
                return true;
            case R.id.mark_menu_delete:
                query.whitelist.remove(this.item);
                if (nextItem()) reloadView();
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
                    .setPositiveButton(android.R.string.yes, (i1, i2) -> {
                        setResult(RESULT_CANCELED, resultData.putExtra(EXTRA_QUERY, this.query));
                        super.onBackPressed();
                    })
                    .create().show();
        } else {
            setResult(RESULT_OK, resultData.putExtra(EXTRA_ITEM, this.item));
            super.onBackPressed();
        }
    }
}
