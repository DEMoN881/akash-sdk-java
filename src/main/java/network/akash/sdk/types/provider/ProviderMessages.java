package network.akash.sdk.types.provider;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Msg;
import network.akash.sdk.types.base.Attribute;
import java.util.List;

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
            var builder = akash.provider.v1beta3.MsgCreateProvider.newBuilder()
                    .setOwner(owner)
                    .setHostUri(hostUri);
            if (email != null) builder.setEmail(email);
            if (website != null) builder.setWebsite(website);
            if (attributes != null) {
                for (Attribute attr : attributes) {
                    builder.addAttributes(akash.base.v1beta1.Attribute.newBuilder()
                            .setKey(attr.key()).setValue(attr.value()).build());
                }
            }
            return builder.build().toByteArray();
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
            var builder = akash.provider.v1beta3.MsgUpdateProvider.newBuilder()
                    .setOwner(owner)
                    .setHostUri(hostUri);
            if (email != null) builder.setEmail(email);
            if (website != null) builder.setWebsite(website);
            if (attributes != null) {
                for (Attribute attr : attributes) {
                    builder.addAttributes(akash.base.v1beta1.Attribute.newBuilder()
                            .setKey(attr.key()).setValue(attr.value()).build());
                }
            }
            return builder.build().toByteArray();
        }
    }

    public record MsgDeleteProvider(String owner) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "delete-provider"; }

        @Override public void validateBasic() { AccAddress.fromBech32(owner); }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(owner));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.provider.v1beta3.MsgDeleteProvider.newBuilder()
                    .setOwner(owner)
                    .build().toByteArray();
        }
    }
}
