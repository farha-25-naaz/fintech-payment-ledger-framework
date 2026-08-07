package com.fintech.ledger.tests;

import com.fintech.ledger.mocks.WireMockSetup;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class LedgerAndTimeoutTest {

    @BeforeClass
    public void setup() {
        WireMockSetup.startServer();
        RestAssured.baseURI = "http://localhost:8089";
    }

    /**
     * Test Case 1: Webhook / Gateway Timeout Simulation.
     * Verifies that network delays over 3000ms return an explicit HTTP 504 error payload.
     */
    @Test(priority = 1)
    public void testGatewayTimeoutHandling() {
        long startTime = System.currentTimeMillis();

        given()
            .contentType("application/json")
            .body("{\"account_id\": \"ACC-001\", \"amount\": 500.00}")
        .when()
            .post("/v1/payments/charge-delayed")
        .then()
            .statusCode(504)
            .body("error", equalTo("GATEWAY_TIMEOUT"))
            .body("message", containsString("timed out"));

        long duration = System.currentTimeMillis() - startTime;
        Assert.assertTrue(duration >= 3000, "Request should take at least 3000ms due to simulated delay.");
    }

    /**
     * Test Case 2: Dynamic Ledger Reconciliation & Account Status Validation.
     * Queries the account ledger and asserts balance type, currency, and active status.
     */
    @Test(priority = 2)
    public void testLedgerAccountReconciliation() {
        Response response = given()
            .when()
                .get("/v1/ledger/account/ACC-001")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .extract().response();

        // Dynamic JSON Path Assertions
        String accountId = response.jsonPath().getString("account_id");
        float balance = response.jsonPath().getFloat("balance");
        String currency = response.jsonPath().getString("currency");
        String status = response.jsonPath().getString("status");

        Assert.assertEquals(accountId, "ACC-001", "Account ID mismatch.");
        Assert.assertTrue(balance > 0, "Account balance must be positive.");
        Assert.assertEquals(currency, "USD", "Currency code mismatch.");
        Assert.assertEquals(status, "ACTIVE", "Account should be active for transactions.");
    }

    @AfterClass
    public void tearDown() {
        WireMockSetup.stopServer();
    }
}