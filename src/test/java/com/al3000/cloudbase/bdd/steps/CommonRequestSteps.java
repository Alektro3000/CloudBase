package com.al3000.cloudbase.bdd.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class CommonRequestSteps extends BaseStepDefinitions {

    @When("the client POSTs the payload to {string}")
    public void theClientPostsThePayloadTo(String endpoint) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(context.getApiBasePath() + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(context.getRequestPayload()));
        context.setLastResult(perform(requestBuilder));
    }

    @When("the client POSTs to {string}")
    public void theClientPostsTo(String endpoint) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(context.getApiBasePath() + endpoint)
                .accept(MediaType.APPLICATION_JSON);
        context.setLastResult(perform(requestBuilder));
    }

    @When("the client GETs {string} with query parameters:")
    public void theClientGetsWithQueryParameters(String endpoint, DataTable dataTable) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(context.getApiBasePath() + endpoint)
                .accept(MediaType.APPLICATION_JSON);
        context.setLastResult(perform(withQueryParameters(requestBuilder, dataTable)));
    }

    @When("the client POSTs to {string} with query parameters:")
    public void theClientPostsToWithQueryParameters(String endpoint, DataTable dataTable) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post(context.getApiBasePath() + endpoint)
                .accept(MediaType.APPLICATION_JSON);
        context.setLastResult(perform(withQueryParameters(requestBuilder, dataTable)));
    }

    @When("the client DELETEs {string} with query parameters:")
    public void theClientDeletesWithQueryParameters(String endpoint, DataTable dataTable) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete(context.getApiBasePath() + endpoint)
                .accept(MediaType.APPLICATION_JSON);
        context.setLastResult(perform(withQueryParameters(requestBuilder, dataTable)));
    }

    @When("the client uploads a file to {string}:")
    public void theClientUploadsAFileTo(String endpoint, DataTable dataTable) throws Exception {
        Map<String, String> values = toPayload(dataTable);
        MockMultipartFile file = new MockMultipartFile(
                "object",
                values.get("filename"),
                values.getOrDefault("contentType", MediaType.APPLICATION_OCTET_STREAM_VALUE),
                values.getOrDefault("content", "").getBytes(StandardCharsets.UTF_8)
        );

        MockHttpServletRequestBuilder requestBuilder = multipart(context.getApiBasePath() + endpoint)
                .file(file)
                .param("path", values.getOrDefault("path", ""));
        context.setLastResult(perform(requestBuilder));
    }
}
