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

import org.sonar.api.utils.ProjectTempFolder;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectTempFolderProviderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ProjectTempFolderProvider tempFolderProvider = new ProjectTempFolderProvider();

  @Test
  public void createTempFolderWithProps() throws Exception {
    File workingDir = temp.newFolder();
    File tmpDir = new File(workingDir, ProjectTempFolderProvider.TMP_NAME);

    ProjectTempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(ImmutableMap.of(CoreProperties.WORKING_DIRECTORY, workingDir.getAbsolutePath())));
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(tmpDir).exists();
    assertThat(tmpDir.list()).hasSize(2);
  }

  @Test
  public void createTempFolder() throws IOException {
    File defaultDir = new File(CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE, ProjectTempFolderProvider.TMP_NAME);

    try {
      ProjectTempFolder tempFolder = tempFolderProvider.provide(new BootstrapProperties(Collections.<String, String>emptyMap()));
      tempFolder.newDir();
      tempFolder.newFile();
      assertThat(defaultDir).exists();
      assertThat(defaultDir.list()).hasSize(2);
    } finally {
      FileUtils.deleteDirectory(defaultDir);
    }
  }
}
