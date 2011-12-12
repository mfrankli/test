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
  
	public static String generateKeys(Context ctx) {
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
			Log.i("publickey", publicString);
			Log.i("privateky", privateString);
			FileOutputStream fos1 = ctx.openFileOutput("public.key", 0);
			FileOutputStream fos2 = ctx.openFileOutput("private.key", 0);
			fos1.write(publicString.getBytes());
			fos2.write(privateString.getBytes());
			return publicString + "\n" + privateString;
		}
		catch (Exception e) {
			return e.getMessage();
		}
	}
	
	public static String getPublicKeyString(Context ctx) {
		try {
			PublicKey key = getPublicKey(ctx);
			byte[] bytes = key.getEncoded();
			return Base64.encodeToString(bytes, Base64.DEFAULT);
		}
		catch (Exception e) {
			StackTraceElement[] ste = e.getStackTrace();
			for (int i = 0; i < ste.length; i++) {
				Log.i("errortag", ste[i].getClassName() + String.valueOf(ste[i].getLineNumber()));
			}
			return e.getMessage();
		}
	}
	
	public static String generateSignature(Context ctx, String content) {
		try{
			PrivateKey privateKey = getPrivateKey(ctx);
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
	
	private static PublicKey getPublicKey(Context ctx) throws Exception {
		FileInputStream fis = ctx.openFileInput("public.key");
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
	
	private static PrivateKey getPrivateKey(Context ctx) throws Exception {
		FileInputStream fis = ctx.openFileInput("private.key");
		byte[] bytes = new byte[fis.available()];
		fis.read(bytes);
		bytes = Base64.decode(bytes,  Base64.DEFAULT);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
		PrivateKey key = factory.generatePrivate(spec);
		return key;
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
		String pubKey = getPublicKeyString(ctx);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA");
			digest.update(pubKey.getBytes());
			return Base64.encodeToString(digest.digest(), Base64.DEFAULT);
		}
		catch (Exception e) {
			Log.e("CryptoMain 168", "Digest exception", e);
			return null;
		}
	}
	
	public static String generateUUID(Context ctx, int length) throws Exception {
		return getUuid(ctx);
	}
	
	// Generates an attestation \sigma_node,me i.e. an attestation from me to node
	// To do this, I sign the node's uuid (i.e. SHA(node's public key))
	// Note: the attestation that must be sent is (attestation, secret seed)
	public static String generateAttestationForNode(TrustNode node, Context ctx) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA");
			digest.update(node.getPubkey().getBytes());
			String toSign = Base64.encodeToString(digest.digest(), Base64.DEFAULT);
			return generateSignature(ctx, toSign).replace("\n", "\\n");
		}
		catch (Exception e) {
			Log.e("CryptoMain 179", "generate attestation failed for " + node.getUuid(), e);
			return null;
		}
	}
	
	// Generates my secret seed, to give along with my attestation
	public static String generateSecretSeed(Context ctx) {
		try {
			PrivateKey ska = getPrivateKey(ctx);
			// 64 is the number of bytes required for a key for HMAC-SHA in Java
			String toReturn = new String(new SecureRandom(ska.getEncoded()).generateSeed(64));
			toReturn = toReturn.replace("\n", "\\n");
			return toReturn;
		}
		catch (Exception e) {
			Log.e("CryptoMain 190", "error in generating secret seed", e);
			return null;
		}
	}
	
	// This is the attesastation to be sent
	public static String generateFullAttestForNode(TrustNode node, Context ctx) {
		return generateAttestationForNode(node, ctx) + "\n" + generateSecretSeed(ctx);
	}
	
	// generate a tab for node
	public static String generateTab(Context ctx, TrustNode node, String reqId) {
		try {
			String fullAttest = node.getAttest();
			if (fullAttest == null || fullAttest.split("\n").length < 2) {
				return null;
			}
			String seed = fullAttest.split("\n")[1].replace("\\n", "\n");
			if (seed == null) return null;
			Key key = new SecretKeySpec(Base64.decode(seed.getBytes(), Base64.DEFAULT), "HmacSHA1");
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
			if (fullAttest == null || fullAttest.split("\n").length < 2) {
				return null;
			}
			String sigma = fullAttest.split("\n")[0].replace("\\n", "\n");
			String seed = fullAttest.split("\n")[1].replace("\\n", "\n"); 
			if (sigma == null || seed == null) return null;
			Key key = new SecretKeySpec(Base64.decode(seed.getBytes(), Base64.DEFAULT), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			String toMac = "key" + reqId;
			mac.update(toMac.getBytes());
			byte[] toBeKey = mac.doFinal();
			key = new SecretKeySpec(toBeKey, "AES");
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
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
		if (c != null && t != null) return new TabbedAttestation(c, t, reqId);
		return null;
	}
	
	// base64 encoding of decrypted attestation
	public static String decryptedAttestation(Context ctx, TabbedAttestation tabbedAttestation) {
		return null;
	}
	
}
