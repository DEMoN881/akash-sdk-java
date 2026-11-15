# Akash SDK Java

> **Unofficial community Java SDK for [Akash Network](https://akash.network/).**
> 
> **This SDK is vibecoded** — a 1:1 logic port of the official [Akash Go SDK](https://github.com/akash-network/akash-api) (`go/node/client/v1beta2`).

## Overview

Java 21+ SDK for interacting with the Akash Network blockchain. Provides the same client architecture as the official Go SDK:

- **QueryClient** — Query deployments, orders, bids, leases, providers, audit attributes, certificates, and Cosmos SDK modules (auth, bank, staking, gov)
- **TxClient** — Broadcast transactions with serial queue, automatic sequence sync, and retry on sequence mismatch
- **NodeClient** — Check node sync status

## Architecture

Mirrors the Go SDK's three-client pattern:

```
AkashClient
├── QueryClient      (REST/LCD queries → Akash + Cosmos modules)
├── TxClient         (SerialBroadcaster → serial queue, seq sync, retry)
└── NodeClient       (Tendermint RPC /status)
```

### Ported Modules

| Go Module | Java Package | Description |
|---|---|---|
| `go/node/client/v1beta2` | `network.akash.sdk.client` | Client, QueryClient, TxClient, NodeClient, SerialBroadcaster |
| `go/node/deployment/v1beta3` | `network.akash.sdk.types.deployment` | DeploymentID, GroupID, deployment messages |
| `go/node/market/v1beta4` | `network.akash.sdk.types.market` | OrderID, BidID, LeaseID, market messages |
| `go/node/provider/v1beta3` | `network.akash.sdk.types.provider` | Provider messages |
| `go/node/audit/v1beta3` | `network.akash.sdk.types.audit` | Audit messages |
| `go/node/cert/v1beta3` | `network.akash.sdk.types.cert` | Certificate messages |
| `go/node/base/v1beta1` | `network.akash.sdk.types.base` | Attribute |
| Cosmos SDK types | `network.akash.sdk.cosmos` | AccAddress, Coin, DecCoin, Msg |
| Cosmos crypto | `network.akash.sdk.crypto` | Bech32, AkashKeyPair (secp256k1) |

## Usage

```java
import network.akash.sdk.client.*;
import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.types.deployment.*;

// Create client
AkashClient client = DefaultAkashClient.builder()
    .rpcEndpoint("http://rpc.akash.network:26657")
    .restEndpoint("http://api.akash.network:1317/")
    .chainId("akashnet-2")
    .keyPair(AkashKeyPair.fromHex("your_private_key_hex"))
    .build();

// Query deployments
var deployments = client.query().deployments(Map.of(
    "filters.owner", "akash1..."
));

// Close a deployment
var result = client.tx().broadcast(
    List.of(new DeploymentMessages.MsgCloseDeployment(
        new DeploymentID("akash1...", 12345)
    )),
    BroadcastOptions.withGas(200000, false),
    BroadcastOptions.withFees("5000uakt")
);

// Check node status
var syncInfo = client.node().syncInfo();
System.out.println("Block height: " + syncInfo.latestBlockHeight());
```

## Build

```bash
mvn clean package
```

Requires Java 21+.

## Dependencies

- **gRPC** (io.grpc) — for future gRPC-native query/tx support
- **Protobuf** (com.google.protobuf) — for transaction serialization
- **BouncyCastle** — secp256k1 signing, RIPEMD-160
- **OkHttp** — REST/RPC HTTP client
- **Jackson** — JSON (de)serialization
- **SLF4J** — logging

## Status

This is a community-maintained, vibecoded port. Key areas for future work:

- [ ] Full protobuf transaction signing (currently stubbed — the signing envelope structure is in place but needs cosmos-sdk protobuf schemas for `SignDoc` serialization)
- [ ] gRPC-native queries (currently uses REST/LCD gateway)
- [ ] SDL (Stack Definition Language) parsing
- [ ] Manifest submission to providers
- [ ] Certificate generation utilities
- [ ] Keyring integration (file-based, OS keychain)

## Official SDK

The official Go SDK lives at [akash-network/akash-api](https://github.com/akash-network/akash-api).

## License

Apache-2.0
