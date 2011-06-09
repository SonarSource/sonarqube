/*
* Sonar, open source software quality management tool.
* Copyright (C) 2008-2011 SonarSource
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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
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
    ArtifactDownloader.RemotePluginLocation checkstyleLocation = ArtifactDownloader.RemotePluginLocation.create("checkstyle");

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(eq(checkstyleLocation))).thenReturn(copyFile("sonar-checkstyle-plugin-2.8.jar"));

    repository = new BatchPluginRepository(downloader, new PropertiesConfiguration());

    repository.doStart(Arrays.asList(checkstyleLocation));

    assertThat(repository.getPlugins().size(), Matchers.is(1));
    assertThat(repository.getPlugin("checkstyle"), not(nullValue()));
    assertThat(repository.getMetadata().size(), Matchers.is(1));
    assertThat(repository.getMetadata("checkstyle").getName(), Matchers.is("Checkstyle"));
  }

  @Test
  public void shouldLoadPluginExtension() throws IOException {
    ArtifactDownloader.RemotePluginLocation checkstyleLocation = ArtifactDownloader.RemotePluginLocation.create("checkstyle");
    ArtifactDownloader.RemotePluginLocation checkstyleExtLocation = ArtifactDownloader.RemotePluginLocation.create("checkstyleextensions");

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(eq(checkstyleLocation))).thenReturn(copyFile("sonar-checkstyle-plugin-2.8.jar"));
    when(downloader.downloadPlugin(eq(checkstyleExtLocation))).thenReturn(copyFile("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    repository = new BatchPluginRepository(downloader, new PropertiesConfiguration());

    repository.doStart(Arrays.asList(checkstyleLocation, checkstyleExtLocation));

    assertThat(repository.getPlugins().size(), Matchers.is(2));
    assertThat(repository.getPlugin("checkstyle"), not(nullValue()));
    assertThat(repository.getPlugin("checkstyleextensions"), not(nullValue()));
    assertThat(repository.getMetadata().size(), Matchers.is(2));
    assertThat(repository.getMetadata("checkstyle").getName(), Matchers.is("Checkstyle"));
    assertThat(repository.getMetadata("checkstyleextensions").getVersion(), Matchers.is("0.1-SNAPSHOT"));
  }

  @Test
  public void shouldExcludePluginAndItsExtensions() throws IOException {
    ArtifactDownloader.RemotePluginLocation checkstyleLocation = ArtifactDownloader.RemotePluginLocation.create("checkstyle");
    ArtifactDownloader.RemotePluginLocation checkstyleExtLocation = ArtifactDownloader.RemotePluginLocation.create("checkstyleextensions");

    ArtifactDownloader downloader = mock(ArtifactDownloader.class);
    when(downloader.downloadPlugin(eq(checkstyleLocation))).thenReturn(copyFile("sonar-checkstyle-plugin-2.8.jar"));
    when(downloader.downloadPlugin(eq(checkstyleExtLocation))).thenReturn(copyFile("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));

    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.EXCLUDE_PLUGINS, "checkstyle");
    repository = new BatchPluginRepository(downloader, conf);

    repository.doStart(Arrays.asList(checkstyleLocation, checkstyleExtLocation));

    assertThat(repository.getPlugins().size(), Matchers.is(0));
    assertThat(repository.getMetadata().size(), Matchers.is(0));
  }

  private File copyFile(String filename) throws IOException {
    File file = FileUtils.toFile(getClass().getResource("/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/" + filename));
    File tempDir = new File("target/test-tmp/BatchPluginRepositoryTest");
    FileUtils.forceMkdir(tempDir);
    FileUtils.copyFileToDirectory(file, tempDir);
    return new File(tempDir, filename);
  }


  @Test
  public void shouldAlwaysAcceptIfNoWhiteListAndBlackList() {
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), new PropertiesConfiguration());
    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
  }

  @Test
  public void whiteListShouldTakePrecedenceOverBlackList() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    conf.setProperty(CoreProperties.EXCLUDE_PLUGINS, "cobertura,pmd");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), conf);

    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
  }

  @Test
  public void shouldCheckWhitelist() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), conf);

    assertThat(repository.isAccepted("checkstyle"), Matchers.is(true));
    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
    assertThat(repository.isAccepted("cobertura"), Matchers.is(false));
  }

  @Test
  public void shouldCheckBlackListIfNoWhiteList() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    repository = new BatchPluginRepository(mock(ArtifactDownloader.class), conf);

    assertThat(repository.isAccepted("checkstyle"), Matchers.is(false));
    assertThat(repository.isAccepted("pmd"), Matchers.is(false));
    assertThat(repository.isAccepted("cobertura"), Matchers.is(true));
  }

}
