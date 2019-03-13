package us.wirsing.drivechain.node;

import us.wirsing.drivechain.blockchain.Block;

import java.util.Arrays;

public class Miner implements Runnable {

	private static final byte[] ZEROES = new byte[Block.DIFFICULTY];

	private Node node;
	private long runtimeMin;

	public Miner(Node node) {
		this(node, 0L);
	}

	public Miner(Node node, long runtimeMin) {
		this.node = node;
		this.runtimeMin = runtimeMin;
		this.node.block.nonce = 0;
		this.node.block.hash = node.block.calculateHash();
	}

	@Override
	public void run() {
		Block block = node.block;
		long start = System.currentTimeMillis();
		while (true) {
			synchronized (block) {
				if (System.currentTimeMillis() - start < runtimeMin || !Arrays.equals(ZEROES, Arrays.copyOf(block.hash.bytes, Block.DIFFICULTY))) {
					block.setNonce(block.nonce + 1);
				} else {
					System.out.println("Block mined: " + block.hash.toBase64());
					return;
				}
			}
		}
	}
}
