package com.al3000.cloudbase.bdd.steps;

import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.service.search.StringSearchBenchmark;
import com.al3000.cloudbase.service.search.SearchAlgorithmMeasurement;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchAlgorithmSteps extends BaseStepDefinitions {

    private final StringSearchBenchmark benchmark = new StringSearchBenchmark();

    private List<FileInfo> searchBenchmarkCandidates = new ArrayList<>();
    private List<SearchAlgorithmMeasurement> searchBenchmarkMeasurements = new ArrayList<>();
    private String searchBenchmarkQuery = "";

    @Given("the search benchmark owner is {string}")
    public void theSearchBenchmarkOwnerIs(String username) {
        context.setAuthenticatedUser(username);
    }

    @Given("the search benchmark candidates are:")
    public void theSearchBenchmarkCandidatesAre(DataTable dataTable) {
        searchBenchmarkCandidates = toFileInfoList(dataTable);
    }

    @Given("the generated search benchmark dataset has {int} scientific files for query {string}")
    public void theGeneratedSearchBenchmarkDatasetHasScientificFilesForQuery(int datasetSize, String query) {
        String ownerPath = context.getAuthenticatedUser() == null
                ? "research/"
                : context.getAuthenticatedUser() + "/research/";

        searchBenchmarkCandidates = IntStream.range(0, datasetSize)
                .mapToObj(index -> {
                    boolean matchingName = index % 11 == 0 || index % 17 == 0;
                    boolean directoryMarker = index % 29 == 0;
                    String name = matchingName
                            ? "experiment-" + index + "-" + query + "-series.dat"
                            : "experiment-" + index + "-control.dat";
                    String type = directoryMarker ? "Directory" : "File";
                    return new FileInfo(ownerPath, name, 100L + index, type);
                })
                .toList();
    }

    @When("the filename search algorithms are compared for query {string} with {int} repetitions")
    public void theFilenameSearchAlgorithmsAreComparedForQueryWithRepetitions(String query, int repetitions) {
        searchBenchmarkQuery = query;
        searchBenchmarkMeasurements =
                benchmark.compare(searchBenchmarkCandidates, query, repetitions);
    }

    @Then("both algorithms should return the following file names:")
    public void bothAlgorithmsShouldReturnTheFollowingFileNames(DataTable dataTable) {
        List<String> expected = dataTable.asMaps().stream()
                .map(row -> row.get("name"))
                .sorted()
                .toList();

        for (SearchAlgorithmMeasurement measurement : searchBenchmarkMeasurements) {
            assertThat(namesOf(measurement.matches())).containsExactlyElementsOf(expected);
        }
    }

    @Then("both algorithms should return the same matches")
    public void bothAlgorithmsShouldReturnTheSameMatches() {
        assertThat(searchBenchmarkMeasurements).hasSizeGreaterThanOrEqualTo(2);

        List<String> baseline = namesOf(searchBenchmarkMeasurements.getFirst().matches());
        for (SearchAlgorithmMeasurement measurement : searchBenchmarkMeasurements) {
            assertThat(namesOf(measurement.matches())).containsExactlyElementsOf(baseline);
        }
    }

    @And("the comparison should agree with the reference substring matcher")
    public void theComparisonShouldAgreeWithTheReferenceSubstringMatcher() {
        List<String> expected = namesOf(
                benchmark.referenceMatches(
                        searchBenchmarkCandidates,
                        searchBenchmarkQuery
                )
        );

        for (SearchAlgorithmMeasurement measurement : searchBenchmarkMeasurements) {
            assertThat(namesOf(measurement.matches())).containsExactlyElementsOf(expected);
        }
    }

    @And("the benchmark should contain measurements for the following algorithms:")
    public void theBenchmarkShouldContainMeasurementsForTheFollowingAlgorithms(DataTable dataTable) {
        List<String> expectedAlgorithms = dataTable.asMaps().stream()
                .map(row -> row.get("algorithm"))
                .toList();

        List<String> actualAlgorithms = searchBenchmarkMeasurements.stream()
                .map(SearchAlgorithmMeasurement::algorithm)
                .toList();

        assertThat(actualAlgorithms).containsExactlyElementsOf(expectedAlgorithms);
    }

    @And("each algorithm should have recorded a positive elapsed time")
    public void eachAlgorithmShouldHaveRecordedAPositiveElapsedTime() {
        assertThat(searchBenchmarkMeasurements)
                .allSatisfy(measurement -> assertThat(measurement.elapsedNanos()).isPositive());
    }

    @And("no algorithm run should be skipped")
    public void noAlgorithmRunShouldBeSkipped() {
        assertThat(searchBenchmarkMeasurements).hasSize(benchmark.algorithmNames().size());
        assertThat(searchBenchmarkMeasurements)
                .allSatisfy(measurement -> {
                    assertThat(measurement.repetitions()).isPositive();
                    assertThat(measurement.matches()).isNotNull();
                });
    }

    private List<String> namesOf(List<FileInfo> files) {
        return files.stream()
                .sorted(Comparator.comparing(FileInfo::name))
                .map(FileInfo::name)
                .toList();
    }
}
