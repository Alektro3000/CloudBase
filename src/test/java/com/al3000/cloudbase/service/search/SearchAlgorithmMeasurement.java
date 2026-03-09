package com.al3000.cloudbase.service.search;

import com.al3000.cloudbase.dto.FileInfo;

import java.util.List;

public record SearchAlgorithmMeasurement(
        String algorithm,
        List<FileInfo> matches,
        long elapsedNanos,
        int repetitions
) {
}
