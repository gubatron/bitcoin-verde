package com.softwareverde.bitcoin.server.module.stratum;

import com.softwareverde.bitcoin.block.MutableBlock;
import com.softwareverde.bitcoin.block.header.BlockHeader;
import com.softwareverde.bitcoin.block.header.MutableBlockHeader;
import com.softwareverde.bitcoin.block.header.difficulty.Difficulty;
import com.softwareverde.bitcoin.block.header.difficulty.ImmutableDifficulty;
import com.softwareverde.bitcoin.server.stratum.message.RequestMessage;
import com.softwareverde.bitcoin.transaction.Transaction;
import com.softwareverde.bitcoin.transaction.TransactionDeflater;
import com.softwareverde.bitcoin.transaction.TransactionInflater;
import com.softwareverde.bitcoin.type.bytearray.FragmentedBytes;
import com.softwareverde.bitcoin.type.hash.Hash;
import com.softwareverde.bitcoin.type.hash.MutableHash;
import com.softwareverde.bitcoin.type.merkleroot.ImmutableMerkleRoot;
import com.softwareverde.bitcoin.type.merkleroot.MerkleRoot;
import com.softwareverde.bitcoin.util.BitcoinUtil;
import com.softwareverde.bitcoin.util.ByteUtil;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayBuilder;
import com.softwareverde.constable.bytearray.ByteArray;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.constable.list.List;
import com.softwareverde.constable.list.immutable.ImmutableListBuilder;
import com.softwareverde.io.Logger;
import com.softwareverde.json.Json;
import com.softwareverde.util.HexUtil;

public class StratumMineBlockTask {
    final static Object _mutex = new Object();
    private static Long _nextId = 1L;
    protected static Long getNextId() {
        synchronized (_mutex) {
            final Long id = _nextId;
            _nextId += 1;
            return id;
        }
    }

    final ByteArray _id;

    protected final MutableBlock _prototypeBlock = new MutableBlock();
    protected List<String> _merkleTreeBranches;
    protected String _extraNonce1;
    protected String _coinbaseTransactionHead;
    protected String _coinbaseTransactionTail;

    private static MerkleRoot _calculateMerkleTree(final Transaction coinbaseTransaction, final List<String> merkleTreeBranches) {
        final ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder();

        byte[] merkleRoot = coinbaseTransaction.getHash().toReversedEndian().getBytes();

        for (final String merkleBranch : merkleTreeBranches) {
            final byte[] concatenatedHashes;
            {
                byteArrayBuilder.appendBytes(merkleRoot);
                byteArrayBuilder.appendBytes(HexUtil.hexStringToByteArray(merkleBranch));
                concatenatedHashes = byteArrayBuilder.build();
                byteArrayBuilder.clear();
            }

            merkleRoot = BitcoinUtil.sha256(BitcoinUtil.sha256(concatenatedHashes));
        }

        return new ImmutableMerkleRoot(ByteUtil.reverseEndian(merkleRoot));
    }

    private String _createByteString(final char a, final char b) {
        return String.valueOf(a) + b;
    }

