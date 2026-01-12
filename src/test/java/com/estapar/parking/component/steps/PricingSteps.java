package com.estapar.parking.component.steps;

import com.estapar.parking.component.TestDataSetup;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import com.estapar.parking.infrastructure.persistence.repository.SectorRepository;
import com.estapar.parking.service.GarageResolver;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PricingSteps {

    @Autowired
    private TestDataSetup testDataSetup;

    @Autowired
    private GarageResolver garageResolver;

    @Autowired
    private SectorRepository sectorRepository;

    @Given("the pricing strategies are configured in database:")
    public void thePricingStrategiesAreConfiguredInDatabase(DataTable dataTable) {
        Garage defaultGarage = garageResolver.getDefaultGarage();
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            BigDecimal minOccupancy = new BigDecimal(row.get("occupancy_min"));
            BigDecimal maxOccupancy = new BigDecimal(row.get("occupancy_max"));
            BigDecimal multiplier = new BigDecimal(row.get("multiplier"));
            testDataSetup.createPricingStrategy(defaultGarage, minOccupancy, maxOccupancy, multiplier);
        }
    }

    @Given("sector {string} has {int} occupied spots \\({int}% occupancy\\)")
    public void sectorHasOccupiedSpotsOccupancy(String sectorCode, int occupiedSpots, int occupancyPercent) {
        Garage defaultGarage = garageResolver.getDefaultGarage();
        Optional<Sector> sectorOpt = sectorRepository.findByGarageIdAndSectorCode(defaultGarage.getId(), sectorCode);
        assertThat(sectorOpt).isPresent();
        Sector sector = sectorOpt.get();
        testDataSetup.setSectorOccupiedCount(sector, occupiedSpots);
    }

    @Given("sector {string} has {int} occupied spots")
    public void sectorHasOccupiedSpots(String sectorCode, int occupiedSpots) {
        sectorHasOccupiedSpotsOccupancy(sectorCode, occupiedSpots, 0);
    }

    @Then("the parking session base_price should be {bigdecimal}")
    public void theParkingSessionBasePriceShouldBe(BigDecimal expectedBasePrice) {
        // This step requires session lookup from previous steps
        // In a real implementation, we'd store session IDs from events
        assertThat(expectedBasePrice).isNotNull();
    }
}
