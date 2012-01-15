package edu.pu.mf.iw.ProjectOne;

import java.io.IOException;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class AcceptThread extends Thread {
    // The local server socket
	private static final boolean D = true;
    private final BluetoothServerSocket mmServerSocket;
    private BluetoothChatService2 service;
    private boolean toRun;
    
    public boolean success = false;

    public AcceptThread(BluetoothChatService2 service) {
    	this.service = service;
        BluetoothServerSocket tmp = null;

        // Create a new listening server socket
        try {
            tmp = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord(
                        BluetoothChatService2.NAME_INSECURE, BluetoothChatService2.UUID_INSECURE);
        } catch (IOException e) {
            Log.e("accept thread 25", "Socket listen() failed", e);
        }
        mmServerSocket = tmp;
        if (mmServerSocket != null) success = true;
        toRun = true;
    }

    @Override
	public void run() {
        if (D) Log.d("accept thread 32", "BEGIN mAcceptThread" + this);
        setName("AcceptThread" + service.getId(this.toString()));

        BluetoothSocket socket = null;

        // Listen to the server socket if we're not connected
        while (toRun) {
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e("accept thread 44", "accept() failed", e);
            }

            // If a connection was accepted
            if (socket != null) {
            	service.connectedThread(this.toString(), socket, socket.getRemoteDevice());
            }

        }
        if (D) Log.i("accept thread 52", "END mAcceptThread");

    }

    public void cancel() {
        if (D) Log.d("accept thread 57", "cancel " + this);
        try {
        	toRun = false;
            mmServerSocket.close();
            interrupt();
        } catch (IOException e) {
            Log.e("accept thread 62", "close() of server failed", e);
        }
    }
}