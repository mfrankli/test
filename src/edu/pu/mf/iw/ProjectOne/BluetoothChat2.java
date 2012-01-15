package edu.pu.mf.iw.ProjectOne;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
//import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import java.util.LinkedList;

public class BluetoothChat2 extends Service {
	
	public static final String KEY_EXTRA = "key_extra";
	
	// This implements the thread that periodically searches for Bluetooth devices
	private class DiscoveryThread extends Thread {
		private BluetoothAdapter ba; private long toWake; 
		private long interval; private boolean toRun = true;
		public DiscoveryThread(BluetoothAdapter ba, long interval) {
			this.ba = ba; this.interval = interval; }
		@Override
		public void run() {
			toWake = System.currentTimeMillis() + interval;
			while (toRun) {
				// this is to make sure that even if it gets interrupted
				// it will still sleep for at least half the intended time
				sleepFunction(); toWake += interval;
				ba.cancelDiscovery(); ba.startDiscovery();
			}
		}
		
		// set the thread to die
		public void cancel() {
			toRun = false; interrupt();
		}
		
		private void sleepFunction() {
			try { sleep(toWake - System.currentTimeMillis());}
			catch (InterruptedException e) {
				if (!toRun) return;
			}
		}
		
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	// Handle Bluetooth events (ie device_found and discovery_finished)
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device == null) return;
				if (!nbc.isBlacklisted(device.getAddress())) {
					if (startOption == SINGLE_CONNECT && device.getAddress().equals(connectAddr)) {
						discoveredDevices.addLast(device);
					}
					else discoveredDevices.addLast(device);
					if (device.getName() != null) Log.i("BluetoothChat2 69", device.getName());
					else Log.i("BluetoothChat2 70", device.getAddress());
				}
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if (discoveredDevices.size() > 0) connectToDevices();
			}
			else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				// reset the list of discovered devices when discovery starts
				discoveredDevices = new LinkedList<BluetoothDevice>();
			}
		}
	};
	
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Log.i("BluetoothChat2 85", "Received a message");
        	nbc.inform(msg);
        	String macAddr = "";
            switch (msg.what) {
            	case BluetoothChatService2.MESSAGE_READ:
            		macAddr = msg.getData().getString(BluetoothChatService2.MAC_ADDR);
            		notifyUser(msg.what, macAddr);
            		String contents = new String((byte[]) msg.obj);
                	beu.updatedCloseConditions(macAddr, contents);
            		Log.i("BluetoothChat2 94", "macAddr: " + macAddr);
            		TrustNode sender = beu.getTrustNode(macAddr);
            		Log.i("BluetoothChat2 96", contents);
            		if (sender == null) { // sender is not trusted
            			Log.i("BluetoothChat2 99", "sender is null: " + macAddr);
            			if (contents.toLowerCase().contains("attestation from")) {
            				String[] parts = contents.split("\n\r\n\r");
            				if (parts != null && parts.length != 3) {
            					// parts[1] is the uuid, parts[2] is the signed content
            					sender = readAttestation(parts[1], parts[2]);
            				}
            				if (sender == null) {
            					Log.i("BluetoothChat2 103", "I don't trust you: " + macAddr);
            					String toWrite = "I don't trust you!\n" + beu.getReqId(macAddr) + "\n";
            					bcs.write(toWrite, macAddr);
            				}
            				else {
            					Log.i("BluetoothChat2 107", "I trust you! " + macAddr);
            					sender.commitTrustNode(); // redundant
            					if (LOG) myLog.addEntry(sender);
            					bcs.write("attestation acknowledged", macAddr);
            				}
            			}
            			else if (contents.toLowerCase().contains("tabbed attestations")) {
            				sender = readTabbedAttestations(contents.split("\n", 2)[1]);
            				if (sender == null) {
            					Log.i("BluetoothChat2 103", "I don't trust you: " + macAddr);
            					String toWrite = "I don't trust you!\n" + beu.getReqId(macAddr) + "\n";
            					bcs.write(toWrite, macAddr);
            				}
            				else {
            					Log.i("BluetoothChat2 107", "I trust you! " + macAddr);
            					sender.commitTrustNode();
            					if (LOG) myLog.addEntry(sender);
            					bcs.write("attestation acknowledged", macAddr);
            				}
            			}
            			else if (contents.startsWith("I don't trust you!")) {
            				String reqId = contents.split("\n")[1];
            				Log.i("BluetoothChat2 120", reqId);
            				sendTabbedAttestations(reqId, macAddr);
            				String toWrite = "I don't trust you!\n" + beu.getReqId(macAddr) + "\n";
            				bcs.write(toWrite, macAddr);
            			}
            			else {
            				bcs.write("I don't trust you!\n" + beu.getReqId(macAddr) + "\n", macAddr);
            			}
            		}
            		else { // sender is trusted
            			if (contents.toLowerCase().contains("attestation from")) {
            				Log.i("BluetoothChat2 123", "attestation acknowledged from " + macAddr);
            				bcs.write("Attestation acknowledged", macAddr);
            			}
            			else if (contents.toLowerCase().contains("i don't trust you!")) {
            				sendTabbedAttestations(contents.split("\n")[1], macAddr);
            			}
            			else if (contents.toLowerCase().contains("attestation acknowledged")) {
            				// todo: verify sender
            				sendMessages(macAddr);
            			}
            			else {
            				beu.handleMessage(contents, sender);
            			}
            		}
            	case BluetoothChatService2.CONNECT_ERROR:
            		macAddr = msg.getData().getString(BluetoothChatService2.MAC_ADDR);
            		notifyUser(msg.what, macAddr);
            		return;
            	case BluetoothChatService2.CONNECTED_ERROR:
            		macAddr = msg.getData().getString(BluetoothChatService2.MAC_ADDR);
            		notifyUser(msg.what, macAddr);
            		return;
            	case BluetoothChatService2.CONNECTED_SUCCESS:
            		macAddr = msg.getData().getString(BluetoothChatService2.MAC_ADDR);
            		Log.i("BluetoothChat2 143", "got connected message with " + macAddr);
            		sendAttestation(macAddr);
            		notifyUser(msg.what, macAddr);
            }
            if (beu.shouldBeClosed(macAddr)) {
            	bcs.close(macAddr);
            }
        }
    };
	
	private final int DEFAULT_INTERVAL = 45*60*1000; // 45 minutes
	private final boolean LOG = false;
	private final int SINGLE_CONNECT = 0;
	
	private TrustDbAdapter db;
	private BluetoothChatService2 bcs;
	private DiscoveryThread discoveryThread;
	private BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
	private BluetoothExchangeUtils beu;
	private NegativeBluetoothCache nbc = null;
	private NegCacheDbAdapter nbcDb = null;
	private MyLog myLog;
	private int startOption;
	private String connectAddr;
	
	private LinkedList<BluetoothDevice> discoveredDevices = new LinkedList<BluetoothDevice>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d("BluetoothChat2 74", "In onCreate");
		db = new TrustDbAdapter(this);
		db.open();
		discoveryThread = new DiscoveryThread(BluetoothAdapter.getDefaultAdapter(), DEFAULT_INTERVAL);
		if (ba == null) { // Bluetooth isn't supported
			Log.e("BluetoothChat2 86", "ba is null");
			stopSelf();
			return;
		}
		myLog = new MyLog(CryptoMain.getUuid(this));
		beu = new BluetoothExchangeUtils(db, this, myLog);
		nbcDb = new NegCacheDbAdapter(this);
		nbcDb.open();
		nbc = new NegativeBluetoothCache(nbcDb);
		startOption = 1;
		connectAddr = null;
		bcs = new BluetoothChatService2(mHandler, 60, this);
	}
	
	@Override
	public int onStartCommand(Intent myIntent, int flags, int startid) {
		Log.i("BluetoothChat2 219", "in onStartCommand");
		if (!db.isOpen()) {
			db.open();
		}
		if (!nbcDb.isOpen()) {
			nbcDb.open();
		}
		ensureDiscoverable();
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        this.registerReceiver(mReceiver, filter);
        if (!discoveryThread.isAlive()) discoveryThread.start();
        if (!ba.isEnabled()) { // make sure Bluetooth is enabled
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivity(enableBtIntent);
		}
        String singleMac = myIntent.getStringExtra(KEY_EXTRA);
        if (singleMac != null) {
        	BluetoothDevice device = ba.getRemoteDevice(singleMac);
        	bcs.connectToDevice(device);
        }
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Log.i("BluetoothChat2 155", "in onDestroy");
		super.onDestroy();
		if (db != null) db.close();
		if (bcs != null) bcs.close();
		if (nbcDb != null && nbcDb.isOpen()) nbcDb.close();
		if (discoveryThread != null) discoveryThread.cancel();
		if (myLog != null) myLog.cancelLogThread();
		try {
        	unregisterReceiver(mReceiver);
        }
        catch (Exception e) {
        	
        }
	}
	
	private void connectToDevices() {
		for (int i = 0; i < 6 && i < discoveredDevices.size(); i++) {
			BluetoothDevice current = discoveredDevices.removeFirst();
			Log.i("BluetoothChat2 164", "connecting to " + current.getName());
			bcs.connectToDevice(current);
			Log.i("BluetoothChat2 164", "Finished connecting to " + current.getName());
			discoveredDevices.addLast(current);
		}
	}
	
	private void sendMessages(String macAddr) {
		TrustNode receiver = beu.getTrustNode(macAddr);
		if (receiver != null) {
			String[] messages = beu.generateMessageList();
			for (int i = 0; i < messages.length; i++) {
				bcs.write(messages[i], macAddr);
			}
		}
		else {
			bcs.write("I don't trust you!", macAddr);
		}
	}
	
	private boolean sendAttestation(String macAddr) {
		String attestation;
		attestation = beu.getMyAttestation(macAddr);
		if (attestation == null) return false;
		Log.i("BluetoothChat2 220", attestation);
		bcs.write(attestation, macAddr);
		return true;
	}
	
	private void sendTabbedAttestations(String reqId, String macAddr) {
		String[] tabbedAttestations = beu.getTabbedAttestations(reqId);
		String uuid = CryptoMain.getUuid(this);
		String pubkey = CryptoMain.getPublicKeyString(this);
		String metadata = reqId + ":::::" + uuid + ":::::" + pubkey;
		if (tabbedAttestations != null && tabbedAttestations.length > 0) {
			tabbedAttestations[0] = metadata + "/////" + tabbedAttestations[0];
		}
		String toWrite = "tabbed attestations from " + uuid + "\n\r\n\r";
		for (int i = 0; i < tabbedAttestations.length; i++) {
			toWrite += tabbedAttestations[i] + "\n\r\n\r";
		}
		bcs.write(toWrite, macAddr);
	}
	
	private TrustNode readTabbedAttestations(String tabbedAttestations) {
		tabbedAttestations = tabbedAttestations.trim();
		Log.i("BluetoothChat2 297", tabbedAttestations);
		String[] tabbedAttestationArray = tabbedAttestations.split("\n\r\n\r");
		if (tabbedAttestationArray != null && tabbedAttestationArray.length > 0) {
			String[] parts = tabbedAttestationArray[0].split("/////", 2);
			Log.i("BluetoothChat2 301", tabbedAttestationArray[0] + "\ncontains sequence: " + tabbedAttestationArray[0].contains("/////"));
			Log.i("BluetoothChat2 302", parts[0] + "\n" + parts[1]);
			if (parts == null || parts.length < 2) {
				Log.i("BluetoothChat2 303", "parts is null or too short: " + parts);
				return null;
			}
			tabbedAttestationArray[0] = parts[1];
			String metadata = parts[0];
			parts = metadata.split(":::::");
			if (parts != null && parts.length == 3) {
				String claim = parts[1];
				String reqId = parts[0];
				TrustNode shared = beu.checkTabbedAttestations(tabbedAttestationArray, reqId, claim);
				if (shared != null) {
					TrustNode newNode = new TrustNode(claim.trim(), false, db);
					newNode.setPubkey(parts[2]);
					newNode.setDistance(shared.getDistance() + 1);
					return newNode;
				}
				else {
					return null;
				}
			}
			else {
				Log.i("BluetoothChat2 311", "something wrong with metadata: " + metadata);
				return null;
			}
		}
		else {
			return null;
		}
		
	}
	
	// give this function a uuid, and the string that is claimed to be a signature on the uuid
	private TrustNode readAttestation(String uuid, String attestation) {
		TrustNode node = new TrustNode(uuid, false, db);
		if (node == null || node.getDistance() == Integer.MAX_VALUE) return null;
		if (CryptoMain.verifySignature(node.getPubkey(), attestation, uuid)) {
			return node;
		}
		return null;
	}
	
	private void ensureDiscoverable() {
        if (ba.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
      	  	discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(discoverableIntent);
        }
	}
	
	private void notifyUser(int flag, String macAddr) {
		String event = "";
		if (flag == BluetoothChatService2.MESSAGE_READ) event = "Message read from: ";
		if (flag == BluetoothChatService2.CONNECTED_SUCCESS) event = "Connected to ";
		if (flag == BluetoothChatService2.CONNECT_ERROR) event = "Failed to connect to ";
		if (flag == BluetoothChatService2.CONNECTED_ERROR) event = "Lost connection with ";
		//Context ctx = getApplicationContext();
		//Toast toast = Toast.makeText(ctx, event + macAddr, Toast.LENGTH_SHORT);
    	//toast.show();
    	Log.i("BluetoothChat2 273", event + macAddr);
	}
}
