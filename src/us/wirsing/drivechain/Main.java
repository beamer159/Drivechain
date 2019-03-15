package us.wirsing.drivechain;

import us.wirsing.drivechain.blockchain.*;
import us.wirsing.drivechain.drive.CertificateAuthority;
import us.wirsing.drivechain.node.Node;
import us.wirsing.drivechain.drive.TransactionDrive;
import us.wirsing.drivechain.util.Return2;
import us.wirsing.drivechain.util.Serialization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

public class Main {

	public static Map<String, Node> users = new HashMap<String, Node>();
	public static CertificateAuthority ca = new CertificateAuthority();

	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		boolean exit = false;
		while(true) {
			System.out.print("> ");
			String[] input = scanner.nextLine().split("\\s+");
			switch(input[0].toLowerCase()) {
				case "newuser":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					newUser(input[1]);
					break;
				case "listusers":
					if (input.length != 1) {
						wrongArgumentCount(input[0], 0, input.length - 1);
						break;
					}
					listUsers();
					break;
				case "connect":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					connect(input[1], input[2]);
					break;
				case "listcxns":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					listCxns(input[1]);
					break;
				case "newtxn":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					newTxn(input[1], input[2]);
					break;
				case "listtxns":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					listTxns(input[1]);
					break;
				case "txndetails":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					txnDetails(input[1], input[2]);
					break;
				case "broadcasttxn":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					broadcastTxn(input[1], input[2]);
					break;
				case "addtoblock":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					addToBlock(input[1], input[2]);
					break;
				case "blockdetails":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					blockDetails(input[1]);
					break;
				case "chainblockdetails":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					chainBlockDetails(input[1], input[2]);
					break;
				case "setprevblock":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					setPrevBlock(input[1], input[2]);
					break;
				case "mine":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					mine(input[1]);
					break;
				case "minetime":
					if (input.length != 3) {
						wrongArgumentCount(input[0], 2, input.length - 1);
						break;
					}
					mineTime(input[1], input[2]);
					break;
				case "broadcastblock":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					broadcastBlock(input[1]);
					break;
				case "addtochain":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					addToChain(input[1]);
					break;
				case "chaindetails":
					if (input.length != 2) {
						wrongArgumentCount(input[0], 1, input.length - 1);
						break;
					}
					chainDetails(input[1]);
					break;
				case "changetxndriver":
					if (input.length != 4) {
						wrongArgumentCount(input[0], 3, input.length - 1);
						break;
					}
					changeTxnDriver(input[1], input[2], input[3]);
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

		users.put(name, new Node(name, ca));
		System.out.println("Node " + name + " created.");
	}

	private static void listUsers() {
		System.out.println("Users");
		for (String name : users.keySet()) {
			System.out.println(" " + name);
		}
	}

	private static void connect(String name1, String name2) {
		Return2<Node, Node> users = getUserUser(name1, name2);

		if (users == null) {
			return;
		}

		Node user1 = users.ret1;
		Node user2 = users.ret2;

		user1.connect(user2);
		System.out.println(name1 + " and " + name2 + " connected.");
	}

	private static void listCxns(String name) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		System.out.println("Connections (" + name + ")");
		for (String cxn : user.getConnections()) {
			System.out.println(" " + cxn);
		}
	}

	private static void newTxn(String name1, String name2) {
		Return2<Node, Node> userUser = getUserUser(name1, name2);

		if (userUser == null) {
			return;
		}

		Node user1 = userUser.ret1;
		Node user2 = userUser.ret2;

		TransactionDrive txn = new TransactionDrive(user1, user2);
		user1.addTransaction(txn);
		System.out.println("New transaction between " + name1 + " and " + name2 + " created (" + txn.hash.toBase64() + "). Given to " + name1 + ".");
	}

	private static void listTxns(String name) {
		Node user = users.get(name);

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

	private static void broadcastTxn(String name, String txnSubstring) {
		Return2<Node, Transaction> userTxn = getUserTxn(name, txnSubstring);

		if (userTxn == null) {
			return;
		}

		Node user = userTxn.ret1;
		Transaction txn = userTxn.ret2;

		user.broadcast(Serialization.serialize(txn), Node.PACKET_TYPE_TRANSACTION);

		System.out.println(name + " broadcasted transaction " + txn.hash.toBase64());
	}

	private static void addToBlock(String name, String txnSubstring) {
		Return2<Node, Transaction> userTxn = getUserTxn(name, txnSubstring);

		if (userTxn == null) {
			return;
		}

		Node user = userTxn.ret1;
		Transaction txn = userTxn.ret2;

		if (!user.addToBlock(txn)) {
			System.out.println("TransactionDrive already in " + name + "'s block: " + txn.hash.toBase64());
			return;
		}

		System.out.println("TransactionDrive added to " + name + "'s block: " + txn.hash.toBase64());
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

	private static void setPrevBlock(String name, String blockSubstring) {
		Return2<Node, Block> userBlock = getUserBlock(name, blockSubstring);

		if (userBlock == null) {
			return;
		}

		Hash hash = userBlock.ret2.hash;

		userBlock.ret1.setPrevHash(hash);

		System.out.println(name + "'s block's previous block set to " + hash.toBase64());
	}

	private static void mine(String name) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		System.out.println(name + " started mining...");
		user.mine();
	}

	private static void mineTime(String name, String time) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		System.out.println(name + " started mining...");
		user.mine(Long.parseLong(time));
	}

	private static void broadcastBlock(String name) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		Block block = user.getBlock();
		user.broadcast(Serialization.serialize(block), Node.PACKET_TYPE_BLOCK);

		System.out.println(name + " broadcasted block " + block.hash.toBase64());
	}

	private static void addToChain(String name) {
		Node user = users.get(name);

		if (user == null) {
			System.out.println("Node " + name + " does not exist.");
			return;
		}

		user.addToChain();
		System.out.println(name + " added block to chain");
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

	private static void changeTxnDriver(String name, String txnSubstring, String newDriver) {
		Return2<Node, Transaction> userTxn = getUserTxn(name, txnSubstring);

		if (userTxn == null) {
			return;
		}

		Transaction txn = userTxn.ret2;
		((TransactionDrive)txn).nameDriver = newDriver;
	}

	private static Return2<Node, Node> getUserUser(String name1, String name2) {
		Node user1 = users.get(name1);

		if (user1 == null) {
			System.out.println("User " + name1 + " does not exist.");
			return null;
		}

		Node user2 = users.get(name2);

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
