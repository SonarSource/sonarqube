/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.InputDir;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scan.filesystem.internal.DefaultInputDir;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_return_inputDir() throws Exception {
    FileIndex index = new FileIndex(null, null, null, null, null, new PathResolver(), new Project("myProject"));
    File baseDir = temp.newFolder();
    DefaultModuleFileSystem fileSystem = mock(DefaultModuleFileSystem.class);
    when(fileSystem.baseDir()).thenReturn(baseDir);
    File ioFile = new File(baseDir, "src/main/java/com/foo");
    InputDir inputDir = index.inputDir(fileSystem, ioFile);

    assertThat(inputDir.name()).isEqualTo("src/main/java/com/foo");
    assertThat(inputDir.file()).isEqualTo(ioFile);
    assertThat(inputDir.attribute(DefaultInputDir.ATTRIBUTE_COMPONENT_KEY)).isEqualTo("myProject:src/main/java/com/foo");
  }
}
