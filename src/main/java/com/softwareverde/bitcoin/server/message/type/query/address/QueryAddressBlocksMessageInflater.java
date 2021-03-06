package com.softwareverde.bitcoin.server.message.type.query.address;

import com.softwareverde.bitcoin.address.Address;
import com.softwareverde.bitcoin.address.AddressInflater;
import com.softwareverde.bitcoin.server.message.BitcoinProtocolMessageInflater;
import com.softwareverde.bitcoin.server.message.header.BitcoinProtocolMessageHeader;
import com.softwareverde.bitcoin.server.message.type.MessageType;
import com.softwareverde.bitcoin.util.bytearray.ByteArrayReader;
import com.softwareverde.constable.bytearray.MutableByteArray;
import com.softwareverde.util.bytearray.Endian;

public class QueryAddressBlocksMessageInflater extends BitcoinProtocolMessageInflater {

    @Override
    public QueryAddressBlocksMessage fromBytes(final byte[] bytes) {
        final QueryAddressBlocksMessage queryAddressBlocksMessage = new QueryAddressBlocksMessage();
        final ByteArrayReader byteArrayReader = new ByteArrayReader(bytes);

        final BitcoinProtocolMessageHeader protocolMessageHeader = _parseHeader(byteArrayReader, MessageType.QUERY_ADDRESS_BLOCKS);
        if (protocolMessageHeader == null) { return null; }

        final int addressCount = byteArrayReader.readVariableSizedInteger().intValue();
        if ( (addressCount < 0) || (addressCount >= QueryAddressBlocksMessage.MAX_ADDRESS_COUNT) ) { return null; }

        final Integer bytesRequired = (Address.BYTE_COUNT * addressCount);
        if (byteArrayReader.remainingByteCount() < bytesRequired) { return null; }

        final AddressInflater addressInflater = new AddressInflater();
        for (int i=0; i<addressCount; ++i) {
            final Address address = addressInflater.fromBytes(MutableByteArray.wrap(byteArrayReader.readBytes(Address.BYTE_COUNT, Endian.LITTLE)));
            if (address == null) { return null; }

            queryAddressBlocksMessage._addresses.add(address);
        }

        if (byteArrayReader.didOverflow()) { return null; }

        return queryAddressBlocksMessage;
    }
}
