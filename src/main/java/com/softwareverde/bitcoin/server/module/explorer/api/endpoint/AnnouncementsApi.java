package com.softwareverde.bitcoin.server.module.explorer.api.endpoint;

import com.softwareverde.bitcoin.server.Configuration;
import com.softwareverde.bitcoin.server.module.node.rpc.NodeJsonRpcConnection;
import com.softwareverde.concurrent.pool.MainThreadPool;
import com.softwareverde.http.server.servlet.WebSocketServlet;
import com.softwareverde.http.server.servlet.request.WebSocketRequest;
import com.softwareverde.http.server.servlet.response.WebSocketResponse;
import com.softwareverde.http.websocket.WebSocket;
import com.softwareverde.io.Logger;
import com.softwareverde.json.Json;
import com.softwareverde.network.socket.JsonSocket;
import com.softwareverde.util.RotatingQueue;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AnnouncementsApi implements WebSocketServlet {
    protected static final Object MUTEX = new Object();
    protected static final HashMap<Long, WebSocket> WEB_SOCKETS = new HashMap<Long, WebSocket>();

    protected static final ReentrantReadWriteLock.ReadLock QUEUE_READ_LOCK;
    protected static final ReentrantReadWriteLock.WriteLock QUEUE_WRITE_LOCK;
    static {
        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        QUEUE_READ_LOCK = readWriteLock.readLock();
        QUEUE_WRITE_LOCK = readWriteLock.writeLock();
    }

    protected static final RotatingQueue<Json> BLOCK_HEADERS = new RotatingQueue<Json>(10);
    protected static final RotatingQueue<Json> TRANSACTIONS = new RotatingQueue<Json>(32);

    protected static final AtomicLong _nextSocketId = new AtomicLong(1L);

    protected final Configuration.ExplorerProperties _explorerProperties;
    protected final Object _socketConnectionMutex = new Object();
    protected Boolean _isShuttingDown = false;
    protected JsonSocket _socketConnection = null;

    protected final NodeJsonRpcConnection.AnnouncementHookCallback _announcementHookCallback = new NodeJsonRpcConnection.AnnouncementHookCallback() {
        @Override
        public void onNewBlockHeader(final Json blockJson) {
            _onNewBlock(blockJson);
        }

        @Override
        public void onNewTransaction(final Json transactionJson) {
            _onNewTransaction(transactionJson);
        }
    };

    protected Json _wrapObject(final String objectType, final Json object) {
        final Json json = new Json();
        json.put("objectType", objectType);
        json.put("object", object);
        return json;
    }

    // NOTE: A light JSON message is sent instead of the whole Transaction Json in order to keep WebSocket._maxPacketByteCount small...
    protected Json _transactionJsonToTransactionHashJson(final Json transactionJson) {
        final Json transactionHashJson = new Json(false);
        transactionHashJson.put("hash", transactionJson.getString("hash"));
        return transactionHashJson;
    }

    protected void _checkRpcConnection() {
        { // Lock-less check...
            final JsonSocket jsonSocket = _socketConnection;
            if ((jsonSocket != null) && (jsonSocket.isConnected())) {
                return;
            }
        }

        synchronized (_socketConnectionMutex) {
            { // Locked, 2nd check...
                final JsonSocket jsonSocket = _socketConnection;
                if ((jsonSocket != null) && (jsonSocket.isConnected())) {
                    return;
                }
            }

            if (_isShuttingDown) { return; }

            final String bitcoinRpcUrl = _explorerProperties.getBitcoinRpcUrl();
            final Integer bitcoinRpcPort = _explorerProperties.getBitcoinRpcPort();
            _socketConnection = null;

            try {
                final NodeJsonRpcConnection nodeJsonRpcConnection = new NodeJsonRpcConnection(bitcoinRpcUrl, bitcoinRpcPort, _threadPool);
                final Boolean wasSuccessful = nodeJsonRpcConnection.upgradeToAnnouncementHook(_announcementHookCallback);
                if (wasSuccessful) {
                    _socketConnection = nodeJsonRpcConnection.getJsonSocket();
                }
            }
            catch (final Exception exception) {
                Logger.log(exception);
            }
        }
    }

    public AnnouncementsApi(final Configuration.ExplorerProperties explorerProperties) {
        _explorerProperties = explorerProperties;
    }

    protected final MainThreadPool _threadPool = new MainThreadPool(256, 1000L);

    protected void _broadcastNewBlockHeader(final Json blockHeaderJson) {
        final String message;
        {
            final Json messageJson = _wrapObject("BLOCK", blockHeaderJson);
            message = messageJson.toString();
        }

        synchronized (MUTEX) {
            for (final WebSocket webSocket : WEB_SOCKETS.values()) {
                webSocket.sendMessage(message);
            }
        }
    }

    protected void _broadcastNewTransaction(final Json transactionJson) {
        final String message;
        {
            final Json trimmedTransactionJson = _transactionJsonToTransactionHashJson(transactionJson);
            final Json messageJson = _wrapObject("TRANSACTION_HASH", trimmedTransactionJson);
            message = messageJson.toString();
        }

        synchronized (MUTEX) {
            for (final WebSocket webSocket : WEB_SOCKETS.values()) {
                webSocket.sendMessage(message);
            }
        }
    }

    protected void _onNewBlock(final Json blockJson) {
        try {
            QUEUE_WRITE_LOCK.lock();

            BLOCK_HEADERS.add(blockJson);
        }
        finally {
            QUEUE_WRITE_LOCK.unlock();
        }

        _broadcastNewBlockHeader(blockJson);
    }

    protected void _onNewTransaction(final Json transactionJson) {
        try {
            QUEUE_WRITE_LOCK.lock();

            TRANSACTIONS.add(transactionJson);
        }
        finally {
            QUEUE_WRITE_LOCK.unlock();
        }

        _broadcastNewTransaction(transactionJson);
    }

    @Override
    public WebSocketResponse onRequest(final WebSocketRequest webSocketRequest) {
        final WebSocketResponse webSocketResponse = new WebSocketResponse();
        if (! _isShuttingDown) {
            final Long webSocketId = _nextSocketId.getAndIncrement();
            webSocketResponse.setWebSocketId(webSocketId);
            webSocketResponse.upgradeToWebSocket();
        }
        return webSocketResponse;
    }

    @Override
    public void onNewWebSocket(final WebSocket webSocket) {
        if (_isShuttingDown) {
            webSocket.close();
            return;
        }

        _checkRpcConnection();

        final Long webSocketId = webSocket.getId();
        synchronized (MUTEX) {
            WEB_SOCKETS.put(webSocketId, webSocket);
        }

        // webSocket.setMessageReceivedCallback(new WebSocket.MessageReceivedCallback() {
        //     @Override
        //     public void onMessage(final String message) {
        //         // Nothing.
        //     }
        // });

        webSocket.setConnectionClosedCallback(new WebSocket.ConnectionClosedCallback() {
            @Override
            public void onClose(final int code, final String message) {
                synchronized (MUTEX) {
                    WEB_SOCKETS.remove(webSocketId);
                }
            }
        });

        // webSocket.startListening();

        try {
            QUEUE_READ_LOCK.lock();

            for (final Json blockHeaderJson : BLOCK_HEADERS) {
                final Json messageJson = _wrapObject("BLOCK", blockHeaderJson);
                final String message = messageJson.toString();
                webSocket.sendMessage(message);
            }

            for (final Json transactionJson : TRANSACTIONS) {
                final Json trimmedTransactionJson = _transactionJsonToTransactionHashJson(transactionJson);
                final Json messageJson = _wrapObject("TRANSACTION_HASH", trimmedTransactionJson);
                final String message = messageJson.toString();
                webSocket.sendMessage(message);
            }
        }
        finally {
            QUEUE_READ_LOCK.unlock();
        }
    }

    public void shutdown() {
        _isShuttingDown = true;

        _threadPool.stop();

        synchronized (_socketConnectionMutex) {
            if (_socketConnection != null) {
                _socketConnection.close();
            }
        }

        synchronized (MUTEX) {
            for (final WebSocket webSocket : WEB_SOCKETS.values()) {
                webSocket.close();
            }
            WEB_SOCKETS.clear();
        }
    }
}
