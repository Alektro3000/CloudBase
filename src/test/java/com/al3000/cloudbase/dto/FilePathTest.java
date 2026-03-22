package com.al3000.cloudbase.dto;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FilePathTest {

    String username;

    Stream<Arguments> getFolderPathName_sources() {
        return Stream.of(
                Arguments.of("abc/bc/", "abc/", "bc"),
                Arguments.of("a/b/", "a/", "b"),
                Arguments.of("a/", "", "a")
        );
    }

    @ParameterizedTest
    @MethodSource("getFolderPathName_sources")
    void getFolderPathName(String folderPath, String expectedFolderPath, String folderName)   {
        var path = new FilePath(username, folderPath);

        assertThat(path.getDirectoryPath()).isEqualTo(expectedFolderPath);
        assertThat(path.getDirectoryName()).isEqualTo(folderName);

    }
}
