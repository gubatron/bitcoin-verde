package com.softwareverde.bitcoin.server.module.node.handler.block;

import com.softwareverde.bitcoin.block.BlockId;
import com.softwareverde.bitcoin.server.database.BlockDatabaseManager;
import com.softwareverde.bitcoin.server.message.type.query.block.QueryBlocksMessage;
import com.softwareverde.bitcoin.server.message.type.query.response.QueryResponseMessage;
import com.softwareverde.bitcoin.server.message.type.query.response.hash.InventoryItem;
import com.softwareverde.bitcoin.server.message.type.query.response.hash.InventoryItemType;
import com.softwareverde.bitcoin.server.module.node.handler.AbstractQueryBlocksHandler;
import com.softwareverde.bitcoin.server.node.BitcoinNode;
import com.softwareverde.bitcoin.type.hash.sha256.Sha256Hash;
import com.softwareverde.constable.list.List;
import com.softwareverde.database.DatabaseException;
import com.softwareverde.database.mysql.MysqlDatabaseConnection;
import com.softwareverde.database.mysql.MysqlDatabaseConnectionFactory;
import com.softwareverde.io.Logger;
import com.softwareverde.network.p2p.node.NodeConnection;

public class QueryBlocksHandler extends AbstractQueryBlocksHandler implements BitcoinNode.QueryBlocksCallback {
    public static final BitcoinNode.QueryBlocksCallback IGNORE_REQUESTS_HANDLER = new BitcoinNode.QueryBlocksCallback() {
        @Override
        public void run(final List<Sha256Hash> blockHashes, final Sha256Hash desiredBlockHash, final NodeConnection nodeConnection) { }
    };

    public QueryBlocksHandler(final MysqlDatabaseConnectionFactory databaseConnectionFactory) {
        super(databaseConnectionFactory);
    }

    @Override
    public void run(final List<Sha256Hash> blockHashes, final Sha256Hash desiredBlockHash, final NodeConnection nodeConnection) {
        try (final MysqlDatabaseConnection databaseConnection = _databaseConnectionFactory.newConnection()) {
            final BlockDatabaseManager blockDatabaseManager = new BlockDatabaseManager(databaseConnection);

            final StartingBlock startingBlock = _getStartingBlock(blockHashes, desiredBlockHash, databaseConnection);

            if (startingBlock == null) {
                Logger.log("Unable to send blocks: No blocks available.");
                return;
            }

            final QueryResponseMessage responseMessage = new QueryResponseMessage();
            {
                final List<BlockId> childrenBlockIds = _findBlockChildrenIds(startingBlock.startingBlockId, desiredBlockHash, startingBlock.selectedBlockChainSegmentId, QueryBlocksMessage.MAX_BLOCK_HASH_COUNT, blockDatabaseManager);
                for (final BlockId blockId : childrenBlockIds) {
                    final Sha256Hash blockHash = blockDatabaseManager.getBlockHashFromId(blockId);
                    responseMessage.addInventoryItem(new InventoryItem(InventoryItemType.BLOCK, blockHash));
                }
            }

            { // Debug Logging...
                final Sha256Hash firstBlockHash = ((! blockHashes.isEmpty()) ? blockHashes.get(0) : null);
                final List<InventoryItem> responseHashes = responseMessage.getInventoryItems();
                final Sha256Hash responseHash = ((! responseHashes.isEmpty()) ? responseHashes.get(0).getObjectHash() : null);
                Logger.log("QueryBlocksHandler : " + firstBlockHash + " - " + desiredBlockHash + " -> " + responseHash);
            }

            nodeConnection.queueMessage(responseMessage);
        }
        catch (final DatabaseException exception) { Logger.log(exception); }
    }
}