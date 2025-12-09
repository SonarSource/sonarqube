/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.analysis.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.ServerFileSystem;

import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.deleteIfExists;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ScannerEngineHandlerImplTest {

  ServerFileSystem serverFileSystem = mock(ServerFileSystem.class);

  ScannerEngineHandlerImpl scannerEngineHandler = new ScannerEngineHandlerImpl(serverFileSystem);

  @TempDir
  private File tempDir;

  private Path scannerDir;

  @BeforeEach
  public void setup() throws IOException {
    when(serverFileSystem.getHomeDir()).thenReturn(tempDir);
    scannerDir = createDirectories(Path.of(tempDir.getAbsolutePath(), "lib/scanner"));
  }

  @Test
  void getScannerEngineMetadata() throws IOException {
    createFile(scannerDir.resolve("scanner.jar"));
    ScannerEngineMetadata expected = new ScannerEngineMetadata("scanner.jar", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

    ScannerEngineMetadata result = scannerEngineHandler.getScannerEngineMetadata();

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getScannerEngineMetadata_shouldFail_whenHashComputingFailed() {
    ScannerEngineHandlerImpl spy = spy(new ScannerEngineHandlerImpl(serverFileSystem));
    doReturn(new File("no-file")).when(spy).getScannerEngine();
    assertThatThrownBy(spy::getScannerEngineMetadata)
      .isInstanceOf(UncheckedIOException.class)
      .hasMessageContaining("Unable to compute SHA-256 checksum of the Scanner Engine");
  }

  @Test
  void getScannerEngine_shouldFail_whenScannerDirNotFound() throws IOException {
    deleteIfExists(scannerDir);
    assertThatThrownBy(() -> scannerEngineHandler.getScannerEngine())
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Scanner directory not found: %s", scannerDir.toAbsolutePath()));
  }

  @Test
  void getScannerEngine_shouldReturnScannerJar() throws IOException {
    File scanner = createTempFile(scannerDir, "scanner", ".jar").toFile();

    File result = scannerEngineHandler.getScannerEngine();

    assertThat(result).isEqualTo(scanner);
  }

  @Test
  void getScannerEngine_shouldFail_whenScannerNotFound() throws IOException {
    Path tempDirectory = createDirectories(scannerDir);

    assertThatThrownBy(() -> scannerEngineHandler.getScannerEngine())
      .isInstanceOf(NotFoundException.class)
      .hasMessage(format("Scanner JAR not found in directory: %s", tempDirectory.toAbsolutePath()));
  }


}
