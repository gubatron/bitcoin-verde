package com.softwareverde.bitcoin.server.stratum.task;

import com.softwareverde.bitcoin.block.CanonicalMutableBlock;
import com.softwareverde.bitcoin.block.header.difficulty.Difficulty;
import com.softwareverde.bitcoin.bytearray.FragmentedBytes;
import com.softwareverde.bitcoin.hash.sha256.Sha256Hash;
import com.softwareverde.bitcoin.transaction.MutableTransaction;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionDeflater;
import com.softwareverde.bitcoin.transaction.TransactionWithFee;
import com.softwareverde.bitcoin.transaction.coinbase.CoinbaseTransaction;
import com.softwareverde.bitcoin.transaction.coinbase.MutableCoinbaseTransaction;
import com.softwareverde.bitcoin.util.BitcoinUtil;
import com.softwareverde.bitcoin.util.ByteUtil;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.io.Logger;
import com.softwareverde.util.HexUtil;
import com.softwareverde.util.type.time.SystemTime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StratumMineBlockTaskFactory {
    final static Object _mutex = new Object();
    private static Long _nextId = 1L;
    protected static Long getNextId() {
        synchronized (_mutex) {
            final Long id = _nextId;
            _nextId += 1;
            return id;
        }
    }

    protected final SystemTime _systemTime = new SystemTime();

    protected final ConcurrentHashMap<Sha256Hash, TransactionWithFee> _transactionsWithFee = new ConcurrentHashMap<Sha256Hash, TransactionWithFee>();
    protected final CanonicalMutableBlock _prototypeBlock = new CanonicalMutableBlock();
    protected final Integer _totalExtraNonceByteCount;

    protected List<String> _merkleTreeBranches; // Little-endian merkle tree (intermediary) branch hashes...
    protected String _extraNonce1;
    protected String _coinbaseTransactionHead;
    protected String _coinbaseTransactionTail;
    protected Long _blockHeight;

    protected final ReentrantReadWriteLock.ReadLock _prototypeBlockReadLock;
    protected final ReentrantReadWriteLock.WriteLock _prototypeBlockWriteLock;

    protected void _setCoinbaseTransaction(final Transaction coinbaseTransaction) {
        try {
            _prototypeBlockWriteLock.lock();

            final TransactionDeflater transactionDeflater = new TransactionDeflater();
            final FragmentedBytes coinbaseTransactionParts;
            coinbaseTransactionParts = transactionDeflater.fragmentTransaction(coinbaseTransaction);

            // NOTE: _coinbaseTransactionHead contains the unlocking script. This script contains two items:
            //  1. The Coinbase Message (ex: "/Mined via Bitcoin-Verde v0.0.1/")
            //  2. The extraNonce (which itself is composed of two components: extraNonce1 and extraNonce2...)
            // extraNonce1 is usually defined by the Mining Pool, not the Miner. The Miner is sent (by the Pool) the number
            // of bytes it should use when generating the extraNonce2 during the Pool's response to the Miner's SUBSCRIBE message.
            // Despite extraNonce just being random data, it still needs to be pushed like regular data within the unlocking script.
            //  Thus, the unlocking script is generated by pushing N bytes (0x00), where N is the byteCount of the extraNonce
            //  (extraNonceByteCount = extraNonce1ByteCount + extraNonce2ByteCount). This results in appropriate operation code
            //  being prepended to the script.  These 0x00 bytes are omitted when stored within _coinbaseTransactionHead,
            //  otherwise, the Miner would appending the extraNonce after the 0x00 bytes instead of replacing them...
            //
            //  Therefore, assuming N is 8, the 2nd part of the unlocking script would originally look something like:
            //
            //      OPCODE  | EXTRA NONCE 1         | EXTRA NONCE 2
            //      -----------------------------------------------------
            //      0x08    | 0x00 0x00 0x00 0x00   | 0x00 0x00 0x00 0x00
            //
            //  Then, stored within _coinbaseTransactionHead (to be sent to the Miner) simply as:
            //      0x08    |                       |
            //

            final Integer headByteCountExcludingExtraNonces = (coinbaseTransactionParts.headBytes.length - _totalExtraNonceByteCount);
            _coinbaseTransactionHead = HexUtil.toHexString(ByteUtil.copyBytes(coinbaseTransactionParts.headBytes, 0, headByteCountExcludingExtraNonces));
            _coinbaseTransactionTail = HexUtil.toHexString(coinbaseTransactionParts.tailBytes);

            _prototypeBlock.replaceTransaction(0, coinbaseTransaction);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public StratumMineBlockTaskFactory(final Integer totalExtraNonceByteCount) {
        _totalExtraNonceByteCount = totalExtraNonceByteCount;
        _prototypeBlock.addTransaction(new MutableTransaction());

        // NOTE: Actual nonce and timestamp are updated later within the MineBlockTask...
        _prototypeBlock.setTimestamp(0L);
        _prototypeBlock.setNonce(0L);

        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        _prototypeBlockReadLock = readWriteLock.readLock();
        _prototypeBlockWriteLock = readWriteLock.writeLock();
    }

    public void setBlockVersion(final String stratumBlockVersion) {
        try {
            _prototypeBlockWriteLock.lock();

            final Long blockVersion = ByteUtil.bytesToLong(HexUtil.hexStringToByteArray(stratumBlockVersion));
            _prototypeBlock.setVersion(blockVersion);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setBlockVersion(final Long blockVersion) {
        try {
            _prototypeBlockWriteLock.lock();

            _prototypeBlock.setVersion(blockVersion);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setPreviousBlockHash(final String stratumPreviousBlockHash) {
        try {
            _prototypeBlockWriteLock.lock();

            final Sha256Hash previousBlockHash = Sha256Hash.fromHexString(BitcoinUtil.reverseEndianString(StratumUtil.swabHexString(stratumPreviousBlockHash)));
            _prototypeBlock.setPreviousBlockHash(previousBlockHash);

        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setPreviousBlockHash(final Sha256Hash previousBlockHash) {
        try {
            _prototypeBlockWriteLock.lock();

            _prototypeBlock.setPreviousBlockHash(previousBlockHash);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setExtraNonce(final String stratumExtraNonce) {
        _extraNonce1 = stratumExtraNonce;
    }

    public void setExtraNonce(final ByteArray extraNonce) {
        _extraNonce1 = HexUtil.toHexString(extraNonce.getBytes());
    }

    public void setDifficulty(final String stratumDifficulty) {
        try {
            _prototypeBlockWriteLock.lock();

            final Difficulty difficulty = Difficulty.decode(HexUtil.hexStringToByteArray(stratumDifficulty));
            _prototypeBlock.setDifficulty(difficulty);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setDifficulty(final Difficulty difficulty) {
        try {
            _prototypeBlockWriteLock.lock();

            _prototypeBlock.setDifficulty(difficulty);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    // ViaBTC provides the merkleTreeBranches as little-endian byte strings.
    public void setMerkleTreeBranches(final List<String> merkleTreeBranches) {
        try {
            _prototypeBlockWriteLock.lock();

            _merkleTreeBranches = merkleTreeBranches.asConst();
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void addTransaction(final TransactionWithFee transactionWithFee) {
        try {
            _prototypeBlockWriteLock.lock();

            final Transaction transaction = transactionWithFee.transaction;
            final Long transactionFee = transactionWithFee.transactionFee;

            _prototypeBlock.addTransaction(transaction);
            _transactionsWithFee.put(transaction.getHash(), transactionWithFee);

            final CoinbaseTransaction coinbaseTransaction = _prototypeBlock.getCoinbaseTransaction();
            final MutableCoinbaseTransaction mutableCoinbaseTransaction = new MutableCoinbaseTransaction(coinbaseTransaction);
            final Long currentBlockReward = coinbaseTransaction.getBlockReward();
            mutableCoinbaseTransaction.setBlockReward(currentBlockReward + transactionFee);

            _setCoinbaseTransaction(mutableCoinbaseTransaction);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public CoinbaseTransaction getCoinbaseTransaction() {
        return _prototypeBlock.getCoinbaseTransaction();
    }

    public void removeTransaction(final Sha256Hash transactionHash) {
        try {
            _prototypeBlockWriteLock.lock();

            _prototypeBlock.removeTransaction(transactionHash);

            final TransactionWithFee transactionWithFee = _transactionsWithFee.get(transactionHash);
            if (transactionWithFee == null) {
                Logger.log("Unable to remove transaction from prototype block: " + transactionHash);
                return;
            }

            final Long transactionFee = transactionWithFee.transactionFee;

            final CoinbaseTransaction coinbaseTransaction = _prototypeBlock.getCoinbaseTransaction();
            final MutableCoinbaseTransaction mutableCoinbaseTransaction = new MutableCoinbaseTransaction(coinbaseTransaction);
            final Long currentBlockReward = coinbaseTransaction.getBlockReward();
            mutableCoinbaseTransaction.setBlockReward(currentBlockReward - transactionFee);

            _setCoinbaseTransaction(mutableCoinbaseTransaction);

        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void clearTransactions() {
        try {
            _prototypeBlockWriteLock.lock();

            final Transaction coinbaseTransaction = _prototypeBlock.getCoinbaseTransaction();
            _prototypeBlock.clearTransactions();
            _prototypeBlock.addTransaction(coinbaseTransaction);
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setCoinbaseTransaction(final String stratumCoinbaseTransactionHead, final String stratumCoinbaseTransactionTail) {
        try {
            _prototypeBlockWriteLock.lock();

            _coinbaseTransactionHead = stratumCoinbaseTransactionHead;
            _coinbaseTransactionTail = stratumCoinbaseTransactionTail;
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public void setCoinbaseTransaction(final Transaction coinbaseTransaction) {
        _setCoinbaseTransaction(coinbaseTransaction);
    }

    public StratumMineBlockTask buildMineBlockTask() {
        try {
            _prototypeBlockReadLock.lock();

            final ByteArray id = MutableByteArray.wrap(ByteUtil.integerToBytes(StratumMineBlockTaskFactory.getNextId()));
            return new StratumMineBlockTask(id, _prototypeBlock, _coinbaseTransactionHead, _coinbaseTransactionTail, _extraNonce1);
        }
        finally {
            _prototypeBlockReadLock.unlock();
        }
    }

    public void setBlockHeight(final Long blockHeight) {
        try {
            _prototypeBlockWriteLock.lock();

            _blockHeight = blockHeight;
        }
        finally {
            _prototypeBlockWriteLock.unlock();
        }
    }

    public Long getBlockHeight() {
        try {
            _prototypeBlockReadLock.lock();

            return _blockHeight;
        }
        finally {
            _prototypeBlockReadLock.unlock();
        }
    }
}
