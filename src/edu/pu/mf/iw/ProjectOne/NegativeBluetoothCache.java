package edu.pu.mf.iw.ProjectOne;

import android.os.Message;
import android.util.Log;
import android.database.Cursor;

public class NegativeBluetoothCache {
	
	private class NBCNode {
		
		public String macAddr = null;
		public int numFailsSince;
		public long lastFailTime;
		public long lastSuccessTime;
		
		public NBCNode(String macAddr) {
			this.macAddr = macAddr;
			Cursor c = NegativeBluetoothCache.this.db.selectEntryByUuid(macAddr);
			if (c != null && c.moveToFirst() && c.getCount() > 0) {
				//Log.i("NegativeBluetoothCache 23", "cursor is not null. count is " + c.getCount());
				int numFailsIndex = c.getColumnIndex(NegCacheDbAdapter.KEY_FAILS);
				int lastFailIndex = c.getColumnIndex(NegCacheDbAdapter.KEY_LAST_FAIL);
				int lastSucceedIndex = c.getColumnIndex(NegCacheDbAdapter.KEY_LAST_SUCCEED);
				if (numFailsIndex != -1) numFailsSince = c.getInt(numFailsIndex);
				else numFailsSince = 0;
				if (lastFailIndex != -1) lastFailTime = c.getLong(lastFailIndex);
				else lastFailTime = System.currentTimeMillis();
				if (lastSucceedIndex != -1) lastSuccessTime = c.getLong(lastSucceedIndex);
				else lastSuccessTime = System.currentTimeMillis();
			}
			else {
				Log.i("NegativeBluetoothCache 32", "cursor was null or empty");
				numFailsSince = 0;
				lastFailTime = System.currentTimeMillis();
				lastSuccessTime = System.currentTimeMillis();
			}
		}
		
		public void commit() {
			//Log.i("NegativeBluetoothCache 38", "Committing:\n" + this.toString());
			if (!NegativeBluetoothCache.this.db.updateCacheEntry(macAddr, numFailsSince, lastFailTime, lastSuccessTime)) {
				Log.i("NegativeBluetoothCache 39", "Creating");
				NegativeBluetoothCache.this.db.createCacheEntry(macAddr, numFailsSince, lastFailTime, lastSuccessTime);
			}
		}
		
		public String toString() {
			return macAddr + "\n" + numFailsSince + "\n" + lastFailTime + "\n" + lastSuccessTime + "\n";
		}
	}
	
	// soft state length
	public static final long SS_LENGTH = 1000*60*10; // 10 minutes
	private NegCacheDbAdapter db;
	
	public NegativeBluetoothCache(NegCacheDbAdapter db) {
		this.db = db;
		if (db != null && !db.isOpen()) db.open();
	}
	
	public synchronized void inform(Message msg) {
		if (!db.isOpen()) db.open();
		String macAddr = msg.getData().getString(BluetoothChatService2.MAC_ADDR);
		NBCNode node = new NBCNode(macAddr);
		//Log.i("NegativeBluetoothCache 63", node.toString());
		if (msg.what == BluetoothChatService2.CONNECT_ERROR) {
				node.lastFailTime = System.currentTimeMillis();
				node.numFailsSince++;
				node.commit();
				Log.i("NegativeBluetoothCache 77", "node " + node.macAddr + " has failed " + node.numFailsSince + " times\n");
		}
		else if (msg.what == BluetoothChatService2.CONNECTED_SUCCESS) {
				node.lastSuccessTime = System.currentTimeMillis();
				node.numFailsSince = 0;
				node.commit();
		}
	}
	
	public synchronized boolean isBlacklisted(String macAddr) {
		if (!db.isOpen()) db.open();
		if (macAddr == null) return false;
		NBCNode node = new NBCNode(macAddr);
		//Log.i("NegativeBluetoothCache 82", node.toString());
		if (node.lastFailTime + SS_LENGTH > System.currentTimeMillis()) {
			if (node.numFailsSince > 20) {
				return true;
			}
			return false;
		}
		else {
			Log.i("NegativeBluetoothCache 89", "numFailsSince: " + node.lastFailTime + "\ncurrent time: " + System.currentTimeMillis());
			node.numFailsSince = 0;
			node.commit();
			return false;
		}
	}
}
