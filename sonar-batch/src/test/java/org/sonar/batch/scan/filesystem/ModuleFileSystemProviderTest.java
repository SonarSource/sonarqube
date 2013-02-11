/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.scan.filesystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.FileFilter;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.batch.bootstrap.TempDirectories;

import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ModuleFileSystemProviderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_provide() throws IOException {
    ModuleFileSystemProvider provider = new ModuleFileSystemProvider();
    ProjectDefinition module = ProjectDefinition.create()
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder());
    ModuleFileSystem fs = provider.provide(module, new PathResolver(), new TempDirectories(), mock(LanguageFileFilters.class),
      new Settings(), new FileFilter[0]);

    assertThat(fs).isNotNull();
  }
}
