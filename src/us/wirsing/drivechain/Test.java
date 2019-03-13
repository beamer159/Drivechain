package us.wirsing.drivechain;

import us.wirsing.drivechain.blockchain.Block;
import us.wirsing.drivechain.blockchain.Blockchain;
import us.wirsing.drivechain.blockchain.Transaction;
import us.wirsing.drivechain.blockchain.TransactionDrive;
import us.wirsing.drivechain.node.Node;

import java.util.Arrays;

public class Test {
    public static void main(String[] args) {
        Node user1 = new Node("Alice");
        Node user2 = new Node("Bob");
        Transaction txn = new TransactionDrive(user1, user2);
        Blockchain chain = new Blockchain();
        Block block = new Block(chain.tip.hash);
        block.add(txn);
        while (!Arrays.equals(new byte[Block.DIFFICULTY], Arrays.copyOf(block.hash.bytes, Block.DIFFICULTY))) {
            block.setNonce(block.nonce + 1);
        }
        chain.add(block);
        System.out.println(chain.validate());
    }
}
