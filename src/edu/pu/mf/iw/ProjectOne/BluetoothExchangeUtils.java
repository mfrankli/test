package edu.pu.mf.iw.ProjectOne;

import android.database.Cursor;
import android.util.Log;
import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.Arrays;

public class BluetoothExchangeUtils {
	
	public long INTERVAL = 1*60*1000; //10 minutes soft state length
	private final boolean LOG = false;
	
	private TrustDbAdapter db;
	private Context ctx;
	private MyLog myLog;
	
	public BluetoothExchangeUtils(TrustDbAdapter db, Context ctx, MyLog myLog) {
		this.db = db;
		this.ctx = ctx;
		this.myLog = myLog;
	}
	
	public String[] generateMessageList() {
    	String[] toReturn = null;
    	if (!db.isOpen()) {
    		db.open();
    	}
    	Cursor c = db.selectAllEntries();
    	if (c != null) {
    		toReturn = new String[c.getCount()];
    		for (int i = 0; i < c.getCount(); i++) {
    			c.moveToPosition(i);
    			String[] columns = c.getColumnNames();
    			String toPut = "";
        		String columnOrder = "";
    			for (int j = 0; j < columns.length; j++) {
    				if (c.getColumnIndex(columns[j]) != -1) {
    					boolean put = false;
    					if (!columns[j].equals(TrustDbAdapter.KEY_DIST)) {
    						String s = c.getString(c.getColumnIndex(columns[j]));
    						if (s != null && s.length() != 0) {
    							Log.i("BluetoothExchangeUtils 53", "s: " + s);
    							toPut += s;
    							toPut += "\n\r\n\r";
    							put = true;
    						}
    					}
    					else {
    						if (c.getInt(c.getColumnIndex(columns[j])) != Integer.MAX_VALUE) {
    							toPut += c.getInt(c.getColumnIndex(columns[j])) + "\n\r\n\r";
    							put = true;
    						}
    					}
    					if (put) columnOrder += columns[j] + ":::";
    				}
    				else {
    					Log.i("BluetoothExchangeUtils 67", "column index of " + columns[j] + " was -1");
    				}
    			}
    			toPut += columnOrder + "\n\r\n\r";
    			Log.i("BluetoothExchangeUtils 71", "columnOrder:\n" + columnOrder);
    			Log.i("BluetoothExchangeUtils 72", "toPut:\n" + toPut);
    			toReturn[i] = toPut;
    		}
    	}
    	return toReturn;
    }
	
	public boolean shouldBeClosed(String macAddr) {
		return false;
	}
	
	public void updatedCloseConditions(String macAddr, String contents) {
		
	}
	
	public void handleMessage(String message, TrustNode senderNode) {
    	if (senderNode == null) {// we don't trust the sender...
    		Log.i("BluetoothExchangeUtils 48", "TrustNode of sender is null");
    		return;
    	}
    	String[] columns = message.split("\n\r\n\r");
    	Log.i("BluetoothExchangeUtils 82", "columns.length() = " + columns.length);
    	Log.i("BluetoothExchangeUtils 83", "last line: " + columns[columns.length - 1]);
    	String[] columnOrder = null;
    	for (int i = 0; i < columns.length; i++) {
    		if (columns[i].contains(":::"))
    			columnOrder = columns[i].split(":::");
    	}
    	if (columnOrder == null) return;
    	int pubIndex = -1;
    	int distIndex = -1;
    	int uuidIndex = -1;
    	int nameIndex = -1;
    	int attestIndex = -1;
    	for (int i = 0; i < columnOrder.length; i++) {
    		if (columnOrder[i].contains(TrustDbAdapter.KEY_PUBKEY)) pubIndex = i;
    		if (columnOrder[i].contains(TrustDbAdapter.KEY_DIST)) distIndex = i;
    		if (columnOrder[i].contains(TrustDbAdapter.KEY_UUID)) uuidIndex = i;
    		if (columnOrder[i].contains(TrustDbAdapter.KEY_NAME)) nameIndex = i;
    		if (columnOrder[i].contains(TrustDbAdapter.KEY_ATTEST)) attestIndex = i;
    	}
    	Log.i("BluetoothExchangeUtils 61", "pubIndex\tdistIndex\tuuidIndex\tnameIndex\t\n" + pubIndex + "\t\t" + distIndex + "\t\t" + uuidIndex + "\t\t" + nameIndex + "\t");
    	String pubKey;
    	int dist;
    	String uuid;
    	String name;
    	String attest;
    	TrustNode toCommit = null;
    	if (uuidIndex != -1) {
    		uuid = columns[uuidIndex];
    		toCommit = new TrustNode(uuid, false, db);
    	}
    	else { // we only accept messages with uuids
    		return;
    	}
    	if (toCommit.getSource() != null) {
    		TrustNode source = getTrustNode(toCommit.getSource());
    		if (source.getDistance() < senderNode.getDistance()) {
    			return;
    		}
    	}
    	if (pubIndex != -1) {
    		pubKey = columns[pubIndex];
    		if (toCommit != null) {
    			toCommit.setPubkey(pubKey);
    		}
    	}
    	else {
    		pubKey = null;
    		return; // we only accept messages with public keys
    	}
    	if (distIndex != -1) {
    		dist = Integer.parseInt(columns[distIndex]) + senderNode.getDistance();
    		if (toCommit != null) {
    			toCommit.setDistance(dist);
    		}
    	}
    	else {
    		dist = senderNode.getDistance()*2;
    	}
    	if (nameIndex != -1) {
    		name = columns[nameIndex];
    		if (toCommit != null) {
    			toCommit.setName(name);
    		}
    	}
    	if (attestIndex != -1) {
    		attest = columns[attestIndex];
    		if (toCommit != null) {
    			toCommit.setAttest(attest);
    		}
    	}
    	String source = senderNode.getUuid();
    	if (toCommit != null) {
    		if (toCommit.getSource() != null) {
        		TrustNode oldSource = getTrustNode(toCommit.getSource());
        		if (oldSource.getAttest() == null || attestIndex != -1) {
            		toCommit.setSource(source);
        		}
        	}
    	}
    	toCommit.commitTrustNode();
    	if (LOG) myLog.addEntry(toCommit);
    }
	
