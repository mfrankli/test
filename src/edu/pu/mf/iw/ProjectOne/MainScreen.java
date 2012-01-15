package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainScreen extends Activity {
	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.mainscreen);
	    TrustDbAdapter db = new TrustDbAdapter(this);
	    db.open();
	    TrustNode myself = new TrustNode(CryptoMain.getPublicKeyString(this), true, db);
	    if (myself.getDistance() == Integer.MAX_VALUE) {
	    	myself.setUuid(CryptoMain.getUuid(this));
	    	myself.setAttest(CryptoMain.generateFullAttestForNode(myself, this));
	    	myself.setDistance(0);
	    	myself.setSource(myself.getUuid());
	    	myself.commitTrustNode();
	    }
	}
	
	public void viewFriends(View view) {
		Intent myIntent = new Intent(MainScreen.this, ViewFriends.class);
		startActivity(myIntent);
	}
	
	public void addFriend(View view) {
		Intent myIntent = new Intent(MainScreen.this, AddFriends.class);
		startActivity(myIntent);
	}
	
	public void shareFriends(View view) {
		Intent myIntent = new Intent(MainScreen.this, BluetoothView.class);
		startActivity(myIntent);
	}
	
	public void manageApp(View view) {
		Intent myIntent = new Intent(MainScreen.this, ManageApp.class);
		startActivity(myIntent);
	}
}
