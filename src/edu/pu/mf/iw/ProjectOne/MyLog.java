package edu.pu.mf.iw.ProjectOne;

import android.util.Log;
import java.net.Socket;
import java.net.InetAddress;
import java.io.OutputStream;
import java.io.InputStream;

public class MyLog {
	
	private class LogThread extends Thread {
		
		private int PORT = 5840;
		
		private long interval = -1;
		private boolean toRun = true;
		
		public LogThread(long interval) {
			this.interval = interval;
		}
		
		public void run() {
			while (toRun) {
				try {
					sleep(interval);
				}
				catch (Exception e) {
					toRun = false;
				}
				sendLog();
			}
		}
		
		private void sendLog() {
			if (PORT == -1) return;
			if (!MyLog.this.toLog) return;
			try {
				InetAddress addr = InetAddress.getAllByName("128.112.7.80")[0];
				Socket sock = new Socket(addr, PORT);
				Log.i("MyLog 37", String.valueOf(sock.getLocalPort()));
				OutputStream fos = sock.getOutputStream();
				fos.write(MyLog.this.currentLog.getBytes());
				InputStream fis = sock.getInputStream();
				byte[] buf = new byte[1024];
				String aggregate = "";
				int read = 0;
				while ((read = fis.read(buf)) == buf.length) {
					aggregate += new String(buf);
					buf = new byte[1024];
				}
				byte[] aggBuf = new byte[read];
				for (int i = 0; i < read; i++) {
					aggBuf[i] = buf[i];
				}
				aggregate += new String(aggBuf);
				if (aggregate.toLowerCase().contains("append successful")) {
					MyLog.this.currentLog = "";
				}
			}
			catch (Exception e) {
				Log.e("MyLog 41", "error in sendLog", e);
			}
		}
		
		public void cancel() {
			toRun = false;
			interrupt();
		}
		
	}
	
	private String currentLog = "";
	private String loggerUUID;
	private final long INTERVAL = 10*60*1000; // 10 minutes in millis
	private LogThread logThread;
	private boolean toLog = false;
	
	public MyLog(String loggerUUID) {
		this.loggerUUID = new String(loggerUUID.getBytes());
		logThread = new LogThread(INTERVAL);
		if (!logThread.isAlive()) {
			logThread.start();
		}
	}
	
	/* Adds an entry to the (currently unsynced) log. This is in the form of
	 * a new line that has data formatted as follows:
	 * <LOGGER_UUID>:::<INFO_UUID>:::<INFO_DISTANCE>:::<INFO_SOURCE>:::<INFO_PUBKEY>:::<INFO_ATTEST_EXISTS>
	 */
	public void addEntry(TrustNode node) {
		currentLog += loggerUUID + ":::" + node.getUuid() + ":::" + 
				node.getDistance() + ":::" + node.getSource() + ":::" + 
				node.getPubkey() + ":::" + (node.getAttest() != null) + "\n";
	}
	
	public void cancelLogThread() {
		logThread.cancel();
	}

}
