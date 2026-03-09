package com.al3000.cloudbase.service;

import com.al3000.cloudbase.dto.FileFullInfo;
import com.al3000.cloudbase.dto.FileInfo;
import com.al3000.cloudbase.dto.FilePath;
import com.al3000.cloudbase.exception.DestinationAlreadyExist;
import com.al3000.cloudbase.exception.FileDoesNotExist;
import com.al3000.cloudbase.exception.InternalServerException;
import com.al3000.cloudbase.repository.FileRepository;
import com.al3000.cloudbase.service.search.LibrarySearch;
import com.al3000.cloudbase.service.search.StringSearchAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    FileRepository fileRepository;

    StringSearchAlgorithm stringSearchAlgorithm = new LibrarySearch();

    @InjectMocks
    FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(fileRepository, stringSearchAlgorithm);
    }

    // Helpers
    private static byte[] readAll(InputStream in) throws IOException {
        try (in) {
            return in.readAllBytes();
        }
    }
    private void MockCreateFolder() throws InternalServerException {
        when(fileRepository.createFolder(any())).thenAnswer(
                invocation -> {
                        var arg = (FilePath) invocation.getArguments()[0];
                        return new FileFullInfo(
                                username,
                                arg.getDirectoryPath(),
                                arg.getDirectoryName(),
                                0L,
                                true
                        );});
    }

    private final String username = "alice";

    @Captor
    private ArgumentCaptor<List<FilePath>> deleteCaptor;

    private FileFullInfo makeFile(String path, String name) {
        return new FileFullInfo(username, path, name, 12L, false);
    }
    private FileFullInfo makeDirectory(String path, String name) {
        return new FileFullInfo(username,  path, name, 0L, true);
    }


    // ---------------- addRecursivelyFolders ----------------

    @Test
    void addRecursivelyFolders_createsAllParents() throws Exception {
        // Arrange
        FilePath p = new FilePath(username, "a/b/c/");

        MockCreateFolder();

        // Act
        fileService.addRecursivelyFolders(p);

        // Assert
        ArgumentCaptor<FilePath> pathCaptor = ArgumentCaptor.forClass(FilePath.class);
        verify(fileRepository, times(3)).createFolder(pathCaptor.capture());

        List<FilePath> created = pathCaptor.getAllValues();
        assertThat(created).extracting(FilePath::path)
                .containsExactly("a/", "a/b/", "a/b/c/");
    }

    @Test
    void addRecursivelyFolders_createsAllParentsExceptFile() throws Exception {
        // Arrange
        FilePath p = new FilePath(username, "a/b/c/a");
        MockCreateFolder();

        // Act
        fileService.addRecursivelyFolders(p);

        // Assert
        ArgumentCaptor<FilePath> pathCaptor = ArgumentCaptor.forClass(FilePath.class);
        verify(fileRepository, times(3)).createFolder(pathCaptor.capture());

        List<FilePath> created = pathCaptor.getAllValues();
        assertThat(created).extracting(FilePath::path)
                .containsExactly("a/", "a/b/", "a/b/c/");
    }

    // ---------------- uploadFile ----------------

    @Test
    void uploadFile_callsAddRecursivelyFolders_thenUploads() throws Exception {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hi".getBytes(StandardCharsets.UTF_8)
        );
        FilePath path = new FilePath(username, "a/");

        MockCreateFolder();

        // Act
        fileService.uploadFile(file, path);

        // Assert
        verify(fileRepository).createFolder(path);
        verify(fileRepository).uploadFile(file, path);
        verifyNoMoreInteractions(fileRepository);
    }

    // ---------------- getFolderFiles ----------------


    @Test
    void getFolderFiles_filtersDirectories_andMapsToFileInfo() {
        // Arrange
        FilePath folder = new FilePath(username, "a/");
        FileFullInfo directory = makeDirectory("","a");
        FileFullInfo realFile = makeFile("a/", "x.txt");

        when(fileRepository.getFolderContent(folder, false))
                .thenReturn(Stream.of(directory, realFile));

        // Act
        List<FileInfo> result = fileService.getFolderFiles(folder).toList();

        // Assert
        assertThat(result).containsExactly(realFile.getFileInfo());
    }

    @Test
    void getFolderFilesInRoot_filtersDirectories_andMapsToFileInfo() {
        // Arrange
        FilePath folder = new FilePath(username, "");
        FileFullInfo rootDirectory = makeDirectory("","");
        FileFullInfo directory = makeDirectory("","a");
        FileFullInfo realFile = makeFile("a/", "x.txt");

        when(fileRepository.getFolderContent(folder, false))
                .thenReturn(Stream.of(rootDirectory,  directory, realFile));

        // Act
        List<FileInfo> result = fileService.getFolderFiles(folder).toList();

        // Assert
        assertThat(result).contains(realFile.getFileInfo(), directory.getFileInfo());
    }

    // ---------------- removeFile ----------------

    @Test
    void remove_removeDir_buildsRemoveList() {
        // Arrange
        FilePath folder = new FilePath(username, "a/");

        FileFullInfo directory = makeDirectory("","a");
        FileFullInfo realFile = makeFile("a/", "x.txt");
        FileFullInfo directory1 = makeDirectory("a/","b");
        FileFullInfo realFile1 = makeFile("a/b/", "x.txt");

        when(fileRepository.getFolderContent(folder, true))
                .thenReturn(Stream.of(directory, realFile, directory1, realFile1));

        // Act
        fileService.removeFile(folder);

        // Assert
        verify(fileRepository).removeFiles(deleteCaptor.capture());
        List<FilePath> list = deleteCaptor.getValue();

        assertThat(list).hasSize(4);

        assertThat(list).containsExactlyElementsOf(
                Stream.of(directory, realFile, directory1, realFile1)
                        .map(FileFullInfo::getFilePath)
                        .toList());

    }

    @Test
    void remove_removeFile_buildsRemoveItem() {
        // Arrange
        FilePath file = new FilePath(username, "a/x.txt");

        // Act
        fileService.removeFile(file);

        // Assert
        verify(fileRepository).removeFiles(deleteCaptor.capture());
        List<FilePath> list = deleteCaptor.getValue();

        assertThat(list).hasSize(1);

        assertThat(list).containsExactlyElementsOf(listOf(file));

    }

    // ---------------- moveFile ----------------

    @Test
    void move_whenMoveFile_copiesAll_removesSources_returnsTargetInfo() throws InternalServerException, DestinationAlreadyExist, FileDoesNotExist {
        // Arrange
        FilePath source = new FilePath(username, "a/x.txt");
        FilePath target = new FilePath(username, "b/x.txt");

        FileFullInfo targetFullInfo = makeFile("a/", "x.txt");

        when(fileRepository.fileExist(argThat(fp -> fp.path().equals("b/x.txt"))))
                .thenReturn(false);
        when(fileRepository.getFileInformation(target)).thenReturn(targetFullInfo);

        // Act
        fileService.move(source, target);

        // Assert
        verify(fileRepository).removeFiles(deleteCaptor.capture());
        List<FilePath> del = deleteCaptor.getValue();

        assertThat(del).hasSize(1);
    }
    @Test
    void move_whenAnyTargetExists_throwsDestinationAlreadyExist_andDoesNotCopyOrRemove() {
        // Arrange
        FilePath source = new FilePath(username, "a/");
        FilePath target = new FilePath(username, "b/");

        FileFullInfo obj = makeFile("a/", "x.txt");

        when(fileRepository.getFolderContent(source, true))
                .thenReturn(Stream.of(obj));

        when(fileRepository.fileExist(argThat(fp -> fp.path().equals("b/x.txt"))))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> fileService.move(source, target))
                .isInstanceOf(DestinationAlreadyExist.class);

        verify(fileRepository).getFolderContent(any(), any());
        verifyNoMoreInteractions(fileRepository);
    }

    @Test
    void move_whenSuccess_copiesAll_removesSources_returnsTargetInfo() throws Exception {
        // Arrange
        FilePath source = new FilePath(username, "a/");
        FilePath target = new FilePath(username, "b/");

        FileFullInfo obj1 = makeFile("a/", "x.txt");
        FileFullInfo obj2 = makeFile("a/c/", "y.txt");

        when(fileRepository.getFolderContent(source, true)).thenReturn(Stream.of(obj1, obj2));

        when(fileRepository.fileExist(any())).thenReturn(false);

        MockCreateFolder();

        FileFullInfo targetFullInfo = makeFile("b/c/", "y.txt");
        when(fileRepository.getFileInformation(target)).thenReturn(targetFullInfo);

        // Act
        FileInfo result = fileService.move(source, target);

        // Assert
        assertThat(result).isEqualTo(targetFullInfo.getFileInfo());

        verify(fileRepository).copyObject(eq(obj1.getFilePath()), argThat(fp -> fp.path().equals("b/x.txt")));
        verify(fileRepository).copyObject(eq(obj2.getFilePath()), argThat(fp -> fp.path().equals("b/c/y.txt")));

        verify(fileRepository).removeFiles(deleteCaptor.capture());
        List<FilePath> del = deleteCaptor.getValue();

        assertThat(del).hasSize(2);
    }

    @Test
    void move_whenRepositoryThrows_wrapsAsInternalServerException() throws Exception {
        // Arrange
        FilePath source = new FilePath(username, "a/");
        FilePath target = new FilePath(username, "b/");

        FileFullInfo obj = makeFile("a/", "x.txt");

        when(fileRepository.getFolderContent(source, true)).thenReturn(Stream.of(obj));
        when(fileRepository.fileExist(any())).thenReturn(false);

        doThrow(new RuntimeException("boom")).when(fileRepository)
                .copyObject(any(), any());

        // Act & Assert
        assertThatThrownBy(() -> fileService.move(source, target))
                .isInstanceOf(InternalServerException.class);
    }

    // ---------------- findFiles ----------------

    @Test
    void findFiles_filtersBySubstring() {
        // Arrange
        FilePath root = new FilePath(username, "a");
        String query = "cat";

        FileFullInfo directory = makeDirectory("a/", "cat");
        FileFullInfo cat = makeFile("a/", "cat.png");
        FileFullInfo noMatch = makeFile("a/", "dog.png");

        when(fileRepository.getFolderContent(root, true))
                .thenReturn(Stream.of(directory, cat, noMatch));

        // Act
        List<FileInfo> result = fileService.findFiles(root, query).toList();

        // Assert
        assertThat(result).containsOnly(cat.getFileInfo());
    }

    // ---------------- downloadFile in downloadObject ----------------

    @Test
    void downloadObject_whenFile_returnsInputStreamResourceFromRepository() throws Exception {
        // Arrange
        FilePath file = new FilePath(username, "a/x.txt");

        FileFullInfo info = makeFile("a/", "x.txt");
        when(fileRepository.getFileInformation(file)).thenReturn(info);

        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        when(fileRepository.downloadFile(file)).thenReturn(new ByteArrayInputStream(payload));

        // Act
        InputStreamResource res = fileService.downloadObject(file);

        // Assert
        assertThat(res).isNotNull();
        assertThat(readAll(res.getInputStream())).isEqualTo(payload);
    }
    // ---------------- downloadFolder in downloadObject  ----------------

    @Test
    void downloadFolder_skipsDirectories_andAddsRelativeEntryNames() throws Exception {
        // Arrange
        FilePath folder = new FilePath(username, "a");

        FileFullInfo dir = makeDirectory("a/", "sub");
        FileFullInfo file = makeFile("a/sub/", "x.txt");

        when(fileRepository.getFolderContent(folder, true)).thenReturn(Stream.of(dir, file));

        byte[] payload = "content".getBytes(StandardCharsets.UTF_8);
        when(fileRepository.downloadFile(file.getFilePath())).thenReturn(new ByteArrayInputStream(payload));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(baos)) {
            // Act
            fileService.downloadFolder(folder, zout);
        }

        // Assert
        byte[] zipBytes = baos.toByteArray();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zin.getNextEntry();
            assertThat(entry).isNotNull();

            // relative to "a"
            assertThat(entry.getName()).isEqualTo("sub/x.txt");
            assertThat(zin.readAllBytes()).isEqualTo(payload);

            assertThat(zin.getNextEntry()).isNull();
        }
    }

    @Test
    void downloadFolder_catchError_Rethrows() throws Exception {
        // Arrange
        FilePath folder = new FilePath(username, "a");

        FileFullInfo dir = makeDirectory("a/", "sub");
        FileFullInfo file = makeFile("a/sub/", "x.txt");

        when(fileRepository.getFolderContent(folder, true)).thenReturn(Stream.of(dir, file));

        when(fileRepository.downloadFile(file.getFilePath())).thenThrow(new RuntimeException());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zout = new ZipOutputStream(baos)) {
            // Act & Assert
            assertThatThrownBy(() -> fileService.downloadFolder(folder, zout)).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void downloadObject_whenDir_returnsZipStreamContainingFiles() throws Exception {
        // Arrange
        FilePath folder = new FilePath(username, "a");

        FileFullInfo info = makeDirectory("", "a");

        // One folder marker + one file
        FileFullInfo directory = makeDirectory("a/", "b");
        FileFullInfo file = makeFile("a/", "x.txt");


        when(fileRepository.getFolderContent(folder, true)).thenReturn(Stream.of(directory, file));
        when(fileRepository.getFileInformation(folder)).thenReturn(info);

        byte[] payload = "HELLO".getBytes(StandardCharsets.UTF_8);
        when(fileRepository.downloadFile(file.getFilePath())).thenReturn(new ByteArrayInputStream(payload));

        // Act
        InputStreamResource res = fileService.downloadObject(folder);
        byte[] zipBytes = readAll(res.getInputStream());

        // Assert
        // Verify zip contains entry "x.txt" with correct content
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zin.getNextEntry();

            //Already tested
            assertThat(entry).isNotNull();
            assertThat("x.txt").isEqualTo(entry.getName());

            byte[] extracted = zin.readAllBytes();
            assertThat(extracted).isEqualTo(payload);

            assertThat(zin.getNextEntry()).isNull();
        }
    }


    // ---------------- createFolder ----------------

    @Test
    void createFolder_callsAddRecursivelyFolders_thenRepositoryCreateFolder_andReturnsFileInfo() throws Exception {
        // Arrange
        FilePath folder = new FilePath(username, "a/b");

        FileFullInfo created = makeDirectory("a/" , "b");
        when(fileRepository.createFolder(folder)).thenReturn(created);

        // Act
        FileInfo result = fileService.createFolder(folder);

        // Assert
        assertThat(result).isEqualTo(created.getFileInfo());

        verify(fileRepository, only()).createFolder(folder);
    }

}