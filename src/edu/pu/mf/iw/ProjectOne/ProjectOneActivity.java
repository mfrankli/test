package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import android.bluetooth.BluetoothAdapter;

public class ProjectOneActivity extends Activity
{
	
	private final int UUID_LENGTH = 64;
  
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
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
  
  public void onDestroy() {
	  super.onDestroy();
	  //closeClient(null);
	  //closeServer(null);
  }
  
  public void generateKeys(View view) {
    try {
    	TextView tv = (TextView) findViewById(R.id.text_id);
    	tv.setText(CryptoMain.generateKeys(this) + "\n" + CryptoMain.generateUUID(this, UUID_LENGTH));
    }
    catch (Exception e) {
      TextView tv = (TextView) findViewById(R.id.text_id);
      tv.setText(e.getMessage());
    }
    TrustDbAdapter db = new TrustDbAdapter(this);
	  db.open();
    TrustNode myself = new TrustNode(CryptoMain.getPublicKeyString(this), true, db);
	  if (myself.getDistance() == Integer.MAX_VALUE) {
		  myself.setUuid(CryptoMain.getUuid(this));
		  myself.setAttest(CryptoMain.generateFullAttestForNode(myself, this));
		  myself.setDistance(0);
		  myself.setSource(myself.getUuid());
		  myself.setName("me");
		  myself.commitTrustNode();
	  }
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
	  boolean committed = myself.commitTrustNode();
	  Log.i("ProjectOneActivity 75", "" + committed);
	  myself = new TrustNode(CryptoMain.getUuid(this), false, db);
	  Log.i("ProjectOneActivity 76", "" + myself.getAttest());
  }
  
  public void shareContent(View view) {
	  Intent myIntent = new Intent(ProjectOneActivity.this, ShareContent.class);
	  startActivity(myIntent);
  }
  
  public void readContent(View view) {
	  Intent myIntent = new Intent(ProjectOneActivity.this, ReadContent.class);
	  startActivity(myIntent);
  }
  
  public void launchBluetooth(View view) {
	  Intent myIntent = new Intent(ProjectOneActivity.this, BluetoothChat2.class);
	  startService(myIntent);
  }
  
  public void closeBluetooth(View view) {
	  Intent myIntent = new Intent(ProjectOneActivity.this, BluetoothChat2.class);
	  stopService(myIntent);
  }
  
  public void tryBluetoothTransfer(View view) {
	  BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
	  ba.cancelDiscovery();
	  ba.startDiscovery();
  }
  
  public void broadcastKeys(View view) {
	  Intent toBroadcast = new Intent("edu.pu.mf.iw.ProjectOne.ACTION_KEYS");
	  toBroadcast.putExtra("publickey", CryptoMain.getPublicKeyString(this));
	  toBroadcast.putExtra("privatekey", CryptoMain.getPrivateKeyString(this));
	  sendBroadcast(toBroadcast);
  }
  
  public void testTabbedAttestation(View view) {
	  TrustDbAdapter db = new TrustDbAdapter(this);
	  db.open();
	  TrustNode[] nodes = TrustNode.getAllTrustNodes(db, this);
	  TrustNode node = null;
	  for (int i = 0; i < nodes.length; i++) {
		  Log.i("ProjectOneActivity 116", "" + nodes[i].getName());
		  Log.i("ProjectOneActivity 117", "" + nodes[i].getAttest());
		  if (nodes[i].getAttest() != null) {
			  node = nodes[i];
			  break;
		  }
	  }
	  try {
		  if (node != null) {
			  TabbedAttestation tabbedAttestation = CryptoMain.generateTabbedAttestation(this, node, "0", false);
			  Log.i("ProjectOneActivity 114", tabbedAttestation.toString());
		  }
		  else {
			  Log.i("ProjectOneActivity 128", "node is null");
		  }
	  }
	  catch (Exception e) {
		  Log.e("ProjectOneActivity 119", "exception", e);
	  }
	  
	  BluetoothExchangeUtils beu = new BluetoothExchangeUtils(db, this, null);
	  String[] tabbedAttestations = beu.getTabbedAttestations("0");
	  TrustNode node2 = beu.checkTabbedAttestations(tabbedAttestations, "0", CryptoMain.getUuid(this));
	  if (node2 != null) Log.i("ProjectOneActivity 138", "node2: " + node2.getUuid());
  }
}
