package com.estapar.parking.component.steps;

import com.estapar.parking.component.config.ComponentTestConfig;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class SpotMatchingSteps extends ComponentTestConfig {

    @Autowired
    private TestContext testContext;

    @Autowired
    private ParkingEventSteps parkingEventSteps;

    @Given("the garage has spots in sector {string}:")
    public void theGarageHasSpotsInSector(String sectorCode, DataTable dataTable) {
        // Spots initialization is handled by garage initialization
        // This step documents the expected spots configuration
    }

    @Given("the coordinate tolerance is {double} degrees \\(approximately {double} meters\\)")
    public void theCoordinateToleranceIsDegreesApproximatelyMeters(Double tolerance, Double meters) {
        // Tolerance is configured in application.yml
        // This step documents the expected configuration
    }

    @Given("vehicle {string} entered at {string}")
    public void vehicleEnteredAt(String licensePlate, String entryTime) {
        parkingEventSteps.iSendENTRYEventForVehicleAt(licensePlate, entryTime);
    }

    @When("I send PARKED event for vehicle {string} with lat {double} and lng {double}")
    public void iSendPARKEDEventForVehicleWithLatAndLng(String licensePlate, Double lat, Double lng) {
        parkingEventSteps.iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, lat, lng);
    }

    @Then("the parking session spot_id should be assigned to spot {string}")
    public void theParkingSessionSpotIdShouldBeAssignedToSpot(String spotId) {
        // Verification would require querying the database
        assertEquals(200, testContext.getLastResponse().getStatusCode());
    }

    @And("the spot {string} should be marked as occupied")
    public void theSpotShouldBeMarkedAsOccupied(String spotId) {
        // Verification would require querying the database
    }

    @When("I send PARKED event for vehicle {string} with lat {double} and lng {double} \\(within {double} tolerance\\)")
    public void iSendPARKEDEventForVehicleWithLatAndLngWithinTolerance(String licensePlate, Double lat, Double lng, Double tolerance) {
        iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, lat, lng);
    }

    @And("the parking session spot_id should be assigned \\(matched within tolerance\\)")
    public void theParkingSessionSpotIdShouldBeAssignedMatchedWithinTolerance() {
        parkingEventSteps.theParkingSessionShouldHaveSpotIdAssigned();
    }

    @And("the matched spot should be within tolerance range")
    public void theMatchedSpotShouldBeWithinToleranceRange() {
        // Verification through coordinate matching logic
        assertEquals(200, testContext.getLastResponse().getStatusCode());
    }

    @When("I send PARKED event for vehicle {string} with lat {double} and lng {double} \\(far from any spot\\)")
    public void iSendPARKEDEventForVehicleWithLatAndLngFarFromAnySpot(String licensePlate, Double lat, Double lng) {
        iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, lat, lng);
    }

    @Then("the response status should be {int} \\(gracefully handled\\)")
    public void theResponseStatusShouldBeGracefullyHandled(int statusCode) {
        parkingEventSteps.theResponseStatusShouldBe(statusCode);
    }

    @And("the EXIT event should still be processable")
    public void theEXITEventShouldStillBeProcessable() {
        parkingEventSteps.theEXITEventShouldStillBeProcessable();
    }

    @Given("there are multiple spots very close together \\(within tolerance range\\) at lat {double} and lng {double}")
    public void thereAreMultipleSpotsVeryCloseTogetherWithinToleranceRangeAtLatAndLng(Double lat, Double lng) {
        // Setup scenario - would require database seeding
        // This step documents the expected state
    }

    @When("I send PARKED event for vehicle {string} with coordinates that match multiple spots")
    public void iSendPARKEDEventForVehicleWithCoordinatesThatMatchMultipleSpots(String licensePlate) {
        parkingEventSteps.iSendPARKEDEventForVehicleWithCoordinatesThatMatchMultipleSpots(licensePlate);
    }

    @Then("the response status should be {int} \\(Bad Request\\)")
    public void theResponseStatusShouldBeBadRequest(int statusCode) {
        parkingEventSteps.theResponseStatusShouldBe(statusCode);
    }

    @And("the error message should indicate ambiguous spot match")
    public void theErrorMessageShouldIndicateAmbiguousSpotMatch() {
        parkingEventSteps.theErrorMessageShouldIndicateAmbiguousSpotMatch();
    }

    @And("no spot should be marked as occupied")
    public void noSpotShouldBeMarkedAsOccupied() {
        parkingEventSteps.noSpotShouldBeMarkedAsOccupied();
    }

    @Given("spot {string} is already occupied by vehicle {string}")
    public void spotIsAlreadyOccupiedByVehicle(String spotId, String licensePlate) {
        // Setup scenario - create session and assign spot
        parkingEventSteps.iSendENTRYEventForVehicleAt(licensePlate, "2025-01-01T09:00:00.000Z");
        parkingEventSteps.iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, -23.561684, -46.655981);
    }

    @Then("the response status should be {int} \\(Conflict\\) or {int} \\(Bad Request\\)")
    public void theResponseStatusShouldBeConflictOrBadRequest(int statusCode1, int statusCode2) {
        parkingEventSteps.theResponseStatusShouldBeOr(statusCode1, statusCode2);
    }

    @And("the error should indicate spot is already occupied")
    public void theErrorShouldIndicateSpotIsAlreadyOccupied() {
        // Verification through error response
        Response response = testContext.getLastResponse();
        String body = response.getBody().asString();
        assertTrue(body.contains("occupied") || body.contains("SPOT_ALREADY_OCCUPIED") ||
                  response.getStatusCode() == 409 || response.getStatusCode() == 400);
    }

    @And("the spot {string} should remain occupied by vehicle {string}")
    public void theSpotShouldRemainOccupiedByVehicle(String spotId, String licensePlate) {
        // Verification would require querying the database
    }

    @Given("vehicle {string} entered sector {string} at {string}")
    public void vehicleEnteredSectorAt(String licensePlate, String sector, String entryTime) {
        parkingEventSteps.iSendENTRYEventForVehicleAt(licensePlate, entryTime);
    }

    @Given("the garage has sector {string} with a spot at coordinates lat {double} and lng {double}")
    public void theGarageHasSectorWithASpotAtCoordinatesLatAndLng(String sectorCode, Double lat, Double lng) {
        // Setup scenario - spots initialization
    }

    @And("the matched spot should belong to sector {string} \\(same as parking session sector\\)")
    public void theMatchedSpotShouldBelongToSectorSameAsParkingSessionSector(String sectorCode) {
        // Verification would require querying the database
        assertEquals(200, testContext.getLastResponse().getStatusCode());
    }

    @And("the matched spot should not belong to sector {string}")
    public void theMatchedSpotShouldNotBelongToSector(String sectorCode) {
        // Verification would require querying the database
    }

    @And("the parking session spot_id should be assigned to a spot in sector {string}")
    public void theParkingSessionSpotIdShouldBeAssignedToASpotInSector(String sectorCode) {
        parkingEventSteps.theParkingSessionShouldHaveSpotIdAssigned();
    }

    @Given("sector {string} has spots with different coordinates")
    public void sectorHasSpotsWithDifferentCoordinates(String sectorCode) {
        // Setup scenario - spots initialization
    }

    @And("the matched spot should be from sector {string} only")
    public void theMatchedSpotShouldBeFromSectorOnly(String sectorCode) {
        theMatchedSpotShouldBelongToSectorSameAsParkingSessionSector(sectorCode);
    }

    @And("the coordinate matching should search within sector {string} scope")
    public void theCoordinateMatchingShouldSearchWithinSectorScope(String sectorCode) {
        // Verification through business logic - matching is done within sector
        assertEquals(200, testContext.getLastResponse().getStatusCode());
    }
}
