package com.softwareverde.network.p2p.message.type;

import com.softwareverde.network.p2p.message.ProtocolMessage;

public interface PongMessage extends ProtocolMessage {
    Long getNonce();
}
