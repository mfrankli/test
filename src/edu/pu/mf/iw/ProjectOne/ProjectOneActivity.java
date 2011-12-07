package edu.pu.mf.iw.ProjectOne;

import android.app.Activity;
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
  }
  
  public void resetDb(View view) {
	  TrustDbAdapter db = new TrustDbAdapter(this);
	  db.open();
	  db.reset();
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
}
