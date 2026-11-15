package network.akash.sdk.types.deployment;

import com.google.protobuf.ByteString;
import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.Msg;

import java.util.List;

/**
 * Deployment transaction messages with full protobuf serialization.
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
            var builder = akash.deployment.v1beta3.MsgCreateDeployment.newBuilder()
                    .setId(akash.deployment.v1beta3.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
                    .setVersion(ByteString.copyFrom(version))
                    .setDeposit(cosmos.base.v1beta1.CoinOuterClass.Coin.newBuilder()
                            .setDenom(deposit.denom())
                            .setAmount(deposit.amount().toString())
                            .build())
                    .setDepositor(depositor);
            return builder.build().toByteArray();
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
            return akash.deployment.v1beta3.MsgUpdateDeployment.newBuilder()
                    .setId(akash.deployment.v1beta3.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
                    .setVersion(ByteString.copyFrom(version))
                    .build().toByteArray();
        }
    }

    public record MsgCloseDeployment(DeploymentID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-deployment"; }

        @Override public void validateBasic() { id.validate(); }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.deployment.v1beta3.MsgCloseDeployment.newBuilder()
                    .setId(akash.deployment.v1beta3.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
                    .build().toByteArray();
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
            if (amount.isZero()) throw new IllegalArgumentException("invalid deposit: zero amount");
            AccAddress.fromBech32(depositor);
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            return akash.deployment.v1beta3.MsgDepositDeployment.newBuilder()
                    .setId(akash.deployment.v1beta3.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
                    .setAmount(cosmos.base.v1beta1.CoinOuterClass.Coin.newBuilder()
                            .setDenom(amount.denom())
                            .setAmount(amount.amount().toString())
                            .build())
                    .setDepositor(depositor)
                    .build().toByteArray();
        }
    }

    public record MsgCloseGroup(GroupID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "close-group"; }
        @Override public void validateBasic() { id.validate(); }
        @Override public List<AccAddress> getSigners() { return List.of(AccAddress.fromBech32(id.owner())); }

        @Override
        public byte[] getSignBytes() {
            return akash.deployment.v1beta3.MsgCloseGroup.newBuilder()
                    .setId(akash.deployment.v1beta3.GroupID.newBuilder()
                            .setOwner(id.owner()).setDseq(id.dseq()).setGseq(id.gseq()).build())
                    .build().toByteArray();
        }
    }

    public record MsgPauseGroup(GroupID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "pause-group"; }
        @Override public void validateBasic() { id.validate(); }
        @Override public List<AccAddress> getSigners() { return List.of(AccAddress.fromBech32(id.owner())); }

        @Override
        public byte[] getSignBytes() {
            return akash.deployment.v1beta3.MsgPauseGroup.newBuilder()
                    .setId(akash.deployment.v1beta3.GroupID.newBuilder()
                            .setOwner(id.owner()).setDseq(id.dseq()).setGseq(id.gseq()).build())
                    .build().toByteArray();
        }
    }

    public record MsgStartGroup(GroupID id) implements Msg {
        @Override public String getRoute() { return ROUTE; }
        @Override public String getType() { return "start-group"; }
        @Override public void validateBasic() { id.validate(); }
        @Override public List<AccAddress> getSigners() { return List.of(AccAddress.fromBech32(id.owner())); }

        @Override
        public byte[] getSignBytes() {
            return akash.deployment.v1beta3.MsgStartGroup.newBuilder()
                    .setId(akash.deployment.v1beta3.GroupID.newBuilder()
                            .setOwner(id.owner()).setDseq(id.dseq()).setGseq(id.gseq()).build())
                    .build().toByteArray();
        }
    }
}
