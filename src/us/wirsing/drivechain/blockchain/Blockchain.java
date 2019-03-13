package us.wirsing.drivechain.blockchain;

import us.wirsing.drivechain.util.TransactionTransfers;

import java.io.Serializable;
import java.util.*;

public class Blockchain implements Serializable {

	public Map<Hash, Block> blocks = new HashMap<>();
	public Map<Block, Integer> blockHeights = new HashMap<>();
	public List<Block> leaves = new ArrayList<>();
	public Block tip;
	public static final Block GENESIS = new Block(new Hash(new byte[32]));

	static {
		GENESIS.timestamp = 0;
		GENESIS.nonce = 121446;
		GENESIS.hash = new Hash(new byte[] {
				0, 0, 0, 25, -10, -111, 101, -79,
				54, 94, -59, 114, 38, 66, 92, 52,
				77, 23, 87, -7, 77, -108, -58, 100,
				44, 102, 95, -125, -44, -62, 76, 26 });
	}

	// Constructors

	public Blockchain() {
		blocks.put(GENESIS.hash, GENESIS);
		blockHeights.put(GENESIS, 0);
		leaves.add(GENESIS);
		tip = GENESIS;
	}

	public Blockchain(Blockchain blockchain) {
		for (Block block : blockchain.blocks.values()) {
			Block newBlock = new Block(block);
			blocks.put(newBlock.hash, newBlock);
		}
		for (Map.Entry<Block, Integer> entry : blockchain.blockHeights.entrySet()) {
			blockHeights.put(blocks.get(entry.getKey().hash), entry.getValue());
		}
		for (Block block : blockchain.leaves) {
			leaves.add(blocks.get(block.hash));
		}
		tip = blocks.get(blockchain.tip.hash);
	}

	// Methods

	public TransactionTransfers add(Block block) {
		TransactionTransfers ret = new TransactionTransfers();
		Block previousBlock = blocks.get(block.hashPrevious);
		if (previousBlock == null) {
			return ret;
		}

		blocks.put(block.hash, block);
		blockHeights.put(block, blockHeights.get(previousBlock) + 1);
		int i = leaves.indexOf(previousBlock);
		if (i == -1) {
			leaves.add(block);
		} else {
			leaves.set(i, block);
		}

		if (tip == previousBlock) {
			for (Transaction txn : block.txns) {
				ret.removeFrom.add(txn);
			}
			tip = block;
			return ret;
		}

		if (blockHeights.get(block) > blockHeights.get(tip)) {
			Block newChainRoot = block;
			Block oldChainRoot = tip;

			for (Transaction txn : newChainRoot.txns) {
				ret.removeFrom.add(txn);
			}

			for (Transaction txn : oldChainRoot.txns) {
				ret.addTo.add(txn);
			}

			while (blockHeights.get(newChainRoot) > blockHeights.get(oldChainRoot)) {
				newChainRoot = blocks.get(newChainRoot.hashPrevious);

				for (Transaction txn : newChainRoot.txns) {
					ret.removeFrom.add(txn);
				}
			}

			while (blocks.get(newChainRoot.hashPrevious) != blocks.get(oldChainRoot.hashPrevious)) {
				newChainRoot = blocks.get(newChainRoot.hashPrevious);
				oldChainRoot = blocks.get(oldChainRoot.hashPrevious);

				for (Transaction txn : newChainRoot.txns) {
					ret.removeFrom.add(txn);
				}

				for (Transaction txn : oldChainRoot.txns) {
					ret.addTo.add(txn);
				}
			}


			tip = block;
		}

		return ret;
	}

	public boolean validate() {
		Set<Block> validated = new HashSet<>(blocks.size());
		for (Block blockLeaf : leaves) {
			Block block = blockLeaf;
			while (!validated.contains(block)) {
				if (!block.validate()) {
					return false;
				}
				validated.add(block);
				if (block.equals(GENESIS)) {
					break;
				}
				Hash hashPrevious = block.hashPrevious;
				block = blocks.get(hashPrevious);
				if (block == null || !block.hash.equals(hashPrevious)) {
					return false;
				}
			}
		}
		return true;
	}

	// Risky - Returning the block instead of a copy

	public Block blockFromSubstring(String blockSubstring) {
		for (Block block : blocks.values()) {
			if (block.hash.toBase64().substring(0, blockSubstring.length()).equals(blockSubstring)) {
				return block;
			}
		}

		return null;
	}
}
