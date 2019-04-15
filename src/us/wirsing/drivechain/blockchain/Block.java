package us.wirsing.drivechain.blockchain;

import us.wirsing.drivechain.node.Status;
import us.wirsing.drivechain.util.Crypto;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class Block implements Serializable {

	private static final int DIFFICULTY = 24;

	public Set<Transaction> txns = new LinkedHashSet<>();
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
		if (txns.add(txn)) {
			hash = calculateHash();
		}
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
	public Status validate() {
		// Validate proof of work
        if (!validateProofOfWork()) {
            return Status.INVALID_BLOCK;
        }

		// Validate hash
		if (!hash.equals(calculateHash())) {
			return Status.INVALID_BLOCK;
		}

		// Validate block transactions
		for (Transaction txn : txns) {
			if (!txn.validate()) {
				return Status.INVALID_TRANSACTION;
			}
		}
		return Status.OK;
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

	public boolean validateProofOfWork() {
        int zeroBytes = Block.DIFFICULTY / 8;
        int zeroBits = Block.DIFFICULTY % 8;
        if (hash.bytes == null) {
        	int zero = 0;
		}
        return Arrays.equals(new byte[zeroBytes], Arrays.copyOf(hash.bytes, zeroBytes))
                && (hash.bytes[zeroBytes] & 0xFF) >>> (8 - zeroBits) == 0;
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
