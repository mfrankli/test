package edu.pu.mf.iw.ProjectOne;

import java.lang.Exception;
import java.security.*;
import java.security.spec.*;
import android.util.Base64;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import android.content.Context;
import javax.crypto.Cipher;
import 	javax.crypto.spec.SecretKeySpec;
import 	javax.crypto.Mac;

public class CryptoMain
{
  
	public static String generateKeys(Context ctx, String publicfile, String privatefile) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair keys = kpg.generateKeyPair();
			PublicKey publicKey = keys.getPublic();
			PrivateKey privateKey = keys.getPrivate();
			byte[] encodedPublic = publicKey.getEncoded();
			byte[] encodedPrivate = privateKey.getEncoded();
			X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(encodedPublic);
			PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(encodedPrivate);
			byte[] encodedX509 = publicSpec.getEncoded();
			byte[] encodedPKCS8 = privateSpec.getEncoded();
			String publicString = Base64.encodeToString(encodedX509,  Base64.DEFAULT);
			String privateString = Base64.encodeToString(encodedPKCS8,  Base64.DEFAULT);
			if (publicfile != null || privatefile != null) {
				Log.i(publicfile, publicString);
				Log.i(privatefile, privateString);
				FileOutputStream fos1 = ctx.openFileOutput(publicfile, 0);
				FileOutputStream fos2 = ctx.openFileOutput(privatefile, 0);
				fos1.write(publicString.getBytes());
				fos1.flush();
				fos1.close();
				fos2.write(privateString.getBytes());
				fos2.flush();
				fos2.close();
			}
			return publicString + "\n" + privateString;
		}
		catch (Exception e) {
			Log.i("CryptoMain 48", "generateKeys failed", e);
			return e.getMessage();
		}
	}
	
	public static String generateKeys(Context ctx) {
		return generateKeys(ctx, "public.key", "private.key");
	}
	
	public static String generateTestKeys(Context ctx) {
		return generateKeys(ctx, "testpublic.key", "testprivate.key");
	}
	
	public static String generateKeysToShare(Context ctx) {
		return generateKeys(ctx, "sharepublic.key", "shareprivate.key");
	}
	
	public static String getPublicKeyString(Context ctx) {
		try {
			PublicKey key = getPublicKey(ctx);
			byte[] bytes = key.getEncoded();
			String toReturn = Base64.encodeToString(bytes, Base64.DEFAULT);
			Log.i("CryptoMain 70", new String(toReturn));
			return toReturn;
		}
		catch (Exception e) {
			StackTraceElement[] ste = e.getStackTrace();
			for (int i = 0; i < ste.length; i++) {
				Log.i("errortag", ste[i].getClassName() + String.valueOf(ste[i].getLineNumber()));
			}
			return e.getMessage();
		}
	}
	
	public static String getPrivateKeyString(Context ctx) {
		try {
			PrivateKey key = getPrivateKey(ctx);
			byte[] bytes = key.getEncoded();
			return Base64.encodeToString(bytes, Base64.DEFAULT);
		}
		catch (Exception e) {
			Log.e("CryptoMain 68", "error in getPrivateKeyString", e);
			return null;
		}
	}
	
	public static String generateSignature(Context ctx, String content) {
		return generateSignature(ctx, content, false);
	}
	
	public static String generateSignature(Context ctx, String content, boolean isTest) {
		try{
			PrivateKey privateKey;
			if (!isTest) privateKey = getPrivateKey(ctx);
			else privateKey = getTestPrivateKey(ctx);
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initSign(privateKey);
			byte[] bytesToSign = content.getBytes();
			signature.update(bytesToSign);
			byte[] signedBytes = signature.sign();
			return Base64.encodeToString(signedBytes, Base64.DEFAULT);
		}
		catch (Exception e) {
			Log.i("errortag1", "length: " + String.valueOf(content.getBytes().length));
			StackTraceElement[] ste = e.getStackTrace();
			for (int i = 0; i < ste.length; i++) {
				Log.i("errortag1", ste[i].getClassName() + String.valueOf(ste[i].getLineNumber()) + "\n");
			}
			Log.i("errortag1", e.getMessage());
			return e.getMessage();
		}
	}
	
	public static boolean verifySignature(String publicKeyString, String content, String claim) {
		try {
			byte[] bytes = Base64.decode(publicKeyString.getBytes(), Base64.DEFAULT);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
			PublicKey key = factory.generatePublic(spec);
			byte[] bytesToVerify = content.getBytes();
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify(key);
			signature.update(bytesToVerify);
			byte[] claimBytes = Base64.decode(claim.getBytes(), Base64.DEFAULT);
			return signature.verify(claimBytes);
		}
		catch (Exception e) {
			return false;
		}
	}
	
	private static PublicKey getPublicKey(Context ctx, String filename) throws Exception {
		FileInputStream fis = ctx.openFileInput(filename);
		// this is a potential bug - doesn't represent exactly how many bytes there are
		// on this scale, shouldn't matter
		byte[] bytes = new byte[fis.available()];
		fis.read(bytes);
		bytes = Base64.decode(bytes, Base64.DEFAULT);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
		PublicKey key = factory.generatePublic(spec);
		return key;
	}
	
	private static PublicKey getPublicKey(Context ctx) throws Exception {
		return getPublicKey(ctx, "public.key");
	}
	
	private static PrivateKey getPrivateKey(Context ctx, String filename) throws Exception {
		FileInputStream fis = ctx.openFileInput("private.key");
		byte[] bytes = new byte[fis.available()];
		fis.read(bytes);
		bytes = Base64.decode(bytes,  Base64.DEFAULT);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		PrivateKey key = factory.generatePrivate(spec);
		return key;
	}
	
	private static PrivateKey getPrivateKey(Context ctx) throws Exception {
		return getPrivateKey(ctx, "private.key");
	}
	
	private static PrivateKey getTestPrivateKey(Context ctx) throws Exception {
		return getPrivateKey(ctx, "testprivate.key");
	}
	
	public static String encryptContent(String pubKeyString, String content) {
		try {
			byte[] bytes = pubKeyString.getBytes();
			bytes = Base64.decode(bytes, Base64.DEFAULT);
			KeyFactory factory = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
			PublicKey key = factory.generatePublic(spec);
			Cipher ciph = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			ciph.init(Cipher.ENCRYPT_MODE, key);
			byte[] toEncrypt = content.getBytes();
			byte[] encrypted = ciph.doFinal(toEncrypt);
			return Base64.encodeToString(encrypted, Base64.DEFAULT);
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public static String decryptContent(Context ctx, String content) {
		try {
			PrivateKey key = getPrivateKey(ctx);
			Cipher ciph = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			ciph.init(Cipher.DECRYPT_MODE, key);
			byte[] bytesToDecrypt = content.getBytes();
			bytesToDecrypt = Base64.decode(bytesToDecrypt, Base64.DEFAULT);
			byte[] decrypted = ciph.doFinal(bytesToDecrypt);
			return new String(decrypted);
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public static String getUuid(Context ctx) {
		String pubKey = getPublicKeyString(ctx).trim();
		return sha(pubKey);
	}
	
	public static String generateUUID(Context ctx, int length) throws Exception {
		return getUuid(ctx);
	}
	
	// Generates an attestation \sigma_node,me i.e. an attestation from me to node
	// To do this, I sign the node's uuid (i.e. SHA(node's public key))
	// Note: the attestation that must be sent is (attestation, secret seed)
	public static String generateAttestationForNode(TrustNode node, Context ctx) {
		try {
			String toSign = sha(node.getPubkey().trim());
			Log.i("CryptoMain 216", toSign);
			byte[] debugBytes = toSign.getBytes();
			for (int i = 0; i < debugBytes.length; i++) {
				Log.i("CryptoMain 219", "" + debugBytes[i]);
			}
			return generateSignature(ctx, toSign);
		}
		catch (Exception e) {
			Log.e("CryptoMain 179", "generate attestation failed for " + node.getUuid(), e);
			return null;
		}
	}
	
	public static String sha(String arg) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA", "BC");
			digest.update(arg.getBytes("UTF-8"));
			return Base64.encodeToString(digest.digest(), Base64.DEFAULT);
		}
		catch (Exception e) {
			return null;
		}
	}
	
	// Generates my secret seed, to give along with my attestation
	public static String generateSecretSeed(Context ctx) {
		try {
			PrivateKey ska = getPrivateKey(ctx);
			// 64 is the number of bytes required for a key for HMAC-SHA in Java
			SecureRandom secRan = SecureRandom.getInstance("SHA1PRNG");
			secRan.setSeed(ska.getEncoded());
			byte[] toReturnBytes = new byte[64];
			secRan.nextBytes(toReturnBytes);
			String toReturn = Base64.encodeToString(toReturnBytes, Base64.DEFAULT);
			Log.i("CryptoMain 242", toReturn);
			return toReturn;
		}
		catch (Exception e) {
			Log.e("CryptoMain 190", "error in generating secret seed", e);
			return null;
		}
	}
	
	public static String generateSecretSeed(Context ctx, boolean isTest) {
		try {
			PrivateKey ska;
			if (!isTest) ska = getPrivateKey(ctx);
			else ska = getTestPrivateKey(ctx);
			SecureRandom secRan = SecureRandom.getInstance("SHA1PRNG");
			secRan.setSeed(ska.getEncoded());
			byte[] toReturnBytes = secRan.generateSeed(64);
			String toReturn = Base64.encodeToString(toReturnBytes, Base64.DEFAULT);
			Log.i("CryptoMain 242", toReturn);
			return toReturn;
		}
		catch (Exception e) {
			Log.e("CryptoMain 190", "error in generating secret seed", e);
			return null;
		}
	}
	
	// This is the attesastation to be sent
	public static String generateFullAttestForNode(TrustNode node, Context ctx) {
		return getUuid(ctx) + "\n\r\n\r" + generateAttestationForNode(node, ctx) + "\n\r\n\r" + generateSecretSeed(ctx);
	}
	
	// generate a tab for node
	public static String generateTab(Context ctx, TrustNode node, String reqId) {
		try {
			String fullAttest = node.getAttest();
			if (fullAttest == null && node.getUuid().equals(getUuid(ctx))) {
				TrustNode myself = new TrustNode(node.getUuid(), false, new TrustDbAdapter(ctx).open());
				myself.setPubkey(node.getPubkey().trim());
				fullAttest = generateFullAttestForNode(myself, ctx);
			}
			Log.i("CryptoMain 332", "fullAttest: " + fullAttest);
			if (fullAttest == null || fullAttest.split("\n\r\n\r").length < 2) {
				return null;
			}
			String[] parts = fullAttest.split("\n\r\n\r");
			String seed = null;
			if (parts.length == 2) seed = parts[1];
			else if (parts.length == 3) seed = parts[2];
			Log.i("CryptoMain 284", "" + seed);
			if (seed == null) return null;
			byte[] seedBytes = Base64.decode(seed.getBytes(), Base64.DEFAULT);
			Log.i("CryptoMain 295", "seedBytes.length: " + seedBytes.length);
			Key key = new SecretKeySpec(seedBytes, "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			String toMac = "tab" + reqId;
			mac.update(toMac.getBytes());
			return Base64.encodeToString(mac.doFinal(), Base64.DEFAULT);
		}
		catch (Exception e) {
			Log.e("CryptoMain 220", "failed to generate tab for " + node.getUuid(), e);
			return null;
		}
	}
	
	// generate the encrypted attestation for node
	// There's probably something incorrect going on here with the crypto stuff
	// Specifically: what's the difference between initializing a SecretKeySpec 
	// and using a SecretKeyFactory?
	public static String generateCiphertext(Context ctx, TrustNode node, String reqId) {
		try {
			String fullAttest = node.getAttest();
			if (fullAttest == null || fullAttest.split("\n\r\n\r").length < 2) {
				Log.i("CryptoMain 359", "returning @359 " + fullAttest);
				return null;
			}
			String[] parts = fullAttest.split("\n\r\n\r");
			String sigma = parts[parts.length - 2];
			Log.i("CryptoMain 365", sigma);
			String seed = parts[parts.length - 1]; 
			if (sigma == null || seed == null) {
				Log.i("CryptoMain 368", "returning @365");
				return null;
			}
			seed = seed.trim();
			Log.i("CryptoMain 372", seed);
			Key key = new SecretKeySpec(Base64.decode(seed.getBytes(), Base64.DEFAULT), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			String toMac = "key" + reqId;
			mac.update(toMac.getBytes());
			byte[] toBeKey = mac.doFinal();
			Log.i("CryptoMain 378", "toBeKey.length: " + toBeKey.length);
			byte[] realKey = new byte[16];
			for (int i = 0; i < 16; i++) {
				// this is probably obviously insecure. I'm not sure why HmacSHA1 doesn't
				// give me variable length output - I need to figure that out
				realKey[i] = toBeKey[i];
				Log.i("CryptoMain 383", "symmetric byte: " + realKey[i]);
			}
			key = new SecretKeySpec(realKey, "AES");
			Cipher c = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
			c.init(Cipher.ENCRYPT_MODE, key);
			return Base64.encodeToString(c.doFinal(Base64.decode(sigma.getBytes(), Base64.DEFAULT)), Base64.DEFAULT);
		}
		catch (Exception e) {
			Log.e("CryptoMain 234", "failed to generate ciphertext for " + node.getAttest(), e);
			return null;
		}
	}
	
	public static TabbedAttestation generateTabbedAttestation(Context ctx, TrustNode node, String reqId, boolean forCheck) {
		String c = "";
		if (!forCheck) c = generateCiphertext(ctx, node, reqId);
		String t = generateTab(ctx, node, reqId);
		Log.i("CryptoMain 384", "c: " + c);
		Log.i("CryptoMain 395", "t: " + t);
		if (c != null && t != null) return new TabbedAttestation(c, t, reqId);
		return null;
	}
	
	// returns base64 encoding of decrypted attestation we know that the tab matches
	public static String decryptedAttestation(Context ctx, TrustNode witness, String c, String reqId) {
		try {
			// This method needs to decrypt their attestation, and return the string that was encrypted
			String fullAttest = witness.getAttest(); // this is the attest we have
			if (fullAttest == null || fullAttest.split("\n\r\n\r").length < 2) {
				if (witness.getUuid().equals(getUuid(ctx))) {
					TrustNode myself = new TrustNode(witness.getUuid(), false, new TrustDbAdapter(ctx).open());
					myself.setPubkey(witness.getPubkey().trim());
					fullAttest = generateFullAttestForNode(myself, ctx);
				}
				if (fullAttest == null || fullAttest.split("\n\r\n\r").length < 2) {
					Log.i("CryptoMain 379", "returning @ 379");
					return null;
				}
			}
			// the seed from the trust node who was supposed to be attesting for the claiming node
			String[] parts = fullAttest.split("\n\r\n\r");
			String seed = null;
			if (parts.length == 2) seed = parts[1];
			else if (parts.length == 3) seed = parts[2];
			if (seed == null) return null;
			// the key generated from this seed
			Key key = new SecretKeySpec(Base64.decode(seed.getBytes(), Base64.DEFAULT), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			String toMac = "key" + reqId;
			mac.update(toMac.getBytes());
			// run the Mac to get the symmetric key
			byte[] symmetricKeyBytes = mac.doFinal();
			byte[] realSymmetricKeyBytes = new byte[16];
			for (int i = 0; i < 16; i++) {
				realSymmetricKeyBytes[i] = symmetricKeyBytes[i];
				Log.i("CryptoMain 429", "symmetric byte: " + realSymmetricKeyBytes[i]);
			}
			// make the new AES key from those bytes
			key = new SecretKeySpec(realSymmetricKeyBytes, "AES");
			// get the AES cipher
			Cipher ciph = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC");
			ciph.init(Cipher.DECRYPT_MODE, key);
			Log.i("CryptoMain 437", c);
			byte[] bytes = Base64.decode(c.getBytes(), Base64.DEFAULT);
			String toReturn = Base64.encodeToString(ciph.doFinal(bytes), Base64.DEFAULT);
			return toReturn;
		}
		catch (Exception e) {
			Log.e("CryptoMain 287", "error in decryptedAttestation", e);
			return null;
		}
	}
	
}
