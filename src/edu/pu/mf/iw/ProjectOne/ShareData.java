package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.database.Cursor;
import java.util.ArrayList;


public class ShareData extends Activity {

	private String signature = null;
	private String plaintext = null;
	private String ciphertext = null;
	private String name = null;
	
	private TrustDbAdapter db;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.sharedata);
	    db = new TrustDbAdapter(this);
	    db.open();
	}
	
	public void signContent(View view) {
		if (ciphertext != null) {
			signature = doSign(ciphertext);
			TextView tv = (TextView) findViewById(R.id.sharedata_text);
			tv.setText("Signed plaintext (has been encrypted): " + plaintext);
			return;
		}
		EditText et = (EditText) findViewById(R.id.sharedata_edit);
		String to_sign = et.getText().toString();
		plaintext = to_sign;
		signature = doSign(to_sign);
		TextView tv = (TextView) findViewById(R.id.sharedata_text);
		tv.setText("Signed content: " + plaintext);
	}
	
	public void encryptContent(View view) {
		EditText et = (EditText) findViewById(R.id.sharedata_edit);
		plaintext = et.getText().toString();
		ArrayList<String> strings = new ArrayList<String>();
		Cursor mCursor = db.selectAllEntries();
		if (mCursor.getCount() == 0) return;
		// there MUST be a better way to do this
		if (mCursor.moveToFirst()) {
			if (mCursor.getString(mCursor.getColumnIndex(TrustDbAdapter.KEY_NAME)) != null)
				strings.add(mCursor.getString(mCursor.getColumnIndex(TrustDbAdapter.KEY_NAME)));
			while (mCursor.moveToNext()) {
				if (mCursor.getString(mCursor.getColumnIndex(TrustDbAdapter.KEY_NAME)) != null)
					strings.add(mCursor.getString(mCursor.getColumnIndex(TrustDbAdapter.KEY_NAME)));
			}
		}
		// Just launches the class to get the human-readable name of the receiver
		Intent myIntent = new Intent(ShareData.this, DoEncryption.class);
		String[] names = new String[strings.size()];
		int i = 0;
		for (String current : strings) {
			names[i] = current;
			i++;
		}
		myIntent.putExtra("NAMES", names);
		startActivityForResult(myIntent, 0);
	}
	
	private String doSign(String plaintext) {
		try {
			return CryptoMain.generateSignature(this, plaintext);
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				finishEncrypt(intent);
			}
		}
	}
	
	private void finishEncrypt(Intent intent) {
		name = intent.getStringExtra("NAME");
		Cursor mCursor = db.selectEntryByName(name);
		String publicKeyString = mCursor.getString(mCursor.getColumnIndex(TrustDbAdapter.KEY_PUBKEY));
		ciphertext = CryptoMain.encryptContent(publicKeyString, plaintext);
		TextView tv = (TextView) findViewById(R.id.sharedata_text);
		tv.setText("Encrypted content for " + name + ": " + plaintext);
		if (signature != null) signature = doSign(ciphertext);
	}
	
	public void displayQr(View view) {
		EditText et = (EditText) findViewById(R.id.sharedata_edit);
		String to_display = "";
		if (plaintext == null) to_display = et.getText().toString();
		else to_display = plaintext;
		if (ciphertext != null) {
			String header = "";
			if (signature != null) {
				header = "Encrypted and signed content for " + name + ":\n\r\n\r";
			}
			else {
				header = "Encrypted content for " + name + "\n\r\n\r";
			}
			to_display = header;
			to_display += ciphertext;
			if (signature != null) {
				to_display += "\n\r\n\r" + signature;
				to_display += "\n\r\n\r" + CryptoMain.getUuid(this);
			}
		}
		else if (signature != null && plaintext.equals(to_display)) {
			to_display += "\n\r\n\r" + signature;
			to_display += "\n\r\n\r" + CryptoMain.getUuid(this);
		}
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.i("", e.getMessage());
	    }
	}

}
