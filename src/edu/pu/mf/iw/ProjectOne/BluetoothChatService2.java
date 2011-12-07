/* BluetoothChatService2 provides the abstraction of reads/writes from/to
 * MAC Addresses (of Bluetooth Adapters) by coordinating the threads that
 * implement the actual communication. It coordinates the various threads 
 * used for created, accepting, and managing connections.  
 */

/*
 * Issues: I'm not sure what will happen as a result of making every method
 * synchronized. Hopefully, it will eliminate errors without a crippling hit
 * to the performance.
 */

package edu.pu.mf.iw.ProjectOne;

import android.bluetooth.*;
import java.util.TreeMap;
import java.util.LinkedList;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import java.util.UUID;
import android.content.Context;

public class BluetoothChatService2 {
	
	public final static String MAC_ADDR = "READING_MAC_ADDR";
	public final static UUID UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
	public final static String NAME_INSECURE = "BluetoothChatInsecure";
	public final static int MESSAGE_READ = 1;
	public final static int CONNECT_ERROR = 2;
	public final static int CONNECTED_ERROR = 3;
	public final static int CONNECTED_SUCCESS = 4;
	public final static byte EOM = -1;
	
	private int x = 0;
	private Handler handler;
	private Context ctx;
	
	private TreeMap<String, ConnectThread> nameToConnect = new TreeMap<String, ConnectThread>();
	private AcceptThread accept = null;
	private TreeMap<String, BCSNode> macAddrToThreads = new TreeMap<String, BCSNode>();
	private TreeMap<String, String> connectedToMacAddr = new TreeMap<String, String>();
	private TreeMap<String, Integer> nameToIntId = new TreeMap<String, Integer>();
	private TreeMap<String, String> uuidToMac = new TreeMap<String, String>();
	private TreeMap<String, LinkedList<byte []>> uuidToBufferedRead = new TreeMap<String, LinkedList<byte []>>();
	
	// timeoutLength is in seconds (not millis). This is a bit of a weird way
	// around the fact that I'm calling it from a class that can't call
	// activities for a result (i.e. asynch bluetooth enabling)
	public BluetoothChatService2(Handler handler, long timeoutLength, Context ctx) {
		this.handler = handler;
		this.ctx = ctx;
		long timeStarted = System.currentTimeMillis();
		while (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			if (System.currentTimeMillis() > (timeStarted + timeoutLength*1000)) {
				return;
			}
		}
		accept = new AcceptThread(this);
		accept.start();
		Log.i("BluetoothChatService2 51", "accept.start() called");
	}
	
	/* These methods are used by the client that instantiated this BCS2 */
	
