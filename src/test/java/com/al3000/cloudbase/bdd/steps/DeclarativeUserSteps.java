package com.al3000.cloudbase.bdd.steps;

import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import io.cucumber.java.en.Given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class DeclarativeUserSteps extends BaseStepDefinitions {

    @Given("an authenticated session exists for {string}")
    public void anAuthenticatedSessionExistsFor(String username) {
        context.setAuthenticatedUser(username);
    }

    @When("the current profile is requested")
    public void theCurrentProfileIsRequested() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(context.getApiBasePath() + "/me")
                .accept(MediaType.APPLICATION_JSON);
        context.setLastResult(perform(requestBuilder));
    }
}
