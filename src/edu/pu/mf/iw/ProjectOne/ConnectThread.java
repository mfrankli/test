package edu.pu.mf.iw.ProjectOne;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.lang.reflect.Method;

import java.io.IOException;

public class ConnectThread extends Thread {
    private BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private BluetoothChatService2 service;
    
    public boolean success = false;

    public ConnectThread(BluetoothChatService2 service, BluetoothDevice device) {
    	this.service = service;
        mmDevice = device;
        BluetoothSocket tmp = null;

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(
                        BluetoothChatService2.UUID_INSECURE);
        } catch (IOException e) {
            Log.e("connect thread 25", "create() failed", e);
        }
        mmSocket = tmp;
        if (mmSocket != null) success = true;
    }

    @Override
	public void run() {
        Log.i("connect thread 33", "BEGIN mConnectThread");
        //setName("ConnectThread" + service.getId(this.toString()));

        // Always cancel discovery because it will slow down a connection
        //BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        // Make a connection to the BluetoothSocket
        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
        	Log.i("ConnectThread 44", "connecting to " + mmDevice.getName());
        	Log.i("ConnectThread 45", "accept thread is alive: " + service.acceptIsAlive(this.toString()));
            mmSocket.connect();
            Log.i("ConnectThread 47", "finished connecting to " + mmDevice.getName());
        } catch (IOException e) {
        	try {
        		Method m = mmDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});
                mmSocket = (BluetoothSocket) m.invoke(mmDevice, 1);
                mmSocket.connect();
	            // Close the socket
	            try {
	            	mmSocket.close();
	            } 
	            catch (IOException e2) {
	            	Log.e("connect thread 54", "unable to close() socket during connection failure", e);
	            }
        	}
        	catch (Exception e1) {
	            service.connectionFailed(this.toString(), e1);
	            return;
        	}
        }

        // Finish the thread with a connection
        service.connectedThread(this.toString(), mmSocket, mmDevice);

    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e("connect thread 72", "close() of connect socket failed", e);
        }
    }
    
    // Used for generating a meaningful error message
    public String getInfo() {
    	return mmDevice.getAddress();
    }
}
