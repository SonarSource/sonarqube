/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.ce.container;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class CePluginJarExploderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DumbFileSystem fs = new DumbFileSystem(temp);
  CePluginJarExploder underTest = new CePluginJarExploder(fs);

  @Test
  public void explode_jar_to_temp_directory() throws Exception {
    PluginInfo info = PluginInfo.create(plugin1Jar());

    ExplodedPlugin exploded = underTest.explode(info);

    // all the files loaded by classloaders (JAR + META-INF/libs/*.jar) are copied to a dedicated temp directory
    File copiedJar = exploded.getMain();

    assertThat(exploded.getKey()).isEqualTo("test");
    assertThat(copiedJar).isFile().exists();
    assertThat(copiedJar.getParentFile()).isDirectory().hasName("test");
    assertThat(copiedJar.getParentFile().getParentFile()).isDirectory().hasName("ce-exploded-plugins");
  }

  @Test
  public void plugins_do_not_overlap() throws Exception {
    PluginInfo info1 = PluginInfo.create(plugin1Jar());
    PluginInfo info2 = PluginInfo.create(plugin2Jar());

    ExplodedPlugin exploded1 = underTest.explode(info1);
    ExplodedPlugin exploded2 = underTest.explode(info2);

    assertThat(exploded1.getKey()).isEqualTo("test");
    assertThat(exploded1.getMain()).isFile().exists().hasName("sonar-test-plugin-0.1-SNAPSHOT.jar");
    assertThat(exploded2.getKey()).isEqualTo("test2");
    assertThat(exploded2.getMain()).isFile().exists().hasName("sonar-test2-plugin-0.1-SNAPSHOT.jar");
  }

  private File plugin1Jar() {
    return new File("src/test/plugins/sonar-test-plugin/target/sonar-test-plugin-0.1-SNAPSHOT.jar");
  }

  private File plugin2Jar() {
    return new File("src/test/plugins/sonar-test2-plugin/target/sonar-test2-plugin-0.1-SNAPSHOT.jar");
  }

  private class DumbFileSystem implements ServerFileSystem {
    private final TemporaryFolder temp;
    private File tempDir;

    public DumbFileSystem(TemporaryFolder temp) {
      this.temp = temp;
    }

    @Override
    public File getHomeDir() {
      throw new UnsupportedOperationException();
    }

    @Override
    public File getTempDir() {
      if (tempDir == null) {
        try {
          this.tempDir = temp.newFolder();
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
      }
      return tempDir;
    }

    @Override
    public List<File> getExtensions(String dirName, String... suffixes) {
      throw new UnsupportedOperationException();
    }
  }
}
