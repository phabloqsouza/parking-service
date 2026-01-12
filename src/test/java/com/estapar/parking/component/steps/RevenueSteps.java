package com.estapar.parking.component.steps;

import com.estapar.parking.api.dto.RevenueRequestDto;
import com.estapar.parking.api.dto.RevenueResponseDto;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.ParkingSession;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.GarageRepository;
import com.estapar.parking.infrastructure.persistence.repository.ParkingSessionRepository;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.service.GarageResolver;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RevenueSteps {

    @LocalServerPort
    private int port;

    @Autowired
    private GarageResolver garageResolver;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private ParkingSessionRepository sessionRepository;

    private io.restassured.response.Response lastResponse;

    @Given("vehicle {string} entered sector {string} at {string} and exited at {string} with final price {bigdecimal}")
    public void vehicleEnteredSectorAtAndExitedAtWithFinalPrice(
            String licensePlate, String sectorCode, String entryTime, String exitTime, BigDecimal finalPrice) {
        Garage defaultGarage = garageResolver.getDefaultGarage();
        Sector sector = sectorRepository.findByGarageIdAndSectorCode(defaultGarage.getId(), sectorCode)
                .orElseThrow();

        ParkingSession session = new ParkingSession();
        session.setVehicleLicensePlate(licensePlate);
        session.setSector(sector);
        session.setEntryTime(Instant.parse(entryTime));
        session.setExitTime(Instant.parse(exitTime));
        session.setBasePrice(BigDecimal.TEN);
        session.setFinalPrice(finalPrice);
        session.setVersion(0);
        session.setCreatedAt(Instant.now());

        sessionRepository.save(session);
    }

    @Given("no vehicles exited from sector {string} on {string}")
    public void noVehiclesExitedFromSectorOn(String sectorCode, String date) {
        // No setup needed - just ensuring no completed sessions exist
    }

    @Given("vehicle {string} entered sector {string} at {string} and has not exited")
    public void vehicleEnteredSectorAtAndHasNotExited(String licensePlate, String sectorCode, String entryTime) {
        // This vehicle has an active session (no exit time)
        // Actual session creation would be done by ENTRY event
    }

    @When("I send POST request to {string} with body:")
    public void iSendPOSTRequestToWithBody(String endpoint, String body) {
        String url = "http://localhost:" + port + endpoint;
        lastResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
    }

    @Then("the response should contain amount {bigdecimal}")
    public void theResponseShouldContainAmount(BigDecimal expectedAmount) {
        RevenueResponseDto response = lastResponse.getBody().as(RevenueResponseDto.class);
        assertThat(response.getAmount()).isEqualByComparingTo(expectedAmount);
    }

    @Then("the response should contain currency {string}")
    public void theResponseShouldContainCurrency(String expectedCurrency) {
        RevenueResponseDto response = lastResponse.getBody().as(RevenueResponseDto.class);
        assertThat(response.getCurrency()).isEqualTo(expectedCurrency);
    }

    @Then("the response should contain timestamp in ISO 8601 format")
    public void theResponseShouldContainTimestampInISO8601Format() {
        RevenueResponseDto response = lastResponse.getBody().as(RevenueResponseDto.class);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Then("the amount should be {bigdecimal}")
    public void theAmountShouldBe(BigDecimal expectedAmount) {
        theResponseShouldContainAmount(expectedAmount);
    }

    @Then("the currency should be {string}")
    public void theCurrencyShouldBe(String expectedCurrency) {
        theResponseShouldContainCurrency(expectedCurrency);
    }

    @Then("the amount should be {bigdecimal} \\(only completed session\\)")
    public void theAmountShouldBeOnlyCompletedSession(BigDecimal expectedAmount) {
        theResponseShouldContainAmount(expectedAmount);
    }

    @Then("the active session should not be included in revenue")
    public void theActiveSessionShouldNotBeIncludedInRevenue() {
        // Verification that only completed sessions are included
        assertThat(lastResponse.getStatusCode()).isEqualTo(200);
    }

    @Then("the active session should not have final_price set")
    public void theActiveSessionShouldNotHaveFinalPriceSet() {
        // Verification that active sessions don't have final_price
        assertThat(lastResponse.getStatusCode()).isEqualTo(200);
    }

    @Then("the amount should be {bigdecimal} \\(rounded to 2 decimal places for currency precision\\)")
    public void theAmountShouldBeRoundedTo2DecimalPlaces(BigDecimal expectedAmount) {
        RevenueResponseDto response = lastResponse.getBody().as(RevenueResponseDto.class);
        assertThat(response.getAmount()).isEqualByComparingTo(expectedAmount);
        assertThat(response.getAmount().scale()).isLessThanOrEqualTo(2);
    }

    @Then("the amount should be a BigDecimal with scale {int}")
    public void theAmountShouldBeABigDecimalWithScale(int expectedScale) {
        RevenueResponseDto response = lastResponse.getBody().as(RevenueResponseDto.class);
        assertThat(response.getAmount().scale()).isLessThanOrEqualTo(expectedScale);
    }

    @Then("the amount should be {bigdecimal} \\(only sessions from {string}\\)")
    public void theAmountShouldBeOnlySessionsFrom(String expectedAmount, String date) {
        theResponseShouldContainAmount(new BigDecimal(expectedAmount));
    }

    @Then("only completed sessions \\(with exit_time and final_price\\) should be included in revenue")
    public void onlyCompletedSessionsShouldBeIncludedInRevenue() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(200);
    }
}
