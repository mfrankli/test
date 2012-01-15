package edu.pu.mf.iw.ProjectOne;

import java.util.TreeMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ViewFriends extends Activity {
	
	private TreeMap<String, TrustNode> listNameToNode;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewfriends);
	}
	
	public void selectFriend(View view) {
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
		Intent myIntent = new Intent(ViewFriends.this, DoEncryption.class);
		myIntent.putExtra("NAMES", names);
		startActivityForResult(myIntent, 0);
	}
	
	private void finishSelectFriend(TrustNode node) {
		setContentView(R.layout.friendview);
		TextView tv = (TextView) findViewById(R.id.friendview_text);
		String toDisplay = "";
		if (node.getName() != null) toDisplay += "Name:\t" + node.getName() + "\n";
		toDisplay += "Distance:\t" + node.getDistance() + "\n";
		toDisplay += "Source:\t" + node.getSource() + "\n";
		toDisplay += "Attestable:\t" + String.valueOf(node.getAttest()!= null);
		tv.setText(toDisplay);
	}
	
	public void viewAll(View view) {
		TrustNode[] allNodes = TrustNode.getAllTrustNodes(new TrustDbAdapter(this), this);
		String toDisplay = "";
		for (int i = 0; i < allNodes.length; i++) {
			toDisplay += allNodes[i].getName() + "\t" + allNodes[i].getDistance() + "\n";
		}
		setContentView(R.layout.allfriendsview);
		TextView tv = (TextView) findViewById(R.id.allfriendsview_text);
		tv.setText(toDisplay);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				TrustNode node = listNameToNode.get(intent.getStringExtra("NAME"));
				finishSelectFriend(node);
			}
		}
	}

}
