package network.akash.sdk.types.cert;

import com.google.protobuf.ByteString;
import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Msg;
import java.util.List;

public final class CertMessages {

    public static final String ROUTE = "cert";

    private CertMessages() {}

    public record MsgCreateCertificate(
            String owner,
            byte[] cert,
            byte[] pubkey
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "create-certificate"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
            if (cert == null || cert.length == 0) {
                throw new IllegalArgumentException("certificate cannot be empty");
            }
            if (pubkey == null || pubkey.length == 0) {
                throw new IllegalArgumentException("pubkey cannot be empty");
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(owner));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.cert.v1.MsgCreateCertificate.newBuilder()
                    .setOwner(owner)
                    .setCert(ByteString.copyFrom(cert))
                    .setPubkey(ByteString.copyFrom(pubkey))
                    .build().toByteArray();
        }
    }

    public record MsgRevokeCertificate(
            String owner,
            String serial
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "revoke-certificate"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
            if (serial == null || serial.isEmpty()) {
                throw new IllegalArgumentException("serial cannot be empty");
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(owner));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.cert.v1.MsgRevokeCertificate.newBuilder()
                    .setId(akash.cert.v1.ID.newBuilder()
                            .setOwner(owner)
                            .setSerial(serial)
                            .build())
                    .build().toByteArray();
        }
    }
}
