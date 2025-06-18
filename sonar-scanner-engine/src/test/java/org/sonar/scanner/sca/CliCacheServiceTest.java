/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.sca;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.scanner.WsTestUtil;
import org.sonar.scanner.bootstrap.SonarUserHome;
import org.sonar.scanner.http.DefaultScannerWsClient;
import org.sonar.scanner.repository.TelemetryCache;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.sca.CliCacheService.CLI_WS_URL;

@ExtendWith(MockitoExtension.class)
class CliCacheServiceTest {
  @Mock
  private SonarUserHome sonarUserHome;
  @Mock
  private DefaultScannerWsClient scannerWsClient;
  @Mock
  private System2 system2;
  @Mock
  private TelemetryCache telemetryCache;
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  @TempDir
  public Path cacheDir;

  private CliCacheService underTest;

  @BeforeEach
  void setup() {
    lenient().when(sonarUserHome.getPath()).thenReturn(cacheDir);
    lenient().when(telemetryCache.put(any(), any())).thenReturn(telemetryCache);

    underTest = new CliCacheService(sonarUserHome, scannerWsClient, telemetryCache, system2);
  }

  @Test
  void cacheCli_shouldDownloadCli_whenCacheDoesNotExist() {
    String checksum = "checksum";
    String id = "tidelift";
    WsTestUtil.mockReader(scannerWsClient, CLI_WS_URL, new StringReader("""
      [
        {
          "id": "%s",
          "filename": "tidelift_darwin",
          "sha256": "%s",
          "os": "mac",
          "arch": "x64_86"
        }
      ]""".formatted(id, checksum)));

    WsTestUtil.mockStream(scannerWsClient, CLI_WS_URL + "/" + id, new ByteArrayInputStream("cli content".getBytes()));

    assertThat(cacheDir).isEmptyDirectory();

    File generatedFile = underTest.cacheCli();

    assertThat(generatedFile).exists().isExecutable();
    assertThat(cacheDir.resolve("cache").resolve(checksum)).exists().isNotEmptyDirectory();

    verify(telemetryCache).put(eq("scanner.sca.download.cli.duration"), any());
    verify(telemetryCache).put("scanner.sca.download.cli.success", "true");
    verify(telemetryCache).put("scanner.sca.get.cli.cache.hit", "false");
    verify(telemetryCache).put("scanner.sca.get.cli.success", "true");
  }

  @Test
  void cacheCli_shouldThrowException_whenMultipleMetadatas() {
    WsTestUtil.mockReader(scannerWsClient, CLI_WS_URL, new StringReader("""
      [
        {
          "id": "tidelift",
          "filename": "tidelift_darwin",
          "sha256": "1",
          "os": "mac",
          "arch": "x64_86"
        },
        {
          "id": "tidelift_other",
          "filename": "tidelift",
          "sha256": "2",
          "os": "mac",
          "arch": "x64_86"
        }
      ]"""));

    assertThatThrownBy(underTest::cacheCli).isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Multiple CLI matches found. Unable to correctly cache CLI.");

    verify(telemetryCache).put("scanner.sca.get.cli.success", "false");

  }

  @Test
  void cacheCli_shouldThrowException_whenNoMetadata() {
    WsTestUtil.mockReader(scannerWsClient, CLI_WS_URL, new StringReader("[]"));

    assertThatThrownBy(underTest::cacheCli).isInstanceOf(IllegalStateException.class)
      .hasMessageMatching("Could not find CLI for .+ .+");

    verify(telemetryCache).put("scanner.sca.get.cli.success", "false");

  }

  @Test
  void cacheCli_shouldThrowException_whenServerError() {
    HttpException http = new HttpException("url", 500, "some error message");
    IllegalStateException e = new IllegalStateException("http error", http);
    WsTestUtil.mockException(scannerWsClient, e);

    assertThatThrownBy(underTest::cacheCli).isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("http error");

    verify(telemetryCache).put("scanner.sca.get.cli.success", "false");
  }

  @Test
  void cacheCli_shouldNotOverwrite_whenCachedFileExists() throws IOException {
    String checksum = "checksum";
    WsTestUtil.mockReader(scannerWsClient, CLI_WS_URL, new StringReader("""
      [
        {
          "id": "tidelift",
          "filename": "tidelift_darwin",
          "sha256": "%s",
          "os": "mac",
          "arch": "x64_86"
        }
      ]""".formatted(checksum)));
    when(system2.isOsWindows()).thenReturn(false);

    String fileContent = "test content";
    File existingFile = underTest.cacheDir().resolve(checksum).resolve("tidelift").toFile();
    FileUtils.createParentDirectories(existingFile);
    FileUtils.writeStringToFile(existingFile, fileContent, Charset.defaultCharset());

    assertThat(existingFile).exists();
    if (!SystemUtils.IS_OS_WINDOWS) {
      assertThat(existingFile.canExecute()).isFalse();
    }
    assertThat(FileUtils.readFileToString(existingFile, Charset.defaultCharset())).isEqualTo(fileContent);

    underTest.cacheCli();

    WsTestUtil.verifyCall(scannerWsClient, CLI_WS_URL);
    assertThat(existingFile).exists();
    if (!SystemUtils.IS_OS_WINDOWS) {
      assertThat(existingFile.canExecute()).isFalse();
    }
    assertThat(FileUtils.readFileToString(existingFile, Charset.defaultCharset())).isEqualTo(fileContent);

    verify(telemetryCache).put("scanner.sca.get.cli.cache.hit", "true");
    verify(telemetryCache).put("scanner.sca.get.cli.success", "true");
  }

