package network.akash.sdk.cosmos;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import cosmos.base.v1beta1.CoinOuterClass;
import cosmos.crypto.secp256k1.Keys;
import cosmos.tx.signing.v1beta1.Signing;
import cosmos.tx.v1beta1.TxOuterClass;
import network.akash.sdk.crypto.AkashKeyPair;

import java.util.List;

/**
 * Full Cosmos SDK protobuf transaction builder and signer.
 * Handles: TxBody → AuthInfo → SignDoc → sign → TxRaw → base64.
 */
public final class TxSigner {

    private TxSigner() {}

    public static byte[] buildAndSign(
            AkashKeyPair keyPair,
            String chainId,
            long accountNumber,
            long sequence,
            List<Any> messages,
            long gasLimit,
            List<CoinOuterClass.Coin> feeCoins,
            String memo,
            Long timeoutHeight
    ) {
        TxOuterClass.TxBody.Builder bodyBuilder = TxOuterClass.TxBody.newBuilder()
                .addAllMessages(messages);
        if (memo != null && !memo.isEmpty()) {
            bodyBuilder.setMemo(memo);
        }
        if (timeoutHeight != null) {
            bodyBuilder.setTimeoutHeight(timeoutHeight);
        }
        byte[] bodyBytes = bodyBuilder.build().toByteArray();

        Keys.PubKey pubKey = Keys.PubKey.newBuilder()
                .setKey(ByteString.copyFrom(keyPair.getPublicKey()))
                .build();

        TxOuterClass.ModeInfo modeInfo = TxOuterClass.ModeInfo.newBuilder()
                .setSingle(TxOuterClass.ModeInfo.Single.newBuilder()
                        .setMode(Signing.SignMode.SIGN_MODE_DIRECT)
                        .build())
                .build();

        TxOuterClass.SignerInfo signerInfo = TxOuterClass.SignerInfo.newBuilder()
                .setPublicKey(Any.pack(pubKey))
                .setModeInfo(modeInfo)
                .setSequence(sequence)
                .build();

        TxOuterClass.Fee.Builder feeBuilder = TxOuterClass.Fee.newBuilder()
                .setGasLimit(gasLimit);
        if (feeCoins != null) {
            feeBuilder.addAllAmount(feeCoins);
        }

        TxOuterClass.AuthInfo authInfo = TxOuterClass.AuthInfo.newBuilder()
                .addSignerInfos(signerInfo)
                .setFee(feeBuilder.build())
                .build();
        byte[] authInfoBytes = authInfo.toByteArray();

        cosmos.tx.v1beta1.TxOuterClass.SignDoc signDoc = cosmos.tx.v1beta1.TxOuterClass.SignDoc.newBuilder()
                .setBodyBytes(ByteString.copyFrom(bodyBytes))
                .setAuthInfoBytes(ByteString.copyFrom(authInfoBytes))
                .setChainId(chainId)
                .setAccountNumber(accountNumber)
                .build();

        byte[] signDocBytes = signDoc.toByteArray();
        byte[] signature = keyPair.sign(signDocBytes);

        TxOuterClass.TxRaw txRaw = TxOuterClass.TxRaw.newBuilder()
                .setBodyBytes(ByteString.copyFrom(bodyBytes))
                .setAuthInfoBytes(ByteString.copyFrom(authInfoBytes))
                .addSignatures(ByteString.copyFrom(signature))
                .build();

        return txRaw.toByteArray();
    }
}
