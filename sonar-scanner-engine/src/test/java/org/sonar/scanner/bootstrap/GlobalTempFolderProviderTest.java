/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalTempFolderProviderTest {

  private final SonarUserHome sonarUserHome = mock(SonarUserHome.class);
  private final GlobalTempFolderProvider underTest = new GlobalTempFolderProvider();

  @Test
  void createTempFolderProps(@TempDir Path workingDir) throws Exception {
    Files.delete(workingDir);

    var tempFolder = underTest.provide(
      new ScannerProperties(Map.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.toAbsolutePath().toString())), sonarUserHome);
    tempFolder.newDir();
    tempFolder.newFile();

    assertThat(workingDir).isDirectory();
    assertThat(workingDir.toFile().list()).hasSize(1);
    var rootTmpDir = workingDir.toFile().listFiles()[0];
    assertThat(rootTmpDir.list()).hasSize(2);
  }

  @Test
  void cleanUpOld(@TempDir Path workingDir) throws IOException {
    long creationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100);

    for (int i = 0; i < 3; i++) {
      Path tmp = workingDir.resolve(".sonartmp_" + i);
      Files.createDirectories(tmp);
      setFileCreationDate(tmp, creationTime);
      assumeCorrectFileCreationDate(tmp, creationTime);
    }

    underTest.provide(
      new ScannerProperties(Map.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.toAbsolutePath().toString())), sonarUserHome);

    // this also checks that all other temps were deleted
    assertThat(workingDir.toFile().list()).hasSize(1);
  }

  // See SONAR-22159, the test started failing due to issues in setting the correct creation time on Cirrus, skipping the test if the problem
  // happens
  private void assumeCorrectFileCreationDate(Path tmp, long creationTime) throws IOException {
    FileTime fileCreationTimeSet = Files.getFileAttributeView(tmp, BasicFileAttributeView.class).readAttributes().creationTime();
    FileTime expectedCreationTime = FileTime.fromMillis(creationTime);

    assumeTrue(expectedCreationTime.compareTo(fileCreationTimeSet) >= 0,
      String.format("Incorrect creation date set on temporary file %s: %s set instead of %s", tmp.toAbsolutePath(), fileCreationTimeSet, expectedCreationTime));
  }

  @Test
  void createTempFolderFromSonarHome(@TempDir Path sonarUserHomePath) {
    // with sonar home, it will be in {sonar.home}/.sonartmp
    when(sonarUserHome.getPath()).thenReturn(sonarUserHomePath);
    
    var expectedWorkingDir = sonarUserHomePath.resolve(CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE);

    TempFolder tempFolder = underTest.provide(new ScannerProperties(Map.of()), sonarUserHome);
    tempFolder.newDir();
    tempFolder.newFile();

    assertThat(expectedWorkingDir).isDirectory();
    assertThat(expectedWorkingDir.toFile().list()).hasSize(1);
    var rootTmpDir = expectedWorkingDir.toFile().listFiles()[0];
    assertThat(rootTmpDir.list()).hasSize(2);
  }

  @Test
  void dotWorkingDir(@TempDir Path sonarUserHomePath) {
    when(sonarUserHome.getPath()).thenReturn(sonarUserHomePath);
    String globalWorkDir = ".";
    ScannerProperties globalProperties = new ScannerProperties(
      Map.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, globalWorkDir));

    var tempFolder = underTest.provide(globalProperties, sonarUserHome);
    File newFile = tempFolder.newFile();
    
    assertThat(newFile.getParentFile().getParentFile().toPath()).isEqualTo(sonarUserHomePath);
    assertThat(newFile.getParentFile().getName()).startsWith(".sonartmp_");
  }

  @Test
  void homeIsSymbolicLink(@TempDir Path realSonarHome, @TempDir Path symlink) throws IOException {
    assumeTrue(!System2.INSTANCE.isOsWindows());
    symlink.toFile().delete();
    Files.createSymbolicLink(symlink, realSonarHome);
    when(sonarUserHome.getPath()).thenReturn(symlink);

    ScannerProperties globalProperties = new ScannerProperties(Map.of());

    TempFolder tempFolder = underTest.provide(globalProperties, sonarUserHome);
    File newFile = tempFolder.newFile();
    assertThat(newFile.getParentFile().getParentFile().toPath().toAbsolutePath()).isEqualTo(symlink);
    assertThat(newFile.getParentFile().getName()).startsWith(".sonartmp_");
  }

  private void setFileCreationDate(Path f, long time) throws IOException {
    BasicFileAttributeView attributes = Files.getFileAttributeView(f, BasicFileAttributeView.class);
    FileTime creationTime = FileTime.fromMillis(time);
    attributes.setTimes(creationTime, creationTime, creationTime);
  }
}
