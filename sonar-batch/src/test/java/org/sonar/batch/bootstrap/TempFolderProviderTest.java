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
import java.util.Collections;

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
    File workingDir = temp.newFolder();

    TempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(ImmutableMap.of(CoreProperties.GLOBAL_WORKING_DIRECTORY, workingDir.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(new File(workingDir, TempFolderProvider.TMP_NAME)).exists();
    assertThat(new File(workingDir, ".sonartmp").list()).hasSize(2);
  }

  @Test
  public void createTempFolderSonarHome() throws Exception {
    // with sonar home, it will be in {sonar.home}/.sonartmp
    File sonarHome = temp.newFolder();
    File tmpDir = new File(new File(sonarHome, CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE), TempFolderProvider.TMP_NAME);

    TempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(ImmutableMap.of("sonar.userHome", sonarHome.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(tmpDir).exists();
    assertThat(tmpDir.list()).hasSize(2);
  }

  @Test
  public void createTempFolderDefault() throws Exception {
    // if nothing is defined, it will be in {user.home}/.sonar/.sonartmp
    File defaultSonarHome = new File(System.getProperty("user.home"), ".sonar");
    File tmpDir = new File(new File(defaultSonarHome, CoreProperties.GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE), TempFolderProvider.TMP_NAME);

    try {
      TempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(Collections.<String, String>emptyMap()));
      tempFolder.newDir();
      tempFolder.newFile();
      assertThat(tmpDir).exists();
      assertThat(tmpDir.list()).hasSize(2);
    } finally {
      FileUtils.deleteDirectory(tmpDir);
    }
  }
}
