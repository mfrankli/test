package edu.pu.mf.iw.ProjectOne;

import android.content.Intent;
import android.os.Bundle;
import android.app.ListActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.app.Activity;
//import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.AdapterView;
//import android.widget.EditText;

public class DoEncryption extends ListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  Intent callerIntent = this.getIntent();
	  String[] names = callerIntent.getStringArrayExtra("NAMES");
	  if (names == null || names.length == 0) return;
	  setListAdapter(new ArrayAdapter<String>(this, R.layout.list_item, names));

	  ListView lv = getListView();
	  lv.setTextFilterEnabled(true);

	  lv.setOnItemClickListener(new OnItemClickListener() {
	    @Override
		public void onItemClick(AdapterView<?> parent, View view,
	        int position, long id) {
	      // When clicked, show a toast with the TextView text
	      String toReturn = ((TextView) view).getText().toString();
	      Intent resultIntent = new Intent();
	      resultIntent.putExtra("NAME", toReturn);
	      setResult(Activity.RESULT_OK, resultIntent);
	      finish();
	    }
	  });
	}

}
