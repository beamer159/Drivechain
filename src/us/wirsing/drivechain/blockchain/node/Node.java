package us.wirsing.drivechain.blockchain.node;

import us.wirsing.drivechain.blockchain.*;
import us.wirsing.drivechain.util.Hash;
import us.wirsing.drivechain.util.Serialization;
import us.wirsing.drivechain.util.TransactionTransfers;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Node {

	public static final byte PACKET_TYPE_TRANSACTION = 0;
	public static final byte PACKET_TYPE_BLOCK = 1;
	public static final byte PACKET_TYPE_BLOCKCHAIN = 2;

	protected List<Node> cxns = new ArrayList<>();
	protected Blockchain blockchain = new Blockchain();
	protected Block block = new Block(blockchain.tip.hash);
	protected Set<Transaction> txnsUnconfirmed = new HashSet<>();
	public ConcurrentLinkedQueue<byte[]> packets = new ConcurrentLinkedQueue<>();

	public void start() {
		new Thread(new Miner(this)).start();
	}

	// Information Gathering Methods

	/**
	 * @return a copy of the node's block
	 */
	public Block getBlock() {
		return new Block(block);
	}

	/**
	 * @return a copy of the node's blockchain
	 */
	public Blockchain getBlockchain() {
		return new Blockchain(blockchain);
	}

	// Connects two users in the network

	public void connect(Node node) {
		if (!cxns.contains(node)) {
			cxns.add(node);
			node.send(Serialization.serialize(node.blockchain), PACKET_TYPE_BLOCKCHAIN, this);
		}
		if (!node.cxns.contains(this)) {
			node.cxns.add(this);
			send(Serialization.serialize(blockchain), PACKET_TYPE_BLOCKCHAIN, node);
		}
	}

	public boolean addToBlock(Transaction txn) {
		synchronized (block) {
			if (block.txns.contains(txn)) {
				return false;
			}
			block.add(txn);
			return true;
		}
	}

	public boolean removeFromBlock(Transaction txn) {
		synchronized (block) {
			if (!block.txns.contains(txn)) {
				return false;
			}
			block.remove(txn);
			return true;
		}
	}

	// Risky - Returning the transaction instead of a copy

	public Transaction txnFromSubstring(String txnSubstring) {

		// First, look in the node's unconfirmed transactions

		for (Transaction txn : txnsUnconfirmed) {
			if (txn.hash.toBase64().substring(0, txnSubstring.length()).equals(txnSubstring)) {
				return txn;
			}
		}

		// Next, look in the node's blockchain

		for (Block block : blockchain.blocks.values()) {
			for (Transaction txn : block.txns) {
				if (txn.hash.toBase64().substring(0, txnSubstring.length()).equals(txnSubstring)) {
					return txn;
				}
			}
		}

		return null;
	}

	public void broadcastTxn(Transaction txn) {
		broadcast(Serialization.serialize(txn), PACKET_TYPE_TRANSACTION);
	}

	public void broadcastBlock(Block block) {
		broadcast(Serialization.serialize(block), PACKET_TYPE_BLOCK);
	}

	private void broadcast(byte[] payload, byte header) {
		for (Node node : cxns) {
			send(payload, header, node);
		}
	}

	public static void send(byte[] payload, byte header, Node receiver) {
		byte[] packet = new byte[payload.length + 1];
		packet[0] = header;
		System.arraycopy(payload, 0, packet, 1, payload.length);
		receiver.packets.add(packet);
	}

	public void addToChain(Block block) {
		TransactionTransfers txnTransfers = blockchain.add(block);

		for (Transaction txn : txnTransfers.addTo) {
			txnsUnconfirmed.add(txn);
			broadcastTxn(txn);
			addToBlock(txn);
		}

		for (Transaction txn : txnTransfers.removeFrom) {
			txnsUnconfirmed.remove(txn);
			removeFromBlock(txn);
		}

		this.block.hashPrevious = blockchain.tip.hash;
	}

	public void processPackets() {
		for(byte[] packet : packets) {
			onPacketReceived(packet);
			packets.remove(packet);
		}
	}

	public void onPacketReceived(byte[] packet) {

		byte header = packet[0];
		byte[] payload = Arrays.copyOfRange(packet, 1, packet.length);

		switch (header) {
			case PACKET_TYPE_TRANSACTION:
				onTxnReceived(Serialization.deserialize(payload));
				break;
			case PACKET_TYPE_BLOCK:
				onBlockReceived(Serialization.deserialize(payload));
				break;
			case PACKET_TYPE_BLOCKCHAIN:
				onBlockchainReceived(Serialization.deserialize(payload));
				break;
			default:
				break;
		}
	}

	public Status onTxnReceived(Transaction txn) {
		// Check if the node already has this transaction

		if (txnsUnconfirmed.contains(txn)) {
			return Status.DUPLICATE_TRANSACTION_UNCONFIRMED;
		}

		for (Block chainBlock = blockchain.blocks.get(block.hashPrevious);
			 chainBlock != blockchain.GENESIS;
			 chainBlock = blockchain.blocks.get(chainBlock.hashPrevious)) {
			if (chainBlock.txns.contains(txn)) {
				return Status.DUPLICATE_TRANSACTION_BLOCKCHAIN;
			}
		}

		// Check the validity of the transaction

		if (!txn.validate()) {
			return Status.INVALID_TRANSACTION;
		}

		// TransactionDrive is good, add it and broadcast it

		txnsUnconfirmed.add(txn);
		block.add(txn);
		broadcastTxn(txn);
		return Status.OK;
	}

	public Status onBlockReceived(Block block) {
		// Check if no block has a hash equal to this block's previous hash

		if (!blockchain.blocks.containsKey(block.hashPrevious)) {
			return Status.MISSING_PREDECESSOR;
		}

		// Check if the node already has this block

		if (blockchain.blocks.containsKey(block.hash)) {
			return Status.DUPLICATE_BLOCK;
		}

		for (Transaction txn : block.txns) {
			for (Block chainBlock = blockchain.blocks.get(block.hashPrevious);
					chainBlock != blockchain.GENESIS;
					chainBlock = blockchain.blocks.get(chainBlock.hashPrevious)) {
				if (chainBlock.txns.contains(txn)) {
					return Status.DUPLICATE_TRANSACTION_BLOCKCHAIN;
				}
			}
		}

		// Check the validity of the block
		Status status = block.validate();
		if (status != Status.OK) {
			return status;
		}

		// Block is good, add it

		addToChain(block);
		broadcastBlock(block);
		return Status.OK;
	}

	public void onBlockchainReceived(Blockchain blockchain) {
		int countPrevious = -1;
		while (countPrevious != blockchain.blocks.size()) {
			countPrevious = blockchain.blocks.size();
			Iterator<Map.Entry<Hash, Block>> iter = blockchain.blocks.entrySet().iterator();
			while (iter.hasNext()) {
				Block block = iter.next().getValue();
				if (onBlockReceived(block) != Status.MISSING_PREDECESSOR) {
					iter.remove();
				}
			}
		}
	}

	public void onBlockMined() {
		Block blockMined = new Block(block);
		addToChain(blockMined);
		broadcastBlock(blockMined);
		block = new Block(blockchain.tip.hash);
	}
}
