package network.akash.sdk.types.deployment;

import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.Msg;
import java.util.List;

/**
 * Deployment transaction messages.
 * Port of akash-api/go/node/deployment/v1beta3 messages.
 */
public final class DeploymentMessages {

    public static final String ROUTE = "deployment";
    public static final int MANIFEST_VERSION_LENGTH = 32;

    private DeploymentMessages() {}

    public record MsgCreateDeployment(
            DeploymentID id,
            List<byte[]> groups,
            byte[] version,
            Coin deposit,
            String depositor
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "create-deployment"; }

        @Override
        public void validateBasic() {
            id.validate();
            deposit.validate();
            if (groups == null || groups.isEmpty()) {
                throw new IllegalArgumentException("invalid groups: empty");
            }
            if (version == null || version.length == 0) {
                throw new IllegalArgumentException("empty version");
            }
            if (version.length != MANIFEST_VERSION_LENGTH) {
                throw new IllegalArgumentException("invalid version length");
            }
            AccAddress.fromBech32(depositor);
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0]; // protobuf serialization in real impl
        }
    }

    public record MsgUpdateDeployment(
            DeploymentID id,
            byte[] version
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "update-deployment"; }

        @Override
        public void validateBasic() {
            id.validate();
            if (version == null || version.length == 0) {
                throw new IllegalArgumentException("empty version");
            }
            if (version.length != MANIFEST_VERSION_LENGTH) {
                throw new IllegalArgumentException("invalid version length");
            }
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgCloseDeployment(DeploymentID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-deployment"; }

        @Override
        public void validateBasic() {
            id.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgDepositDeployment(
            DeploymentID id,
            Coin amount,
            String depositor
    ) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "deposit-deployment"; }

        @Override
        public void validateBasic() {
            id.validate();
            if (amount.isZero()) {
                throw new IllegalArgumentException("invalid deposit: zero amount");
            }
            AccAddress.fromBech32(depositor);
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgCloseGroup(GroupID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-group"; }

        @Override
        public void validateBasic() {
            id.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgPauseGroup(GroupID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "pause-group"; }

        @Override
        public void validateBasic() {
            id.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }

    public record MsgStartGroup(GroupID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "start-group"; }

        @Override
        public void validateBasic() {
            id.validate();
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return new byte[0];
        }
    }
}
