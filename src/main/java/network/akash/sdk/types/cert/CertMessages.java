package network.akash.sdk.types.cert;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Msg;
import java.util.List;

/**
 * Certificate transaction messages.
 * Port of akash-api/go/node/cert/v1beta3 messages.
 */
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
            return new byte[0];
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
            return new byte[0];
        }
    }
}
