# Akash SDK Java

[![](https://jitpack.io/v/DEMoN881/akash-sdk-java.svg)](https://jitpack.io/#DEMoN881/akash-sdk-java)

> **Unofficial community Java SDK for [Akash Network](https://akash.network/).**
> 
> **Vibecoded** — a 1:1 logic port of the official(According to Claude) [Akash Go SDK](https://github.com/akash-network/akash-api) (`go/node/client/v1beta2`).

## Features

- **Full protobuf transaction signing** — SignDoc serialization, secp256k1 signing, TxRaw broadcast
- **gRPC-native queries** — Generated blocking stubs for all Akash and Cosmos modules
- **REST/LCD fallback** — Works with any Cosmos LCD endpoint out of the box
- **SDL parser** — Parse Akash Stack Definition Language YAML into deployment groups
- **Manifest submission** — Submit manifests to providers over mTLS
- **Certificate generation** — X.509 certificates for tenant-provider mTLS
- **Keyring** — Encrypted file-based (AES-256-GCM) and in-memory key storage
- **Serial TX broadcaster** — Queue with automatic sequence sync and retry on mismatch

## Installation

### Maven (JitPack)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.DEMoN881</groupId>
    <artifactId>akash-sdk-java</artifactId>
    <version>v0.1.0</version>
</dependency>
```

### Gradle (JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.DEMoN881:akash-sdk-java:v0.1.0'
}
```

## Quick Start

### Create a client (REST mode)

```java
import network.akash.sdk.client.*;
import network.akash.sdk.crypto.AkashKeyPair;

AkashClient client = DefaultAkashClient.builder()
    .rpcEndpoint("http://rpc.akash.network:26657")
    .restEndpoint("http://api.akash.network:1317/")
    .chainId("akashnet-2")
    .keyPair(AkashKeyPair.fromHex("your_private_key_hex"))
    .build();
```

### Create a client (gRPC mode)

```java
AkashClient client = DefaultAkashClient.builder()
    .rpcEndpoint("http://rpc.akash.network:26657")
    .restEndpoint("http://api.akash.network:1317/")
    .grpcEndpoint("grpc.akash.network", 9090)
    .chainId("akashnet-2")
    .keyPair(AkashKeyPair.fromHex("your_private_key_hex"))
    .build();
```

### Query deployments

```java
var deployments = client.query().deployments(Map.of(
    "filters.owner", "akash1..."
));
```

### Create and close a deployment

```java
import network.akash.sdk.types.deployment.*;

// Close a deployment
var result = client.tx().broadcast(
    List.of(new DeploymentMessages.MsgCloseDeployment(
        new DeploymentID("akash1...", 12345)
    )),
    BroadcastOptions.withGas(200000, false),
    BroadcastOptions.withFees("5000uakt")
);
```

### Parse an SDL file

```java
import network.akash.sdk.sdl.*;

Sdl sdl = SdlParser.parse("""
    version: "2.0"
    services:
      web:
        image: nginx:latest
        expose:
          - port: 80
            as: 80
            to:
              - global: true
    profiles:
      compute:
        web:
          resources:
            cpu:
              units: 0.5
            memory:
              size: 512Mi
            storage:
              size: 1Gi
      placement:
        dcloud:
          pricing:
            web:
              denom: uakt
              amount: 1000
    """);

sdl.validate();
System.out.println("Groups: " + sdl.groups().size());
```

### Generate mTLS certificate

```java
import network.akash.sdk.crypto.CertificateGenerator;
import network.akash.sdk.crypto.CertificateGenerator.CertificateResult;

CertificateResult cert = CertificateGenerator.generateForAkash("akash1youraddress");
System.out.println(cert.certPem());
System.out.println(cert.privateKeyPem());
```

### Submit manifest to provider

```java
import network.akash.sdk.manifest.*;
import network.akash.sdk.sdl.*;

Sdl sdl = SdlParser.parseFile(new File("deploy.yaml"));
Manifest manifest = ManifestBuilder.fromSdl(sdl);

ManifestClient manifestClient = new ManifestClient();
manifestClient.submitManifest(
    "https://provider.akash.network",
    12345L,  // dseq
    manifest,
    cert     // CertificateResult from above
);
```

### Keyring (encrypted key storage)

```java
import network.akash.sdk.keyring.*;

// File-based (AES-256-GCM, PBKDF2 600k iterations)
Keyring keyring = new FileKeyring(Path.of("~/.akash/keyring"), "passphrase");
keyring.save("mykey", privateKeyBytes);
byte[] loaded = keyring.load("mykey").orElseThrow();

// In-memory (for testing)
Keyring memKeyring = new MemoryKeyring();
```

## Architecture

```
AkashClient
├── QueryClient      (gRPC blocking stubs or REST/LCD fallback)
├── TxClient         (SerialBroadcaster → serial queue, seq sync, retry)
└── NodeClient       (Tendermint RPC /status)
```

### Module Map

| Go Module | Java Package | Description |
|---|---|---|
| `go/node/client/v1beta2` | `network.akash.sdk.client` | Client, QueryClient, TxClient, NodeClient, SerialBroadcaster |
| `go/node/deployment/v1beta3` | `network.akash.sdk.types.deployment` | DeploymentID, GroupID, deployment messages |
| `go/node/market/v1beta4` | `network.akash.sdk.types.market` | OrderID, BidID, LeaseID, market messages |
| `go/node/provider/v1beta3` | `network.akash.sdk.types.provider` | Provider messages |
| `go/node/audit/v1beta3` | `network.akash.sdk.types.audit` | Audit messages |
| `go/node/cert/v1beta3` | `network.akash.sdk.types.cert` | Certificate messages |
| — | `network.akash.sdk.sdl` | SDL parser (YAML → deployment groups) |
| — | `network.akash.sdk.manifest` | Manifest builder & mTLS submission |
| — | `network.akash.sdk.crypto` | Bech32, AkashKeyPair, CertificateGenerator |
| — | `network.akash.sdk.keyring` | Encrypted file-based & in-memory keyring |
| Cosmos SDK types | `network.akash.sdk.cosmos` | AccAddress, Coin, DecCoin, TxSigner |

## Build

```bash
mvn clean package
```

Requires **Java 21+**.

## Tests

```bash
mvn test
```

168 unit tests covering all modules.

## Dependencies

| Library | Purpose |
|---|---|
| gRPC (io.grpc) | gRPC-native queries via blocking stubs |
| Protobuf (com.google.protobuf) | Transaction serialization & signing |
| BouncyCastle | secp256k1, RIPEMD-160, X.509 certificate generation |
| Jackson | JSON + YAML (SDL parsing) |
| OkHttp | REST/RPC HTTP client |
| SLF4J | Logging |

## Official SDK

The official Go SDK lives at [akash-network/akash-api](https://github.com/akash-network/akash-api).

## License

Apache-2.0
