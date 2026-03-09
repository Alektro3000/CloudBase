package com.al3000.cloudbase.service.search;

import com.al3000.cloudbase.dto.FileInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class StringSearchBenchmarkCsvExporter {

    private static final String DEFAULT_QUERY = "]";
    private static final String DEFAULT_HARD_QUERY = "a".repeat(511) + "b";
    private static final int DEFAULT_SYNTHETIC_REPETITIONS = 100;
    private static final int DEFAULT_HARD_REPETITIONS = 10;
    private static final int DEFAULT_HARD_SIZE = 100;


    public static void main(String[] args) throws IOException {
        Path outputDir = Path.of(System.getProperty(
                "searchBenchmark.outputDir",
                "build/reports/benchmarks/search"
        ));
        Files.createDirectories(outputDir);

        StringSearchBenchmark benchmark = new StringSearchBenchmark();

        exportSyntheticBenchmarks(benchmark, outputDir.resolve("synthetic.csv"));
        exportHardBenchmarks(benchmark, outputDir.resolve("hard.csv"));
    }

    private static void exportSyntheticBenchmarks(StringSearchBenchmark benchmark, Path output) throws IOException {
        String query = System.getProperty("searchBenchmark.syntheticQuery", DEFAULT_QUERY);
        int repetitions = Integer.getInteger("searchBenchmark.syntheticRepetitions", DEFAULT_SYNTHETIC_REPETITIONS);
        int[] sizes = parseList(System.getProperty("searchBenchmark.syntheticSizes", "30000"));

        List<String> lines = new ArrayList<>();
        lines.add(header());

        for (int size : sizes) {
            List<FileInfo> candidates = StringSearchBenchmark.SyntheticCandidates(query, size);
            List<SearchAlgorithmMeasurement> measurements = benchmark.compare(candidates, query, repetitions);
            appendRows(lines,
                    "synthetic",
                    datasetName("SYN", size, averageNameLength(candidates)),
                    candidates,
                    query,
                    measurements);
        }

        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private static void exportHardBenchmarks(StringSearchBenchmark benchmark, Path output) throws IOException {
        String query = System.getProperty("searchBenchmark.hardQuery", DEFAULT_HARD_QUERY);
        int repetitions = Integer.getInteger("searchBenchmark.hardRepetitions", DEFAULT_HARD_REPETITIONS);
        int size = Integer.getInteger("searchBenchmark.hardSize", DEFAULT_HARD_SIZE);
        int[] lengths = parseList(System.getProperty("searchBenchmark.hardLengths", "64,128,256,512,1024"));

        List<String> lines = new ArrayList<>();
        lines.add(header());

        for (int length : lengths) {
            List<FileInfo> candidates = StringSearchBenchmark.HardCandidates(size, length);
            List<SearchAlgorithmMeasurement> measurements = benchmark.compare(candidates, query, repetitions);
            appendRows(lines,
                    "hard",
                    datasetName("HARD", size, length),
                    candidates,
                    query,
                    measurements);
        }

        Files.write(output, lines, StandardCharsets.UTF_8);
    }

    private static void appendRows(
            List<String> lines,
            String datasetType,
            String datasetName,
            List<FileInfo> candidates,
            String query,
            List<SearchAlgorithmMeasurement> measurement
    ) {
        long searchableFiles = candidates.stream().filter(file -> "File".equals(file.type())).count();
        int meanNameLength = averageNameLength(candidates);

            lines.add(String.join(",",
                    datasetType,
                    datasetName,
                    Integer.toString(candidates.size()),
                    Long.toString(searchableFiles),
                    Integer.toString(query.length()),
                    Integer.toString(meanNameLength),
                    Integer.toString(measurement.getFirst().repetitions()),
                    Integer.toString(measurement.getFirst().matches().size()),
                    Long.toString(measurement.getFirst().elapsedNanos()),
                    Long.toString(measurement.getLast().elapsedNanos())
            ));

    }

    private static String header() {
        return "dataset_type,dataset_name,candidate_count,searchable_files,query_length,mean_name_length,repetitions,matches,elapsed_nanos_naive,elapsed_nanos_kmp";
    }

    private static int averageNameLength(List<FileInfo> candidates) {
        return (int) Math.round(candidates.stream()
                .filter(file -> "File".equals(file.type()))
                .mapToInt(file -> file.name().length())
                .average()
                .orElse(0.0));
    }

    private static int[] parseList(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    private static String datasetName(String prefix, int size, int length) {
        return prefix + "_N" + size + "_L" + length;
    }

}
