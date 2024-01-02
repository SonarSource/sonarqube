/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;
import org.sonar.server.plugins.ServerPlugin;
import org.sonar.server.plugins.ServerPluginRepository;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.plugin.PluginType.BUNDLED;

public class GeneratePluginIndexTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem serverFileSystem = mock(ServerFileSystem.class);
  private ServerPluginRepository serverPluginRepository = mock(ServerPluginRepository.class);
  private File index;

  @Before
  public void createIndexFile() throws IOException {
    index = temp.newFile();
    when(serverFileSystem.getPluginIndex()).thenReturn(index);
  }

  @Test
  public void shouldWriteIndex() throws IOException {
    ServerPlugin javaPlugin = newInstalledPlugin("java", true);
    ServerPlugin gitPlugin = newInstalledPlugin("scmgit", false);
    when(serverPluginRepository.getPlugins()).thenReturn(asList(javaPlugin, gitPlugin));

    GeneratePluginIndex underTest = new GeneratePluginIndex(serverFileSystem, serverPluginRepository);
    underTest.start();

    List<String> lines = FileUtils.readLines(index);
    assertThat(lines).containsExactly(
      "java,true," + javaPlugin.getJar().getFile().getName() + "|" + javaPlugin.getJar().getMd5(),
      "scmgit,false," + gitPlugin.getJar().getFile().getName() + "|" + gitPlugin.getJar().getMd5());

    underTest.stop();
  }

  @Test
  public void shouldThrowWhenUnableToWrite() throws IOException {
    File wrongParent = temp.newFile();
    wrongParent.createNewFile();
    File wrongIndex = new File(wrongParent, "index.txt");
    when(serverFileSystem.getPluginIndex()).thenReturn(wrongIndex);

    assertThatThrownBy(() -> new GeneratePluginIndex(serverFileSystem, serverPluginRepository).start())
      .isInstanceOf(IllegalStateException.class);
  }

  private ServerPlugin newInstalledPlugin(String key, boolean supportSonarLint) throws IOException {
    FileAndMd5 jar = new FileAndMd5(temp.newFile());
    PluginInfo pluginInfo = new PluginInfo(key).setJarFile(jar.getFile()).setSonarLintSupported(supportSonarLint);
    return new ServerPlugin(pluginInfo, BUNDLED, null, jar, null);
  }
}
