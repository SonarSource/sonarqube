/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.TempFolder;
import com.google.common.collect.ImmutableMap;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TempFolderProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private TempFolderProvider tempFolderProvider = new TempFolderProvider();

  @Test
  public void createTempFolderProps() throws Exception {
    File workingDir = temp.getRoot();

    TempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(ImmutableMap.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(getCreatedTempDir(workingDir)).exists();
    assertThat(getCreatedTempDir(workingDir).list()).hasSize(2);
  }

  @Test
  public void cleanUpOld() throws IOException {
    long creationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100);
    File workingDir = temp.getRoot();

    for (int i = 0; i < 3; i++) {
      File tmp = new File(workingDir, ".sonartmp_" + i);
      tmp.mkdirs();
      setFileCreationDate(tmp, creationTime);
    }

    tempFolderProvider.provide(new BootstrapProperties(ImmutableMap.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.getAbsolutePath())));
    // this also checks that all other temps were deleted
    assertThat(getCreatedTempDir(workingDir)).exists();
  }

  @Test
  public void createTempFolderSonarHome() throws Exception {
    // with sonar home, it will be in {sonar.home}/.sonartmp
    File sonarHome = temp.getRoot();
    File workingDir = new File(sonarHome, CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE);

    TempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(ImmutableMap.of("sonar.userHome", sonarHome.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(getCreatedTempDir(workingDir)).exists();
    assertThat(getCreatedTempDir(workingDir).list()).hasSize(2);
  }

  @Test
  public void createTempFolderDefault() throws Exception {
    File userHome = temp.getRoot();
    System.setProperty("user.home", userHome.getAbsolutePath());

    // if nothing is defined, it will be in {user.home}/.sonar/.sonartmp
    File defaultSonarHome = new File(System.getProperty("user.home"), ".sonar");
    File workingDir = new File(defaultSonarHome, CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE).getAbsoluteFile();

    try {
      TempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(Collections.<String, String>emptyMap()));
      tempFolder.newDir();
      tempFolder.newFile();
      assertThat(getCreatedTempDir(workingDir)).exists();
      assertThat(getCreatedTempDir(workingDir).list()).hasSize(2);
    } finally {
      FileUtils.deleteDirectory(getCreatedTempDir(workingDir));
    }
  }

  private File getCreatedTempDir(File workingDir) {
    assertThat(workingDir.listFiles()).hasSize(1);
    return workingDir.listFiles()[0];
  }

  private void setFileCreationDate(File f, long time) throws IOException {
    BasicFileAttributeView attributes = Files.getFileAttributeView(f.toPath(), BasicFileAttributeView.class);
    FileTime creationTime = FileTime.fromMillis(time);
    attributes.setTimes(creationTime, creationTime, creationTime);
  }
}
