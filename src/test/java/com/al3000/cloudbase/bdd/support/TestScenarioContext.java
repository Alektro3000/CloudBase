package com.al3000.cloudbase.bdd.support;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ScenarioScope
public class TestScenarioContext {
    private String apiBasePath = "";
    private String authenticatedUser;
    private Map<String, String> requestPayload = new LinkedHashMap<>();
    private MvcResult lastResult;

    public String getApiBasePath() {
        return apiBasePath;
    }

    public void setApiBasePath(String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    public void setAuthenticatedUser(String authenticatedUser) {
        this.authenticatedUser = authenticatedUser;
    }

    public Map<String, String> getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(Map<String, String> requestPayload) {
        this.requestPayload = requestPayload;
    }

    public MvcResult getLastResult() {
        return lastResult;
    }

    public void setLastResult(MvcResult lastResult) {
        this.lastResult = lastResult;
    }

}
