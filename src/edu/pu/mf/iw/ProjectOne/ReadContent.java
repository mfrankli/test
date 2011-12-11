package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

public class ReadContent extends Activity {
	
	// flag plus constants, to define whether the content most recently scanned was a key or actual content
	private int scanned = -1;
	private static final int KEY_CODE = 0;
	private static final int CONTENT_CODE = 1;
	private static final int ATTEST_CODE = 2;
	private final boolean LOG = false;
	private String contents = null;
	
	private TrustDbAdapter db;
	private MyLog myLog;

	/*public ReadContent(Activity ctx) {
		db = new TrustDbAdapter(ctx);
		db.open();
	}*/
	
	public void onCreate(Bundle savedInstanceState)
	  {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.readcontent);
	    db = new TrustDbAdapter(this);
	    db.open();
	    myLog = new MyLog(CryptoMain.getUuid(this));
	  }
	
	public void scanKey(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	    scanned = KEY_CODE;
	    startActivityForResult(intent, 0);
	}
	
	public void scanAttestation(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	    scanned = ATTEST_CODE;
	    startActivityForResult(intent, 0);
	}
	
	public void scanContent(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
		intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
		scanned = CONTENT_CODE;
		startActivityForResult(intent, 0);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		TextView tv = (TextView) findViewById(R.id.readcontent_displaytext);
		if (requestCode == 0) {
    		if (resultCode == RESULT_OK) {
    			contents = intent.getStringExtra("SCAN_RESULT");
    		}
    		else if (resultCode == RESULT_CANCELED) {
    			tv.setText("Error or canceled!");
    		}
    	}
    	if (requestCode == 1) {
    		if (resultCode == RESULT_OK) {
    			String name = intent.getStringExtra("NAME");
    			Log.i("nametag", name);
    			handleKey(contents, name);
    		}
    	}
    	if (scanned == KEY_CODE) {
    		scanned = -1;
    		Log.i("mytag", "starting EnterContactInfo");
			Intent myIntent = new Intent(ReadContent.this, EnterContactInfo.class);
			startActivityForResult(myIntent, 1);
    	}
    	else if (scanned == CONTENT_CODE) {
    		handleContent(contents);
    	}
    	else if (scanned == ATTEST_CODE) {
    		scanned = -1;
    		Log.i("ReadContent 84", "attest code scanned");
    		handleAttestation(contents);
    	}
    }
	
	public void handleKey(String contents, String name) {
		if (contents == null) return;
		String[] parts = contents.split("\n\r\n\r");
		if (parts.length != 2) return;
		String key = parts[0];
		String id = parts[1].trim();
		TrustNode newNode = new TrustNode(id, false, db);
		newNode.setPubkey(key.trim());
		newNode.setDistance(0);
		newNode.setSource(id);
		newNode.commitTrustNode();
		if (LOG) myLog.addEntry(newNode);
	}
	
	public void handleAttestation(String contents) {
		String[] parts = contents.split("\n\r\n\r");
		if (parts == null || parts.length == 0) {
			Log.i("ReadContent 109", "parts was null or 0 length");
			return;
		}
		String pubKey = parts[0];
		pubKey = pubKey.trim();
		String attest = parts[1];
		attest = attest.trim();
		TrustNode sender = new TrustNode(pubKey, true, db);
		if (sender != null && sender.getDistance() != Integer.MAX_VALUE) {
			Log.i("ReadContent 118", "sender was valid");
			sender.setAttest(attest);
			Log.i("ReadContent 120", new String(sender.getAttest().getBytes()));
			sender.commitTrustNode();
			if (LOG) myLog.addEntry(sender);
		}
	}
	
	public void handleContent(String contents) {
		if (contents == null) return;
		String to_display = contents;
		// Things need to be fixed here
		if (contents.contains("\n\r\n\r")) {
			String[] parts = contents.split("\n\r\n\r");
			String content = parts[0];
			if (content.startsWith("Encrypted content for") || content.startsWith("Encrypted and signed content for")) {
				String header = content;
				content = parts[1];
				String plaintext = CryptoMain.decryptContent(this, content);
				Log.i("plaintexttag", plaintext);
				to_display = plaintext + "\nWas encrypted\n";
				if (header.startsWith("Encrypted and signed content for")) {
					String signatureString = parts[2];
					String uuid = parts[3];
					Cursor c = db.selectEntryByUuid(uuid);
					String pubKeyString = c.getString(c.getColumnIndex("pubkey"));
					boolean verify = CryptoMain.verifySignature(pubKeyString, content, signatureString);
					String name = c.getString(c.getColumnIndex("readable_id"));
					Log.i("mytag", String.valueOf(c.getColumnIndex("readable_id")));
					Log.i("mytag", name);
					to_display = "Was signed by " + name + ": " + String.valueOf(verify);
				}
			}
			else {
				to_display = content;
				String signatureString = parts[1];
				String uuid = parts[2].trim();
				Log.i("mytag", "uuid: " + uuid);
				Cursor c = db.selectEntryByUuid(uuid);
				String[] columnNames = c.getColumnNames();
				for (int i = 0; i < columnNames.length; i++) {
					Log.i("mytag", columnNames[i]);
				}	
				Log.i("mytag", String.valueOf(c.getCount()));
				if (c.getCount() == 0) {
					to_display = "No Trust Info\n";
					TextView tv = (TextView) findViewById(R.id.readcontent_displaytext);
					tv.setText(to_display);
					return;
				}
				String pubKeyString = c.getString(c.getColumnIndex(TrustDbAdapter.KEY_PUBKEY));
				Log.i("mytag", pubKeyString);
				boolean verify = CryptoMain.verifySignature(pubKeyString, content, signatureString);
				if (verify) {
					String name = "";
					name = c.getString(c.getColumnIndex("readable_id"));
					if (name == null) {
						name = c.getString(c.getColumnIndex(TrustDbAdapter.KEY_UUID));
					}
					to_display += "\n" + name + " created this content: ";
					Log.i("mytag", String.valueOf(c.getColumnIndex("readable_id")));
					Log.i("mytag", name);
				}	
				to_display += "\n" + String.valueOf(verify);
			}
		}
		TextView tv = (TextView) findViewById(R.id.readcontent_displaytext);
		tv.setText(to_display);
	}

}