	public TrustNode getTrustNode(String macAddr) {
		if (macAddr == null) return null;
		macAddr = macAddr.trim();
		Log.i("BluetoothExchangeUtils 170", "Getting macAddr: " + macAddr);
    	if (macAddr.trim().contains(CryptoMain.getUuid(ctx).trim())) {
    		Log.i("BluetoothExchangeUtils 172", "getting myself");
    		TrustNode myself = new TrustNode(macAddr, false, db);
    		myself.setPubkey(CryptoMain.getPublicKeyString(ctx));
    		myself.setDistance(0);
    		myself.setAttest(CryptoMain.generateSignature(ctx, CryptoMain.getPublicKeyString(ctx)));
    		return myself;
    	}
    	TrustNode newEntry = new TrustNode(macAddr, false, db);
    	if (newEntry.getDistance() == Integer.MAX_VALUE) {
    		Log.i("BluetoothExchangeUtils 181", "new entry has MAX_VALUE distance");
    		return null;
    	}
    	return newEntry;
    }
	
	// We might want a better implementation later
	public String[] getTabbedAttestations(String reqId) {
		TrustNode[] nodes = TrustNode.getAllTrustNodes(db);
		ArrayList<String> tabbedAttestsList = new ArrayList<String>();
		for (int i = 0; i < nodes.length; i++) {
			TabbedAttestation ta = CryptoMain.generateTabbedAttestation(ctx, nodes[i], reqId, false);
			String toAdd = ta.c + "::::::::::" + ta.t;
			tabbedAttestsList.add(toAdd);
		}
		 Collections.shuffle(tabbedAttestsList);
		 return (String []) tabbedAttestsList.toArray();
	}
	
	// This searches through an array of tabbed attestations, and returns the 
	// node that forms the path, if such a node exists
	public TrustNode checkTabbedAttestations(String[] tabbedAttestations, String reqId) {
		// parse tabbedAttestations
		TreeMap<String, TabbedAttestation> theirTabbedAttestations = new TreeMap<String, TabbedAttestation>();
		Set<String> theirTabs = new TreeSet<String>();
		for (int i = 0; i < tabbedAttestations.length; i++) {
			String[] parts = tabbedAttestations[i].split("::::::::::");
			if (parts != null && parts.length == 2) {
				theirTabbedAttestations.put(parts[1], new TabbedAttestation(parts[0], parts[1], reqId));
				theirTabs.add(parts[1]);
			}
		}
		TrustNode[] nodes = TrustNode.getAllTrustNodes(db);
		TreeMap<String, TabbedAttestation> myTabbedAttestations = new TreeMap<String, TabbedAttestation>();
		Set<String> myTabs = new TreeSet<String>();
		for (int i = 0; i < nodes.length; i++) {
			TabbedAttestation newTA = CryptoMain.generateTabbedAttestation(ctx, nodes[i], reqId, true);
			if (newTA != null) {
				myTabbedAttestations.put(newTA.t, newTA);
				myTabs.add(newTA.t);
			}
		}
		Set<String> sharedTabs = new TreeSet<String>(myTabs);
		sharedTabs.retainAll(theirTabs);
		ArrayList<TrustNode> toReturnList = new ArrayList<TrustNode>();
		// check each of the shared tabs. we return the node with the minimum distance
		for (String sharedTab : sharedTabs) {
			TabbedAttestation mySharedTabbedAttestation = myTabbedAttestations.get(sharedTab);
			TabbedAttestation theirSharedTabbedAttestation = theirTabbedAttestations.get(sharedTab);
			TrustNode temp = null;
			if ((temp = mySharedTabbedAttestation.checkMatch(ctx, theirSharedTabbedAttestation)) != null) {
				toReturnList.add(temp);
			}
		}
		if (toReturnList.size() > 0) {
			TrustNode[] sharedNodes = (TrustNode []) toReturnList.toArray();
			Arrays.sort(sharedNodes);
			return sharedNodes[0];
		}
		return null;
	}
}
