package edu.pu.mf.iw.ProjectOne;

import android.database.Cursor;
import android.util.Log;
import android.content.Context;

public class BluetoothExchangeUtils {
	
	public long INTERVAL = 1*60*1000; //10 minutes soft state length
	
	private TrustDbAdapter db;
	private Context ctx;
	
	public BluetoothExchangeUtils(TrustDbAdapter db, Context ctx) {
		this.db = db;
		this.ctx = ctx;
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
    	if (pubIndex != -1) {
    		pubKey = columns[pubIndex];
    		if (toCommit != null) {
    			toCommit.setPubkey(pubKey);
    		}
    	}
    	else {
    		pubKey = null;
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
    		toCommit.setSource(source);
    	}
    	toCommit.commitTrustNode();
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
	 * - SIG-RSA_sk-shared(pk-shared)
	 * - public key of claiming node
	 */
	public String getAttestation(String macAddr) {
		TrustNode toConvince = getTrustNode(macAddr);
		if (toConvince == null) { // we don't trust this macAddr
			return null;
		}
		String sharedMac = null;
		if (toConvince != null) {
			sharedMac = toConvince.getSource();
		}
		if (sharedMac.equals(CryptoMain.getUuid(ctx))) {
			// this means that I am the shared node
			String dist = "0";
			String attest = CryptoMain.generateSignature(ctx, CryptoMain.getPublicKeyString(ctx));
			String myPK = CryptoMain.getPublicKeyString(ctx);
			return "Attestation from " + CryptoMain.getUuid(ctx) + ":\n\r\n\r" + sharedMac.trim() + "\n\r\n\r" + dist + "\n\r\n\r" + attest.trim() + "\n\r\n\r" + myPK.trim() + "\n\r\n\r";
		}
		TrustNode sharedNode = getTrustNode(sharedMac);
		if (sharedNode == null) sharedNode = getTrustNode(macAddr);
		String dist = String.valueOf(sharedNode.getDistance());
		String attest = sharedNode.getAttest();
		String myPK = CryptoMain.getPublicKeyString(ctx);
		return "Attestation from " + CryptoMain.getUuid(ctx) + ":\n\r\n\r" + sharedMac.trim() + "\n\r\n\r" + dist + "\n\r\n\r" + attest.trim() + "\n\r\n\r" + myPK.trim() + "\n\r\n\r";
	}
	
	public TrustNode readAttestation(String macAddr, String attest) {
		Log.i("BluetoothExchangeUtils 192", "Attesation from " + macAddr + ":\n" + attest);
		String[] parts = attest.split("\n\r\n\r");
		if (parts == null) {
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
		if (sharedNode.verifyContent(sharedNode.getPubkey(), parts[3])) {
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