    private String _reverseEndian(final String input) {
        if (input.length() % 2 != 0) {
            Logger.log("reverseEndian: Invalid Hex String: "+ input);
            return null;
        }

        final StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<input.length()/2; ++i) {
            final int index = (input.length() - (i * 2) - 2);
            final String byteString = _createByteString(input.charAt(index), input.charAt(index + 1));
            stringBuilder.append(byteString);
        }
        return stringBuilder.toString();
    }

    private String _swabBytes(final String input) {
        // 00 01 02 03 | 04 05 06 07 -> 03 02 01 00 | 07 06 05 04
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i=0; i<input.length()/8; ++i) {
            final int index = (i*8);
            final String byteString0 = _createByteString(input.charAt(index + 0), input.charAt(index + 1));
            final String byteString1 = _createByteString(input.charAt(index + 2), input.charAt(index + 3));
            final String byteString2 = _createByteString(input.charAt(index + 4), input.charAt(index + 5));
            final String byteString3 = _createByteString(input.charAt(index + 6), input.charAt(index + 7));

            stringBuilder.append(byteString3);
            stringBuilder.append(byteString2);
            stringBuilder.append(byteString1);
            stringBuilder.append(byteString0);
        }
        return stringBuilder.toString();
    }

    public StratumMineBlockTask() {
        _id = MutableByteArray.wrap(ByteUtil.integerToBytes(getNextId()));
    }

    public void setBlockVersion(final String stratumBlockVersion) {
        final Integer blockVersion = ByteUtil.bytesToInteger(HexUtil.hexStringToByteArray(stratumBlockVersion));
        _prototypeBlock.setVersion(blockVersion);
    }

    public void setBlockVersion(final Integer blockVersion) {
        _prototypeBlock.setVersion(blockVersion);
    }

    public void setPreviousBlockHash(final String stratumPreviousBlockHash) {
        final Hash previousBlockHash = MutableHash.fromHexString(_reverseEndian(_swabBytes(stratumPreviousBlockHash)));
        _prototypeBlock.setPreviousBlockHash(previousBlockHash);
    }

    public void setPreviousBlockHash(final Hash previousBlockHash) {
        _prototypeBlock.setPreviousBlockHash(previousBlockHash);
    }

    public void setExtraNonce(final String stratumExtraNonce) {
        _extraNonce1 = stratumExtraNonce;
    }

    public void setExtraNonce(final ByteArray extraNonce) {
        _extraNonce1 = HexUtil.toHexString(extraNonce.getBytes());
    }

    public void setDifficulty(final String stratumDifficulty) {
        final Difficulty difficulty = ImmutableDifficulty.decode(HexUtil.hexStringToByteArray(stratumDifficulty));
        _prototypeBlock.setDifficulty(difficulty);
    }

    public void setDifficulty(final Difficulty difficulty) {
        _prototypeBlock.setDifficulty(difficulty);
    }

    public void setMerkleTreeBranches(final List<String> merkleTreeBranches) {
        _merkleTreeBranches = merkleTreeBranches.asConst();
    }

    public void setCoinbaseTransaction(final String stratumCoinbaseTransactionHead, final String stratumCoinbaseTransactionTail) {
        _coinbaseTransactionHead = stratumCoinbaseTransactionHead;
        _coinbaseTransactionTail = stratumCoinbaseTransactionTail;
    }

    public void setCoinbaseTransaction(final Transaction coinbaseTransaction, final Integer totalExtraNonceByteCount) {
        final TransactionDeflater transactionDeflater = new TransactionDeflater();
        final FragmentedBytes coinbaseTransactionParts;
        coinbaseTransactionParts = transactionDeflater.fragmentTransaction(coinbaseTransaction);

        final Integer headByteCountExcludingExtraNonces = (coinbaseTransactionParts.headBytes.length - totalExtraNonceByteCount);
        _coinbaseTransactionHead = HexUtil.toHexString(ByteUtil.copyBytes(coinbaseTransactionParts.headBytes, 0, headByteCountExcludingExtraNonces));
        _coinbaseTransactionTail = HexUtil.toHexString(coinbaseTransactionParts.tailBytes);
    }

    public BlockHeader assembleBlockHeader(final String stratumNonce, final String stratumExtraNonce2, final String stratumTimestamp) {
        final MutableBlockHeader blockHeader = new MutableBlockHeader(_prototypeBlock);

        final TransactionInflater transactionInflater = new TransactionInflater();

        final MerkleRoot merkleRoot;
        {
            final Transaction coinbaseTransaction = transactionInflater.fromBytes(HexUtil.hexStringToByteArray(
                _coinbaseTransactionHead +
                _extraNonce1 +
                stratumExtraNonce2 +
                _coinbaseTransactionTail
            ));

            merkleRoot = _calculateMerkleTree(coinbaseTransaction, _merkleTreeBranches);
        }
        blockHeader.setMerkleRoot(merkleRoot);

        blockHeader.setNonce(ByteUtil.bytesToLong(HexUtil.hexStringToByteArray(stratumNonce)));

        final Long timestamp = ByteUtil.bytesToLong(HexUtil.hexStringToByteArray(stratumTimestamp));
        blockHeader.setTimestamp(timestamp);

        return blockHeader;
    }

    public RequestMessage createRequest() {
        final RequestMessage mineBlockMessage = new RequestMessage(RequestMessage.ServerCommand.NOTIFY.getValue());

        final Long timestamp = (System.currentTimeMillis() / 1000L);

        final Json parametersJson = new Json(true);
        parametersJson.add(HexUtil.toHexString(_id.getBytes()));
        parametersJson.add(_swabBytes(_reverseEndian(HexUtil.toHexString(_prototypeBlock.getPreviousBlockHash().getBytes()))));
        parametersJson.add(_coinbaseTransactionHead);
        parametersJson.add(_coinbaseTransactionTail);

        final Json partialMerkleTreeJson = new Json(true);
        { // Create the partialMerkleTree Json...
            final ImmutableListBuilder<String> listBuilder = new ImmutableListBuilder<String>();
            final List<Hash> partialMerkleTree = _prototypeBlock.getPartialMerkleTree(0);
            for (final Hash hash : partialMerkleTree) {
                final String hashString = hash.toString();
                partialMerkleTreeJson.add(hashString);
                listBuilder.add(hashString);
            }
            _merkleTreeBranches = listBuilder.build();
        }
        parametersJson.add(partialMerkleTreeJson);

        parametersJson.add(HexUtil.toHexString(ByteUtil.integerToBytes(_prototypeBlock.getVersion())));
        parametersJson.add(HexUtil.toHexString(_prototypeBlock.getDifficulty().encode()));
        parametersJson.add(HexUtil.toHexString(ByteUtil.integerToBytes(timestamp)));
        parametersJson.add(true);

        mineBlockMessage.setParameters(parametersJson);

        return mineBlockMessage;
    }
}