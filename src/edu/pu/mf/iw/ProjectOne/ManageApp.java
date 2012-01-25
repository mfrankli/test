package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ManageApp extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.manageapp);   
	}
	
	public void resetKeys(View view) {
		TrustDbAdapter db = new TrustDbAdapter(this);
	    db.open();
	    TrustNode myself = new TrustNode(CryptoMain.getPublicKeyString(this), true, db);
	    CryptoMain.generateKeys(this);
	    myself.setPubkey(CryptoMain.getPublicKeyString(this));
	    myself.setUuid(CryptoMain.getUuid(this));
		myself.setAttest(CryptoMain.generateFullAttestForNode(myself, this));
		myself.setDistance(0);
		myself.setSource(myself.getUuid());
		myself.setName("me");
		myself.commitTrustNode();
		Intent toBroadcast = new Intent("edu.pu.mf.iw.ProjectOne.ACTION_KEYS");
		toBroadcast.putExtra("publickey", CryptoMain.getPublicKeyString(this));
		toBroadcast.putExtra("privatekey", CryptoMain.getPrivateKeyString(this));
		sendBroadcast(toBroadcast);
	}
	
	public void resetDb(View view) {
		TrustDbAdapter db = new TrustDbAdapter(this);
		db.open();
		db.reset();
		TrustNode myself = new TrustNode(CryptoMain.getPublicKeyString(this), true, db);
		myself.setPubkey(CryptoMain.getPublicKeyString(this));
		myself.setUuid(CryptoMain.getUuid(this));
		myself.setAttest(CryptoMain.generateFullAttestForNode(myself, this));
		Log.i("ProjectOneActivity 70", "myself.attest: " + myself.getAttest());
		myself.setDistance(0);
		myself.setSource(myself.getUuid());
		myself.setName("me");
		myself.commitTrustNode();
	}
	
	public void broadcastKeys(View view) {
		Intent toBroadcast = new Intent("edu.pu.mf.iw.ProjectOne.ACTION_KEYS");
		toBroadcast.putExtra("publickey", CryptoMain.getPublicKeyString(this));
		toBroadcast.putExtra("privatekey", CryptoMain.getPrivateKeyString(this));
		sendBroadcast(toBroadcast);
	}
	
	public void createTestNode(View view) {
		int[] possibleNumNodes = new int[10];
		for (int i = 1; i < 10; i++) {
			possibleNumNodes[i] = 100*i;
		}
		int random = (int) (Math.random()*10);
		if (random == 10) random = 9;
		Log.i("ManageApp 63", "possibleNumNodes[random]: " + possibleNumNodes[random]);
		for (int i = 0; i < possibleNumNodes[random]; i++) {
			TrustNode newNode = new TrustNode(String.valueOf(i), true, new TrustDbAdapter(this));
			newNode.setUuid(String.valueOf(i));
			newNode.commitTrustNode();
		}
	}

}
