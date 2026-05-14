package com.banking.analyzer.rest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end smoke test for the REST API.
 *
 * <p>Run only when {@code -Drest.url=http://localhost:8080/transaction-analyzer}
 * is supplied. The server is expected to already be running with the seeded data
 * (start it in another terminal via {@code mvn cargo:run}).</p>
 *
 * <p>Flow covered:</p>
 * <ol>
 *   <li>USER login (alice) &rarr; /api/auth/me &rarr; deposit &rarr; withdraw &rarr; transfer to ACC002 &rarr; logout</li>
 *   <li>ADMIN login (admin) &rarr; list users &rarr; list accounts &rarr; admin stats &rarr; logout</li>
 *   <li>Negative: unauthenticated request returns 401</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "rest.url", matches = ".+")
class RestApiSmokeTest {

    private static String baseUrl;
    private static HttpClient userClient;
    private static HttpClient adminClient;
    private static final Gson GSON = new Gson();

    @BeforeAll
    static void setUp() {
        baseUrl = System.getProperty("rest.url");
        // Each client gets its own cookie manager so the two sessions don't interfere.
        userClient  = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(5)).build();
        adminClient = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    // ---------- USER FLOW ----------

    @Test @Order(1)
    void unauthenticatedReturns401() throws Exception {
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(get("/api/auth/me"), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, resp.statusCode(), "Unauthenticated /api/auth/me must be 401");
    }

    @Test @Order(2)
    void userLogin() throws Exception {
        HttpResponse<String> resp = userClient.send(
                postJson("/api/auth/login", Map.of("username", "alice", "password", "alice123")),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), resp.body());
        JsonObject user = GSON.fromJson(resp.body(), JsonObject.class);
        assertEquals("alice", user.get("username").getAsString());
        assertEquals("USER", user.get("role").getAsString());
    }

    @Test @Order(3)
    void userDashboard() throws Exception {
        HttpResponse<String> resp = userClient.send(get("/api/auth/me"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());
        JsonObject body = GSON.fromJson(resp.body(), JsonObject.class);
        assertEquals("alice", body.getAsJsonObject("user").get("username").getAsString());
        assertTrue(body.getAsJsonArray("accounts").size() >= 1, "alice should own at least ACC001");
    }

    @Test @Order(4)
    void depositAndWithdraw() throws Exception {
        HttpResponse<String> dep = userClient.send(
                postJson("/api/accounts/ACC001/deposit",
                        Map.of("amount", "250.00", "description", "smoke deposit")),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, dep.statusCode(), dep.body());
        assertEquals("CREDIT", GSON.fromJson(dep.body(), JsonObject.class).get("transactionType").getAsString());

        HttpResponse<String> wd = userClient.send(
                postJson("/api/accounts/ACC001/withdraw",
                        Map.of("amount", "50.00", "description", "smoke withdraw")),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, wd.statusCode(), wd.body());
        assertEquals("DEBIT", GSON.fromJson(wd.body(), JsonObject.class).get("transactionType").getAsString());
    }

    @Test @Order(5)
    void transferToAnotherAccount() throws Exception {
        HttpResponse<String> resp = userClient.send(
                postJson("/api/transfers",
                        Map.of("fromAccount", "ACC001",
                               "toAccount",   "ACC002",
                               "amount",      "100.00",
                               "description", "smoke transfer")),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, resp.statusCode(), resp.body());
        JsonObject body = GSON.fromJson(resp.body(), JsonObject.class);
        assertNotNull(body.getAsJsonObject("debit"));
        assertNotNull(body.getAsJsonObject("credit"));
    }

    @Test @Order(6)
    void userCannotAccessAdminEndpoints() throws Exception {
        HttpResponse<String> resp = userClient.send(get("/api/users"), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resp.statusCode(), "USER must be forbidden from /api/users");
    }

    @Test @Order(7)
    void userCannotTouchOtherAccount() throws Exception {
        HttpResponse<String> resp = userClient.send(
                postJson("/api/accounts/ACC002/deposit",
                        Map.of("amount", "1.00", "description", "intrusion")),
                HttpResponse.BodyHandlers.ofString());
        // BusinessException maps to 400; either 400 or 403 is acceptable proof of rejection.
        assertTrue(resp.statusCode() == 400 || resp.statusCode() == 403,
                "Expected 400/403 but got " + resp.statusCode() + " body=" + resp.body());
    }

    @Test @Order(8)
    void userLogout() throws Exception {
        HttpResponse<String> resp = userClient.send(
                postJson("/api/auth/logout", Map.of()),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode());

        HttpResponse<String> after = userClient.send(get("/api/auth/me"), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, after.statusCode(), "Session should be invalidated after logout");
    }

    // ---------- ADMIN FLOW ----------

    @Test @Order(9)
    void adminLogin() throws Exception {
        HttpResponse<String> resp = adminClient.send(
                postJson("/api/auth/login", Map.of("username", "admin", "password", "admin123")),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), resp.body());
        assertEquals("ADMIN", GSON.fromJson(resp.body(), JsonObject.class).get("role").getAsString());
    }

    @Test @Order(10)
    void adminListsAllUsersAndAccounts() throws Exception {
        HttpResponse<String> users = adminClient.send(get("/api/users"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, users.statusCode(), users.body());
        List<?> userList = GSON.fromJson(users.body(), new TypeToken<List<Map<String, Object>>>(){}.getType());
        assertTrue(userList.size() >= 3, "Seed should have at least admin/alice/bob");

        HttpResponse<String> accts = adminClient.send(get("/api/accounts"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, accts.statusCode());
        assertTrue(accts.body().contains("ACC001") && accts.body().contains("ACC002"));
    }

    @Test @Order(11)
    void adminStats() throws Exception {
        HttpResponse<String> resp = adminClient.send(get("/api/admin/stats"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resp.statusCode(), resp.body());
        JsonObject body = GSON.fromJson(resp.body(), JsonObject.class);
        assertTrue(body.get("totalUsers").getAsInt()        >= 3);
        assertTrue(body.get("totalAccounts").getAsInt()     >= 3);
        assertTrue(body.get("totalTransactions").getAsInt() >= 4);
        assertNotNull(body.get("systemBalance"));
        assertTrue(new BigDecimal(body.get("systemBalance").getAsString()).signum() >= 0);
    }

    // ---------- helpers ----------

    private static HttpRequest get(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET().build();
    }

    private static HttpRequest postJson(String path, Map<String, ?> body) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();
    }
}
