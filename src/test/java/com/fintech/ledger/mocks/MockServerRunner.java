package com.fintech.ledger.mocks;

public class MockServerRunner {
    public static void main(String[] args) throws Exception {
        // Start the WireMock server on port 8089
        WireMockSetup.startServer();

        System.out.println("\n========================================================");
        System.out.println("🚀 MOCK FINTECH SERVER IS LIVE & RUNNING!");
        System.out.println("Open this URL in your browser right now:");
        System.out.println("👉 http://localhost:8089/v1/ledger/account/ACC-001");
        System.out.println("========================================================");
        System.out.println("Press ENTER in this VS Code terminal when you want to stop the server...\n");

        // Pauses execution so the server stays alive indefinitely
        System.in.read();

        // Stop server when user hits enter
        WireMockSetup.stopServer();
    }
}