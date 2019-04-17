package us.wirsing.drivechain.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Base64;

/**
 * Wrapper class for a byte-array hash
 */
public class Hash implements Serializable {

	public byte[] bytes;

	// Constructors

	public Hash(byte[] bytes) {
		this.bytes = Arrays.copyOf(bytes, bytes.length);
	}

	/**
	 * Copy constructor
	 * @param hash The hash to be copied
	 */
	public Hash(Hash hash) {
		this(hash.bytes);
	}

	// Methods

	/**
	 * Converts the hash to a Base64 string
	 * @return Base64 string representation of the hash
	 */
	public String toBase64() {
		return Base64.getEncoder().encodeToString(bytes);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return Arrays.equals(bytes, ((Hash)obj).bytes);
	}
}
