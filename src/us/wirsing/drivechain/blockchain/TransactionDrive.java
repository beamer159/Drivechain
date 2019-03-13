package us.wirsing.drivechain.blockchain;

import us.wirsing.drivechain.node.Node;
import us.wirsing.drivechain.util.Crypto;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * Represents a transaction between a driver and a passenger
 * Driver name, passenger name, and timestamp are signed by both
 * Also includes both party's public keys
 */
public class TransactionDrive extends Transaction {

	private static final long serialVersionUID = 1L;
	public String nameDriver;
	public String namePassenger;
	public long timestamp;
	public byte[] sigDriver;
	public byte[] sigPassenger;
	public PublicKey keyPublicDriver; // TODO: Replace with driver certificate
	public PublicKey keyPublicPassenger; // TODO: Replace with passenger certificate

	// Constructors

	public TransactionDrive(Node driver, Node passenger) {
		nameDriver = driver.getName();
		namePassenger = passenger.getName();
		timestamp = System.currentTimeMillis();
		keyPublicDriver = driver.publicKey();
		keyPublicPassenger = passenger.publicKey();

		// Collate data into byte array and have driver and passenger sign
		byte[] keyPublicDriverBytes = keyPublicDriver.getEncoded();
		byte[] keyPublicPassengerBytes = keyPublicPassenger.getEncoded();
		ByteBuffer buffer = ByteBuffer.allocate(nameDriver.length() + namePassenger.length() + Long.BYTES);
		buffer.put(nameDriver.getBytes());
		buffer.put(namePassenger.getBytes());
		buffer.putLong(timestamp);
		byte[] toSign = buffer.array();
		sigDriver = driver.sign(toSign);
		sigPassenger = passenger.sign(toSign);

		// Collate data into byte array to calculate hash
		buffer = ByteBuffer.allocate(toSign.length + sigDriver.length + sigPassenger.length
				+ keyPublicDriverBytes.length + keyPublicPassengerBytes.length);
		buffer.put(toSign);
		buffer.put(sigDriver);
		buffer.put(sigPassenger);
		buffer.put(keyPublicDriverBytes);
		buffer.put(keyPublicPassengerBytes);
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
		keyPublicDriver = Crypto.copyKeyPublic(txn.keyPublicDriver	);
		keyPublicPassenger = Crypto.copyKeyPublic(txn.keyPublicPassenger);
		hash = new Hash(txn.hash);
	}

	// Methods

	/**
	 * Validates a transaction
	 * @return true if validation is successful, otherwise false
	 */
	@Override
	public boolean validate() {
		// Verify driver and passenger signatures
		byte[] keyPublicDriverBytes = keyPublicDriver.getEncoded();
		byte[] keyPublicPassengerBytes = keyPublicPassenger.getEncoded();
		ByteBuffer buffer = ByteBuffer.allocate(nameDriver.length() + namePassenger.length() + Long.BYTES);
		buffer.put(nameDriver.getBytes());
		buffer.put(namePassenger.getBytes());
		buffer.putLong(timestamp);
		byte[] signed = buffer.array();
		if (!Crypto.verify(signed, sigDriver, keyPublicDriver)) {
			return false;
		}
		if (!Crypto.verify(signed, sigPassenger, keyPublicPassenger)) {
			return false;
		}

		// Validate hash
		buffer = ByteBuffer.allocate(signed.length + sigDriver.length + sigPassenger.length
				+ keyPublicDriverBytes.length + keyPublicPassengerBytes.length);
		buffer.put(signed);
		buffer.put(sigDriver);
		buffer.put(sigPassenger);
		buffer.put(keyPublicDriverBytes);
		buffer.put(keyPublicPassengerBytes);

		return Arrays.equals(Crypto.SHA256(buffer.array()), hash.bytes);
	}

	@Override
	public Transaction copy() {
		return new TransactionDrive(this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(hash.bytes);
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
}
