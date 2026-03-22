package com.al3000.cloudbase.repository;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FileRepositoryGetFolderContentFuzzTest {

    private static final String USERNAME = "alice";

    @BeforeEach
    void setUp() {
    }

    @FuzzTest(maxDuration = "240s")
    void getFolderContentProducesConsistentEntries(FuzzedDataProvider data) {
        MinioClient client = Mockito.mock(MinioClient.class);
        FileRepository repository = new FileRepository(client, "bucket");


        String folderPathValue = sanitizePathSegment(data.consumeString(40), true);
        FilePath folderPath = new FilePath(USERNAME, folderPathValue);
        boolean recursive = data.consumeBoolean();

        int itemCount = data.consumeInt(0, 8);
        List<Result<Item>> items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++) {
            if (data.consumeInt(0, 9) == 0) {
                items.add(defectiveResult());
                continue;
            }

            boolean isDir = data.consumeBoolean();
            String relative = sanitizePathSegment(data.consumeString(60), isDir);
            String objectName = folderPath.getPrefix() + relative;

            items.add(item(objectName, data.consumeLong(0, 1024)));
        }

        when(client.listObjects(any())).thenReturn(items);

        List<FileFullInfo> result = repository.getFolderContent(folderPath, recursive).toList();

        Mockito.verify(client).listObjects(any(ListObjectsArgs.class));

        for (FileFullInfo info : result) {
            assertThat(USERNAME).isEqualTo(info.user());
            assertThat(info.path().isEmpty() || info.path().endsWith("/")).isTrue();
            assertThat(info.name())
                    .isNotEmpty()
                    .doesNotContain("/");

            FilePath reconstructed = info.getFilePath();
            assertThat(reconstructed.getFullPath()).startsWith(folderPath.getPrefix());
            assertThat(reconstructed.path()).startsWith(info.path());
            assertThat(reconstructed.isDir()).isEqualTo(info.isDir());
        }
    }

    private static Result<Item> item(String objectName, long size) {
        Item item = Mockito.mock(Item.class);
        when(item.objectName()).thenReturn(objectName);
        when(item.size()).thenReturn(size);
        return new Result<>(item);
    }

    @SuppressWarnings("unchecked")
    private static Result<Item> defectiveResult() {
        Result<Item> result = Mockito.mock(Result.class);
        try {
            when(result.get()).thenThrow(new RuntimeException("broken item"));
        } catch (Exception ignored) {
            throw new IllegalStateException(ignored);
        }
        return result;
    }

    private static String sanitizePathSegment(String raw, boolean directory) {
        String normalized = raw
                .replace('\\', '/')
                .replace("\r", "")
                .replace("\n", "");

        if (normalized.isEmpty()) {
            return directory ? "dir/" : "file";
        }

        String sanitized = getSanitized(normalized);
        if (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }

        if (sanitized.isEmpty()) {
            sanitized = directory ? "dir/" : "file";
        }

        if (directory && !sanitized.endsWith("/")) {
            sanitized += "/";
        }
        if (!directory && sanitized.endsWith("/")) {
            sanitized += "file";
        }

        return sanitized;
    }

    private static @NotNull String getSanitized(String normalized) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasSlash = false;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (current == '/') {
                if (!previousWasSlash) {
                    builder.append(current);
                }
                previousWasSlash = true;
            } else {
                builder.append(current);
                previousWasSlash = false;
            }
        }

        return builder.toString();
    }
}
