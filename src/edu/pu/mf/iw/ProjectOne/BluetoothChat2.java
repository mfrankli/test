package edu.pu.mf.iw.ProjectOne;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import java.util.LinkedList;

public class BluetoothChat2 extends Service {
	
	
	// This implements the thread that periodically searches for Bluetooth devices
	private class DiscoveryThread extends Thread {
		private BluetoothAdapter ba; private long toWake; 
		private long interval; private boolean toRun = true;
		public DiscoveryThread(BluetoothAdapter ba, long interval) {
			this.ba = ba; this.interval = interval; }
		public void run() {
			if (!ba.isEnabled()) { // make sure Bluetooth is enabled
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivity(enableBtIntent);
			}
			toWake = System.currentTimeMillis() + interval;
			while (toRun) {
				ba.cancelDiscovery(); ba.startDiscovery();
				// this is to make sure that even if it gets interrupted
				// it will still sleep for at least half the intended time
				sleepFunction(); toWake += interval;
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
				if ((toWake - System.currentTimeMillis())*2 < interval) sleepFunction();
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
				if (!nbc.isBlacklisted(device.getAddress())) { // check the negative cache
					discoveredDevices.addLast(device);
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
            		Log.d("BluetoothChat2 96", contents);
            		if (sender == null) { // sender is not trusted
            			Log.i("BluetoothChat2 99", "sender is null: " + macAddr);
            			if (contents.startsWith("Attestation from")) {
            				sender = beu.readAttestation(macAddr, contents);
            				if (sender == null) {
            					bcs.write("I don't trust you!", macAddr);
            				}
            				else {
            					sender.commitTrustNode();
            					if (LOG) myLog.addEntry(sender);
            					bcs.write("Attestation acknowledged", macAddr);
            				}
            			}
            			else if (contents.startsWith("I don't trust you!")) {
            				sendAttestation(macAddr);
            				bcs.write("I don't trust you!", macAddr);
            			}
            			else {
            				bcs.write("I don't trust you!", macAddr);
            			}
            		}
            		else { // sender is trusted
            			if (contents.toLowerCase().contains("attestation from")) {
            				bcs.write("Attestation acknowledged", macAddr);
            			}
            			else if (contents.toLowerCase().contains("i don't trust you!")) {
            				sendAttestation(macAddr);
            			}
            			else if (contents.toLowerCase().contains("attestation acknowledged")) {
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
	
	private TrustDbAdapter db;
	private BluetoothChatService2 bcs;
	private DiscoveryThread discoveryThread;
	private BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
	private BluetoothExchangeUtils beu;
	private NegativeBluetoothCache nbc = null;
	private NegCacheDbAdapter nbcDb = null;
	private MyLog myLog;
	
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
	}
	
	@Override
	public int onStartCommand(Intent myIntent, int flags, int startid) {
		Log.i("BluetoothChat2 166", "in onStartCommand");
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
        bcs = new BluetoothChatService2(mHandler, 60, this);
        if (!discoveryThread.isAlive()) discoveryThread.start();
		return START_STICKY;
	}
	
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
		attestation = beu.getAttestation(macAddr);
		Log.i("BluetoothChat2 220", attestation);
		if (attestation == null) return false;
		bcs.write(attestation, macAddr);
		return true;
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
		Context ctx = getApplicationContext();
		Toast toast = Toast.makeText(ctx, event + macAddr, Toast.LENGTH_SHORT);
    	toast.show();
    	Log.i("BluetoothChat2 245", event + macAddr);
	}
}
