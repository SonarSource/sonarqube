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
package org.sonar.server.startup;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeneratePluginIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultServerFileSystem fileSystem;
  private File index;

  @Before
  public void createIndexFile() {
    fileSystem = mock(DefaultServerFileSystem.class);
    index = new File("target/test-tmp/GeneratePluginIndexTest/plugins.txt");
    when(fileSystem.getPluginIndex()).thenReturn(index);
  }

  @Test
  public void shouldWriteIndex() throws IOException {
    PluginRepository repository = mock(PluginRepository.class);
    PluginMetadata sqale = newMetadata("sqale");
    PluginMetadata checkstyle = newMetadata("checkstyle");
    when(repository.getMetadata()).thenReturn(Arrays.asList(sqale, checkstyle));

    new GeneratePluginIndex(fileSystem, repository).start();

    List<String> lines = FileUtils.readLines(index);
    assertThat(lines.size(), Is.is(2));
    assertThat(lines.get(0), containsString("sqale"));
    assertThat(lines.get(1), containsString("checkstyle"));
  }

  private PluginMetadata newMetadata(String pluginKey) throws IOException {
    PluginMetadata plugin = mock(DefaultPluginMetadata.class);
    when(plugin.getKey()).thenReturn(pluginKey);
    File pluginFile = temp.newFile(pluginKey + ".jar");
    when(plugin.getFile()).thenReturn(pluginFile);
    return plugin;
  }
}
