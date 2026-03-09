package com.al3000.cloudbase.bdd.steps;

import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.bdd.support.TestScenarioContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

abstract class BaseStepDefinitions {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestScenarioContext context;

    protected Map<String, String> toPayload(DataTable dataTable) {
        return new LinkedHashMap<>(dataTable.asMap(String.class, String.class));
    }

    protected List<FileInfo> toFileInfoList(DataTable dataTable) {
        return dataTable.asMaps().stream()
                .map(row -> new FileInfo(
                        row.get("path"),
                        row.get("name"),
                        Long.valueOf(row.get("size")),
                        row.get("type")
                ))
                .toList();
    }

    protected FileInfo singleFileInfo(DataTable dataTable) {
        List<FileInfo> rows = toFileInfoList(dataTable);
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }

    protected MvcResult perform(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        if (context.getAuthenticatedUser() != null) {
            requestBuilder.with(user(context.getAuthenticatedUser()));
        }
        return mockMvc.perform(requestBuilder).andReturn();
    }

    protected MockHttpServletRequestBuilder withQueryParameters(
            MockHttpServletRequestBuilder requestBuilder,
            DataTable dataTable
    ) {
        Map<String, String> params = toPayload(dataTable);
        params.forEach((key, value) -> requestBuilder.queryParam(key, value == null ? "" : value));
        return requestBuilder;
    }

    protected JsonNode responseJson() throws Exception {
        assertThat(context.getLastResult()).isNotNull();
        String content = context.getLastResult().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }
}
