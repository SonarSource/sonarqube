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
package org.sonar.scanner.bootstrap;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerPluginJarExploderTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  private File tempDir;
  private ScannerPluginJarExploder underTest;

  @Before
  public void setUp() throws IOException {
    tempDir = temp.newFolder();
    PluginFiles pluginFiles = mock(PluginFiles.class);
    when(pluginFiles.createTempDir()).thenReturn(tempDir);
    underTest = new ScannerPluginJarExploder(pluginFiles);
  }

  @Test
  public void copy_and_extract_libs() throws IOException {
    File jar = loadFile("sonar-checkstyle-plugin-2.8.jar");
    ExplodedPlugin exploded = underTest.explode(PluginInfo.create(jar));

    assertThat(exploded.getKey()).isEqualTo("checkstyle");
    assertThat(exploded.getMain()).isFile().exists();
    assertThat(exploded.getLibs()).extracting(File::getName).containsExactlyInAnyOrder("antlr-2.7.6.jar", "checkstyle-5.1.jar", "commons-cli-1.0.jar");
    assertThat(new File(jar.getParent(), "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(jar.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/META-INF/lib/checkstyle-5.1.jar")).exists();
  }

  @Test
  public void extract_only_libs() throws IOException {
    File jar = loadFile("sonar-checkstyle-plugin-2.8.jar");

    underTest.explode(PluginInfo.create(jar));

    assertThat(new File(jar.getParent(), "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(jar.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/META-INF/MANIFEST.MF")).doesNotExist();
    assertThat(new File(jar.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/org/sonar/plugins/checkstyle/CheckstyleVersion.class")).doesNotExist();
  }

  private File loadFile(String filename) throws IOException {
    File src = FileUtils.toFile(getClass().getResource(getClass().getSimpleName() + "/" + filename));
    File dest = new File(temp.newFolder(), filename);
    FileUtils.copyFile(src, dest);
    return dest;
  }

}
