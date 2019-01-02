/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.plugins;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.server.platform.ServerFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PluginUninstallerTest {
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private File uninstallDir;
  private PluginUninstaller underTest;
  private ServerPluginRepository serverPluginRepository;
  private ServerFileSystem fs;

  @Before
  public void setUp() throws IOException {
    serverPluginRepository = mock(ServerPluginRepository.class);
    uninstallDir = testFolder.newFolder("uninstall");
    fs = mock(ServerFileSystem.class);
    when(fs.getUninstalledPluginsDir()).thenReturn(uninstallDir);
    underTest = new PluginUninstaller(serverPluginRepository, fs);
  }

  @Test
  public void uninstall() {
    when(serverPluginRepository.hasPlugin("plugin")).thenReturn(true);
    underTest.uninstall("plugin");
    verify(serverPluginRepository).uninstall("plugin", uninstallDir);
  }

  @Test
  public void fail_uninstall_if_plugin_not_installed() {
    when(serverPluginRepository.hasPlugin("plugin")).thenReturn(false);
    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Plugin [plugin] is not installed");
    underTest.uninstall("plugin");
    verifyZeroInteractions(serverPluginRepository);
  }

  @Test
  public void create_uninstall_dir() {
    File dir = new File(testFolder.getRoot(), "dir");
    when(fs.getUninstalledPluginsDir()).thenReturn(dir);
    underTest = new PluginUninstaller(serverPluginRepository, fs);
    underTest.start();
    assertThat(dir).isDirectory();
  }

  @Test
  public void cancel() {
    underTest.cancelUninstalls();
    verify(serverPluginRepository).cancelUninstalls(uninstallDir);
    verifyNoMoreInteractions(serverPluginRepository);
  }

  @Test
  public void list_uninstalled_plugins() throws IOException {
    new File(uninstallDir, "file1").createNewFile();
    copyTestPluginTo("test-base-plugin", uninstallDir);
    assertThat(underTest.getUninstalledPlugins()).extracting("key").containsOnly("testbase");
  }

  private File copyTestPluginTo(String testPluginName, File toDir) throws IOException {
    File jar = TestProjectUtils.jarOf(testPluginName);
    // file is copied because it's supposed to be moved by the test
    FileUtils.copyFileToDirectory(jar, toDir);
    return new File(toDir, jar.getName());
  }
}
