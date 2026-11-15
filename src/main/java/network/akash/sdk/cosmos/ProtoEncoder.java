package network.akash.sdk.cosmos;

import com.google.protobuf.MessageLite;

/**
 * Helper for encoding Msg implementations to protobuf bytes.
 * Each Msg's getSignBytes() delegates here with its protobuf representation.
 */
public final class ProtoEncoder {

    private ProtoEncoder() {}

    public static byte[] encode(MessageLite message) {
        return message.toByteArray();
    }
}
