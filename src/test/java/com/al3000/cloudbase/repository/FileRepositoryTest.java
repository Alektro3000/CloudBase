package com.al3000.cloudbase.repository;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FileFullInfoTest;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.InternalServerException;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileRepositoryTest {
    @Mock
    MinioClient client;

    FileRepository fileRepository;

    static String username = "alice";

    @BeforeEach
    void setUp()
    {
        fileRepository = new FileRepository(client, "bucket");
    }
    static Result<Item> makeItem(String name, long length) {
        var mockItem = mock(Item.class);
        when(mockItem.objectName()).thenReturn(new FilePath(username,name).getFullPath());
        when(mockItem.size()).thenReturn(length);
        return new Result<>(mockItem);
    }
    static Result<Item> makeDefectItem() throws Exception {
        Result<Item>  mockItem = mock(Result.class);
        when(mockItem.get()).thenThrow(new RuntimeException());
        return (mockItem);
    }

    static private FileFullInfo makeFile(String path, String name, long length) {
        return new FileFullInfo(username, path, name, length, false);
    }
    static private FileFullInfo makeDirectory(String path, String name) {
        return new FileFullInfo(username, path, name, 0L, true);
    }

    static Stream<Arguments> folderContentCases() throws Exception {
        return Stream.of(
                Arguments.of(
                        listOf(
                                makeItem("a/",0),
                                makeItem("a/b/",0),
                                makeItem("a.txt",1),
                                makeItem("a/b.txt",2),
                                makeDefectItem()
                                ),
                        new FilePath(username, ""),
                        true,
                        listOf(
                                makeDirectory("", "a"),
                                makeDirectory("a/", "b"),
                                makeFile("","a.txt",1),
                                makeFile("a/","b.txt",2)
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource("folderContentCases")
    void getFolderContent(List<Result<Item>> minioItems, FilePath folderPath, Boolean recursive, List<FileFullInfoTest> expected) {
        // Arrange
        when(client.listObjects(argThat( x-> Objects.equals(x.prefix(), "user-" + username + "/" + folderPath.path()))))
                .thenReturn(minioItems);

        // Act
        var result = fileRepository.getFolderContent(folderPath, recursive);

        // Assert
        assertThat(result.toList()).isEqualTo(expected);
    }

    @Test
    void uploadFile_throwsException_ExceptionRethrown() throws Exception {

        // Arrange
        byte[] payload = "hello from integration".getBytes(StandardCharsets.UTF_8);
        var multipart = new MockMultipartFile("file", "hello.txt", "text/plain", payload);

        when(client.putObject(ArgumentMatchers.any())).thenThrow(new RuntimeException("Minio error"));

        FilePath path = new FilePath(username, "a/");

        // Act & Assert
        assertThatThrownBy(() -> fileRepository.uploadFile(multipart, path)).isInstanceOf(InternalServerException.class);

    }

}
