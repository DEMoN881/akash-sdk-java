package network.akash.sdk.integration;

import network.akash.sdk.crypto.AkashKeyPair;
import network.akash.sdk.client.DefaultAkashClient;
import network.akash.sdk.client.AkashClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "AKASH_MNEMONIC", matches = ".+")
class MainnetIntegrationTest {

    private static final String MNEMONIC = System.getenv("AKASH_MNEMONIC");
    private static final String REST = "https://akash-api.polkachu.com/";
    private static final String RPC = "https://akash-rpc.polkachu.com";

    private AkashClient buildClient() {
        AkashKeyPair kp = AkashKeyPair.fromMnemonic(MNEMONIC);
        return DefaultAkashClient.builder()
                .rpcEndpoint(RPC)
                .restEndpoint(REST)
                .chainId("akashnet-2")
                .keyPair(kp)
                .build();
    }

    @Test
    void deriveAddressFromMnemonic() {
        AkashKeyPair kp = AkashKeyPair.fromMnemonic(MNEMONIC);
        String addr = kp.getAddress().toBech32();
        System.out.println("Derived address: " + addr);
        assertTrue(addr.startsWith("akash1"));
        assertEquals(44, addr.length());
    }

    @Test
    void queryBalanceOnMainnet() throws Exception {
        AkashClient client = buildClient();
        String addr = AkashKeyPair.fromMnemonic(MNEMONIC).getAddress().toBech32();
        System.out.println("Address: " + addr);

        var balance = client.query().cosmos().bankBalance(addr, "uakt");
        System.out.println("Balance: " + balance);
        assertNotNull(balance);
        assertTrue(balance.containsKey("balance"));
    }

    @Test
    void queryAllBalances() throws Exception {
        AkashClient client = buildClient();
        String addr = AkashKeyPair.fromMnemonic(MNEMONIC).getAddress().toBech32();

        var balances = client.query().cosmos().bankAllBalances(addr);
        System.out.println("All balances: " + balances);
        assertNotNull(balances);
        assertTrue(balances.containsKey("balances"));
    }

    @Test
    void queryNodeStatus() throws Exception {
        AkashClient client = buildClient();
        var syncInfo = client.node().syncInfo();
        System.out.println("Latest block: " + syncInfo.latestBlockHeight());
        System.out.println("Syncing: " + syncInfo.catchingUp());
        assertTrue(syncInfo.latestBlockHeight() > 0);
    }

    @Test
    void queryAuthAccount() throws Exception {
        AkashClient client = buildClient();
        String addr = AkashKeyPair.fromMnemonic(MNEMONIC).getAddress().toBech32();

        var account = client.query().cosmos().authAccount(addr);
        System.out.println("Account: " + account);
        assertNotNull(account);
        assertTrue(account.containsKey("account"));
    }

    @Test
    void queryDeploymentsRequiresGrpc() throws Exception {
        AkashClient client = buildClient();
        String addr = AkashKeyPair.fromMnemonic(MNEMONIC).getAddress().toBech32();

        try {
            var deployments = client.query().deployments(Map.of("filters.owner", addr));
            System.out.println("Deployments: " + deployments);
            assertNotNull(deployments);
        } catch (RuntimeException e) {
            System.out.println("Expected: Akash LCD deprecated on this node — " + e.getMessage());
            assertTrue(e.getMessage().contains("501") || e.getMessage().contains("Not Implemented"));
        }
    }

    @Test
    void queryProvidersRequiresGrpc() throws Exception {
        AkashClient client = buildClient();
        try {
            var providers = client.query().providers(Map.of());
            System.out.println("Providers: " + providers);
            assertNotNull(providers);
        } catch (RuntimeException e) {
            System.out.println("Expected: Akash LCD deprecated on this node — " + e.getMessage());
            assertTrue(e.getMessage().contains("501") || e.getMessage().contains("Not Implemented"));
        }
    }
}