  @Test
  void cacheCli_shouldAllowLocationOverride(@TempDir Path tempDir) throws IOException {
    File alternateCliFile = tempDir.resolve("alternate_cli").toFile();
    FileUtils.writeStringToFile(alternateCliFile, "alternate cli content", Charset.defaultCharset());
    when(system2.envVariable("TIDELIFT_CLI_LOCATION")).thenReturn(alternateCliFile.getAbsolutePath());

    var returnedFile = underTest.cacheCli();

    assertThat(returnedFile.getAbsolutePath()).isEqualTo(alternateCliFile.getAbsolutePath());
    assertThat(logTester.logs(Level.INFO)).contains("Using alternate location for Tidelift CLI: " + alternateCliFile.getAbsolutePath());
    verify(scannerWsClient, never()).call(any());
  }

  @Test
  void cacheCli_whenOverrideDoesntExist_shouldRaiseError() {
    var location = "incorrect_location";
    when(system2.envVariable("TIDELIFT_CLI_LOCATION")).thenReturn(location);

    assertThatThrownBy(underTest::cacheCli).isInstanceOf(IllegalStateException.class)
      .hasMessageMatching("Alternate location for Tidelift CLI has been set but no file was found at " + location);

    assertThat(logTester.logs(Level.INFO)).contains("Using alternate location for Tidelift CLI: " + location);
    verify(scannerWsClient, never()).call(any());
  }

  @Test
  void apiOsName_shouldReturnApiCompatibleName() {
    when(system2.isOsWindows()).thenReturn(true);
    when(system2.isOsMac()).thenReturn(false);
    assertThat(underTest.apiOsName()).isEqualTo("windows");
    reset(system2);

    when(system2.isOsWindows()).thenReturn(false);
    when(system2.isOsMac()).thenReturn(true);
    assertThat(underTest.apiOsName()).isEqualTo("mac");

    reset(system2);
    when(system2.isOsWindows()).thenReturn(false);
    when(system2.isOsMac()).thenReturn(false);
    assertThat(underTest.apiOsName()).isEqualTo("linux");
  }

  @Test
  void createTempDir_shouldReturnExistingDir() throws IOException {
    Path dir = sonarUserHome.getPath().resolve("_tmp");
    Files.createDirectory(dir);

    assertThat(underTest.createTempDir()).isEqualTo(dir);
  }

  @Test
  void createTempDir_shouldHandleIOException() {
    try (MockedStatic<Files> mockFilesClass = mockStatic(Files.class)) {
      mockFilesClass.when(() -> Files.createDirectory(any(Path.class))).thenThrow(IOException.class);

      Path expectedDir = sonarUserHome.getPath().resolve("_tmp");
      assertThatThrownBy(underTest::createTempDir).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(format("Unable to create temp directory at %s", expectedDir));
    }
  }

  @Test
  void moveFile_shouldHandleIOException(@TempDir Path sourceFile, @TempDir Path targetFile) {
    try (MockedStatic<Files> mockFilesClass = mockStatic(Files.class)) {
      mockFilesClass.when(() -> Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE)).thenThrow(IOException.class);
      mockFilesClass.when(() -> Files.move(sourceFile, targetFile)).thenThrow(IOException.class);

      assertThatThrownBy(() -> CliCacheService.moveFile(sourceFile, targetFile)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(format("Fail to move %s to %s", sourceFile, targetFile));

      assertThat(logTester.logs(Level.WARN)).contains(format("Unable to rename %s to %s", sourceFile, targetFile));
      assertThat(logTester.logs(Level.WARN)).contains("A copy/delete will be tempted but with no guarantee of atomicity");
    }
  }

  @Test
  void mkdir_shouldHandleIOException(@TempDir Path dir) {
    try (MockedStatic<Files> mockFilesClass = mockStatic(Files.class)) {
      mockFilesClass.when(() -> Files.createDirectories(dir)).thenThrow(IOException.class);

      assertThatThrownBy(() -> CliCacheService.mkdir(dir)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(format("Fail to create cache directory: %s", dir));
    }
  }

  @Test
  void downloadBinaryTo_shouldHandleIOException(@TempDir Path downloadLocation) {
    WsResponse mockResponse = mock(WsResponse.class);
    InputStream mockStream = mock(InputStream.class);
    when(mockResponse.contentStream()).thenReturn(mockStream);

    try (MockedStatic<FileUtils> mockFileUtils = mockStatic(FileUtils.class)) {
      mockFileUtils.when(() -> FileUtils.copyInputStreamToFile(mockStream, downloadLocation.toFile())).thenThrow(IOException.class);

      assertThatThrownBy(() -> CliCacheService.downloadBinaryTo(downloadLocation, mockResponse)).isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(format("Fail to download SCA CLI into %s", downloadLocation));
    }
  }
}
