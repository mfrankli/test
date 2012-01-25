package edu.pu.mf.iw.ProjectOne;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private BluetoothChatService2 service;
    private boolean toRun = true;

    public ConnectedThread(BluetoothChatService2 service, BluetoothSocket socket) {
    	this.service = service;
        Log.d("connected thread 16", "create ConnectedThread");
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e("connected thread 28", "temp sockets not created", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
	public void run() {
        Log.i("connected thread 36", "BEGIN mConnectedThread");
        byte[] buffer = new byte[1024];
        int bytes;
        Log.i("ConnectedThread 40", "remote device address is " + mmSocket.getRemoteDevice().getAddress());
        service.sendConnectedMessage(mmSocket.getRemoteDevice().getAddress());
        // Keep listening to the InputStream while connected
        while (toRun) {
            try {
            	StringBuilder builder = new StringBuilder("");
            	int numBytes = 0;
                // Read from the InputStream
                while((bytes = mmInStream.read(buffer)) == 1024) {
                	builder.append(new String(buffer));
                	numBytes += bytes;
                }
                numBytes += bytes;
                builder.append(new String(buffer, 0, bytes));
                // Send the obtained bytes to the UI Activity
                service.bytesRead(this.toString(), numBytes, builder.toString());
            } catch (IOException e) {
                Log.e("connected thread 50", "disconnected", e);
                service.connectionLost(this.toString());
                toRun = false;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     * @param buffer  The bytes to write
     */
    public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);
        } catch (IOException e) {
            Log.e("connected thread 64", "Exception during write", e);
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
            toRun = false;
        } catch (IOException e) {
            Log.e("connected thread 72", "close() of connect socket failed", e);
        }
    }
}
