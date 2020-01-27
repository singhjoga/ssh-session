package com.thetechnovator.ssh;

import java.io.Closeable;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class Utils {
	public static String msToNamedTimeInterval(long milliseconds) {
		long millis = milliseconds % 1000;
		long second = (milliseconds / 1000) % 60;
		long minute = (milliseconds / (1000 * 60)) % 60;
		long hour = (milliseconds / (1000 * 60 * 60)) % 24;	
		StringBuilder sb = new StringBuilder();
		if (hour > 0) {
			sb.append(hour).append(hour==1?" hour":" hours");
		}
		if (minute > 0) {
			sb.append(sb.length()==0?"":" ").append(minute).append(minute==1?" minute":" minutes");
		}
		if (second > 0) {
			sb.append(sb.length()==0?"":" ").append(second).append(second==1?" second":" seconds");
		}
		if (millis > 0) {
			sb.append(sb.length()==0?"":" ").append(millis).append(millis==1?" millisecond":" milliseconds");
		}
		
		return sb.toString();
	}
	public static KeyPair privateKey2KeyPair(String privateKey) throws SshSessionlException {
		privateKey = privateKey.replace("-----BEGIN RSA PRIVATE KEY-----\n", "");
		privateKey = privateKey.replace("-----END RSA PRIVATE KEY-----", "");

        byte[] decoded = Base64.getDecoder().decode(privateKey);
		try {
	        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
	        KeyFactory kf;
			kf = KeyFactory.getInstance("RSA");
	        PrivateKey privkKey = kf.generatePrivate(keySpec);
	        RSAPrivateCrtKey priRsa = (RSAPrivateCrtKey)privkKey;

	        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(priRsa.getModulus(), priRsa.getPublicExponent());

	        PublicKey pubKey = kf.generatePublic(publicKeySpec);
	        return new KeyPair(pubKey, privkKey);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new SshSessionlException("SSH Key Error: "+e.getMessage(), e);
		}
        
	}
	public static void closeQuietly(Closeable closable) {
		if (closable == null)
			return;
		try {
			closable.close();
		} catch (IOException e) {
			// Nothing. this method is mean to suppress the exception
		}
	}
	public static void print(String str) {
		System.out.print(str);
	}
	public static void println(String str) {
		System.out.println(str);
	}
}
