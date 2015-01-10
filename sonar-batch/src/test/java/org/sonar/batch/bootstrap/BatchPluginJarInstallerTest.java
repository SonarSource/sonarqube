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
package org.sonar.batch.bootstrap;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.home.cache.FileCacheBuilder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchPluginJarInstallerTest {

  private BatchPluginJarInstaller extractor;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File userHome;

  @Before
  public void setUp() throws IOException {
    userHome = temporaryFolder.newFolder();
    extractor = new BatchPluginJarInstaller(new FileCacheBuilder().setUserHome(userHome).build());
  }

  @Test
  public void should_copy_and_extract_dependencies() throws IOException {
    File fileFromCache = getFileFromCache("sonar-checkstyle-plugin-2.8.jar");
    DefaultPluginMetadata metadata = extractor.installToCache(fileFromCache, true);

    assertThat(metadata.getKey()).isEqualTo("checkstyle");
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/META-INF/lib/checkstyle-5.1.jar")).exists();
  }

  @Test
  public void should_extract_only_dependencies() throws IOException {
    File fileFromCache = getFileFromCache("sonar-checkstyle-plugin-2.8.jar");
    extractor.installToCache(fileFromCache, true);

    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/META-INF/MANIFEST.MF")).doesNotExist();
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/org/sonar/plugins/checkstyle/CheckstyleVersion.class")).doesNotExist();
  }

  File getFileFromCache(String filename) throws IOException {
    File src = FileUtils.toFile(BatchPluginJarInstallerTest.class.getResource("/org/sonar/batch/bootstrap/BatchPluginJarInstallerTest/" + filename));
    File destFile = new File(new File(userHome, "" + filename.hashCode()), filename);
    FileUtils.copyFile(src, destFile);
    return destFile;
  }

}
