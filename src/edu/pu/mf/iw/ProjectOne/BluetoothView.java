package edu.pu.mf.iw.ProjectOne;

import java.util.TreeMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class BluetoothView extends Activity {
	
	private TreeMap<String, TrustNode> listNameToNode;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetoothview);
	}
	
	public void toggleBluetooth(View view) {
		Button button = (Button) findViewById(R.id.bluetoothview_toggle);
		String text = (String) button.getText();
		if (text.equals("Turn Bluetooth Exchange On")) {
			Intent myIntent = new Intent(BluetoothView.this, BluetoothChat2.class);
			button.setText("Turn Bluetooth Exchange Off");
			startService(myIntent);
		}
		else if (text.equals("Turn Bluetooth Exchange Off")) {
			Intent myIntent = new Intent(BluetoothView.this, BluetoothChat2.class);
			button.setText("Turn Bluetooth Exchange On");
			stopService(myIntent);
		}
	}
	
	public void singleConnect(View view) {
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
		Intent myIntent = new Intent(BluetoothView.this, DoEncryption.class);
		myIntent.putExtra("NAMES", names);
		startActivityForResult(myIntent, 0);
	}
	  
	public void finishSingleConnect(TrustNode node) {
		Intent myIntent = new Intent(BluetoothView.this, BluetoothChat2.class);
		myIntent.putExtra(BluetoothChat2.KEY_EXTRA, node.getMacAddr());
		Log.i("ProjectOneActivity 134", "Put key extra: " + node.getMacAddr());
		startService(myIntent);
	}
	
	public void tryBluetooth(View view) {
		Intent myIntent = new Intent(BluetoothView.this, BluetoothChat2.class);
		startService(myIntent);
		BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
		if (!ba.isEnabled() || ba.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
	        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
	      	discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        startActivity(discoverableIntent);
		}
		ba.cancelDiscovery();
		ba.startDiscovery();
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
