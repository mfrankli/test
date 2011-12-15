package edu.pu.mf.iw.ProjectOne;

//import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import java.lang.Exception;
import android.util.Log;
import java.util.TreeMap;

public class ShareContent extends Activity {

	private TreeMap<String, TrustNode> listNameToNode = new TreeMap<String, TrustNode>();
	
	public void onCreate(Bundle savedInstanceState)
	  {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.sharecontent);
	  }
	
	public void sharePublicKey(View view) {
	    Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	String to_display = CryptoMain.getPublicKeyString(this);
	    	to_display += "\n\r\n\r" + CryptoMain.getUuid(this);
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	TextView tv = (TextView) findViewById(R.id.sharecontent_text);
	    	tv.setText(String.valueOf(to_display));
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.i("errortag", e.getMessage());
	    }
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
		Intent myIntent = new Intent(ShareContent.this, DoEncryption.class);
		myIntent.putExtra("NAMES", names);
		startActivityForResult(myIntent, 0);
	}
	
	public void finishShareAttestation(TrustNode node) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
	    try {
	    	String to_display = CryptoMain.generateFullAttestForNode(node, this);
	    	intent.putExtra("ENCODE_DATA", to_display);
	    	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
	    	TextView tv = (TextView) findViewById(R.id.sharecontent_text);
	    	tv.setText(String.valueOf(to_display));
	    	startActivity(intent);
	    }
	    catch (Exception e) {
	    	Log.i("errortag", e.getMessage());
	    }
	}
	
	public void shareData(View view) {
		Intent myIntent = new Intent(ShareContent.this, ShareData.class);
		startActivity(myIntent);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				TrustNode node = listNameToNode.get(intent.getStringExtra("NAME"));
				finishShareAttestation(node);
			}
		}
	}
	
}
