package us.wirsing.drivechain.util;

import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Crypto {

	private Crypto() {
	}

	public static KeyPair generateKey() {
		try {
			KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(2048, new SecureRandom());
			return gen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static X509Certificate copyCertificate(X509Certificate certificate) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			return (X509Certificate)certificateFactory.generateCertificate(new ByteArrayInputStream(certificate.getEncoded()));
		} catch (CertificateException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] sign(byte[] message, PrivateKey key) {
		try {
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initSign(key);
			sig.update(message);
			return sig.sign();
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static boolean verify(byte[] message, byte[] signature, PublicKey key) {
		try {
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initVerify(key);
			sig.update(message);
			return sig.verify(signature);
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static byte[] SHA256(byte[] message) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(message);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
}
