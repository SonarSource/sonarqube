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
package org.sonar.server.startup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.server.platform.ServerFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeneratePluginIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem fileSystem = mock(ServerFileSystem.class);
  private File index;

  @Before
  public void createIndexFile() throws IOException {
    index = temp.newFile();
    when(fileSystem.getPluginIndex()).thenReturn(index);
  }

  @Test
  public void shouldWriteIndex() throws IOException {
    PluginRepository repository = mock(PluginRepository.class);
    PluginInfo sqale = newInfo("sqale");
    PluginInfo checkstyle = newInfo("checkstyle");
    when(repository.getPluginInfos()).thenReturn(Arrays.asList(sqale, checkstyle));

    GeneratePluginIndex underTest = new GeneratePluginIndex(fileSystem, repository);
    underTest.start();
    underTest.stop(); // For coverage

    List<String> lines = FileUtils.readLines(index);
    assertThat(lines).hasSize(2);
    assertThat(lines.get(0)).contains("sqale");
    assertThat(lines.get(1)).contains("checkstyle");
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowWhenUnableToWrite() throws IOException {
    File wrongParent = temp.newFile();
    wrongParent.createNewFile();
    File wrongIndex = new File(wrongParent, "index.txt");
    when(fileSystem.getPluginIndex()).thenReturn(wrongIndex);

    PluginRepository repository = mock(PluginRepository.class);

    new GeneratePluginIndex(fileSystem, repository).start();
  }

  private PluginInfo newInfo(String pluginKey) throws IOException {
    return new PluginInfo(pluginKey).setJarFile(temp.newFile(pluginKey + ".jar"));
  }
}
