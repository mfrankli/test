package edu.pu.mf.iw.ProjectOne;

import android.database.Cursor;
import android.util.Log;
import android.content.Context;

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
    	Log.i("BluetoothExchangeUtils 86", "columns.length() = " + columns.length);
    	Log.i("BluetoothExchangeUtils 87", "last line: " + columns[columns.length - 1]);
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
		macAddr = macAddr.trim();
		if (macAddr == null) return null;
		Log.i("BluetoothExchangeUtils 158", "Getting macAddr: " + macAddr);
    	if (macAddr.equals(CryptoMain.getUuid(ctx))) {
    		Log.i("BluetoothExchangeUtils 167", "getting myself");
    		TrustNode myself = new TrustNode(macAddr, false, db);
    		myself.setPubkey(CryptoMain.getPublicKeyString(ctx));
    		myself.setDistance(0);
    		return myself;
    	}
    	TrustNode newEntry = new TrustNode(macAddr, false, db);
    	if (newEntry.getDistance() == Integer.MAX_VALUE) {
    		Log.i("BluetoothExchangeUtils 175", "new entry has MAX_VALUE distance");
    		return null;
    	}
    	return newEntry;
    }
	
	/* Attestation format:
	 * - macAddr of shared node
	 * - distance
	 * - AES_sk-shared(SIG-RSA_sk-shared(pk-shared))
	 * - public key of claiming node
	 */
	public String getAttestation(String macAddr) {
		TrustNode toConvince = getTrustNode(macAddr);
		if (toConvince == null) { 
			// we don't trust this macAddr, so we use my own attestation (i.e. I prove that I am who I am)
			String dist = "0";
			TrustNode me = new TrustNode(CryptoMain.getUuid(ctx), false, db);
			me.setAttest(CryptoMain.generateSignature(ctx, CryptoMain.getPublicKeyString(ctx)));
			String attest = CryptoMain.generateAttestation(me, ctx);
			String myPK = CryptoMain.getPublicKeyString(ctx);
			return "Attestation from " + CryptoMain.getUuid(ctx) + ":\n\r\n\r" + CryptoMain.getUuid(ctx).trim() + "\n\r\n\r" + dist + "\n\r\n\r" + attest.trim() + "\n\r\n\r" + myPK.trim() + "\n\r\n\r";
		}
		else {
			String sharedMac = toConvince.getSource();
			TrustNode sharedNode = getTrustNode(sharedMac);
			if (sharedNode == null) sharedNode = getTrustNode(macAddr); // this probably shouldn't happen
			String dist = String.valueOf(sharedNode.getDistance());
			String attest = CryptoMain.generateAttestation(sharedNode, ctx);
			String myPK = CryptoMain.getPublicKeyString(ctx);
			return "Attestation from " + CryptoMain.getUuid(ctx) + ":\n\r\n\r" + sharedMac.trim() + "\n\r\n\r" + dist + "\n\r\n\r" + attest.trim() + "\n\r\n\r" + myPK.trim() + "\n\r\n\r";
		}
	}
	
	public TrustNode readAttestation(String macAddr, String attest) {
		Log.i("BluetoothExchangeUtils 192", attest);
		String[] parts = attest.split("\n\r\n\r");
		if (parts == null || parts.length < 5) {
			Log.i("BluetoothExchangeUtils 195", "parts was null");
			return null;
		}
		String sharedMacAddr = parts[1];
		Log.i("BluetoothExchangeUtils 199", "sharedMacAddr: " + sharedMacAddr);
		int sharedDist = Integer.parseInt(parts[2]);
		Log.i("BluetoothExchangeUtils 101", "sharedDist: " + sharedDist);
		TrustNode sharedNode = getTrustNode(sharedMacAddr);
		if (sharedNode == null || sharedNode.getDistance() == Integer.MAX_VALUE) {
			Log.i("BluetoothExchangeUtils 204", "shared node doesn't exist");
			return null;
		}
		String plaintextAttest = CryptoMain.decryptAttestation(sharedNode, ctx, parts[3]);
		if (sharedNode.getPubkey().contains(plaintextAttest)) {
			Log.i("BluetoothExchangeUtils 208", "contents were verified!");
			sharedDist += sharedNode.getDistance();
			TrustNode newNode = new TrustNode(macAddr, false, db);
			if (newNode.getDistance() == Integer.MAX_VALUE) {
				newNode.setPubkey(parts[4]);
				newNode.setDistance(sharedDist);
			}
			Log.i("BluetoothExchangeUtils 215", "Attestation was valid from " + macAddr);
			return newNode;
		}
		Log.i("BluetoothExchangeUtils 218", "Invalid attestation");
		return null;
	}

}
