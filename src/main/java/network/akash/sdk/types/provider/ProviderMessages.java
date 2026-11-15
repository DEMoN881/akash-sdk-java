package network.akash.sdk.types.provider;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Msg;
import network.akash.sdk.types.base.Attribute;
import java.util.List;

/**
 * Provider transaction messages.
 * Port of akash-api/go/node/provider/v1beta3 messages.
 */
public final class ProviderMessages {

    public static final String ROUTE = "provider";

    private ProviderMessages() {}

    public record MsgCreateProvider(
            String owner,
            String hostUri,
            List<Attribute> attributes,
            String email,
            String website
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "create-provider"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
            if (hostUri == null || hostUri.isEmpty()) {
                throw new IllegalArgumentException("provider host URI cannot be empty");
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

    public record MsgUpdateProvider(
            String owner,
            String hostUri,
            List<Attribute> attributes,
            String email,
            String website
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "update-provider"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
            if (hostUri == null || hostUri.isEmpty()) {
                throw new IllegalArgumentException("provider host URI cannot be empty");
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

    public record MsgDeleteProvider(String owner) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "delete-provider"; }

        @Override
        public void validateBasic() {
            AccAddress.fromBech32(owner);
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
