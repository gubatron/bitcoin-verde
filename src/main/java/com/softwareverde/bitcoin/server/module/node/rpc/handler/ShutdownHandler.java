package com.softwareverde.bitcoin.server.module.node.rpc.handler;

import com.softwareverde.bitcoin.server.State;
import com.softwareverde.bitcoin.server.module.node.handler.SynchronizationStatusHandler;
import com.softwareverde.bitcoin.server.module.node.rpc.NodeRpcHandler;
import com.softwareverde.bitcoin.server.module.node.sync.BlockHeaderDownloader;
import com.softwareverde.bitcoin.server.module.node.sync.BlockchainBuilder;
import com.softwareverde.bitcoin.server.module.node.sync.block.BlockDownloader;
import com.softwareverde.io.Logger;

public class ShutdownHandler implements NodeRpcHandler.ShutdownHandler {
    protected final Thread _mainThread;
    protected final BlockHeaderDownloader _blockHeaderDownloader;
    protected final BlockDownloader _blockDownloader;
    protected final BlockchainBuilder _blockchainBuilder;
    protected final SynchronizationStatusHandler _synchronizationStatusHandler;

    public ShutdownHandler(final Thread mainThread, final BlockHeaderDownloader blockHeaderDownloader, final BlockDownloader blockDownloader, final BlockchainBuilder blockchainBuilder, final SynchronizationStatusHandler synchronizationStatusHandler) {
        _mainThread = mainThread;
        _blockHeaderDownloader = blockHeaderDownloader;
        _blockDownloader = blockDownloader;
        _blockchainBuilder = blockchainBuilder;
        _synchronizationStatusHandler = synchronizationStatusHandler;
    }

    @Override
    public Boolean shutdown() {
        _synchronizationStatusHandler.setState(State.SHUTTING_DOWN);

        Logger.log("[Stopping Syncing Headers]");
        _blockHeaderDownloader.stop();
        Logger.log("[Stopping Block Downloads]");
        _blockDownloader.stop();
        Logger.log("[Stopping Block Processing]");
        _blockchainBuilder.stop();
        Logger.log("[Shutting Down]");
        _mainThread.interrupt();
        return true;
    }
}
