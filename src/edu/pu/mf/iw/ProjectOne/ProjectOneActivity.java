package edu.pu.mf.iw.ProjectOne;

import java.util.TreeMap;

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
	TreeMap<String, TrustNode> listNameToNode;
  
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
  
  @Override
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
  
  public void connectToSingle(View view) {
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
	  Intent myIntent = new Intent(ProjectOneActivity.this, DoEncryption.class);
	  myIntent.putExtra("NAMES", names);
	  startActivityForResult(myIntent, 0);
  }
  
  public void finishSingleConnect(TrustNode node) {
	  Intent myIntent = new Intent(ProjectOneActivity.this, BluetoothChat2.class);
	  myIntent.putExtra(BluetoothChat2.KEY_EXTRA, node.getMacAddr());
	  Log.i("ProjectOneActivity 134", "Put key extra: " + node.getMacAddr());
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
  
  @Override
public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				TrustNode node = listNameToNode.get(intent.getStringExtra("NAME"));
				finishSingleConnect(node);
			}
		}
	}
}
