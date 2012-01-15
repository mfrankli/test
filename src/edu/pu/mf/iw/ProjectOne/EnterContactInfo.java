package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.content.Intent;

public class EnterContactInfo extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.entercontactinfo);
	}
	
	public void returnResult(View view) {
		EditText et = (EditText) findViewById(R.id.entercontactinfo_edittext);
		String toReturn = et.getText().toString();
		Intent resultIntent = new Intent();
		resultIntent.putExtra("NAME", toReturn);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

}
