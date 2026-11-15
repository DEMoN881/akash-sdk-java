# Akash SDK Java

[![](https://jitpack.io/v/DEMoN881/akash-sdk-java.svg)](https://jitpack.io/#DEMoN881/akash-sdk-java)

> **Unofficial community Java SDK for [Akash Network](https://akash.network/).**
> 
> **Vibecoded** — a 1:1 logic port of the official(According to Claude) [Akash Go SDK](https://github.com/akash-network/akash-api) (`go/node/client/v1beta2`).

## Example

[JAkash Desktop](https://github.com/DEMoN881/JAkash-Desktop) is a desktop and CLI built on this SDK. It deploys nginx to mainnet and takes the container URL from the provider `uris` array.

```bash
git clone https://github.com/DEMoN881/JAkash-Desktop.git
cd JAkash-Desktop
./gradlew run --args=ui
```

## Features

- **Full protobuf transaction signing** — SignDoc serialization, secp256k1 signing, TxRaw broadcast
- **gRPC-native queries** — Generated blocking stubs for all Akash and Cosmos modules
- **REST/LCD fallback** — Works with any Cosmos LCD endpoint out of the box
- **SDL parser and builder** — Parse Akash SDL YAML, or build it in Java and patch an existing file
- **Manifest submission** — Submit manifests to providers over mTLS
- **Certificate generation** — X.509 certificates for tenant-provider mTLS
- **Keyring** — Encrypted file-based (AES-256-GCM) and in-memory key storage
- **Serial TX broadcaster** — Queue with automatic sequence sync and retry on mismatch

## Quick start

`v0.3.0` adds `SdlBuilder`. Describe a container in Java, or open an existing YAML file and change one field. The result is the same SDL that `SdlParser` reads.

The builder does not broadcast a transaction or create a deployment. It builds the description. Pass the `Sdl` to `ManifestBuilder.fromSdl` when you need a provider manifest.

### Install

Maven:

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
    <version>v0.3.0</version>
</dependency>
```

Gradle:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.DEMoN881:akash-sdk-java:v0.3.0'
}
```

JitPack builds a new tag for a few minutes. If `v0.3.0` does not resolve yet, refresh the dependencies.

### One container

The names in the example:

- `service("web")` — one container. `"web"` is its name.
- `image` — the container image.
- `expose(80)` — publish port 80 as global TCP. That port is what the provider later returns in `uris`.
- `compute("0.1", "128Mi", "128Mi")` — cpu, memory, and disk. Use the same strings you would write in YAML.
- `group("test")` — the group name stored on chain. One group is one order and one bill.
- `price("web", "uact", "50000")` — the price ceiling for service `web` in that group. The amount is a string. On mainnet the group price is `uact`.
- `end()` — return to the builder.
- `build()` — write the YAML, parse it, and check that an image and at least one group are present.
- `yaml()` — the same text, when you want to save a file. It does not validate.

```java
import network.akash.sdk.sdl.Sdl;
import network.akash.sdk.sdl.SdlBuilder;

Sdl sdl = SdlBuilder.create()
        .service("web")
            .image("nginx:1.27-alpine")
            .expose(80)
            .compute("0.1", "128Mi", "128Mi")
            .end()
        .group("test")
            .price("web", "uact", "50000")
            .end()
        .build();

String yaml = SdlBuilder.from(sdl).yaml();
```

After `build()`:

- `sdl.services().get("web").image()` is `"nginx:1.27-alpine"`
- `sdl.groups().get(0).name()` is `"test"`
- `sdl.placements().get("test").pricing().get("web").amount()` is `"50000"`

Calling `service("web")` or `group("test")` again does not add a second entry. It opens the same one and adds to it.

### Change an existing YAML file

`from` keeps the rest of the file. Only the calls you make are changed.

```java
Sdl updated = SdlBuilder.from(yaml)
        .service("web")
            .image("nginx:1.28-alpine")
            .env("MODE", "prod")
            .end()
        .build();
```

The group, price, cpu, and ports from the source stay. `env("MODE", "prod")` adds a variable. A file that stored env as a `KEY=value` list keeps those entries too.

### Two containers in one group

Put both services in `price` on the same group. They share one bill.

```java
Sdl sdl = SdlBuilder.create()
        .service("web").image("nginx:1.27-alpine").expose(80).compute("0.1", "128Mi", "128Mi").end()
        .service("api").image("my/api:1").expose(8080).compute("0.5", "512Mi", "1Gi").end()
        .group("app")
            .price("web", "uact", "10000")
            .price("api", "uact", "20000")
            .end()
        .build();
```

A second group with a different name is a second order. `group("app")` and `group("jobs")` produce two groups.

### Methods

On a service, before `end()`:

| Method | Writes |
|---|---|
| `image("nginx:1.27-alpine")` | the image |
| `count(2)` | replica count. Omitted means 1 |
| `expose(80)` | port 80, global TCP |
| `expose(80, 80, "udp", false)` | external port, protocol, and the global flag |
| `compute("0.1", "128Mi", "1Gi")` | cpu, memory, and disk |
| `compute("1", "1Gi", "10Gi", "1")` | the same, plus gpu |
| `env("MODE", "prod")` | an environment variable |
| `command("nginx", "-g", "daemon off;")` | command |
| `args("--port", "80")` | args |

On a group, before `end()`:

| Method | Writes |
|---|---|
| `price("web", "uact", "50000")` | the price ceiling for that service |
| `attribute("region", "us-west")` | a provider requirement |
| `signedBy("akash1...")` | an auditor the provider must have |

A port without `global: true` does not appear in `uris`. Cpu, memory, disk, count, and price are not updated on an open deployment: those need a new deployment. Image, env, command, and args change by submitting a new manifest on the same deployment.

## Client

The client below is how that `Sdl` goes on chain. A wallet key comes from a mnemonic: `AkashKeyPair.fromMnemonic(mnemonic)`. For a live nginx deploy, see [JAkash Desktop](https://github.com/DEMoN881/JAkash-Desktop).

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

### Close a deployment

The transaction fee is `uakt`. Leave headroom on gas: 200000 is not enough to close a deployment on mainnet.

```java
import network.akash.sdk.types.deployment.*;

var result = client.tx().broadcast(
    List.of(new DeploymentMessages.MsgCloseDeployment(
        new DeploymentID("akash1...", 12345)
    )),
    BroadcastOptions.withGas(400000, false),
    BroadcastOptions.withFees("20000uakt")
);
```

### Read an existing file

```java
import network.akash.sdk.sdl.Sdl;
import network.akash.sdk.sdl.SdlParser;

Sdl sdl = SdlParser.parse(yaml);
sdl.validate();
```

`SdlParser.parse` only reads. Build or edit the document with `SdlBuilder` above.

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
| `go/node/deployment/v1beta4` | `network.akash.sdk.types.deployment` | DeploymentID, GroupID, deployment messages |
| `go/node/market/v1beta5` | `network.akash.sdk.types.market` | OrderID, BidID, LeaseID, market messages |
| `go/node/provider/v1beta4` | `network.akash.sdk.types.provider` | Provider messages |
| `go/node/audit/v1` | `network.akash.sdk.types.audit` | Audit messages |
| `go/node/cert/v1` | `network.akash.sdk.types.cert` | Certificate messages |
| — | `network.akash.sdk.sdl` | SDL parser and `SdlBuilder` |
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

188 tests covering the modules.

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
