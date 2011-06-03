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

import com.google.common.collect.Lists;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPluginRepositoryTest {

  @Test
  public void shouldLoadPlugin() {
    ArtifactDownloader extensionDownloader = mock(ArtifactDownloader.class);
    when(extensionDownloader.downloadExtension(any(JpaPluginFile.class))).thenReturn(
        FileUtils.toFile(getClass().getResource("/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/sonar-artifact-size-plugin-0.2.jar")));
    BatchPluginRepository repository = new BatchPluginRepository(null, extensionDownloader, new PropertiesConfiguration());

    JpaPlugin plugin = new JpaPlugin("artifactsize");
    plugin.setPluginClass("org.sonar.plugins.artifactsize.ArtifactSizePlugin");
    plugin.createFile("sonar-artifact-size-plugin-0.2.jar");
    repository.doStart(Arrays.asList(plugin));

    Plugin entryPoint = repository.getPlugin("artifactsize");
    assertThat(entryPoint, not(nullValue()));
    ClassRealm classloader = (ClassRealm) entryPoint.getClass().getClassLoader();
    assertThat(classloader.getId(), is("artifactsize"));
  }

  /**
   * Of course clirr does not extend artifact-size plugin in real life !
   */
  @Test
  public void shouldPluginExtensionInTheSameClassloader() {
    ArtifactDownloader extensionDownloader = mock(ArtifactDownloader.class);
    prepareDownloader(extensionDownloader, "artifactsize", "/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/sonar-artifact-size-plugin-0.2.jar");
    prepareDownloader(extensionDownloader, "clirr", "/org/sonar/batch/bootstrap/BatchPluginRepositoryTest/sonar-clirr-plugin-1.1.jar");
    BatchPluginRepository repository = new BatchPluginRepository(null, extensionDownloader, new PropertiesConfiguration());

    JpaPlugin pluginBase = new JpaPlugin("artifactsize");
    pluginBase.setPluginClass("org.sonar.plugins.artifactsize.ArtifactSizePlugin");
    pluginBase.createFile("sonar-artifact-size-plugin-0.2.jar");

    JpaPlugin pluginExtension = new JpaPlugin("clirr");
    pluginExtension.setBasePlugin("artifactsize");
    pluginExtension.setPluginClass("org.sonar.plugins.clirr.ClirrPlugin");
    pluginExtension.createFile("sonar-clirr-plugin-1.1.jar");

    repository.doStart(Arrays.asList(pluginBase, pluginExtension));

    Plugin entryPointBase = repository.getPlugin("artifactsize");
    Plugin entryPointExtension = repository.getPlugin("clirr");
    assertThat(entryPointBase.getClass().getClassLoader(), is(entryPointExtension.getClass().getClassLoader()));
  }

  private void prepareDownloader(ArtifactDownloader extensionDownloader, final String pluginKey, final String filename) {
    when(extensionDownloader.downloadExtension(argThat(new BaseMatcher<JpaPluginFile>() {
      public boolean matches(Object o) {
        return o != null && ((JpaPluginFile) o).getPluginKey().equals(pluginKey);
      }

      public void describeTo(Description description) {

      }
    }))).thenReturn(FileUtils.toFile(getClass().getResource(filename)));
  }

  @Test
  public void shouldAlwaysAcceptIfNoWhiteListAndBlackList() {
    BatchPluginRepository repository = new BatchPluginRepository(mock(JpaPluginDao.class), mock(ArtifactDownloader.class), new PropertiesConfiguration());
    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
  }

  @Test
  public void whiteListShouldTakePrecedenceOverBlackList() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    conf.setProperty(CoreProperties.EXCLUDE_PLUGINS, "cobertura,pmd");
    BatchPluginRepository repository = new BatchPluginRepository(mock(JpaPluginDao.class), mock(ArtifactDownloader.class), conf);

    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
  }

  @Test
  public void shouldCheckWhitelist() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginRepository repository = new BatchPluginRepository(mock(JpaPluginDao.class), mock(ArtifactDownloader.class), conf);

    assertThat(repository.isAccepted("checkstyle"), Matchers.is(true));
    assertThat(repository.isAccepted("pmd"), Matchers.is(true));
    assertThat(repository.isAccepted("cobertura"), Matchers.is(false));
  }

  @Test
  public void shouldCheckBlackListIfNoWhiteList() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginRepository repository = new BatchPluginRepository(mock(JpaPluginDao.class), mock(ArtifactDownloader.class), conf);

    assertThat(repository.isAccepted("checkstyle"), Matchers.is(false));
    assertThat(repository.isAccepted("pmd"), Matchers.is(false));
    assertThat(repository.isAccepted("cobertura"), Matchers.is(true));
  }

  @Test
  public void shouldExcludePluginDependents() {
    JpaPlugin pmd = new JpaPlugin("pmd");
    JpaPlugin checkstyle = new JpaPlugin("checkstyle");
    JpaPlugin checkstyleExtension = new JpaPlugin("checkstyle-ext");
    checkstyleExtension.setBasePlugin("checkstyle");

    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty(CoreProperties.EXCLUDE_PLUGINS, "checkstyle");
    BatchPluginRepository repository = new BatchPluginRepository(mock(JpaPluginDao.class), mock(ArtifactDownloader.class), conf);

    List<JpaPlugin> filteredPlugins = repository.filter(Arrays.asList(checkstyle, checkstyleExtension, pmd));
    assertThat(filteredPlugins.size(), Matchers.is(1));
    assertThat(filteredPlugins, hasItem(pmd));
  }
}
