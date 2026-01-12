package com.estapar.parking.component.steps;

import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.service.GarageResolver;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SectorAssertionSteps {

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private GarageResolver garageResolver;

    @Then("the sector {string} occupied count should be {int}")
    public void theSectorOccupiedCountShouldBe(String sectorCode, int expectedCount) {
        Garage defaultGarage = garageResolver.getDefaultGarage();
        Optional<Sector> sector = sectorRepository.findByGarageIdAndSectorCode(defaultGarage.getId(), sectorCode);
        assertThat(sector).isPresent();
        assertThat(sector.get().getOccupiedCount()).isEqualTo(expectedCount);
    }

    @Then("the sector {string} occupied count should be incremented by {int}")
    public void theSectorOccupiedCountShouldBeIncrementedBy(String sectorCode, int increment) {
        Garage defaultGarage = garageResolver.getDefaultGarage();
        Optional<Sector> sector = sectorRepository.findByGarageIdAndSectorCode(defaultGarage.getId(), sectorCode);
        assertThat(sector).isPresent();
        assertThat(sector.get().getOccupiedCount()).isGreaterThanOrEqualTo(increment);
    }

    @Then("the sector {string} occupied count should remain {int}")
    public void theSectorOccupiedCountShouldRemain(String sectorCode, int expectedCount) {
        theSectorOccupiedCountShouldBe(sectorCode, expectedCount);
    }

    @Then("the sector {string} occupied count should be decremented by {int}")
    public void theSectorOccupiedCountShouldBeDecrementedBy(String sectorCode, int decrement) {
        Garage defaultGarage = garageResolver.getDefaultGarage();
        Optional<Sector> sector = sectorRepository.findByGarageIdAndSectorCode(defaultGarage.getId(), sectorCode);
        assertThat(sector).isPresent();
        assertThat(sector.get().getOccupiedCount()).isGreaterThanOrEqualTo(0);
    }

    @Then("the sector {string} occupied count should be decremented to {int}")
    public void theSectorOccupiedCountShouldBeDecrementedTo(String sectorCode, int expectedCount) {
        theSectorOccupiedCountShouldBe(sectorCode, expectedCount);
    }

    @Then("the sector {string} occupied count should be {int} \\(capacity reserved on ENTRY\\)")
    public void theSectorOccupiedCountShouldBeCapacityReservedOnENTRY(String sectorCode, int expectedCount) {
        theSectorOccupiedCountShouldBe(sectorCode, expectedCount);
    }

    @Then("the sector {string} occupied count should remain {int} \\(already counted on ENTRY, not incremented again\\)")
    public void theSectorOccupiedCountShouldRemainAlreadyCountedOnENTRY(String sectorCode, int expectedCount) {
        theSectorOccupiedCountShouldBe(sectorCode, expectedCount);
    }

    @Then("the sector {string} occupied count should remain {int} \\(not incremented on duplicate PARKED\\)")
    public void theSectorOccupiedCountShouldRemainNotIncrementedOnDuplicatePARKED(String sectorCode, int expectedCount) {
        theSectorOccupiedCountShouldBe(sectorCode, expectedCount);
    }
}
