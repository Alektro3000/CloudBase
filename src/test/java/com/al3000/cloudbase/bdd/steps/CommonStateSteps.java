package com.al3000.cloudbase.bdd.steps;

import io.cucumber.java.en.Given;

public class CommonStateSteps extends BaseStepDefinitions {

    @Given("the API base path is {string}")
    public void theApiBasePathIs(String apiBasePath) {
        context.setApiBasePath(apiBasePath);
    }

    @Given("the client is authenticated as {string}")
    public void theClientIsAuthenticatedAs(String username) {
        context.setAuthenticatedUser(username);
    }

    @Given("the client is unauthenticated")
    public void theClientIsUnauthenticated() {
        context.setAuthenticatedUser(null);
    }
}
