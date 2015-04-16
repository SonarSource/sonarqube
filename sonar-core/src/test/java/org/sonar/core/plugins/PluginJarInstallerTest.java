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
package org.sonar.core.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginJarInstallerTest {

  private PluginJarInstaller extractor;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File userHome;

  @Before
  public void setUp() throws IOException {
    userHome = temporaryFolder.newFolder();
    extractor = new PluginJarInstaller() {
      @Override
      protected File extractPluginDependencies(File pluginFile, File pluginBasedir) throws IOException {
        return null;
      }
    };
  }

  @Test
  public void should_extract_metadata() throws IOException {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFileFromCache("sonar-cobertura-plugin-3.1.1.jar"), true);

    assertThat(metadata.getKey()).isEqualTo("cobertura");
    assertThat(metadata.getBasePlugin()).isNull();
    assertThat(metadata.getName()).isEqualTo("Cobertura");
    assertThat(metadata.isCore()).isEqualTo(true);
    assertThat(metadata.getFile().getName()).isEqualTo("sonar-cobertura-plugin-3.1.1.jar");
    assertThat(metadata.getVersion()).isEqualTo("3.1.1");
    assertThat(metadata.getImplementationBuild()).isEqualTo("b9283404030db9ce1529b1fadfb98331686b116d");
    assertThat(metadata.getHomepage()).isEqualTo("http://www.sonarsource.org/plugins/sonar-cobertura-plugin");
    assertThat(metadata.getIssueTrackerUrl()).isEqualTo("http://jira.codehaus.org/browse/SONAR");
  }

  @Test
  public void should_read_sonar_version() throws IOException {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFileFromCache("sonar-switch-off-violations-plugin-1.1.jar"), false);

    assertThat(metadata.getVersion()).isEqualTo("1.1");
    assertThat(metadata.getSonarVersion()).isEqualTo("2.5");
  }

  @Test
  public void should_extract_extension_metadata() throws IOException {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFileFromCache("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"), true);

    assertThat(metadata.getKey()).isEqualTo("checkstyleextensions");
    assertThat(metadata.getBasePlugin()).isEqualTo("checkstyle");
  }

  @Test
  public void should_extract_requires_plugin_information() throws IOException {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFileFromCache("fake2-plugin-1.1.jar"), true);

    assertThat(metadata.getKey()).isEqualTo("fake2");
    assertThat(metadata.getRequiredPlugins().get(0)).isEqualTo("fake1:1.1");
  }

  File getFileFromCache(String filename) throws IOException {
    File src = FileUtils.toFile(PluginJarInstallerTest.class.getResource("/org/sonar/core/plugins/" + filename));
    File destFile = new File(new File(userHome, "" + filename.hashCode()), filename);
    FileUtils.copyFile(src, destFile);
    return destFile;
  }

}
