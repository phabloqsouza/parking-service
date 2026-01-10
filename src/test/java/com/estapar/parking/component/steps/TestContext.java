package com.estapar.parking.component.steps;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
public class TestContext {
    
    private Response lastResponse;
    private String vehicleLicensePlate;
    private String lastSectorCode;
    
    public Response getLastResponse() {
        return lastResponse;
    }
    
    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }
    
    public String getVehicleLicensePlate() {
        return vehicleLicensePlate;
    }
    
    public void setVehicleLicensePlate(String vehicleLicensePlate) {
        this.vehicleLicensePlate = vehicleLicensePlate;
    }
    
    public String getLastSectorCode() {
        return lastSectorCode;
    }
    
    public void setLastSectorCode(String lastSectorCode) {
        this.lastSectorCode = lastSectorCode;
    }
    
    public void reset() {
        this.lastResponse = null;
        this.vehicleLicensePlate = null;
        this.lastSectorCode = null;
    }
}
