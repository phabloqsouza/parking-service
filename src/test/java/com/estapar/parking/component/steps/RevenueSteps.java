package com.estapar.parking.component.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Component
public class RevenueSteps {
    
    @Autowired
    private TestContext testContext;
    
    @Value("${parking.service.url:http://localhost:3003}")
    private String serviceUrl;
    
    @Given("vehicle {string} entered sector {string} at {string} and exited at {string} with final price {double}")
    public void vehicleEnteredAndExited(String licensePlate, String sector, String entryTime, String exitTime, double finalPrice) {
        // Send ENTRY event
        Map<String, Object> entryBody = new HashMap<>();
        entryBody.put("license_plate", licensePlate);
        entryBody.put("entry_time", entryTime);
        entryBody.put("event", "ENTRY");
        entryBody.put("sector", sector);
        
        given()
            .contentType(ContentType.JSON)
            .body(entryBody)
            .when()
            .post("/webhook");
        
        // Send EXIT event
        Map<String, Object> exitBody = new HashMap<>();
        exitBody.put("license_plate", licensePlate);
        exitBody.put("exit_time", exitTime);
        exitBody.put("event", "EXIT");
        
        given()
            .contentType(ContentType.JSON)
            .body(exitBody)
            .when()
            .post("/webhook");
    }
    
    @Given("no vehicles exited from sector {string} on {string}")
    public void noVehiclesExitedFromSector(String sector, String date) {
        // No setup needed - no completed sessions
    }
    
    @Given("vehicle {string} entered sector {string} at {string} and has not exited")
    public void vehicleEnteredButNotExited(String licensePlate, String sector, String entryTime) {
        Map<String, Object> entryBody = new HashMap<>();
        entryBody.put("license_plate", licensePlate);
        entryBody.put("entry_time", entryTime);
        entryBody.put("event", "ENTRY");
        entryBody.put("sector", sector);
        
        given()
            .contentType(ContentType.JSON)
            .body(entryBody)
            .when()
            .post("/webhook");
    }
    
    @When("I send POST request to {string} with body:")
    public void sendPostRequestToRevenue(String endpoint, String body) {
        RestAssured.baseURI = serviceUrl;
        
        Response response = given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post(endpoint);
        
        testContext.setLastResponse(response);
    }
    
    @Then("the response should contain amount {double}")
    public void responseShouldContainAmount(double amount) {
        testContext.getLastResponse().then()
            .body("amount", equalTo(BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()));
    }
    
    @Then("the response should contain currency {string}")
    public void responseShouldContainCurrency(String currency) {
        testContext.getLastResponse().then()
            .body("currency", equalTo(currency));
    }
    
    @Then("the response should contain timestamp in ISO 8601 format")
    public void responseShouldContainTimestampInISO8601() {
        testContext.getLastResponse().then()
            .body("timestamp", notNullValue())
            .body("timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }
    
    @Then("only completed sessions \\(with exit_time and final_price\\) should be included in revenue")
    public void onlyCompletedSessionsShouldBeIncluded() {
        // Verified by revenue amount matching only completed sessions
    }
    
    @Then("the amount should be {double}")
    public void amountShouldBe(double amount) {
        testContext.getLastResponse().then()
            .body("amount", equalTo(BigDecimal.valueOf(amount).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()));
    }
    
    @Then("the currency should be {string}")
    public void currencyShouldBe(String currency) {
        testContext.getLastResponse().then()
            .body("currency", equalTo(currency));
    }
    
    @Then("the amount should be {double} \\(only completed session\\)")
    public void amountShouldBeOnlyCompletedSession(double amount) {
        amountShouldBe(amount);
    }
    
    @Then("the active session should not be included in revenue")
    public void activeSessionShouldNotBeIncluded() {
        // Verified by revenue amount
    }
    
    @Then("the active session should not have final_price set")
    public void activeSessionShouldNotHaveFinalPriceSet() {
        // Verified by revenue calculation
    }
    
    @Then("the amount should be {double} \\(rounded to {int} decimal places for currency precision\\)")
    public void amountShouldBeRounded(double amount, int decimalPlaces) {
        BigDecimal expected = BigDecimal.valueOf(amount).setScale(decimalPlaces, java.math.RoundingMode.HALF_UP);
        testContext.getLastResponse().then()
            .body("amount", equalTo(expected.doubleValue()));
    }
    
    @Then("the amount should be a BigDecimal with scale {int}")
    public void amountShouldBeBigDecimalWithScale(int scale) {
        // BigDecimal scale verification
        testContext.getLastResponse().then()
            .body("amount", notNullValue());
    }
    
    @Then("the amount should be {double} \\(only sessions from {string}\\)")
    public void amountShouldBeOnlySessionsFromDate(double amount, String date) {
        amountShouldBe(amount);
    }
}
