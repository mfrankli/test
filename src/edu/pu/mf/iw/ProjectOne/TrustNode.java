package edu.pu.mf.iw.ProjectOne;

import android.database.Cursor;
import android.util.Log;

public class TrustNode {
	
	private TrustDbAdapter db;
	private String pubKey; // public key
	private int distance; // distance to node
	private String uuid; // internally will be bluetooth mac addr
	private String name; // human-readable name
	private String source; // node who informed us of this node
	private String attest; // self-signed public key
	
	public TrustNode(TrustDbAdapter db) {
		this.db = db;
		pubKey = null;
		distance = Integer.MAX_VALUE;
		uuid = null;
		name = null;
		source = null;
		attest = null;
	}
	
	public TrustNode(String key, boolean isPubKey, TrustDbAdapter db) {
		this.db = db;
		if (db != null && !db.isOpen()) db.open();
		Cursor c;
		if (isPubKey) c = db.selectEntryByKey(key);
		else c = db.selectEntryByUuid(key);
		if (c != null && c.getCount() != 0) {
			Log.i("TrustNode 32", "c.getCount(): " + c.getCount());
			int uuidIndex = c.getColumnIndex(TrustDbAdapter.KEY_UUID);
			int distIndex = c.getColumnIndex(TrustDbAdapter.KEY_DIST);
			int pubKeyIndex = c.getColumnIndex(TrustDbAdapter.KEY_PUBKEY);
			int sourceIndex = c.getColumnIndex(TrustDbAdapter.KEY_SOURCE);
			int attestIndex = c.getColumnIndex(TrustDbAdapter.KEY_ATTEST);
			if (isPubKey) {
				pubKey = key;
				uuid = c.getString(uuidIndex);
			}
			else {
				pubKey = c.getString(pubKeyIndex);
				uuid = key;
			}
			if (distIndex != -1) distance = c.getInt(distIndex);
			if (sourceIndex != -1) source = c.getString(sourceIndex);
			if (attestIndex != -1) attest = c.getString(attestIndex);
		}
		else {
			if (isPubKey) {
				pubKey = key;
				uuid = null;
			}
			else {
				pubKey = null;
				uuid = key;
			}
			distance = Integer.MAX_VALUE;
			source = null;
			attest = null;
		}
	}
	
	// Verifies that content was sent by this entity
	// i.e. identified either by the uuid or public key
	// passed in the constructor that was called
	public boolean verifyContent(String content, String claim) {
		Log.i("TrustNode 66", "Content:\n" + content);
		Log.i("TrustNode 67", "Claim:\n" + claim);
		return CryptoMain.verifySignature(pubKey, content, claim);
	}
	
	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	public int getDistance() {
		return distance;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public String getPubkey() {
		return pubKey;
	}
	
	public void setPubkey(String pubKey) {
		this.pubKey = pubKey;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSource() {
		return source;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public String getAttest() {
		return attest;
	}
	
	public void setAttest(String attest) {
		this.attest = attest;
	}
	
	public boolean commitTrustNode() {
		if (pubKey != null && uuid != null) {
			if (!db.updateTrustEntry(pubKey, distance, uuid, name, source, attest))
				return (db.createTrustEntry(pubKey, distance, uuid, name, source, attest) != -1);
			return true;
		}
		return false;
	}
	
}
