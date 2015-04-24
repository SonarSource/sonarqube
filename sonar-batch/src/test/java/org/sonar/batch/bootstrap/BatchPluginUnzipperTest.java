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
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.UnzippedPlugin;
import org.sonar.home.cache.FileCache;
import org.sonar.home.cache.FileCacheBuilder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchPluginUnzipperTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  File userHome;
  BatchPluginUnzipper underTest;

  @Before
  public void setUp() throws IOException {
    userHome = temp.newFolder();
    FileCache fileCache = new FileCacheBuilder().setUserHome(userHome).build();
    underTest = new BatchPluginUnzipper(fileCache);
  }

  @Test
  public void copy_and_extract_libs() throws IOException {
    File fileFromCache = getFileFromCache("sonar-checkstyle-plugin-2.8.jar");
    UnzippedPlugin unzipped = underTest.unzip(PluginInfo.create(fileFromCache));

    assertThat(unzipped.getKey()).isEqualTo("checkstyle");
    assertThat(unzipped.getMain()).isFile().exists();
    assertThat(unzipped.getLibs()).extracting("name").containsOnly("antlr-2.7.6.jar", "checkstyle-5.1.jar", "commons-cli-1.0.jar");
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/META-INF/lib/checkstyle-5.1.jar")).exists();
  }

  @Test
  public void extract_only_libs() throws IOException {
    File fileFromCache = getFileFromCache("sonar-checkstyle-plugin-2.8.jar");
    underTest.unzip(PluginInfo.create(fileFromCache));

    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar")).exists();
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/META-INF/MANIFEST.MF")).doesNotExist();
    assertThat(new File(fileFromCache.getParent(), "sonar-checkstyle-plugin-2.8.jar_unzip/org/sonar/plugins/checkstyle/CheckstyleVersion.class")).doesNotExist();
  }

  File getFileFromCache(String filename) throws IOException {
    File src = FileUtils.toFile(BatchPluginUnzipperTest.class.getResource("/org/sonar/batch/bootstrap/BatchPluginUnzipperTest/" + filename));
    File destFile = new File(new File(userHome, "" + filename.hashCode()), filename);
    FileUtils.copyFile(src, destFile);
    return destFile;
  }

}
