package us.wirsing.drivechain.drive;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import us.wirsing.drivechain.util.Hash;
import us.wirsing.drivechain.blockchain.Transaction;
import us.wirsing.drivechain.util.Crypto;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * Represents a transaction between a driver and a passenger
 * Driver name, passenger name, and timestamp are signed by both
 * Also includes both party's public keys
 */
public class TransactionDrive extends Transaction {

	private static final long serialVersionUID = 1L;
	public static X509Certificate certCa;

	public String nameDriver;
	public String namePassenger;
	public long timestamp;
	public byte[] sigDriver;
	public byte[] sigPassenger;
	public X509Certificate certDriver;
	public X509Certificate certPassenger;

	// Constructors

	public TransactionDrive(NodeDrive driver, NodeDrive passenger) {
		nameDriver = driver.getName();
		namePassenger = passenger.getName();
		timestamp = System.currentTimeMillis();
		certDriver = driver.getCertificate();
		certPassenger = passenger.getCertificate();

		// Collate data into byte array and have driver and passenger sign
		ByteBuffer buffer = ByteBuffer.allocate(nameDriver.length() + namePassenger.length() + Long.BYTES);
		buffer.put(nameDriver.getBytes());
		buffer.put(namePassenger.getBytes());
		buffer.putLong(timestamp);
		byte[] toSign = buffer.array();
		sigDriver = driver.sign(toSign);
		sigPassenger = passenger.sign(toSign);

		// Collate data into byte array to calculate hash
		byte[] certDriverBytes = null;
		byte[] certPassengerBytes = null;
		try {
			certDriverBytes = certDriver.getEncoded();
			certPassengerBytes = certPassenger.getEncoded();
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		}
		buffer = ByteBuffer.allocate(toSign.length + sigDriver.length + sigPassenger.length
				+ certDriverBytes.length + certPassengerBytes.length);
		buffer.put(toSign);
		buffer.put(sigDriver);
		buffer.put(sigPassenger);
		buffer.put(certDriverBytes);
		buffer.put(certPassengerBytes);
		hash = new Hash(Crypto.SHA256(buffer.array()));
	}

	/**
	 * Copy constructor
	 * @param txn The transaction to be copied
	 */
	public TransactionDrive(TransactionDrive txn) {
		nameDriver = txn.nameDriver;
		namePassenger = txn.namePassenger;
		timestamp = txn.timestamp;
		sigDriver = Arrays.copyOf(txn.sigDriver, txn.sigPassenger.length);
		sigPassenger = Arrays.copyOf(txn.sigPassenger, txn.sigPassenger.length);
		certDriver = Crypto.copyCertificate(txn.certDriver	);
		certPassenger = Crypto.copyCertificate(txn.certPassenger);
		hash = new Hash(txn.hash);
	}

	// Methods

	/**
	 * Validates a transaction
	 * @return true if validation is successful, otherwise false
	 */
	@Override
	public boolean validate() {
		// Validate driver and passenger certificates
		try {
			certDriver.verify(certCa.getPublicKey());
			certPassenger.verify(certCa.getPublicKey());
		} catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
			return false;
		}
		if (!validateSubjectName(certDriver, nameDriver) || !validateSubjectName(certPassenger, namePassenger)) {
			return false;
		}

		// Verify driver and passenger signatures
		ByteBuffer buffer = ByteBuffer.allocate(nameDriver.length() + namePassenger.length() + Long.BYTES);
		buffer.put(nameDriver.getBytes());
		buffer.put(namePassenger.getBytes());
		buffer.putLong(timestamp);
		byte[] signed = buffer.array();
		if (!Crypto.verify(signed, sigDriver, certDriver.getPublicKey()) ||
				!Crypto.verify(signed, sigPassenger, certPassenger.getPublicKey())) {
			return false;
		}

		// Validate hash
		byte[] certDriverBytes = null;
		byte[] certPassengerBytes = null;
		try {
			certDriverBytes = certDriver.getEncoded();
			certPassengerBytes = certPassenger.getEncoded();
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		}
		buffer = ByteBuffer.allocate(signed.length + sigDriver.length + sigPassenger.length
				+ certDriverBytes.length + certPassengerBytes.length);
		buffer.put(signed);
		buffer.put(sigDriver);
		buffer.put(sigPassenger);
		buffer.put(certDriverBytes);
		buffer.put(certPassengerBytes);

		return Arrays.equals(Crypto.SHA256(buffer.array()), hash.bytes);
	}

	@Override
	public Transaction copy() {
		return new TransactionDrive(this);
	}

	@Override
	public int hashCode() {
		return hash.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return hash.equals(((TransactionDrive)obj).hash);
	}

	private boolean validateSubjectName(X509Certificate certificate, String name) {
		RDN[] cn = null;
		try {
			cn = new JcaX509CertificateHolder(certificate).getSubject().getRDNs(BCStyle.CN);
		} catch (CertificateEncodingException e) {
			e.printStackTrace();
		}
		if (cn.length != 1) {
			return false;
		}
		return name.equals(IETFUtils.valueToString(cn[0].getFirst().getValue()));
	}
}
