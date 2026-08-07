package com.fintech.ledger.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockSetup {

    private static WireMockServer wireMockServer;
    private static final int PORT = 8089;

    public static void startServer() {
        if (wireMockServer == null || !wireMockServer.isRunning()) {
            wireMockServer = new WireMockServer(WireMockConfiguration.options().port(PORT));
            wireMockServer.start();
            configureFor("localhost", PORT);
            setupPaymentStubs();
            System.out.println(">>> WireMock Server started on http://localhost:" + PORT);
        }
    }

    public static void stopServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            System.out.println(">>> WireMock Server stopped.");
        }
    }

    private static void setupPaymentStubs() {
        // 1. Success Charge Endpoint
        stubFor(post(urlEqualTo("/v1/payments/charge"))
            .withHeader("X-Idempotency-Key", matching("IK-NEW-.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{"
                    + "\"status\": \"SUCCESS\","
                    + "\"transaction_id\": \"txn_998877\","
                    + "\"amount\": 250.00,"
                    + "\"currency\": \"USD\","
                    + "\"message\": \"Payment processed successfully.\""
                    + "}")));

        // 2. Duplicate Idempotency Key (Double-Spend Prevention)
        stubFor(post(urlEqualTo("/v1/payments/charge"))
            .withHeader("X-Idempotency-Key", equalTo("IK-DUPLICATE-123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("X-Cache-Lookup", "HIT-IDEMPOTENT")
                .withBody("{"
                    + "\"status\": \"SUCCESS\","
                    + "\"transaction_id\": \"txn_ORIGINAL_123\","
                    + "\"amount\": 100.00,"
                    + "\"currency\": \"USD\","
                    + "\"is_duplicate\": true"
                    + "}")));

        // 3. Simulated Gateway Timeout Delay
        stubFor(post(urlEqualTo("/v1/payments/charge-delayed"))
            .willReturn(aResponse()
                .withStatus(504)
                .withFixedDelay(3500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\": \"GATEWAY_TIMEOUT\", \"message\": \"Payment gateway timed out.\"}")));

        // 4. Ledger Account Balance Stub
        stubFor(get(urlEqualTo("/v1/ledger/account/ACC-001"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{"
                    + "\"account_id\": \"ACC-001\","
                    + "\"balance\": 5000.00,"
                    + "\"currency\": \"USD\","
                    + "\"status\": \"ACTIVE\""
                    + "}")));
    }
}