package edu.pu.mf.iw.ProjectOne;

//import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import java.lang.Exception;
import android.util.Log;


public class ShareContent extends Activity {

	public void onCreate(Bundle savedInstanceState)
	  {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.sharecontent);
	  }
	
	public void sharePublicKey(View view) {
	    Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	String to_display = CryptoMain.getPublicKeyString(this);
	    	to_display += "\n\r\n\r" + CryptoMain.getUuid(this);
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	TextView tv = (TextView) findViewById(R.id.sharecontent_text);
	    	tv.setText(String.valueOf(to_display));
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.i("errortag", e.getMessage());
	    }
	}
	
	public void shareAttestation(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	String to_display = CryptoMain.getPublicKeyString(this);
	    	to_display += "\n\r\n\r" + CryptoMain.generateSignature(this, CryptoMain.getPublicKeyString(this));
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	TextView tv = (TextView) findViewById(R.id.sharecontent_text);
	    	tv.setText(String.valueOf(to_display));
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.i("errortag", e.getMessage());
	    }
	}
	
	public void shareData(View view) {
		Intent myIntent = new Intent(ShareContent.this, ShareData.class);
		startActivity(myIntent);
	}
}
