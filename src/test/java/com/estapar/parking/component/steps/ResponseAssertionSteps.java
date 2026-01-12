package com.estapar.parking.component.steps;

import io.cucumber.java.en.Then;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponseAssertionSteps {

    @Autowired
    private ParkingEventSteps parkingEventSteps;

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int statusCode) {
        Response response = parkingEventSteps.getLastResponse();
        assertThat(response.getStatusCode()).isEqualTo(statusCode);
    }

    @Then("the response status should be {int} or {int}")
    public void theResponseStatusShouldBeOr(int statusCode1, int statusCode2) {
        Response response = parkingEventSteps.getLastResponse();
        int actualStatusCode = response.getStatusCode();
        assertThat(actualStatusCode).isIn(statusCode1, statusCode2);
    }

    @Then("the response status should be {int} \\(idempotent\\)")
    public void theResponseStatusShouldBeIdempotent(int statusCode) {
        theResponseStatusShouldBe(statusCode);
    }

    @Then("the error message should indicate {string}")
    public void theErrorMessageShouldIndicate(String expectedMessage) {
        Response response = parkingEventSteps.getLastResponse();
        String responseBody = response.getBody().asString();
        assertThat(responseBody).containsIgnoringCase(expectedMessage);
    }

    @Then("the error should indicate sector is full")
    public void theErrorShouldIndicateSectorIsFull() {
        theErrorMessageShouldIndicate("full");
    }

    @Then("the error should indicate spot is already occupied")
    public void theErrorShouldIndicateSpotIsAlreadyOccupied() {
        theErrorMessageShouldIndicate("occupied");
    }
}
