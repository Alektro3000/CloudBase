package com.al3000.cloudbase.service.search;

import com.al3000.cloudbase.dto.FileInfo;

import java.util.List;
import java.util.stream.IntStream;

public class StringSearchBenchmark {

    private final List<StringSearchAlgorithm> algorithms;

    public final static List<FileInfo> RealisticCandidates = List.of(
            new FileInfo("lab/", "ababa-spectrum.csv", 120L, "File"),
            new FileInfo("lab/", "beta-aba-notes.txt", 80L, "File"),
            new FileInfo("lab/", "control-sample.txt", 64L, "File"),
            new FileInfo("lab/", "aba-archive/", 0L, "Directory"));

    public static List<FileInfo> SyntheticCandidates(String query, int size)
    {
        return IntStream.range(0, size)
                .mapToObj(index -> {
                    boolean matchingName = index % 9 == 0 || index % 13 == 0;
                    boolean directoryMarker = index % 41 == 0;
                    String name = matchingName
                            ? "dataset-" + index + "-" + query + "-run.txt"
                            : "dataset-" + index + "-control.txt";
                    return new FileInfo("bench/", name, 128L + index, directoryMarker ? "Directory" : "File");
                })
                .toList();
    }

    public static List<FileInfo> HardCandidates(int size, int length)
    {
        return IntStream.range(0, size)
                .mapToObj(index -> {
                    boolean matchingName = index % 5 == 0;
                    String name = matchingName
                            ? "a".repeat(length-1) + "b"
                            : "a".repeat(length-1) + "c";
                    return new FileInfo("stress/", name, 256L + index, "File");
                })
                .toList();
    }

    public StringSearchBenchmark() {
        this(List.of(
                new NaiveSearch(),
                new KMPSearch()
        ));
    }

    public StringSearchBenchmark(List<StringSearchAlgorithm> algorithms) {
        this.algorithms = List.copyOf(algorithms);
    }

    public List<SearchAlgorithmMeasurement> compare(List<FileInfo> candidates, String query, int repetitions) {
        int effectiveRepetitions = Math.max(1, repetitions);
        return algorithms.stream()
                .map(algorithm -> benchmark(candidates, query, effectiveRepetitions, algorithm))
                .toList();
    }

    public List<FileInfo> referenceMatches(List<FileInfo> candidates, String query) {
        return candidates.stream()
                .filter(file -> file.type().equals("File"))
                .filter(file -> file.name().contains(query))
                .toList();
    }

    public List<String> algorithmNames() {
        return algorithms.stream().map(StringSearchAlgorithm::name).toList();
    }

    private SearchAlgorithmMeasurement benchmark(
            List<FileInfo> candidates,
            String query,
            int repetitions,
            StringSearchAlgorithm algorithm
    ) {
        List<FileInfo> matches;
        //Warmup
        matches = matchesFor(candidates, query, algorithm);

        long start = System.nanoTime();

        for (int run = 0; run < repetitions; run++) {
            matches = matchesFor(candidates, query, algorithm);
        }

        long elapsedNanos = Math.max(1L, System.nanoTime() - start);
        return new SearchAlgorithmMeasurement(algorithm.name(), matches, elapsedNanos, repetitions);
    }

    private List<FileInfo> matchesFor(List<FileInfo> candidates, String query, StringSearchAlgorithm algorithm) {
        return candidates.stream()
                .filter(file -> file.type().equals("File"))
                .filter(file -> algorithm.contains(file.name(), query))
                .toList();
    }
}

