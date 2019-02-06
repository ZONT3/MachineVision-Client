package ru.zont.mvc;

import android.support.v7.app.AppCompatActivity;

public class EditActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.activity_edit);
		setSupportActionBar(findViewById(R.id.edit_tb));
		//TODO toolbar up action

		
	}
}
