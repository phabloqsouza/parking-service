package com.estapar.parking.component.steps;

import com.estapar.parking.component.TestDataSetup;
import com.estapar.parking.infrastructure.persistence.entity.Garage;
import com.estapar.parking.infrastructure.persistence.entity.Sector;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class GarageSteps {

    @Autowired
    private TestDataSetup testDataSetup;

    private Garage defaultGarage;

    @Given("the garage is initialized with default garage")
    public void theGarageIsInitializedWithDefaultGarage() {
        defaultGarage = testDataSetup.createDefaultGarage();
    }

    @Given("the garage has sector {string} with base price {bigdecimal} and max capacity {int}")
    public void theGarageHasSectorWithBasePriceAndMaxCapacity(String sectorCode, BigDecimal basePrice, Integer maxCapacity) {
        if (defaultGarage == null) {
            defaultGarage = testDataSetup.createDefaultGarage();
        }
        testDataSetup.createSector(defaultGarage, sectorCode, basePrice, maxCapacity);
    }

    @Given("the garage has sector {string} with base price {bigdecimal}")
    public void theGarageHasSectorWithBasePrice(String sectorCode, BigDecimal basePrice) {
        if (defaultGarage == null) {
            defaultGarage = testDataSetup.createDefaultGarage();
        }
        testDataSetup.createSector(defaultGarage, sectorCode, basePrice, 100);
    }
}
