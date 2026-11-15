package network.akash.sdk.types.audit;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Msg;
import network.akash.sdk.types.base.Attribute;
import java.util.List;

/**
 * Audit transaction messages.
 * Port of akash-api/go/node/audit/v1beta3 messages.
 */
public final class AuditMessages {

    public static final String ROUTE = "audit";

    private AuditMessages() {}

    public record MsgSignProviderAttributes(
            String owner,
            String auditor,
            List<Attribute> attributes
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "sign-provider-attributes"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
            AccAddress.fromBech32(auditor);
            if (attributes == null || attributes.isEmpty()) {
                throw new IllegalArgumentException("attributes cannot be empty");
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(auditor));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgDeleteProviderAttributes(
            String owner,
            String auditor,
            List<String> keys
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "delete-provider-attributes"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
            AccAddress.fromBech32(auditor);
            if (keys == null || keys.isEmpty()) {
                throw new IllegalArgumentException("keys cannot be empty");
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(auditor));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }
}
