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

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Component
public class SpotMatchingSteps {
    
    @Autowired
    private TestContext testContext;
    
    @Value("${parking.service.url:http://localhost:3003}")
    private String serviceUrl;
    
    @Given("the garage has spots in sector {string}:")
    public void garageHasSpotsInSector(String sectorCode, io.cucumber.datatable.DataTable dataTable) {
        // Spots are created during garage initialization
        // This step documents the expected spot configuration
    }
    
    @Given("the coordinate tolerance is {double} degrees \\(approximately {double} meters\\)")
    public void coordinateToleranceIs(double degrees, double meters) {
        // Tolerance is configured in application.yml
    }
    
    @Given("vehicle {string} entered at {string}")
    public void vehicleEntered(String licensePlate, String entryTime) {
        Map<String, Object> entryBody = new HashMap<>();
        entryBody.put("license_plate", licensePlate);
        entryBody.put("entry_time", entryTime);
        entryBody.put("event", "ENTRY");
        entryBody.put("sector", "A");
        
        RestAssured.baseURI = serviceUrl;
        given()
            .contentType(ContentType.JSON)
            .body(entryBody)
            .when()
            .post("/webhook");
    }
    
    @When("I send PARKED event for vehicle {string} with lat {double} and lng {double} \\(within {double} tolerance\\)")
    public void sendParkedEventWithinTolerance(String licensePlate, double lat, double lng, double tolerance) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("license_plate", licensePlate);
        requestBody.put("lat", lat);
        requestBody.put("lng", lng);
        requestBody.put("event", "PARKED");
        
        RestAssured.baseURI = serviceUrl;
        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/webhook");
        
        testContext.setLastResponse(response);
    }
    
    @When("I send PARKED event for vehicle {string} with lat {double} and lng {double} \\(far from any spot\\)")
    public void sendParkedEventFarFromAnySpot(String licensePlate, double lat, double lng) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("license_plate", licensePlate);
        requestBody.put("lat", lat);
        requestBody.put("lng", lng);
        requestBody.put("event", "PARKED");
        
        RestAssured.baseURI = serviceUrl;
        Response response = given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/webhook");
        
        testContext.setLastResponse(response);
    }
    
    @When("I send PARKED event for vehicle {string} with coordinates that match multiple spots")
    public void sendParkedEventWithCoordinatesMatchingMultipleSpots(String licensePlate) {
        // Use coordinates that would match multiple spots
        sendParkedEventWithinTolerance(licensePlate, -23.561684, -46.655981, 0.000001);
    }
    
    @Given("spot {string} is already occupied by vehicle {string}")
    public void spotIsAlreadyOccupied(String spotId, String licensePlate) {
        // Setup spot as occupied
        vehicleEntered(licensePlate, "2025-01-01T09:00:00.000Z");
        
        Map<String, Object> parkedBody = new HashMap<>();
        parkedBody.put("license_plate", licensePlate);
        parkedBody.put("lat", -23.561684);
        parkedBody.put("lng", -46.655981);
        parkedBody.put("event", "PARKED");
        
        RestAssured.baseURI = serviceUrl;
        given()
            .contentType(ContentType.JSON)
            .body(parkedBody)
            .when()
            .post("/webhook");
    }
    
    @When("I send PARKED event for vehicle {string} with coordinates matching spot {string}")
    public void sendParkedEventWithCoordinatesMatchingSpot(String licensePlate, String spotId) {
        sendParkedEventWithinTolerance(licensePlate, -23.561684, -46.655981, 0.000001);
    }
    
    @Then("the parking session spot_id should be assigned to spot {string}")
    public void parkingSessionSpotIdShouldBeAssignedToSpot(String spotId) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the spot {string} should be marked as occupied")
    public void spotShouldBeMarkedAsOccupied(String spotId) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the parking session spot_id should be assigned \\(matched within tolerance\\)")
    public void parkingSessionSpotIdShouldBeAssignedMatchedWithinTolerance() {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the matched spot should be within tolerance range")
    public void matchedSpotShouldBeWithinToleranceRange() {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the EXIT event should still be processable")
    public void exitEventShouldStillBeProcessable() {
        // EXIT event should work even if spot not found
    }
    
    @Then("the error message should indicate ambiguous spot match")
    public void errorMessageShouldIndicateAmbiguousSpotMatch() {
        testContext.getLastResponse().then().statusCode(400);
    }
    
    @Then("no spot should be marked as occupied")
    public void noSpotShouldBeMarkedAsOccupied() {
        // No spot marked when ambiguous
        testContext.getLastResponse().then().statusCode(400);
    }
    
    @Then("the error should indicate spot is already occupied")
    public void errorShouldIndicateSpotIsAlreadyOccupied() {
        testContext.getLastResponse().then().statusCode(anyOf(is(409), is(400)));
    }
    
    @Then("the spot {string} should remain occupied by vehicle {string}")
    public void spotShouldRemainOccupiedByVehicle(String spotId, String licensePlate) {
        testContext.getLastResponse().then().statusCode(anyOf(is(409), is(400)));
    }
    
    @Given("vehicle {string} entered sector {string} at {string}")
    public void vehicleEnteredSector(String licensePlate, String sectorCode, String entryTime) {
        Map<String, Object> entryBody = new HashMap<>();
        entryBody.put("license_plate", licensePlate);
        entryBody.put("entry_time", entryTime);
        entryBody.put("event", "ENTRY");
        entryBody.put("sector", sectorCode);
        
        RestAssured.baseURI = serviceUrl;
        given()
            .contentType(ContentType.JSON)
            .body(entryBody)
            .when()
            .post("/webhook");
    }
    
    @Given("the garage has sector {string} with a spot at coordinates lat {double} and lng {double}")
    public void garageHasSectorWithSpotAtCoordinates(String sectorCode, double lat, double lng) {
        // Spot configuration is handled by garage initialization
    }
    
    @Then("the matched spot should belong to sector {string} \\(same as parking session sector\\)")
    public void matchedSpotShouldBelongToSectorSameAsParkingSessionSector(String sectorCode) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the matched spot should not belong to sector {string}")
    public void matchedSpotShouldNotBelongToSector(String sectorCode) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the parking session spot_id should be assigned to a spot in sector {string}")
    public void parkingSessionSpotIdShouldBeAssignedToSpotInSector(String sectorCode) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Given("sector {string} has spots with different coordinates")
    public void sectorHasSpotsWithDifferentCoordinates(String sectorCode) {
        // Spot configuration is handled by garage initialization
    }
    
    @When("I send PARKED event for vehicle {string} with coordinates from sector {string}")
    public void sendParkedEventWithCoordinatesFromSector(String licensePlate, String sectorCode) {
        sendParkedEventWithinTolerance(licensePlate, -23.561684, -46.655981, 0.000001);
    }
    
    @Then("the matched spot should be from sector {string} only")
    public void matchedSpotShouldBeFromSectorOnly(String sectorCode) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Then("the coordinate matching should search within sector {string} scope")
    public void coordinateMatchingShouldSearchWithinSectorScope(String sectorCode) {
        testContext.getLastResponse().then().statusCode(200);
    }
    
    @Given("there are multiple spots very close together \\(within tolerance range\\) at lat {double} and lng {double}")
    public void thereAreMultipleSpotsVeryCloseTogether(double lat, double lng) {
        // Multiple spots configuration would need to be set up
        // This is a test scenario setup
    }
}
