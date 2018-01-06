/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.TempFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GlobalTempFolderProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private GlobalTempFolderProvider tempFolderProvider = new GlobalTempFolderProvider();

  @Test
  public void createTempFolderProps() throws Exception {
    File workingDir = temp.newFolder();
    workingDir.delete();

    TempFolder tempFolder = tempFolderProvider.provide(new GlobalProperties(ImmutableMap.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(getCreatedTempDir(workingDir)).exists();
    assertThat(getCreatedTempDir(workingDir).list()).hasSize(2);

    FileUtils.deleteQuietly(workingDir);
  }

  @Test
  public void cleanUpOld() throws IOException {
    long creationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100);
    File workingDir = temp.newFolder();

    for (int i = 0; i < 3; i++) {
      File tmp = new File(workingDir, ".sonartmp_" + i);
      tmp.mkdirs();
      setFileCreationDate(tmp, creationTime);
    }

    tempFolderProvider.provide(new GlobalProperties(ImmutableMap.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.getAbsolutePath())));
    // this also checks that all other temps were deleted
    assertThat(getCreatedTempDir(workingDir)).exists();

    FileUtils.deleteQuietly(workingDir);
  }

  @Test
  public void createTempFolderSonarHome() throws Exception {
    // with sonar home, it will be in {sonar.home}/.sonartmp
    File sonarHome = temp.newFolder();
    File workingDir = new File(sonarHome, CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE).getAbsoluteFile();

    TempFolder tempFolder = tempFolderProvider.provide(new GlobalProperties(ImmutableMap.of("sonar.userHome", sonarHome.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(getCreatedTempDir(workingDir)).exists();
    assertThat(getCreatedTempDir(workingDir).list()).hasSize(2);

    FileUtils.deleteQuietly(sonarHome);
  }

  @Test
  public void createTempFolderDefault() throws Exception {
    System2 system = mock(System2.class);
    tempFolderProvider = new GlobalTempFolderProvider(system);
    File userHome = temp.newFolder();

    when(system.envVariable("SONAR_USER_HOME")).thenReturn(null);
    when(system.property("user.home")).thenReturn(userHome.getAbsolutePath().toString());

    // if nothing is defined, it will be in {user.home}/.sonar/.sonartmp
    File defaultSonarHome = new File(userHome.getAbsolutePath(), ".sonar");
    File workingDir = new File(defaultSonarHome, CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE).getAbsoluteFile();
    try {
      TempFolder tempFolder = tempFolderProvider.provide(new GlobalProperties(Collections.<String, String>emptyMap()));
      tempFolder.newDir();
      tempFolder.newFile();
      assertThat(getCreatedTempDir(workingDir)).exists();
      assertThat(getCreatedTempDir(workingDir).list()).hasSize(2);
    } finally {
      FileUtils.deleteQuietly(workingDir);
    }
  }

  @Test
  public void dotWorkingDir() throws IOException {
    File sonarHome = temp.getRoot();
    String globalWorkDir = ".";
    GlobalProperties globalProperties = new GlobalProperties(ImmutableMap.of("sonar.userHome", sonarHome.getAbsolutePath(),
      CoreProperties.GLOBAL_WORKING_DIRECTORY, globalWorkDir));

    TempFolder tempFolder = tempFolderProvider.provide(globalProperties);
    File newFile = tempFolder.newFile();
    assertThat(newFile.getParentFile().getParentFile().getAbsolutePath()).isEqualTo(sonarHome.getAbsolutePath());
    assertThat(newFile.getParentFile().getName()).startsWith(".sonartmp_");
  }

  @Test
  public void homeIsSymbolicLink() throws IOException {
    File realSonarHome = temp.newFolder();
    File symlink = temp.newFolder();
    symlink.delete();
    Files.createSymbolicLink(symlink.toPath(), realSonarHome.toPath());
    GlobalProperties globalProperties = new GlobalProperties(ImmutableMap.of("sonar.userHome", symlink.getAbsolutePath()));

    TempFolder tempFolder = tempFolderProvider.provide(globalProperties);
    File newFile = tempFolder.newFile();
    assertThat(newFile.getParentFile().getParentFile().getAbsolutePath()).isEqualTo(symlink.getAbsolutePath());
    assertThat(newFile.getParentFile().getName()).startsWith(".sonartmp_");
  }

  private File getCreatedTempDir(File workingDir) {
    assertThat(workingDir).isDirectory();
    assertThat(workingDir.listFiles()).hasSize(1);
    return workingDir.listFiles()[0];
  }

  private void setFileCreationDate(File f, long time) throws IOException {
    BasicFileAttributeView attributes = Files.getFileAttributeView(f.toPath(), BasicFileAttributeView.class);
    FileTime creationTime = FileTime.fromMillis(time);
    attributes.setTimes(creationTime, creationTime, creationTime);
  }
}
