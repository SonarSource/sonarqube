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
package org.sonar.batch.repository;

import com.google.common.collect.ImmutableMap;

import org.sonar.batch.protocol.input.FileData;
import org.junit.Before;
import org.junit.Test;
import org.sonar.batch.protocol.input.ProjectRepositories;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultProjectSettingsLoaderTest {
  private DefaultProjectSettingsLoader loader;
  private DefaultProjectRepositoriesFactory factory;
  private ProjectRepositories projectRepositories;

  private FileData f1;
  private FileData f2;

  @Before
  public void setUp() {
    createProjectRepo();
    factory = mock(DefaultProjectRepositoriesFactory.class);
    when(factory.create()).thenReturn(projectRepositories);
    loader = new DefaultProjectSettingsLoader(factory);
  }

  private void createProjectRepo() {
    projectRepositories = new ProjectRepositories();
    projectRepositories.setLastAnalysisDate(new Date(1000));

    f1 = new FileData("hash1", "123456789");
    f2 = new FileData("hash2", "123456789");
    projectRepositories.addFileData("module1", "file1", f1);
    projectRepositories.addFileData("module1", "file2", f2);

    projectRepositories.addSettings("module1", ImmutableMap.of("key", "value"));
  }

  @Test
  public void test() {
    ProjectSettingsRepo loaded = loader.load("project", null);

    assertThat(loaded.fileData("module1", "file1")).isEqualTo(f1);
    assertThat(loaded.fileData("module1", "file2")).isEqualTo(f2);
    assertThat(loaded.settings("module1")).isEqualTo(ImmutableMap.of("key", "value"));
    assertThat(loaded.lastAnalysisDate()).isEqualTo(new Date(1000));

    verify(factory).create();
    verifyNoMoreInteractions(factory);
  }
}
