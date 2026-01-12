package com.estapar.parking.component.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class ParkingEventSteps {

    @LocalServerPort
    private int port;

    private io.restassured.response.Response lastResponse;

    @When("I send ENTRY event for vehicle {string} at {string}")
    public void iSendENTRYEventForVehicleAt(String licensePlate, String entryTime) {
        String url = "http://localhost:" + port + "/webhook";
        Map<String, Object> body = Map.of(
                "event_type", "ENTRY",
                "license_plate", licensePlate,
                "entry_time", entryTime,
                "sector", "A"
        );

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
    }

    @When("I send PARKED event for vehicle {string} with lat {bigdecimal} and lng {bigdecimal}")
    public void iSendPARKEDEventForVehicleWithLatAndLng(String licensePlate, BigDecimal lat, BigDecimal lng) {
        String url = "http://localhost:" + port + "/webhook";
        Map<String, Object> body = Map.of(
                "event_type", "PARKED",
                "license_plate", licensePlate,
                "lat", lat,
                "lng", lng
        );

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
    }

    @When("I send PARKED event for vehicle {string} with valid coordinates")
    public void iSendPARKEDEventForVehicleWithValidCoordinates(String licensePlate) {
        iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, 
                BigDecimal.valueOf(-23.561684), BigDecimal.valueOf(-46.655981));
    }

    @When("I send PARKED event for vehicle {string} with lat from sector {string} coordinates")
    public void iSendPARKEDEventForVehicleWithLatFromSectorCoordinates(String licensePlate, String sectorCode) {
        iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, 
                BigDecimal.valueOf(-23.561684), BigDecimal.valueOf(-46.655981));
    }

    @When("I send PARKED event for vehicle {string} with invalid coordinates \\(spot not found\\)")
    public void iSendPARKEDEventForVehicleWithInvalidCoordinatesSpotNotFound(String licensePlate) {
        iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, 
                BigDecimal.valueOf(999.000000), BigDecimal.valueOf(999.000000));
    }

    @When("I send PARKED event for vehicle {string} again with different coordinates")
    public void iSendPARKEDEventForVehicleAgainWithDifferentCoordinates(String licensePlate) {
        iSendPARKEDEventForVehicleWithLatAndLng(licensePlate, 
                BigDecimal.valueOf(-23.561685), BigDecimal.valueOf(-46.655982));
    }

    @When("I send EXIT event for vehicle {string} at {string}")
    public void iSendEXITEventForVehicleAt(String licensePlate, String exitTime) {
        String url = "http://localhost:" + port + "/webhook";
        Map<String, Object> body = Map.of(
                "event_type", "EXIT",
                "license_plate", licensePlate,
                "exit_time", exitTime
        );

        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
    }

    @When("I send EXIT event for vehicle {string} at {string} without PARKED event")
    public void iSendEXITEventForVehicleAtWithoutPARKEDEvent(String licensePlate, String exitTime) {
        iSendEXITEventForVehicleAt(licensePlate, exitTime);
    }

    public io.restassured.response.Response getLastResponse() {
        return lastResponse;
    }
}
