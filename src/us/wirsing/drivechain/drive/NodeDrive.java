package us.wirsing.drivechain.drive;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import us.wirsing.drivechain.blockchain.Block;
import us.wirsing.drivechain.blockchain.Transaction;
import us.wirsing.drivechain.node.Miner;
import us.wirsing.drivechain.node.Node;
import us.wirsing.drivechain.node.Status;
import us.wirsing.drivechain.util.Crypto;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Set;

public class NodeDrive extends Node {

	private String name;
	private KeyPair keyPair = Crypto.generateKey();
	private X509Certificate certNode;

	// Constructors

	public NodeDrive(String name, CertificateAuthority ca) {
		this.name = name;
		this.certNode = ca.issueCertificate(generateCsr());
	}

	@Override
	public void start() {
		new Thread(new MinerDrive(this)).start();
	}

	public String getName() {
		return name;
	}

	public X509Certificate getCertificate() {
		return certNode;
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

	/**
	 * @return array of names of nodes connected to this node
	 */
	public String[] getConnections() {
		String[] ret = new String[cxns.size()];
		int i = 0;
		for (Node node : cxns) {
			ret[i++] = ((NodeDrive)node).getName();
		}
		return ret;
	}

	public byte[] sign(byte[] message) {
		return Crypto.sign(message, keyPair.getPrivate());
	}

	@Override
	public Status onTxnReceived(Transaction txn) {
		Status status = super.onTxnReceived(txn);
		System.out.println(name + " received transaction " + txn.hash.toBase64() + ": " + printStatus(status));
		return status;
	}

	@Override
	public Status onBlockReceived(Block block) {
		Status status = super.onBlockReceived(block);
		System.out.println(name + " received block " + block.hash.toBase64() + ": " + printStatus(status));
		return status;
	}

	private PKCS10CertificationRequest generateCsr() {
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

	private String printStatus(Status status) {
		switch (status) {
			case OK:
				return "Ok";
			case DUPLICATE_TRANSACTION_UNCONFIRMED:
				return "Duplicate unconfirmed transaction";
			case DUPLICATE_TRANSACTION_BLOCKCHAIN:
				return "Duplicate blockchain transaction";
			case DUPLICATE_BLOCK:
				return "Duplicate block";
			case INVALID_TRANSACTION:
				return "Invalid transaction";
			case INVALID_BLOCK:
				return "Invalid block";
			case MISSING_PREDECESSOR:
				return "Missing predecessor block";
			default:
				return "";
		}
	}
}
