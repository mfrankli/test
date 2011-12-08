package edu.pu.mf.iw.ProjectOne;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import java.util.List;
import java.util.ArrayList;
import android.util.Log;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;


public class MyLog {
	
	private class LogThread extends Thread {
		
		private final String URL = "http://www.shgfdas.com/this_is_not_a_real_url";
		
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
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(URL);
			try {
				List<NameValuePair> list = new ArrayList<NameValuePair>();
				list.add(new BasicNameValuePair("logger", "mike"));
				list.add(new BasicNameValuePair("sender_timestamp", String.valueOf(System.currentTimeMillis())));
				list.add(new BasicNameValuePair("contents", MyLog.this.currentLog));
				post.setEntity(new UrlEncodedFormEntity(list));
				HttpResponse response = client.execute(post);
				StatusLine status = response.getStatusLine();
				if (status.getStatusCode() == 200) {
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
