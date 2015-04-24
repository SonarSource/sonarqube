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
package org.sonar.core.platform;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.ZipUtils;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginUnzipperTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void unzip_plugin_with_libs() throws Exception {
    final File jarFile = getFile("sonar-checkstyle-plugin-2.8.jar");
    final File toDir = temp.newFolder();
    PluginInfo pluginInfo = new PluginInfo().setKey("checkstyle").setFile(jarFile);

    PluginUnzipper unzipper = new PluginUnzipper() {
      @Override
      public UnzippedPlugin unzip(PluginInfo info) {
        try {
          ZipUtils.unzip(jarFile, toDir, newLibFilter());
          return UnzippedPlugin.createFromUnzippedDir(info.getKey(), info.getFile(), toDir);
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
    };
    UnzippedPlugin unzipped = unzipper.unzip(pluginInfo);
    assertThat(unzipped.getKey()).isEqualTo("checkstyle");
    assertThat(unzipped.getLibs()).extracting("name").containsOnly("antlr-2.7.6.jar", "checkstyle-5.1.jar", "commons-cli-1.0.jar");
    assertThat(unzipped.getMain()).isSameAs(jarFile);
  }

  @Test
  public void unzip_plugin_without_libs() throws Exception {
    File jarFile = temp.newFile();
    final File toDir = temp.newFolder();
    PluginInfo pluginInfo = new PluginInfo().setFile(jarFile);

    PluginUnzipper unzipper = new PluginUnzipper() {
      @Override
      public UnzippedPlugin unzip(PluginInfo info) {
        return UnzippedPlugin.createFromUnzippedDir("foo", info.getFile(), toDir);
      }
    };
    UnzippedPlugin unzipped = unzipper.unzip(pluginInfo);
    assertThat(unzipped.getKey()).isEqualTo("foo");
    assertThat(unzipped.getLibs()).isEmpty();
    assertThat(unzipped.getMain()).isSameAs(jarFile);
  }

  private File getFile(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/" + filename));
  }
}
