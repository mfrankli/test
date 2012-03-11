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
	private TreeMap<String, String> macAddrToReqId = new TreeMap<String, String>();
	private int reqIdCounter = 0;
	
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
    					if (!columns[j].equals(TrustDbAdapter.KEY_DIST) && !columns[j].equals(TrustDbAdapter.KEY_ATTEST)) {
    						String s = c.getString(c.getColumnIndex(columns[j]));
    						if (columns[j].equals(TrustDbAdapter.KEY_UUID)){
    							if (s.contains(CryptoMain.getUuid(ctx))) continue;
    						}
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
    	String[] columnOrder = null;
    	for (int i = 0; i < columns.length; i++) {
    		if (columns[i].contains(":::")) {
    			columnOrder = columns[i].split(":::");
    			break; 
    		}
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
    		if (uuid.contains(CryptoMain.getUuid(ctx))) return;
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
    		if (pubKey.equals(CryptoMain.getPublicKeyString(ctx))) return;
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
    		if (toCommit != null && dist < toCommit.getDistance()) {
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
    	toCommit.broadcastNode(ctx);
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
	
	// format of my claim to my id:
	// uuid
	// Sign(macAddr)
	public String getMyAttestation(String macAddr) {
		String uuid = CryptoMain.getUuid(ctx);
		String signed = CryptoMain.generateSignature(ctx, macAddr);
		if (uuid == null || signed == null) return null;
		return "attestation from " + uuid + "\n\r\n\r" + uuid + "\n\r\n\r" + signed;
	}
	
	// We might want a better implementation later
	public String[] getTabbedAttestations(String reqId) {
		TrustNode[] nodes = TrustNode.getAllTrustNodes(db, ctx);
		ArrayList<String> tabbedAttestsList = new ArrayList<String>();
		for (int i = 0; i < nodes.length; i++) {
			TabbedAttestation ta = CryptoMain.generateTabbedAttestation(ctx, nodes[i], reqId, false);
			if (ta != null) {
				String toAdd = ta.c + "::::::::::" + ta.t;
				Log.i("BluetoothExchangeUtils 213", "t: " + ta.t);
				tabbedAttestsList.add(toAdd);
			}
		}
		 Collections.shuffle(tabbedAttestsList);
		 String[] toReturn = new String[tabbedAttestsList.size()];
		 return tabbedAttestsList.toArray(toReturn);
	}
	
	// This searches through an array of tabbed attestations, and returns the 
	// node that forms the path, if such a node exists
	public TrustNode checkTabbedAttestations(String[] tabbedAttestations, String reqId, String claim) {
		// parse tabbedAttestations
		TreeMap<String, TabbedAttestation> theirTabbedAttestations = new TreeMap<String, TabbedAttestation>();
		Set<String> theirTabs = new TreeSet<String>();
		for (int i = 0; i < tabbedAttestations.length; i++) {
			Log.i("BluetoothExchangeUtils 227", tabbedAttestations[i]);
			String[] parts = tabbedAttestations[i].split("::::::::::");
			if (parts != null && parts.length == 2) {
				theirTabbedAttestations.put(parts[1], new TabbedAttestation(parts[0], parts[1], reqId));
				theirTabs.add(parts[1]);
			}
		}
		Log.i("BluetoothExchangeUtils 233", "theirTabs.size(): " + theirTabs.size());
		TrustNode[] nodes = TrustNode.getAllTrustNodes(db, ctx);
		TreeMap<String, TabbedAttestation> myTabbedAttestations = new TreeMap<String, TabbedAttestation>();
		TreeMap<String, TrustNode> tabToTrustNode = new TreeMap<String, TrustNode>();
		Set<String> myTabs = new TreeSet<String>();
		for (int i = 0; i < nodes.length; i++) {
			TabbedAttestation newTA = CryptoMain.generateTabbedAttestation(ctx, nodes[i], reqId, true);
			if (newTA != null) {
				myTabbedAttestations.put(newTA.t, newTA);
				myTabs.add(newTA.t);
				tabToTrustNode.put(newTA.t, nodes[i]);
			}
		}
		Set<String> sharedTabs = new TreeSet<String>(myTabs);
		sharedTabs.retainAll(theirTabs);
		Log.i("BluetoothExchangeUtils 248", "sharedTabs.size(): " + sharedTabs.size());
		ArrayList<TrustNode> toReturnList = new ArrayList<TrustNode>();
		// check each of the shared tabs. we return the node with the minimum distance
		for (String sharedTab : sharedTabs) {
			TabbedAttestation theirSharedTabbedAttestation = theirTabbedAttestations.get(sharedTab);
			TrustNode temp = tabToTrustNode.get(sharedTab);
			if ((temp = theirSharedTabbedAttestation.checkMatch(ctx, claim, temp, reqId)) != null) {
				Log.i("BluetoothExchangeUtils 258", "there was a match!");
				toReturnList.add(temp);
			}
		}
		if (toReturnList.size() > 0) {
			TrustNode[] sharedNodes = new TrustNode[toReturnList.size()];
			sharedNodes = toReturnList.toArray(sharedNodes);
			Arrays.sort(sharedNodes);
			return sharedNodes[0];
		}
		return null;
	}
	
	public String getReqId(String macAddr) {
		if (macAddrToReqId.containsKey(macAddr)) {
			return macAddrToReqId.get(macAddr);
		}
		else {
			String toReturn = String.valueOf(reqIdCounter++);
			macAddrToReqId.put(macAddr, toReturn);
			return toReturn;
		}
	}
}
