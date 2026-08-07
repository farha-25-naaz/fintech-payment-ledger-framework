package com.fintech.ledger.tests;

import com.fintech.ledger.mocks.WireMockSetup;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class IdempotencyAndDoubleSpendTest {

    @BeforeClass
    public void setup() {
        WireMockSetup.startServer();
        RestAssured.baseURI = "http://localhost:8089";
    }

    /**
     * Test Case 1: Sequential duplicate request verification.
     * Re-sending a request with an existing idempotency key should return cached response.
     */
    @Test(priority = 1)
    public void testSequentialDuplicateIdempotencyKey() {
        given()
            .header("X-Idempotency-Key", "IK-DUPLICATE-123")
            .contentType("application/json")
            .body("{\"account_id\": \"ACC-001\", \"amount\": 100.00}")
        .when()
            .post("/v1/payments/charge")
        .then()
            .statusCode(200)
            .header("X-Cache-Lookup", "HIT-IDEMPOTENT")
            .body("status", equalTo("SUCCESS"))
            .body("is_duplicate", equalTo(true))
            .body("transaction_id", equalTo("txn_ORIGINAL_123"));
    }

    /**
     * Test Case 2: Concurrency & Double-Spend Validation.
     * Fires 5 parallel requests simultaneously using CountDownLatch to test race conditions.
     */
    @Test(priority = 2)
    public void testConcurrentDoubleSpendProtection() throws InterruptedException {
        int numberOfThreads = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); // Synchronizes exact start time for all threads
        List<Response> responses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.execute(() -> {
                try {
                    latch.await(); // Hold thread until latch releases
                    Response response = given()
                            .header("X-Idempotency-Key", "IK-DUPLICATE-123")
                            .contentType("application/json")
                            .body("{\"account_id\": \"ACC-001\", \"amount\": 100.00}")
                            .post("/v1/payments/charge");
                    responses.add(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Release all 5 threads simultaneously
        latch.countDown();
        executorService.shutdown();

        // Wait for all requests to finish
        while (!executorService.isTerminated()) {
            Thread.sleep(100);
        }

        // Assertions: All 5 parallel requests should be handled gracefully (HTTP 200) without failing
        Assert.assertEquals(responses.size(), numberOfThreads, "All parallel requests should be processed.");
        for (Response resp : responses) {
            Assert.assertEquals(resp.getStatusCode(), 200);
            Assert.assertEquals(resp.jsonPath().getBoolean("is_duplicate"), true);
        }
    }

    @AfterClass
    public void tearDown() {
        WireMockSetup.stopServer();
    }
}