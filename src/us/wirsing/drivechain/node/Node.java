package us.wirsing.drivechain.node;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import us.wirsing.drivechain.blockchain.Block;
import us.wirsing.drivechain.blockchain.Blockchain;
import us.wirsing.drivechain.blockchain.Hash;
import us.wirsing.drivechain.blockchain.Transaction;
import us.wirsing.drivechain.drive.CertificateAuthority;
import us.wirsing.drivechain.util.Crypto;
import us.wirsing.drivechain.util.Serialization;
import us.wirsing.drivechain.util.TransactionTransfers;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.*;

public class Node {

	public static final byte PACKET_TYPE_TRANSACTION = 0;
	public static final byte PACKET_TYPE_BLOCK = 1;
	public static final byte PACKET_TYPE_BLOCKCHAIN = 2;

	private String name;
	private KeyPair keyPair = Crypto.generateKey();
	private X509Certificate certNode;
	private List<Node> cxns = new ArrayList<>();
	private Blockchain blockchain = new Blockchain();
	Block block = new Block(blockchain.tip.hash);
	private Set<Transaction> txnsUnconfirmed = new HashSet<>();

	// Constructors

	public Node(String name, CertificateAuthority ca) {
		this.name = name;
		this.certNode = ca.issueCertificate(generateCsr());
	}

	// Information Gathering Methods

	public String getName() {
		return name;
	}

	public X509Certificate getCertificate() {
		return certNode;
	}

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

	/**
	 * @return array of names of nodes connected to this node
	 */
	public String[] getConnections() {
		String[] ret = new String[cxns.size()];
		int i = 0;
		for (Node node : cxns) {
			ret[i++] = node.getName();
		}
		return ret;
	}

	// Manually set the node's block's previous hash

	public void setPrevHash(Hash hash) {
		block.hashPrevious = hash;
	}

	// Connects two users in the network

	public void connect(Node node) {
		if (!cxns.contains(node)) {
			cxns.add(node);
		}
		if (!node.cxns.contains(this)) {
			node.cxns.add(this);
		}
	}

	public String[] transactions() {
		return transactions(txnsUnconfirmed);
	}

	public String[] blockTransactions() {
		return transactions(block.txns);
	}

	private String[] transactions(Set<Transaction> transactions) {
		String[] ret = new String[transactions.size()];
		int i = 0;
		for (Transaction txn : transactions) {
			ret[i++] = txn.hash.toBase64();
		}
		return ret;
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

	public PublicKey publicKey() {
		return keyPair.getPublic();
	}

	public void broadcast(byte[] payload, byte header) {
		for (Node node : cxns) {
			send(payload, header, node);
		}
	}

	public void send(byte[] payload, byte header, Node receiver) {
		byte[] packet = new byte[payload.length + 1];
		packet[0] = header;
		System.arraycopy(payload, 0, packet, 1, payload.length);
		receiver.receive(packet);
	}

	public void addToChain() {
		addToChain(new Block(block));
		block.txns.clear();
	}

	public void addToChain(Block block) {
		TransactionTransfers txnTransfers = blockchain.add(block);

		for (Transaction txn : txnTransfers.addTo) {
			txnsUnconfirmed.add(txn);
		}

		for (Transaction txn : txnTransfers.removeFrom) {
			txnsUnconfirmed.remove(txn);
			removeFromBlock(txn);
		}

		this.block.hashPrevious = blockchain.tip.hash;
	}

	public void receive(byte[] packet) {

		byte header = packet[0];
		byte[] payload = Arrays.copyOfRange(packet, 1, packet.length);

		switch (header) {
			case PACKET_TYPE_TRANSACTION:
				receiveTxn(Serialization.deserialize(payload));
				break;
			case PACKET_TYPE_BLOCK:
				receiveBlock(Serialization.deserialize(payload));
				break;
			case PACKET_TYPE_BLOCKCHAIN:
				break;
			default:
				break;
		}
	}

	public void addTransaction(Transaction txn) {
		txnsUnconfirmed.add(txn);
	}

	public boolean receiveTxn(Transaction txn) {

		System.out.print(name + " received transaction " + txn.hash.toBase64() + " - ");

		// Check the validity of the transaction


		if (!txn.validate()) {
			System.out.println("Invalid");
			return false;
		}

		// Check if the node already has this transaction

		for (Transaction t : txnsUnconfirmed) {
			if (t.hash.equals(txn.hash)) {
				System.out.println("Duplicate");
				return false;
			}
		}

		// TransactionDrive is good, add it

		System.out.println("Valid");
		txnsUnconfirmed.add(txn);

		broadcast(Serialization.serialize(txn), PACKET_TYPE_TRANSACTION);

		return true;
	}

	public boolean receiveBlock(Block block) {
		System.out.print(name + " received block " + block.hash.toBase64() + " - ");

		// Check the validity of the block

		if (!block.validate()) {
			System.out.println("Invalid");
			return false;
		}

		// Check if the node already has this block

		if (blockchain.blocks.containsKey(block.hash) || block.hash.equals(this.block.hash)) {
			System.out.println("Duplicate");
			return false;
		}

		// Block is good, add it

		System.out.println("Valid");
		addToChain(block);

		broadcast(Serialization.serialize(new Block(block)), PACKET_TYPE_BLOCK);

		return true;
	}

	public void mine() {
		new Thread(new Miner(this)).start();
	}

	public void mine(long runtimeMin) {
		new Thread(new Miner(this, runtimeMin)).start();
	}

	public PKCS10CertificationRequest generateCsr() {
		PKCS10CertificationRequest csr;
		PKCS10CertificationRequestBuilder builderCsr = new JcaPKCS10CertificationRequestBuilder(
				new X500Name("CN=" + name), keyPair.getPublic());
		try {
			ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRsa").build(keyPair.getPrivate());
			return builderCsr.build(contentSigner);
		} catch (OperatorCreationException e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] sign(byte[] message) {
		return Crypto.sign(message, keyPair.getPrivate());
	}
}
