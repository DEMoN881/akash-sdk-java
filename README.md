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

## С чего начать

В `v0.3.0` есть `SdlBuilder`. Им описываешь контейнер на Java, либо открываешь уже готовый YAML и меняешь в нём одно поле. На выходе тот же SDL, который читает `SdlParser`.

Билдер не отправляет транзакцию и не создаёт деплой. Он собирает описание. Дальше `Sdl` можно отдать в `ManifestBuilder.fromSdl`.

### Подключить

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

JitPack собирает тег несколько минут. Если `v0.3.0` ещё не резолвится, подожди и обнови зависимости.

### Один контейнер

Слова, которые встречаются ниже:

- `service("web")` — один контейнер. `"web"` его имя.
- `image` — образ.
- `expose(80)` — открыть порт 80 наружу по TCP. Такой порт провайдер потом отдаёт в `uris`.
- `compute("0.1", "128Mi", "128Mi")` — cpu, память, диск. Пиши так же, как в YAML.
- `group("test")` — имя группы. Это имя уходит на чейн. Одна группа = один заказ и один счёт.
- `price("web", "uact", "50000")` — потолок цены сервиса `web` в этой группе. Сумма строкой. На mainnet цена группы в `uact`.
- `end()` — вернуться к билдеру.
- `build()` — записать YAML, прочитать его парсером и проверить, что есть образ и хотя бы одна группа.
- `yaml()` — тот же текст, если его надо сохранить в файл. Проверку сам не делает.

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

После `build()`:

- `sdl.services().get("web").image()` — `"nginx:1.27-alpine"`
- `sdl.groups().get(0).name()` — `"test"`
- `sdl.placements().get("test").pricing().get("web").amount()` — `"50000"`

Повторный `service("web")` или `group("test")` не создаёт вторую запись. Он открывает ту же и дописывает в неё.

### Поменять готовый YAML

`from` оставляет файл как был. Меняется только то, что ты вызвал.

```java
Sdl updated = SdlBuilder.from(yaml)
        .service("web")
            .image("nginx:1.28-alpine")
            .env("MODE", "prod")
            .end()
        .build();
```

Группа, цена, cpu и порты из исходного текста остаются. `env("MODE", "prod")` дописывает переменную. Если в файле env был списком `KEY=value`, он тоже сохранится.

### Два контейнера в одной группе

Оба сервиса указываешь в `price` одной и той же группы. Счёт один.

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

Вторая группа с другим именем — это уже второй заказ. `group("app")` и `group("jobs")` дадут две группы.

### Методы

На сервисе, до `end()`:

| Метод | Что пишет |
|---|---|
| `image("nginx:1.27-alpine")` | образ |
| `count(2)` | сколько копий. Если не вызвать, будет 1 |
| `expose(80)` | порт 80 наружу, TCP |
| `expose(80, 80, "udp", false)` | свой внешний порт, протокол и флаг global |
| `compute("0.1", "128Mi", "1Gi")` | cpu, память, диск |
| `compute("1", "1Gi", "10Gi", "1")` | то же плюс gpu |
| `env("MODE", "prod")` | переменная окружения |
| `command("nginx", "-g", "daemon off;")` | command |
| `args("--port", "80")` | args |

На группе, до `end()`:

| Метод | Что пишет |
|---|---|
| `price("web", "uact", "50000")` | потолок цены этого сервиса |
| `attribute("region", "us-west")` | требование к провайдеру |
| `signedBy("akash1...")` | аудитор, которого провайдер должен иметь |

Порт без `global: true` в `uris` не появится. Cpu, память, диск, count и цену на уже открытом деплое билдер не обновляет: это новый деплой. Образ, env, command и args меняются новым манифестом поверх того же деплоя.

## Клиент

Ниже тот же SDK, которым этот `Sdl` потом уезжает в сеть. Ключ кошелька берётся из мнемоники: `AkashKeyPair.fromMnemonic(mnemonic)`. Для живого деплоя nginx смотри [JAkash Desktop](https://github.com/DEMoN881/JAkash-Desktop).

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

Комиссия транзакции в `uakt`. На закрытие закладывай газ с запасом: 200000 на mainnet уже не хватает.

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

### Прочитать готовый файл

```java
import network.akash.sdk.sdl.Sdl;
import network.akash.sdk.sdl.SdlParser;

Sdl sdl = SdlParser.parse(yaml);
sdl.validate();
```

`SdlParser.parse` только читает. Собрать или изменить документ можно через `SdlBuilder` выше.

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
