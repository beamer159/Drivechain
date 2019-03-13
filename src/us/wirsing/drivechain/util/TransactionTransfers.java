package us.wirsing.drivechain.util;

import us.wirsing.drivechain.blockchain.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionTransfers {
	public List<Transaction> addTo = new ArrayList<>();
	public List<Transaction> removeFrom = new ArrayList<>();
}
