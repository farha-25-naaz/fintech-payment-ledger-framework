package com.fintech.ledger.tests;

import com.fintech.ledger.mocks.WireMockSetup;
import io.restassured.RestAssured;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class WireMockSanityTest {

    @BeforeClass
    public void setup() {
        WireMockSetup.startServer();
        RestAssured.baseURI = "http://localhost:8089";
    }

    @Test
    public void testSuccessfulPaymentCharge() {
        given()
            .header("X-Idempotency-Key", "IK-NEW-999")
            .contentType("application/json")
            .body("{\"account_id\": \"ACC-001\", \"amount\": 250.00}")
        .when()
            .post("/v1/payments/charge")
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .body("transaction_id", equalTo("txn_998877"));
    }

    @AfterClass
    public void tearDown() {
        WireMockSetup.stopServer();
    }
}