package com.al3000.cloudbase.bdd.steps;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonResponseSteps extends BaseStepDefinitions {

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int status) {
        assertThat(context.getLastResult()).isNotNull();
        assertThat(context.getLastResult().getResponse().getStatus()).isEqualTo(status);
    }

    @And("the response JSON should contain the field {string} with value {string}")
    public void theResponseJsonShouldContainTheFieldWithValue(String fieldName, String expectedValue) throws Exception {
        JsonNode json = responseJson();
        assertThat(json.has(fieldName)).isTrue();
        assertThat(json.get(fieldName).asText()).isEqualTo(expectedValue);
    }

    @And("the response JSON should contain the field {string}")
    public void theResponseJsonShouldContainTheField(String fieldName) throws Exception {
        JsonNode json = responseJson();
        assertThat(json.has(fieldName)).isTrue();
        assertThat(json.get(fieldName).isMissingNode()).isFalse();
    }

    @And("the response JSON array should have size {int}")
    public void theResponseJsonArrayShouldHaveSize(int expectedSize) throws Exception {
        JsonNode json = responseJson();
        assertThat(json.isArray()).isTrue();
        assertThat(json).hasSize(expectedSize);
    }

    @And("the response JSON at index {int} should contain the field {string} with value {string}")
    public void theResponseJsonAtIndexShouldContainTheFieldWithValue(int index, String fieldName, String expectedValue) throws Exception {
        JsonNode json = responseJson();
        assertThat(json.isArray()).isTrue();
        assertThat(json.get(index).get(fieldName).asText()).isEqualTo(expectedValue);
    }

    @And("the response should include a Set-Cookie header containing {string}")
    public void theResponseShouldIncludeASetCookieHeaderContaining(String expectedValue) {
        assertThat(context.getLastResult()).isNotNull();
        assertThat(context.getLastResult().getResponse().getHeaders("Set-Cookie"))
                .anySatisfy(header -> assertThat(header).contains(expectedValue));
    }

    @And("the response header {string} should contain {string}")
    public void theResponseHeaderShouldContain(String headerName, String expectedValue) {
        assertThat(context.getLastResult()).isNotNull();
        assertThat(context.getLastResult().getResponse().getHeader(headerName)).contains(expectedValue);
    }

    @And("the response content type should contain {string}")
    public void theResponseContentTypeShouldContain(String expectedValue) {
        assertThat(context.getLastResult()).isNotNull();
        assertThat(context.getLastResult().getResponse().getContentType()).contains(expectedValue);
    }

    @And("the response body should equal {string}")
    public void theResponseBodyShouldEqual(String expectedBody) throws Exception {
        assertThat(context.getLastResult()).isNotNull();
        assertThat(context.getLastResult().getResponse().getContentAsString(StandardCharsets.UTF_8)).isEqualTo(expectedBody);
    }
}
