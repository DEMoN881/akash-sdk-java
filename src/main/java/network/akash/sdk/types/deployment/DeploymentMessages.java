package network.akash.sdk.types.deployment;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import network.akash.sdk.cosmos.AccAddress;
import network.akash.sdk.cosmos.Coin;
import network.akash.sdk.cosmos.Deposits;
import network.akash.sdk.cosmos.Msg;

import java.util.List;

/**
 * Deployment transaction messages.
 * Port of chain-sdk go/node/deployment/v1beta4, registered by Akash node 2.2.
 */
public final class DeploymentMessages {

    public static final String ROUTE = "deployment";
    public static final int MANIFEST_VERSION_LENGTH = 32;

    private DeploymentMessages() {}

    public record MsgCreateDeployment(
            DeploymentID id,
            List<byte[]> groups,
            byte[] version,
            Coin deposit
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
        }

        @Override
        public List<AccAddress> getSigners() {
            return List.of(AccAddress.fromBech32(id.owner()));
        }

        @Override
        public byte[] getSignBytes() {
            var builder = akash.deployment.v1beta4.MsgCreateDeployment.newBuilder()
                    .setId(akash.deployment.v1beta4.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
                    .setHash(ByteString.copyFrom(version))
                    .setDeposit(Deposits.fromBalance(deposit));
            for (byte[] group : groups) {
                try {
                    builder.addGroups(akash.deployment.v1beta4.GroupSpec.parseFrom(group));
                } catch (InvalidProtocolBufferException e) {
                    throw new IllegalArgumentException("invalid group spec", e);
                }
            }
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
            return akash.deployment.v1beta4.MsgUpdateDeployment.newBuilder()
                    .setId(akash.deployment.v1beta4.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
                    .setHash(ByteString.copyFrom(version))
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
            return akash.deployment.v1beta4.MsgCloseDeployment.newBuilder()
                    .setId(akash.deployment.v1beta4.DeploymentID.newBuilder()
                            .setOwner(id.owner())
                            .setDseq(id.dseq())
                            .build())
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
            return akash.deployment.v1beta4.MsgCloseGroup.newBuilder()
                    .setId(akash.deployment.v1beta4.GroupID.newBuilder()
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
            return akash.deployment.v1beta4.MsgPauseGroup.newBuilder()
                    .setId(akash.deployment.v1beta4.GroupID.newBuilder()
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
            return akash.deployment.v1beta4.MsgStartGroup.newBuilder()
                    .setId(akash.deployment.v1beta4.GroupID.newBuilder()
                            .setOwner(id.owner()).setDseq(id.dseq()).setGseq(id.gseq()).build())
                    .build().toByteArray();
        }
    }
}
