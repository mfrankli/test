package edu.pu.mf.iw.ProjectOne;

import android.content.Context;
import android.util.Log;

public class TabbedAttestation {
	
	public String c;
	public String t;
	public String reqId;
	
	public TabbedAttestation(String c, String t, String reqId) {
		this.c = c;
		this.t = t;
		this.reqId = reqId;
	}
	
	public TrustNode checkMatch(Context ctx, String claim, TrustNode witness, String reqId) {
		String decryptedAttestation = CryptoMain.decryptedAttestation(ctx, witness, c, reqId);
		Log.i("TabbedAttestation 20", "decryptedAttestation: " + decryptedAttestation);
		Log.i("TabbedAttestation 21", "claim: " + claim);
		Log.i("TabbedAttestation 22", "witness.getPubkey(): " + witness.getPubkey());
		boolean claimContains = (claim.contains("\n") || claim.contains("\r"));
		boolean pubkeyContains = (witness.getPubkey().contains("\n") || witness.getPubkey().contains("\r"));
		boolean decryptedContains = (decryptedAttestation.contains("\n") || decryptedAttestation.contains("\r"));
		Log.i("TabbedAttestation 23", "claimContains: " + claimContains + "\npubkeyContains: " + pubkeyContains + "\ndecryptedContains: " + decryptedContains + "\n");
		if (CryptoMain.verifySignature(witness.getPubkey(), claim, decryptedAttestation)) {
			return witness;
		}
		return null;
	}
	
	public String toString() {
		return "reqId: " + reqId + "\nciphertext: " + c + "\ntab: " + t + "\n";
	}
}
