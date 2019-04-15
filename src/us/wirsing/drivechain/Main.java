package us.wirsing.drivechain;

import us.wirsing.drivechain.blockchain.*;
import us.wirsing.drivechain.drive.CertificateAuthority;
import us.wirsing.drivechain.drive.NodeDrive;
import us.wirsing.drivechain.node.Node;
import us.wirsing.drivechain.drive.TransactionDrive;
import us.wirsing.drivechain.util.Return2;
import us.wirsing.drivechain.util.Serialization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

public class Main {

	public static Map<String, NodeDrive> users = new HashMap<>();
	public static CertificateAuthority ca = new CertificateAuthority();

	public static void main(String[] args) {
		TransactionDrive.certCa = ca.certificate;
		Scanner scanner = new Scanner(System.in);
		boolean exit = false;
		while(true) {
			System.out.print("> ");
			String[] input = scanner.nextLine().split("\\s+");
			switch(input[0].toLowerCase()) {
				case "newuser":
				case "nu":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					newUser(input[1]);
					break;
				case "connect":
				case "c":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					connect(input[1], input[2]);
					break;
				case "newtxn":
				case "nt":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					newTxn(input[1], input[2]);
					break;
				case "listusers":
				case "lu":
					if (input.length != 1) {
						wrongArgumentCount(input[0], 0, input.length - 1);
						break;
					}
					listUsers();
					break;
				case "listcxns":
				case "lc":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					listCxns(input[1]);
					break;
				case "listtxns":
				case "lt":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					listTxns(input[1]);
					break;
				case "txndetails":
				case "td":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					txnDetails(input[1], input[2]);
					break;
				case "blockdetails":
				case "bd":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					blockDetails(input[1]);
					break;
				case "chainblockdetails":
				case "cbd":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					chainBlockDetails(input[1], input[2]);
					break;
				case "chaindetails":
				case "cd":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					chainDetails(input[1]);
					break;
				case "threads":
				case "t":
					System.out.println("Threads: " + Thread.activeCount());
					break;
				case "exit":
					exit = true;
					break;
				default:
					System.out.println("Unknown command: " + input[0]);
			}

			if (exit) {
				break;
			}
		}
		scanner.close();
	}

	private static void wrongArgumentCount(String command, int correctCount, int receivedCount) {
		System.out.println("Incorrect number of arguments for " + command + ". Expected " + correctCount + ", got " + receivedCount + ".");
	}

	private static void newUser(String name) {
		if (users.get(name) != null) {
			System.out.println("Node " + name + " already exists.");
			return;
		}

		NodeDrive node = new NodeDrive(name, ca);
		node.start();
		users.put(name, node);
		System.out.println("Node " + name + " created.");
	}

	private static void connect(String name1, String name2) {
		Return2<NodeDrive, NodeDrive> users = getUserUser(name1, name2);

		if (users == null) {
			return;
		}

		Node user1 = users.ret1;
		Node user2 = users.ret2;

		user1.connect(user2);
		System.out.println(name1 + " and " + name2 + " connected.");
	}

	private static void newTxn(String name1, String name2) {
		Return2<NodeDrive, NodeDrive> userUser = getUserUser(name1, name2);

		if (userUser == null) {
			return;
		}

		NodeDrive user1 = userUser.ret1;
		NodeDrive user2 = userUser.ret2;

		TransactionDrive txn = new TransactionDrive(user1, user2);
		Node.send(Serialization.serialize(txn), Node.PACKET_TYPE_TRANSACTION, user1);
		System.out.println("New transaction between " + name1 + " and " + name2 + " created (" + txn.hash.toBase64() + "). Given to " + name1 + ".");
	}

	private static void listUsers() {
		System.out.println("Users");
		for (String name : users.keySet()) {
			System.out.println(" " + name);
		}
	}

	private static void listCxns(String name) {
		NodeDrive user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		System.out.println("Connections (" + name + ")");
		for (String cxn : user.getConnections()) {
			System.out.println(" " + cxn);
		}
	}

	private static void listTxns(String name) {
		NodeDrive user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		System.out.println("Transactions (" + name + ")");
		for (String txn : user.transactions()) {
			System.out.println(" " + txn);
		}
	}

	private static void txnDetails(String name, String txnSubstring) {
		Return2<Node, Transaction> userTxn = getUserTxn(name, txnSubstring);

		if (userTxn == null) {
			return;
		}

		TransactionDrive txn = (TransactionDrive)userTxn.ret2;

		System.out.println("TransactionDrive Details (" + name + " - " + txn.hash.toBase64() + ")");
		System.out.println(" Driver: " + txn.nameDriver);
		System.out.println(" Passenger: " + txn.namePassenger);
		System.out.println(" Timestamp: " + txn.timestamp);
	}

	private static void blockDetails(String name) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		Block block = user.getBlock();

		blockDetails(name, block);
	}

	private static void chainBlockDetails(String name, String blockSubstring) {
		Return2<Node, Block> userBlock = getUserBlock(name, blockSubstring);

		if (userBlock == null) {
			return;
		}

		blockDetails(name, userBlock.ret2);
	}

	private static void blockDetails(String name, Block block) {
		System.out.println("Block Details (" + name + ")");
		System.out.println(" Hash: " + block.hash.toBase64());
		System.out.println(" Previous Hash: " + block.hashPrevious.toBase64());
		System.out.println(" Timestamp: " + block.timestamp);
		System.out.println(" Nonce: " + block.nonce);
		System.out.println(" Transactions:");
		for (Transaction txn : block.txns) {
			System.out.println("  " + txn.hash.toBase64());
		}
	}

	private static void chainDetails(String name) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		Blockchain blockchain = user.getBlockchain();
		Block tip = blockchain.tip;
		Block block = tip;
		HashSet<Hash> activeChain = new HashSet<>();

		System.out.println("Blocks in active chain:");
		while (block != null) {
			activeChain.add(block.hash);
			System.out.println(" " + block.hash.toBase64());
			block = blockchain.blocks.get(block.hashPrevious);
		}

		System.out.println("Inactive blocks:");
		for (Block leaf : blockchain.leaves) {
			block = leaf;
			while (!activeChain.contains(block.hash)) {
				System.out.print(" " + block.hash.toBase64());
				if (block == leaf) {
					System.out.println(" (leaf)");
				} else {
					System.out.println();
				}
				block = blockchain.blocks.get(block.hashPrevious);
			}
		}
	}

	private static Return2<NodeDrive, NodeDrive> getUserUser(String name1, String name2) {
		NodeDrive user1 = users.get(name1);

		if (user1 == null) {
			System.out.println("User " + name1 + " does not exist.");
			return null;
		}

		NodeDrive user2 = users.get(name2);

		if (user2 == null) {
			System.out.println("User " + name2 + " does not exist.");
			return null;
		}

		return new Return2<>(user1, user2);
	}

	private static Return2<Node, Transaction> getUserTxn(String name, String txnSubstring) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return null;
		}

		Transaction txn = user.txnFromSubstring(txnSubstring);

		if (txn == null) {
			System.out.println("Unable to find transaction starting with " + txnSubstring);
			return null;
		}

		return new Return2<>(user, txn);
	}

	private static Return2<Node, Block> getUserBlock(String name, String blockSubstring) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return null;
		}

		Block block = user.getBlockchain().blockFromSubstring(blockSubstring);

		if (block == null) {
			System.out.println("Unable to find block starting with " + blockSubstring);
			return null;
		}

		return new Return2<>(user, block);
	}
}
