package ru.zont.mvc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.Objects;

public class EditActivity extends AppCompatActivity {
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);
		setSupportActionBar(findViewById(R.id.edit_tb));
		Objects.requireNonNull(getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
	}

    @Override
    public boolean onNavigateUp() {
        onBackPressed();
	    return super.onNavigateUp();
    }
}
