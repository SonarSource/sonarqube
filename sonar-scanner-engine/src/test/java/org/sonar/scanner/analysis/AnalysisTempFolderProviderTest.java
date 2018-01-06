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
package org.sonar.scanner.analysis;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.utils.TempFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisTempFolderProviderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private AnalysisTempFolderProvider tempFolderProvider;
  private InputModuleHierarchy moduleHierarchy;

  @Before
  public void setUp() {
    tempFolderProvider = new AnalysisTempFolderProvider();
    moduleHierarchy = mock(InputModuleHierarchy.class);
    DefaultInputModule module = mock(DefaultInputModule.class);
    when(moduleHierarchy.root()).thenReturn(module);
    when(module.getWorkDir()).thenReturn(temp.getRoot().toPath());
  }

  @Test
  public void createTempFolder() throws IOException {
    File defaultDir = new File(temp.getRoot(), AnalysisTempFolderProvider.TMP_NAME);

    TempFolder tempFolder = tempFolderProvider.provide(moduleHierarchy);
    tempFolder.newDir();
    tempFolder.newFile();
    assertThat(defaultDir).exists();
    assertThat(defaultDir.list()).hasSize(2);
  }
}
