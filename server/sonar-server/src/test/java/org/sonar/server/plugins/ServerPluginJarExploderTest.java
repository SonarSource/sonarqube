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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.platform.ServerFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerPluginJarExploderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem fs = mock(ServerFileSystem.class);
  private PluginFileSystem pluginFileSystem = mock(PluginFileSystem.class);
  private ServerPluginJarExploder underTest = new ServerPluginJarExploder(fs, pluginFileSystem);

  @Test
  public void copy_all_classloader_files_to_dedicated_directory() throws Exception {
    File deployDir = temp.newFolder();
    when(fs.getDeployedPluginsDir()).thenReturn(deployDir);
    File sourceJar = TestProjectUtils.jarOf("test-libs-plugin");
    PluginInfo info = PluginInfo.create(sourceJar);

    ExplodedPlugin exploded = underTest.explode(info);

    // all the files loaded by classloaders (JAR + META-INF/libs/*.jar) are copied to the dedicated directory
    // web/deploy/{pluginKey}
    File pluginDeployDir = new File(deployDir, "testlibs");

    assertThat(exploded.getKey()).isEqualTo("testlibs");
    assertThat(exploded.getMain()).isFile().exists().hasParent(pluginDeployDir);
    assertThat(exploded.getLibs()).extracting("name").containsOnly("commons-daemon-1.0.15.jar", "commons-email-20030310.165926.jar");
    for (File lib : exploded.getLibs()) {
      assertThat(lib).exists().isFile();
      assertThat(lib.getCanonicalPath()).startsWith(pluginDeployDir.getCanonicalPath());
    }
    File targetJar = new File(fs.getDeployedPluginsDir(), "testlibs/test-libs-plugin-0.1-SNAPSHOT.jar");
    verify(pluginFileSystem).addInstalledPlugin(info, targetJar);
  }
}
