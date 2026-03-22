package com.al3000.cloudbase.service;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.repository.FileRepository;
import com.al3000.cloudbase.service.search.StringSearchAlgorithm;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FileServiceDownloadObjectFuzzTest {

    private static final String USERNAME = "alice";

    private FileRepository fileRepository;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileRepository = Mockito.mock(FileRepository.class);
        StringSearchAlgorithm searchAlgorithm = Mockito.mock(StringSearchAlgorithm.class);
        fileService = new FileService(fileRepository, searchAlgorithm);
    }

    @FuzzTest(maxDuration = "30s")
    void downloadObjectHandlesFilesAndDirectories(FuzzedDataProvider data) throws Exception {
        boolean directoryRequest = data.consumeBoolean();
        FilePath requestedPath = new FilePath(USERNAME, sanitizePath(data.consumeString(40), directoryRequest));

        if (!directoryRequest) {
            byte[] payload = data.consumeBytes(data.remainingBytes());
            FileFullInfo info = fileInfoFromPath(requestedPath, payload.length);
            when(fileRepository.getFileInformation(requestedPath)).thenReturn(info);
            when(fileRepository.downloadFile(requestedPath)).thenReturn(new ByteArrayInputStream(payload));

            InputStreamResource resource = fileService.downloadObject(requestedPath);

            assertThat(resource.getInputStream().readAllBytes()).isEqualTo(payload);
            return;
        }

        FileFullInfo requestedDirectory = fileInfoFromPath(requestedPath, 0L);
        when(fileRepository.getFileInformation(requestedPath)).thenReturn(requestedDirectory);

        int childCount = data.consumeInt(0, 6);
        List<FileFullInfo> children = new ArrayList<>();
        Set<String> usedChildPaths = new HashSet<>();
        for (int i = 0; i < childCount; i++) {
            boolean childDirectory = data.consumeBoolean();
            String relativePath = uniqueRelativePath(
                    requestedPath.path(),
                    sanitizeRelativePath(data.consumeString(40), childDirectory),
                    childDirectory,
                    usedChildPaths
            );
            FilePath childPath = new FilePath(USERNAME, relativePath);
            FileFullInfo childInfo = fileInfoFromPath(childPath, data.consumeInt(0, 128));
            children.add(childInfo);

            if (childInfo.isFile()) {
                byte[] payload = data.consumeBytes(data.consumeInt(0, Math.min(64, data.remainingBytes())));
                when(fileRepository.downloadFile(childPath)).thenReturn(new ByteArrayInputStream(payload));
            }
        }

        when(fileRepository.getFolderContent(requestedPath, true)).thenReturn(children.stream());

        InputStreamResource resource = fileService.downloadObject(requestedPath);
        List<String> entryNames = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(resource.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                assertThat(entry.getName()).isNotEmpty();
                assertThat(entry.getName()).doesNotStartWith("/");
                assertThat(entry.getName()).doesNotContain("..");
                zipInputStream.readAllBytes();
            }
        }

        long expectedEntries = children.stream().filter(FileFullInfo::isFile).count();
        assertThat(entryNames).hasSize((int) expectedEntries);
    }

    private static FileFullInfo fileInfoFromPath(FilePath path, long size) {
        if (path.isDir()) {
            return new FileFullInfo(path.username(), path.getDirectoryPath(), path.getDirectoryName(), size, true);
        }
        return new FileFullInfo(path.username(), path.getFilePath(), path.getFileName(), size, false);
    }

    private static String sanitizePath(String raw, boolean directory) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return directory ? "root/" : "file.txt";
        }
        if (directory && !normalized.endsWith("/")) {
            normalized += "/";
        }
        if (!directory && normalized.endsWith("/")) {
            normalized += "file.txt";
        }
        return normalized;
    }

    private static String sanitizeRelativePath(String raw, boolean directory) {
        String normalized = normalize(raw);
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isEmpty()) {
            normalized = directory ? "child/" : "child.txt";
        }
        if (directory && !normalized.endsWith("/")) {
            normalized += "/";
        }
        if (!directory && normalized.endsWith("/")) {
            normalized += "child.txt";
        }
        return normalized;
    }

    private static String uniqueRelativePath(String basePath, String candidate, boolean directory, Set<String> usedPaths) {
        String fullPath = basePath + candidate;
        if (usedPaths.add(fullPath)) {
            return fullPath;
        }

        int suffix = 1;
        while (true) {
            String uniqueCandidate = appendSuffix(candidate, suffix++, directory);
            fullPath = basePath + uniqueCandidate;
            if (usedPaths.add(fullPath)) {
                return fullPath;
            }
        }
    }

    private static String appendSuffix(String candidate, int suffix, boolean directory) {
        if (directory) {
            String trimmed = candidate.substring(0, candidate.length() - 1);
            return trimmed + "-" + suffix + "/";
        }

        int dotIndex = candidate.lastIndexOf('.');
        if (dotIndex <= 0) {
            return candidate + "-" + suffix;
        }
        return candidate.substring(0, dotIndex) + "-" + suffix + candidate.substring(dotIndex);
    }

    private static String normalize(String raw) {
        String normalized = raw
                .replace('\\', '/')
                .replace("\r", "")
                .replace("\n", "");
        StringBuilder builder = new StringBuilder();
        boolean previousSlash = false;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current == '/') {
                if (!previousSlash) {
                    builder.append(current);
                }
                previousSlash = true;
            } else {
                builder.append(current);
                previousSlash = false;
            }
        }
        return builder.toString();
    }
}
