package us.wirsing.drivechain.node;

import us.wirsing.drivechain.blockchain.Block;
import us.wirsing.drivechain.blockchain.Hash;

public class Miner implements Runnable {

	protected Node node;
	protected Block block;

	public Miner(Node node) {
		this.node = node;
	}

	@Override
	public void run() {
		while (true) {
			node.processPackets();
			block = node.block;
			if (block.txns.size() == 0) {
				try {
					Thread.sleep(1000);
					continue;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!block.validateProofOfWork()) {
				block.setNonce(block.nonce + 1);
			} else {
				onBlockMined();
			}
		}
	}

	protected void onBlockMined() {
		node.onBlockMined();
	}
}