	// This method attempts to connect to the device given as the argument.
	// The return value of this method represents that both sockets were
	// successfully initialized, as opposed to actually successfully connected.
	// That information is provided asynchronously as a Message.
	public synchronized boolean connectToDevice(BluetoothDevice device) {
		if (accept == null) {
			Log.i("BluetoothChatService2 63", "accept is null");
			if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
				accept = new AcceptThread(this);
				accept.start();
			}
			else return false;
		}
		ConnectThread connect = new ConnectThread(this, device);
		nameToConnect.put(connect.toString(), connect);
		macAddrToThreads.put(device.getAddress(), new BCSNode(connect, accept, null));
		if (connect.success && accept.success) {
			connect.start();
			return true;
		}
		else {
			nameToConnect.remove(connect.toString());
			macAddrToThreads.remove(device.getAddress());
			return false;
		}
	}
	
	// Writes the string in toWrite to the device at macAddr. Returns false
	// if there was some error (e.g. device is not connected yet).
	public synchronized boolean write(String toWrite, String macAddr) {
		if (uuidToMac.containsKey(macAddr)) macAddr = uuidToMac.get(macAddr);
		toWrite = CryptoMain.getUuid(ctx) + "\n" + toWrite + EOM;
		if (toWrite.length() == 1024) {
			toWrite += "\n\r\n\rpadding";
		}
		byte[] buf = toWrite.getBytes();
		byte[] buf2 = new byte[buf.length + 1];
		int i;
		for (i = 0; i < buf.length; i++) {
			buf2[i] = buf[i];
		}
		buf2[i] = -1;
		if (macAddr == null) return false;
		BCSNode thisNode = macAddrToThreads.get(macAddr);
		// ensure that this mac address is one to which we have connected
		if (thisNode == null || thisNode.connected == null) return false;
		thisNode.connected.write(buf2);
		return true;
	}
	
	// This method handles the passing of data up to the creator. It specifies
	// the MAC Address from which this message was sent in the data Bundle
	// with the key "MAC_ADDR"
	private synchronized void sendRead(String threadString, int length, byte[] buf) {
		String read = new String(buf);
		String macAddr = read.split("\n", 2)[0];
		buf = read.split("\n", 2)[1].getBytes();
		uuidToMac.put(macAddr, connectedToMacAddr.get(threadString));
		if (length == 1024){
			Log.i("BluetoothChatService2 123", "buffering read from " + macAddr);
			// buffer the read
			LinkedList<byte []> list = uuidToBufferedRead.get(macAddr);
			if (list != null) {
				list.add(buf);
			}
			else {
				list = new LinkedList<byte []>();
				list.add(buf);
			}
			uuidToBufferedRead.put(macAddr, list);
			return;
		}
		else {
			LinkedList<byte []> chunks = uuidToBufferedRead.get(macAddr);
			byte[] toReturn = new byte[length];
			for (int i = 0; i < length; i++) {
				toReturn[i] = buf[i];
			}
			if (chunks == null) {
				buf = toReturn;
			}
			else {
				String accum = "";
				for (byte[] current : chunks) {
					accum += new String(current);
				}
				accum += toReturn;
				buf = accum.getBytes();
			}
		}
		Bundle b = new Bundle();
		b.putString(MAC_ADDR, macAddr);
		Message m = handler.obtainMessage(MESSAGE_READ, buf);
		m.setData(b);
		m.sendToTarget();
	}
	
	public synchronized void close(String macAddr) {
		BCSNode thisNode = macAddrToThreads.get(macAddr);
		if (thisNode.connect.isAlive()) thisNode.connect.cancel();
		if (thisNode.accept.isAlive()) thisNode.accept.cancel();
		if (thisNode.connected != null && thisNode.connected.isAlive()) thisNode.connected.cancel();
	}
	
	public synchronized void close() {
		for (String key : macAddrToThreads.keySet()) {
			BCSNode current = macAddrToThreads.get(key);
			if (current.connect.isAlive()) current.connect.cancel();
			if (current.accept.isAlive()) current.accept.cancel();
			if (current.connected != null && current.connected.isAlive()) current.connected.cancel();
		}
	}
	
	/* These methods are used by the threads to communicate with the BCS2
	 * 
	 * In each method, we check that the argument is a valid thread being
	 * maintained by this instance of BCS.
	 */
	
	public synchronized boolean acceptIsAlive(String threadString) {
		if (!nameToConnect.containsKey(threadString)) {
			return false;
		}
		return (accept != null && accept.isAlive());
	}
	
	// Display an error Toast for the user and send a message to the caller
	public synchronized void connectionFailed(String failedThreadString, Exception e) {
		Log.e("BluetoothChatService 151", "connectionFailed", e);
		Log.e("BluetoothChatService 151", nameToConnect.get(failedThreadString) + " " + connectedToMacAddr.get(failedThreadString));
		if (!nameToConnect.containsKey(failedThreadString)) {
			return;
		}
		ConnectThread connect = nameToConnect.get(failedThreadString);
    	sendErrorMessage(connect.getInfo(), CONNECT_ERROR);
	}
	
	private void sendErrorMessage(String macAddr, int errorCode) {
		Bundle b = new Bundle();
		b.putString(MAC_ADDR, macAddr);
		Message m = handler.obtainMessage(errorCode);
		m.setData(b);
		m.sendToTarget();
		Log.i("BluetoothChatService2 146", macAddr + " " + errorCode);
	}
	
	public synchronized String getId(String threadString) {
		if (nameToIntId.containsKey(threadString)) {
			return String.valueOf(nameToIntId.get(threadString));
		}
		return String.valueOf(x++);
	}
	
	public synchronized void connectedThread(String threadString, BluetoothSocket socket, BluetoothDevice device) {
		Log.i("BluetoothChatService2 163", "Socket opened with " + socket.getRemoteDevice().getName());
		if (!nameToConnect.containsKey(threadString) && !accept.toString().equals(threadString)) {
			Log.i("BluetoothChatService2 165", "Couldn't find the thread name");
			return;
		}
		BCSNode thisNode = macAddrToThreads.get(device.getAddress());
		ConnectedThread connected = new ConnectedThread(this, socket);
		if (nameToConnect.containsKey(threadString)) {
			// We are the client in this case
			if (thisNode == null) return;
			accept.interrupt();
		}
		else {
			// We are the server
			if (thisNode == null) return;
			ConnectThread connect = thisNode.connect;
			connect.cancel();
			nameToConnect.remove(connect.toString());
		}
		thisNode.connected = connected;
		connected.start();
		connectedToMacAddr.put(connected.toString(), device.getAddress());
		Log.i("BluetoothChatService2 176", "connected thread started to " + device.getAddress() + " aka " + device.getName());
		macAddrToThreads.put(device.getAddress(), thisNode);
	}
	
	public void sendConnectedMessage(String macAddr) {
		Bundle b = new Bundle();
		b.putString(MAC_ADDR, macAddr);
		Message m = handler.obtainMessage(CONNECTED_SUCCESS);
		m.setData(b);
		m.sendToTarget();
	}
	
	public synchronized void bytesRead(String threadString, int length, byte[] buf) {
		sendRead(threadString, length, buf);
	}
	
	public synchronized void connectionLost(String threadString) {
		if (!connectedToMacAddr.containsKey(threadString)) {
			return;
		}
		String macAddr = connectedToMacAddr.get(threadString);
    	sendErrorMessage(macAddr, CONNECTED_ERROR);
	}
	
	// Tuple of ConnectThread, AcceptThread, and ConnectedThread
	
	private class BCSNode {
		
		public ConnectThread connect;
		public AcceptThread accept;
		public ConnectedThread connected;
		
		public BCSNode(ConnectThread connect, AcceptThread accept, ConnectedThread connected) {
			this.connect = connect;
			this.accept = accept;
			this.connected = connected;
		}
	}
}
