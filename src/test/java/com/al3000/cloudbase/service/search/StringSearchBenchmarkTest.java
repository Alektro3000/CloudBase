package com.al3000.cloudbase.service.search;

import com.al3000.cloudbase.dto.FileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class StringSearchBenchmarkTest {

    private final StringSearchBenchmark benchmark = new StringSearchBenchmark();



    @Test
    void compareAlgorithms_matchesReferenceOnRepresentativeScientificNames() {
        String query = "aba";
        List<FileInfo> candidates = StringSearchBenchmark.RealisticCandidates;

        List<SearchAlgorithmMeasurement> measurements = benchmark.compare(candidates, query, 20);
        List<FileInfo> expected = benchmark.referenceMatches(candidates, query);

        assertThat(measurements).hasSize(2);
        assertThat(measurements)
                .extracting(SearchAlgorithmMeasurement::algorithm)
                .containsExactly("Naive", "Knuth-Morris-Pratt");
        assertThat(measurements)
                .allSatisfy(measurement -> {
                    assertThat(measurement.matches()).containsExactlyElementsOf(expected);
                    assertThat(measurement.elapsedNanos()).isPositive();
                });

        printReport("representative scientific names", candidates, query, measurements);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000, 10000})
    void compareAlgorithms_underLoad(int datasetSize) {
        String query = "pattern";
        var candidates = StringSearchBenchmark.SyntheticCandidates(query, datasetSize);

        List<SearchAlgorithmMeasurement> measurements = benchmark.compare(candidates, query, 10);
        List<FileInfo> expected = benchmark.referenceMatches(candidates, query);


        assertThat(measurements)
                .allSatisfy(measurement -> {
                    assertThat(measurement.matches()).containsExactlyElementsOf(expected);
                    assertThat(measurement.repetitions()).isEqualTo(10);
                    assertThat(measurement.elapsedNanos()).isPositive();
                });

        printReport("dataset size " + datasetSize, candidates, query, measurements);
    }

    @Test
    void compareAlgorithms_onLongRepeatedPrefixes() {
        String query = "a".repeat(49) + "b";
        var length = 102;
        List<FileInfo> candidates = StringSearchBenchmark.HardCandidates(100, length);

        List<SearchAlgorithmMeasurement> measurements = benchmark.compare(candidates, query, 20);
        List<FileInfo> expected = benchmark.referenceMatches(candidates, query);

        assertThat(candidates)
                .allSatisfy(file -> assertThat(file.name()).hasSize(length));


        assertThat(measurements)
                .allSatisfy(measurement -> {
                    assertThat(measurement.matches()).containsExactlyElementsOf(expected);
                    assertThat(measurement.repetitions()).isEqualTo(20);
                    assertThat(measurement.elapsedNanos()).isPositive();
                });

        printReport("mean file name length " + length+ ", repeated-prefix workload", candidates, query, measurements);
    }

    private void printReport(
            String label,
            List<FileInfo> candidates,
            String query,
            List<SearchAlgorithmMeasurement> measurements
    ) {
        List<FileInfo> searchableFiles = candidates.stream()
                .filter(file -> "File".equals(file.type()))
                .toList();
        double meanFileNameLength = searchableFiles.stream()
                .mapToInt(file -> file.name().length())
                .average()
                .orElse(0.0);

        System.out.println();
        System.out.println("String search benchmark: " + label);
        System.out.printf(
                Locale.ROOT,
                "Searchable files: %d, query length: %d, mean file name length: %.2f%n",
                searchableFiles.size(),
                query.length(),
                meanFileNameLength
        );
        System.out.printf("%-22s %12s %12s %12s%n", "Algorithm", "Matches", "Runs", "Nanos");
        for (SearchAlgorithmMeasurement measurement : measurements) {
            System.out.printf(
                    "%-22s %12d %12d %12d%n",
                    measurement.algorithm(),
                    measurement.matches().size(),
                    measurement.repetitions(),
                    measurement.elapsedNanos()
            );
        }
    }
}

