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
package org.sonar.server.platform;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.PluginRepository;
import org.sonar.api.platform.ServerFileSystem;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RailsAppsDeployerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void hasRubyRailsApp() throws Exception {
    ClassLoader classLoader = new URLClassLoader(new URL[]{
      getClass().getResource("/org/sonar/server/platform/RailsAppsDeployerTest/FakeRubyRailsApp.jar").toURI().toURL()}, null);

    assertTrue(RailsAppsDeployer.hasRailsApp("fake", classLoader));
    assertFalse(RailsAppsDeployer.hasRailsApp("other", classLoader));
  }

  @Test
  public void deployRubyRailsApp() throws Exception {
    File tempDir = this.temp.getRoot();
    ClassLoader classLoader = new URLClassLoader(new URL[]{
      getClass().getResource("/org/sonar/server/platform/RailsAppsDeployerTest/FakeRubyRailsApp.jar").toURI().toURL()}, null);

    RailsAppsDeployer.deployRailsApp(tempDir, "fake", classLoader);

    File appDir = new File(tempDir, "fake");
    assertThat(appDir.isDirectory(), is(true));
    assertThat(appDir.exists(), is(true));
    assertThat(FileUtils.listFiles(appDir, null, true).size(), is(3));
    assertThat(new File(appDir, "init.rb").exists(), is(true));
    assertThat(new File(appDir, "app/controllers/fake_controller.rb").exists(), is(true));
    assertThat(new File(appDir, "app/views/fake/index.html.erb").exists(), is(true));
  }

  @Test
  public void deployRubyRailsApps_no_apps() throws Exception {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    File tempDir = this.temp.getRoot();
    when(fileSystem.getTempDir()).thenReturn(tempDir);

    PluginRepository pluginRepository = mock(PluginRepository.class);
    when(pluginRepository.getMetadata()).thenReturn(Collections.<PluginMetadata>emptyList());
    new RailsAppsDeployer(fileSystem, pluginRepository).start();

    File appDir = new File(tempDir, "ror");
    assertThat(appDir.isDirectory(), is(true));
    assertThat(appDir.exists(), is(true));
    assertThat(FileUtils.listFiles(appDir, null, true).size(), is(0));
  }

  @Test
  public void prepareRubyRailsRootDirectory() throws Exception {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    File tempDir = this.temp.getRoot();
    when(fileSystem.getTempDir()).thenReturn(tempDir);

    File dir = new RailsAppsDeployer(fileSystem, mock(PluginRepository.class)).prepareRailsDirectory();

    assertThat(dir.isDirectory(), is(true));
    assertThat(dir.exists(), is(true));
    assertThat(dir.getCanonicalPath(), is(new File(tempDir, "ror").getCanonicalPath()));
  }

  @Test
  public void prepareRubyRailsRootDirectory_delete_existing_dir() throws Exception {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    File tempDir = this.temp.getRoot();
    when(fileSystem.getTempDir()).thenReturn(tempDir);

    File file = new File(tempDir, "ror/foo/bar.txt");
    FileUtils.writeStringToFile(file, "foooo");

    File dir = new RailsAppsDeployer(fileSystem, mock(PluginRepository.class)).prepareRailsDirectory();

    assertThat(dir.isDirectory(), is(true));
    assertThat(dir.exists(), is(true));
    assertThat(dir.getCanonicalPath(), is(new File(tempDir, "ror").getCanonicalPath()));
    assertThat(FileUtils.listFiles(new File(tempDir, "ror"), null, true).size(), is(0));
  }
}
