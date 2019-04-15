package us.wirsing.drivechain.drive;

import us.wirsing.drivechain.node.Miner;

public class MinerDrive extends Miner {

	public MinerDrive(NodeDrive node) {
		super(node);
	}

	@Override
	protected void onBlockMined() {
		System.out.println(((NodeDrive)node).getName() + " mined block: " + block.hash.toBase64());
		super.onBlockMined();
	}
}
