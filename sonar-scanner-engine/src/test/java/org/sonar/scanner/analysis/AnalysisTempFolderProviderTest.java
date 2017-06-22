/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.analysis;

import org.sonar.api.batch.bootstrap.ImmutableProjectDefinition;

import org.junit.Before;
import org.sonar.api.utils.TempFolder;
import org.sonar.scanner.analysis.AnalysisTempFolderProvider;
import org.sonar.scanner.scan.ImmutableProjectReactor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;

public class AnalysisTempFolderProviderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private AnalysisTempFolderProvider tempFolderProvider;
  private ImmutableProjectReactor projectReactor;

  @Before
  public void setUp() {
    tempFolderProvider = new AnalysisTempFolderProvider();
    projectReactor = mock(ImmutableProjectReactor.class);
    ImmutableProjectDefinition projectDefinition = mock(ImmutableProjectDefinition.class);
    when(projectReactor.getRoot()).thenReturn(projectDefinition);
    when(projectDefinition.getWorkDir()).thenReturn(temp.getRoot());
  }

  @Test
  public void createTempFolder() throws IOException {
    File defaultDir = new File(temp.getRoot(), AnalysisTempFolderProvider.TMP_NAME);

    TempFolder tempFolder = tempFolderProvider.provide(projectReactor);
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(defaultDir).exists();
    assertThat(defaultDir.list()).hasSize(2);
  }
}
