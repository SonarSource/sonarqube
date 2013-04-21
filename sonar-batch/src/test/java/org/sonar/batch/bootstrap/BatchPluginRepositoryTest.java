/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.core.plugins.RemotePlugin;
import org.sonar.core.plugins.RemotePluginFile;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPluginRepositoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private BatchPluginRepository repository;

  @After
  public void tearDown() {
    if (repository != null) {
      repository.stop();
    }
  }

  @Test
  public void shouldLoadPlugin() throws IOException {
    TempDirectories tempDirs = mock(TempDirectories.class);
    File toDir = temp.newFolder();
    when(tempDirs.getDir("plugins/checkstyle")).thenReturn(toDir);
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);

    PluginDownloader downloader = mock(PluginDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar"));

    repository = new BatchPluginRepository(downloader, tempDirs, new Settings());

    repository.doStart(Arrays.asList(checkstyle));

    assertThat(repository.getPlugin("checkstyle")).isNotNull();
    assertThat(repository.getMetadata()).hasSize(1);
    assertThat(repository.getMetadata("checkstyle").getName()).isEqualTo("Checkstyle");
    assertThat(repository.getMetadata("checkstyle").getDeployedFiles()).hasSize(4); // plugin + 3 dependencies
  }

  @Test
  public void shouldLoadPluginExtension() throws IOException {
    TempDirectories tempDirs = mock(TempDirectories.class);
    File toDir1 = temp.newFolder();
    File toDir2 = temp.newFolder();
    when(tempDirs.getDir("plugins/checkstyle")).thenReturn(toDir1);
    when(tempDirs.getDir("plugins/checkstyleextensions")).thenReturn(toDir2);
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    RemotePlugin checkstyleExt = new RemotePlugin("checkstyleextensions", false);

    PluginDownloader downloader = mock(PluginDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar"));
    when(downloader.downloadPlugin(checkstyleExt)).thenReturn(copyFiles("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    repository = new BatchPluginRepository(downloader, tempDirs, new Settings());

    repository.doStart(Arrays.asList(checkstyle, checkstyleExt));

    assertThat(repository.getPlugin("checkstyle")).isNotNull();
    assertThat(repository.getPlugin("checkstyleextensions")).isNotNull();
    assertThat(repository.getMetadata()).hasSize(2);
    assertThat(repository.getMetadata("checkstyle").getName()).isEqualTo("Checkstyle");
    assertThat(repository.getMetadata("checkstyleextensions").getVersion()).isEqualTo("0.1-SNAPSHOT");
  }

  @Test
  public void shouldLoadPluginDeprecatedExtensions() throws IOException {
    TempDirectories tempDirs = mock(TempDirectories.class);
    File toDir = temp.newFolder();
    when(tempDirs.getDir("plugins/checkstyle")).thenReturn(toDir);
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    checkstyle.getFiles().add(new RemotePluginFile("checkstyle-ext.xml", "fakemd5"));

    PluginDownloader downloader = mock(PluginDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar", "checkstyle-ext.xml"));

    repository = new BatchPluginRepository(downloader, tempDirs, new Settings());

    repository.doStart(Arrays.asList(checkstyle));

    assertThat(repository.getPlugin("checkstyle")).isNotNull();
    assertThat(repository.getMetadata()).hasSize(1);
    assertThat(repository.getMetadata("checkstyle").getName()).isEqualTo("Checkstyle");
    assertThat(repository.getMetadata("checkstyle").getDeployedFiles()).hasSize(5); // plugin + 3 dependencies + 1 deprecated
    // extension
  }

  @Test
  public void shouldExcludePluginAndItsExtensions() throws IOException {
    TempDirectories tempDirs = mock(TempDirectories.class);
    File toDir1 = temp.newFolder();
    File toDir2 = temp.newFolder();
    when(tempDirs.getDir("plugins/checkstyle")).thenReturn(toDir1);
    when(tempDirs.getDir("plugins/checkstyleextensions")).thenReturn(toDir2);
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    RemotePlugin checkstyleExt = new RemotePlugin("checkstyleextensions", false);

    PluginDownloader downloader = mock(PluginDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar"));
    when(downloader.downloadPlugin(checkstyleExt)).thenReturn(copyFiles("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle");
    repository = new BatchPluginRepository(downloader, tempDirs, settings);

    repository.doStart(Arrays.asList(checkstyle, checkstyleExt));

    assertThat(repository.getMetadata()).isEmpty();
  }

  private List<File> copyFiles(String... filenames) throws IOException {
    List<File> files = Lists.newArrayList();
    for (String filename : filenames) {
      File file = TestUtils.getResource("/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/" + filename);
      File tempDir = new File("target/test-tmp/BatchPluginRepositoryTest");
      FileUtils.forceMkdir(tempDir);
      FileUtils.copyFileToDirectory(file, tempDir);
      files.add(new File(tempDir, filename));
    }
    return files;
  }

  @Test
  public void shouldAlwaysAcceptIfNoWhiteListAndBlackList() {
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(new Settings());
    assertThat(filter.accepts("pmd")).isTrue();
  }

  @Test
  public void whiteListShouldTakePrecedenceOverBlackList() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
        .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura,pmd");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.accepts("pmd")).isTrue();
  }

  @Test
  public void corePluginShouldAlwaysBeInWhiteList() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.accepts("core")).isTrue();
  }

  @Test
  public void corePluginShouldNeverBeInBlackList() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "core,findbugs");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.accepts("core")).isTrue();
  }

  // English Pack plugin should never be blacklisted as it is mandatory for the I18nManager on batch side
  @Test
  public void englishPackPluginShouldNeverBeInBlackList() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "l10nen,findbugs");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.accepts("l10nen")).isTrue();
  }

  @Test
  public void shouldCheckWhitelist() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.accepts("checkstyle")).isTrue();
    assertThat(filter.accepts("pmd")).isTrue();
    assertThat(filter.accepts("cobertura")).isFalse();
  }

  @Test
  public void shouldCheckBlackListIfNoWhiteList() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.accepts("checkstyle")).isFalse();
    assertThat(filter.accepts("pmd")).isFalse();
    assertThat(filter.accepts("cobertura")).isTrue();
  }

  @Test
  public void should_concatenate_dry_run_filters() {
    Settings settings = new Settings()
        .setProperty(CoreProperties.DRY_RUN, true)
        .setProperty(CoreProperties.DRY_RUN_INCLUDE_PLUGINS, "cockpit")
        .setProperty(CoreProperties.DRY_RUN_EXCLUDE_PLUGINS, "views")
        .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd");
    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings);
    assertThat(filter.whites).containsOnly("cockpit");
    assertThat(filter.blacks).containsOnly("views", "checkstyle", "pmd");
  }
}
