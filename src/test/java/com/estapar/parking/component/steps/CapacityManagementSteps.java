package com.estapar.parking.component.steps;

import com.estapar.parking.component.config.ComponentTestConfig;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

public class CapacityManagementSteps extends ComponentTestConfig {

    @Autowired
    private ParkingEventSteps parkingEventSteps;

    @Given("the garage has sector {string} with max capacity {int} \\(for testing\\)")
    public void theGarageHasSectorWithMaxCapacityForTesting(String sectorCode, Integer maxCapacity) {
        // Sector initialization is handled by garage initialization
        // This step documents the expected configuration
    }

    @Given("the sector {string} has {int} parking spots")
    public void theSectorHasParkingSpots(String sectorCode, Integer spotCount) {
        // Spots initialization is handled by garage initialization
    }

    @Given("sector {string} has {int} occupied spots \\(100% capacity\\)")
    public void sectorHasOccupiedSpots100PercentCapacity(String sectorCode, Integer occupied) {
        // Setup scenario - would require database seeding for component tests
        // For component tests, we create the required sessions via ENTRY events
    }

    @And("the entry should be successful")
    public void theEntryShouldBeSuccessful() {
        parkingEventSteps.theEntryShouldBeSuccessful();
    }

    @And("the error should indicate sector is full")
    public void theErrorShouldIndicateSectorIsFull() {
        parkingEventSteps.theErrorShouldIndicateSectorIsFull();
    }

    @When("I send EXIT event for vehicle {string} at {string}")
    public void iSendEXITEventForVehicleAt(String licensePlate, String exitTime) {
        parkingEventSteps.iSendEXITEventForVehicleAt(licensePlate, exitTime);
    }

    @And("vehicle {string} should have parking session created")
    public void vehicleShouldHaveParkingSessionCreated(String licensePlate) {
        parkingEventSteps.vehicleShouldHaveParkingSessionCreated(licensePlate);
    }

    @When("I send PARKED event for vehicle {string} with invalid coordinates \\(spot not found\\)")
    public void iSendPARKEDEventForVehicleWithInvalidCoordinatesSpotNotFound(String licensePlate) {
        parkingEventSteps.iSendPARKEDEventForVehicleWithInvalidCoordinatesSpotNotFound(licensePlate);
    }

    @And("the vehicle should still count toward total capacity")
    public void theVehicleShouldStillCountTowardTotalCapacity() {
        parkingEventSteps.theVehicleShouldStillCountTowardTotalCapacity();
    }

    @When("I send PARKED event for vehicle {string} again \\(duplicate PARKED event\\)")
    public void iSendPARKEDEventForVehicleAgainDuplicatePARKEDEvent(String licensePlate) {
        parkingEventSteps.iSendPARKEDEventForVehicleAgainDuplicatePARKEDEvent(licensePlate);
    }

    @And("the capacity should reflect only ENTRY events, not PARKED events")
    public void theCapacityShouldReflectOnlyENTRYEventsNotPARKEDEvents() {
        parkingEventSteps.theCapacityShouldReflectOnlyENTRYEventsNotPARKEDEvents();
    }
}
