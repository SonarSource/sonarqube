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
package org.sonar.server.plugins;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.UnzippedPlugin;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPluginUnzipperTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DefaultServerFileSystem fs = mock(DefaultServerFileSystem.class);
  ServerPluginUnzipper underTest = new ServerPluginUnzipper(fs);

  @Test
  public void copy_all_classloader_files_to_dedicated_directory() throws Exception {
    File deployDir = temp.newFolder();
    when(fs.getDeployedPluginsDir()).thenReturn(deployDir);
    File jar = TestProjectUtils.jarOf("test-libs-plugin");
    PluginInfo info = PluginInfo.create(jar);

    UnzippedPlugin unzipped = underTest.unzip(info);

    // all the files loaded by classloaders (JAR + META-INF/libs/*.jar) are copied to the dedicated directory
    // web/deploy/{pluginKey}
    File pluginDeployDir = new File(deployDir, "testlibs");

    assertThat(unzipped.getKey()).isEqualTo("testlibs");
    assertThat(unzipped.getMain()).isFile().exists().hasParent(pluginDeployDir);
    assertThat(unzipped.getLibs()).extracting("name").containsOnly("commons-daemon-1.0.15.jar", "commons-email-20030310.165926.jar");
    for (File lib : unzipped.getLibs()) {
      assertThat(lib).exists().isFile();
      assertThat(lib.getCanonicalPath()).startsWith(pluginDeployDir.getCanonicalPath());
    }
  }
}
