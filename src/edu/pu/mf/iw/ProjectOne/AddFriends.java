package edu.pu.mf.iw.ProjectOne;

import java.util.TreeMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class AddFriends extends Activity {
	
	private TreeMap<String, TrustNode> listNameToNode;
	private TrustDbAdapter db = new TrustDbAdapter(this);
	private boolean LOG = false;
	private MyLog myLog = new MyLog("abc");
	private String contents = "";
	private int scanned = -1;
	
	private final int KEY_CODE = 0;
	private final int ATTEST_CODE = 1;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.addfriends);
	}
	
	public void shareKey(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	String to_display = CryptoMain.getPublicKeyString(this);
	    	to_display += "\n\r\n\r" + CryptoMain.getUuid(this);
	    	BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
	    	if (!ba.isEnabled()) {
	    		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBtIntent);
	    	}
	    	while (!ba.isEnabled());
	    	to_display += "\n\r\n\r" + ba.getAddress();
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	TextView tv = (TextView) findViewById(R.id.sharecontent_text);
	    	if (tv != null) tv.setText(to_display);
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.e("ShareContent 44", "share pubkey error", e);
	    }
	}
	
	public void scanKey(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	    scanned = KEY_CODE;
	    startActivityForResult(intent, 0);
	}
	
	public void handleKey(String contents, String name) {
		if (contents == null) return;
		String[] parts = contents.split("\n\r\n\r");
		if (parts.length < 1) return;
		String key = parts[0];
		String id = null;
		if (parts.length >= 2) id = parts[1].trim();
		String macAddr = null;
		if (parts.length >= 3) macAddr = parts[2];
		TrustNode newNode = new TrustNode(id, false, db);
		newNode.setPubkey(key);
		newNode.setDistance(1);
		newNode.setSource(id);
		newNode.setName(name);
		newNode.setMacAddr(macAddr);
		newNode.commitTrustNode();
		newNode.broadcastNode(this);
		if (LOG) myLog.addEntry(newNode);
	}
	
	public void shareAttestation(View view) {
		TreeMap<String, Integer> nameToCount = new TreeMap<String, Integer>();
		listNameToNode = new TreeMap<String, TrustNode>();
		TrustNode[] allNodes = TrustNode.getAllTrustNodes(new TrustDbAdapter(this).open(), this);
		String[] names = new String[allNodes.length];
		for (int i = 0; i < allNodes.length; i++) {
			String name = "";
			if (allNodes[i].getName() != null) name = allNodes[i].getName();
			else name = allNodes[i].getUuid();
			if (listNameToNode.containsKey(name)) {
				if (nameToCount.containsKey(name)) {
					int num = nameToCount.get(name) + 1;
					nameToCount.put(name, num);
					name = name + "(" + num + ")";
				}
				else {
					int num = 0;
					nameToCount.put(name, num);
					name = name + "(" + num + ")";
				}
				listNameToNode.put(name, allNodes[i]);
			}
			else {
				listNameToNode.put(name, allNodes[i]);
			}
			names[i] = name;
		}
		Intent myIntent = new Intent(AddFriends.this, DoEncryption.class);
		myIntent.putExtra("NAMES", names);
		startActivityForResult(myIntent, 2);
	}
	
	public void finishShareAttestation(TrustNode node) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	String to_display = CryptoMain.generateFullAttestForNode(node, this);
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.i("AddFriends 123", "error sharing attestation", e);
	    }
	}
	
	public void scanAttestation(View view) {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	    scanned = ATTEST_CODE;
	    startActivityForResult(intent, 0);
	}
	
	public void handleAttestation(String contents) {
		if (contents == null) return;
		String[] parts = contents.split("\n\r\n\r");
		if (parts == null || parts.length != 3) {
			Log.i("ReadContent 109", "parts was null or 0 length");
			return;
		}
		String uuid = parts[0].trim();
		String attest = parts[1];
		String seed = parts[2];
		TrustNode sender = new TrustNode(uuid, false, db);
		if (sender != null && sender.getDistance() != Integer.MAX_VALUE) {
			Log.i("ReadContent 118", "sender was valid");
			Log.i("ReadContent 122", "attest: " + attest);
			Log.i("ReadContent 123", "seed: " + seed);
			sender.setAttest(attest + "\n\r\n\r" + seed);
			Log.i("ReadContent 120", new String(sender.getAttest().getBytes()));
			sender.commitTrustNode();
			if (LOG) myLog.addEntry(sender);
		}
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
    		if (resultCode == RESULT_OK) {
    			contents = intent.getStringExtra("SCAN_RESULT");
    		}
    		else if (resultCode == RESULT_CANCELED) {
    			Log.i("AddFriends 162", "cancelled or error");
    		}
    	}
    	if (requestCode == 1) {
    		if (resultCode == RESULT_OK) {
    			String name = intent.getStringExtra("NAME");
    			Log.i("AddFriends 168", name);
    			handleKey(contents, name);
    		}
    	}
    	if (requestCode == 2) {
    		if (resultCode == RESULT_OK) {
    			String name = intent.getStringExtra("NAME");
    			TrustNode node = listNameToNode.get(name);
    			finishShareAttestation(node);
    		}
    	}
    	if (scanned == KEY_CODE) {
    		scanned = -1;
    		Log.i("mytag", "starting EnterContactInfo");
			Intent myIntent = new Intent(AddFriends.this, EnterContactInfo.class);
			startActivityForResult(myIntent, 1);
    	}
    	else if (scanned == ATTEST_CODE) {
    		scanned = -1;
    		Log.i("ReadContent 84", "attest code scanned");
    		handleAttestation(contents);
    	}
    }

}
