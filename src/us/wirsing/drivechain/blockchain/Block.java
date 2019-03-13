package us.wirsing.drivechain.blockchain;

import us.wirsing.drivechain.util.Crypto;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Block implements Serializable {

	public static final int DIFFICULTY = 2;

	public Set<Transaction> txns = new HashSet<>();
	public long timestamp;
	public Hash hashPrevious;
	public long nonce;
	public Hash hash;

	// Constructors

	public Block(Hash hashPrevious) {
		timestamp = System.currentTimeMillis();
		this.hashPrevious = new Hash(hashPrevious);
		nonce = 0;
		hash = calculateHash();
	}

	/**
	 * Copy constructor
	 * @param block The block to be copied
	 */
	public Block(Block block) {
		for (Transaction txn : block.txns) {
			txns.add(txn.copy());
		}
		timestamp = block.timestamp;
		hashPrevious = new Hash(block.hashPrevious);
		nonce = block.nonce;
		hash = new Hash(block.hash);
	}

	// Methods

	/**
	 * Adds a transaction to the block
	 * @param txn Transaction to add
	 */
	public void add(Transaction txn) {
		txns.add(txn);
		hash = calculateHash();
	}

	/**
	 * Removes a given transaction from the block
	 * @param txn Transaction to remove
	 */
	public void remove(Transaction txn) {
		if (txns.remove(txn)) {
			hash = calculateHash();
		}
	}

	/**
	 * Sets the nonce value for the block
	 * @param nonce Value to assign to the nonce
	 */
	public void setNonce(long nonce) {
		this.nonce = nonce;
		hash = calculateHash();
	}

	/**
	 * Validates the block
	 * @return true if validation is successful, otherwise false
	 */
	public boolean validate() {
		// Validate proof of work
		if (!Arrays.equals(new byte[DIFFICULTY], Arrays.copyOf(hash.bytes, DIFFICULTY))) {
			return false;
		}

		// Validate hash
		if (!hash.equals(calculateHash())) {
			return false;
		}

		// Validate block transactions
		for (Transaction txn : txns) {
			if (!txn.validate()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Calculate hash for the block
	 * @return Calculated hash for the block
	 */
	public Hash calculateHash() {
		int hashCapacity = hashPrevious.bytes.length;
		for (Transaction txn : txns) {
			hashCapacity += txn.hash.bytes.length;
		}
		ByteBuffer buffer = ByteBuffer.allocate(hashCapacity + Long.BYTES * 2);
		for (Transaction txn : txns) {
			buffer.put(txn.hash.bytes);
		}
		buffer.put(hashPrevious.bytes);
		buffer.putLong(timestamp);
		buffer.putLong(nonce);
		return new Hash(Crypto.SHA256(buffer.array()));
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
		return hash.equals(((Block)obj).hash);
	}
}
