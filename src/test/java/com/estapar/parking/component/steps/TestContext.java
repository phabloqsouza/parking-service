package com.estapar.parking.component.steps;

import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TestContext {
    
    private Response lastResponse;
    private final Map<String, Object> contextData = new HashMap<>();
    
    public Response getLastResponse() {
        return lastResponse;
    }
    
    public void setLastResponse(Response response) {
        this.lastResponse = response;
    }
    
    public void put(String key, Object value) {
        contextData.put(key, value);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) contextData.get(key);
    }
    
    public void clear() {
        contextData.clear();
        lastResponse = null;
    }
}
