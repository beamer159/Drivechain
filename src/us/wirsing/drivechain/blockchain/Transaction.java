package us.wirsing.drivechain.blockchain;

import us.wirsing.drivechain.util.Hash;

import java.io.Serializable;


public abstract class Transaction implements Serializable {
	public Hash hash;
	public abstract boolean validate();
	public abstract Transaction copy();
}
