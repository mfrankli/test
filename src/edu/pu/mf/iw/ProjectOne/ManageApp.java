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

}
