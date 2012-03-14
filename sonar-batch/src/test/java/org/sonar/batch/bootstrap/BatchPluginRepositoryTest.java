/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
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
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.core.plugins.RemotePlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPluginRepositoryTest {

  private BatchPluginRepository repository;

  @After
  public void tearDown() {
    if (repository!=null) {
      repository.stop();
    }
  }

  @Test
  public void shouldLoadPlugin() throws IOException {
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar"));

    repository = new BatchPluginRepository(downloader, new Settings());

    repository.doStart(Arrays.asList(checkstyle));

    assertThat(repository.getPlugins().size(), Matchers.is(1));
    assertThat(repository.getPlugin("checkstyle"), not(nullValue()));
    assertThat(repository.getMetadata().size(), Matchers.is(1));
    assertThat(repository.getMetadata("checkstyle").getName(), Matchers.is("Checkstyle"));
    assertThat(repository.getMetadata("checkstyle").getDeployedFiles().size(), Matchers.is(4)); // plugin + 3 dependencies
  }

  @Test
  public void shouldLoadPluginExtension() throws IOException {
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    RemotePlugin checkstyleExt = new RemotePlugin("checkstyleextensions", false);

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar"));
    when(downloader.downloadPlugin(checkstyleExt)).thenReturn(copyFiles("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    repository = new BatchPluginRepository(downloader, new Settings());

    repository.doStart(Arrays.asList(checkstyle, checkstyleExt));

    assertThat(repository.getPlugins().size(), Matchers.is(2));
    assertThat(repository.getPlugin("checkstyle"), not(nullValue()));
    assertThat(repository.getPlugin("checkstyleextensions"), not(nullValue()));
    assertThat(repository.getMetadata().size(), Matchers.is(2));
    assertThat(repository.getMetadata("checkstyle").getName(), Matchers.is("Checkstyle"));
    assertThat(repository.getMetadata("checkstyleextensions").getVersion(), Matchers.is("0.1-SNAPSHOT"));
  }

  @Test
  public void shouldLoadPluginDeprecatedExtensions() throws IOException {
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true)
        .addFilename("checkstyle-ext.xml");

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar", "checkstyle-ext.xml"));

    repository = new BatchPluginRepository(downloader, new Settings());

    repository.doStart(Arrays.asList(checkstyle));

    assertThat(repository.getPlugins().size(), Matchers.is(1));
    assertThat(repository.getPlugin("checkstyle"), not(nullValue()));
    assertThat(repository.getMetadata().size(), Matchers.is(1));
    assertThat(repository.getMetadata("checkstyle").getName(), Matchers.is("Checkstyle"));
    assertThat(repository.getMetadata("checkstyle").getDeployedFiles().size(), Matchers.is(5)); // plugin + 3 dependencies + 1 deprecated extension
  }

  @Test
  public void shouldExcludePluginAndItsExtensions() throws IOException {
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    RemotePlugin checkstyleExt = new RemotePlugin("checkstyleextensions", false);

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(checkstyle)).thenReturn(copyFiles("sonar-checkstyle-plugin-2.8.jar"));
    when(downloader.downloadPlugin(checkstyleExt)).thenReturn(copyFiles("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle");
    repository = new BatchPluginRepository(downloader, settings);

    repository.doStart(Arrays.asList(checkstyle, checkstyleExt));

    assertThat(repository.getPlugins().size(), Matchers.is(0));
    assertThat(repository.getMetadata().size(), Matchers.is(0));
  }

  private List<File> copyFiles(String... filenames) throws IOException {
    List files = Lists.newArrayList();
    for (String filename : filenames) {
      File file = FileUtils.toFile(getClass().getResource("/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/" + filename));
      File tempDir = new File("target/test-tmp/BatchPluginRepositoryTest");
      FileUtils.forceMkdir(tempDir);
      FileUtils.copyFileToDirectory(file, tempDir);
      files.add(new File(tempDir, filename));
    }
    return files;
  }


  @Test
  public void shouldAlwaysAcceptIfNoWhiteListAndBlackList() {
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), new Settings());
    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
  }

  @Test
  public void whiteListShouldTakePrecedenceOverBlackList() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    settings.setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura,pmd");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), settings);

    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
  }

  @Test
  public void corePluginShouldAlwaysBeInWhiteList() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), settings);
    assertThat(repository.isAccepted("core"), Matchers.is(true));
  }

  @Test
  public void corePluginShouldNeverBeInBlackList() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "core,findbugs");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), settings);
    assertThat(repository.isAccepted("core"), Matchers.is(true));
  }

  @Test
  public void shouldCheckWhitelist() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), settings);

    assertThat(repository.isAccepted("checkstyle"), Matchers.is(true));
    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
    assertThat(repository.isAccepted("cobertura"), Matchers.is(false));
  }

  @Test
  public void shouldCheckBlackListIfNoWhiteList() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), settings);

    assertThat(repository.isAccepted("checkstyle"), Matchers.is(false));
    assertThat(repository.isAccepted("pmd"), Matchers.is(false));
    assertThat(repository.isAccepted("cobertura"), Matchers.is(true));
  }

}
