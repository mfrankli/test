package edu.pu.mf.iw.ProjectOne;

import android.content.Context;

public class TabbedAttestation {
	
	public String c;
	public String t;
	public String reqId;
	
	public TabbedAttestation(String c, String t, String reqId) {
		this.c = c;
		this.t = t;
		this.reqId = reqId;
	}
	
	public TrustNode checkMatch(Context ctx, TabbedAttestation other) {
		String toVerify = CryptoMain.decryptedAttestation(ctx, other);
		return null;
	}
}
