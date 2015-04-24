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
///*
// * SonarQube, open source software quality management tool.
// * Copyright (C) 2008-2014 SonarSource
// * mailto:contact AT sonarsource DOT com
// *
// * SonarQube is free software; you can redistribute it and/or
// * modify it under the terms of the GNU Lesser General Public
// * License as published by the Free Software Foundation; either
// * version 3 of the License, or (at your option) any later version.
// *
// * SonarQube is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public License
// * along with this program; if not, write to the Free Software Foundation,
// * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
// */
//package org.sonar.batch.bootstrap;
//
//import com.google.common.io.Resources;
//import org.apache.commons.io.FileUtils;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.rules.TemporaryFolder;
//import org.sonar.api.CoreProperties;
//import org.sonar.api.config.Settings;
//import org.sonar.core.plugins.RemotePlugin;
//import org.sonar.home.cache.FileCache;
//import org.sonar.home.cache.FileCacheBuilder;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.Arrays;
//
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class BatchPluginRepositoryTest {
//
//  @Rule
//  public TemporaryFolder temp = new TemporaryFolder();
//
//  private BatchPluginRepository repository;
//  private DefaultAnalysisMode mode;
//  private FileCache cache;
//  private File userHome;
//
//  @Before
//  public void before() throws IOException {
//    mode = mock(DefaultAnalysisMode.class);
//    when(mode.isPreview()).thenReturn(false);
//    userHome = temp.newFolder();
//    cache = new FileCacheBuilder().setUserHome(userHome).build();
//  }
//
//  @After
//  public void tearDown() {
//    if (repository != null) {
//      repository.stop();
//    }
//  }
//
//  @Test
//  public void shouldLoadPlugin() throws Exception {
//    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
//
//    DefaultPluginRepository installer = mock(DefaultPluginsRepository.class);
//    when(installer.pluginFile(checkstyle)).thenReturn(fileFromCache("sonar-checkstyle-plugin-2.8.jar"));
//
//    repository = new BatchPluginRepository(installer, new Settings(), mode, new BatchPluginJarInstaller(cache));
//
//    repository.doStart(Arrays.asList(checkstyle));
//
//    assertThat(repository.getPlugin("checkstyle")).isNotNull();
//    assertThat(repository.getMetadata()).hasSize(1);
//    assertThat(repository.getMetadata("checkstyle").getName()).isEqualTo("Checkstyle");
//    assertThat(repository.getMetadata("checkstyle").getDeployedFiles()).hasSize(4); // plugin + 3 dependencies
//  }
//
//  @Test
//  public void shouldLoadPluginExtension() throws Exception {
//    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
//    RemotePlugin checkstyleExt = new RemotePlugin("checkstyleextensions", false);
//
//    DefaultPluginsRepository downloader = mock(DefaultPluginsRepository.class);
//    when(downloader.pluginFile(checkstyle)).thenReturn(fileFromCache("sonar-checkstyle-plugin-2.8.jar"));
//    when(downloader.pluginFile(checkstyleExt)).thenReturn(fileFromCache("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));
//
//    repository = new BatchPluginRepository(downloader, new Settings(), mode, new BatchPluginJarInstaller(cache));
//
//    repository.doStart(Arrays.asList(checkstyle, checkstyleExt));
//
//    assertThat(repository.getPlugin("checkstyle")).isNotNull();
//    assertThat(repository.getPlugin("checkstyleextensions")).isNotNull();
//    assertThat(repository.getMetadata()).hasSize(2);
//    assertThat(repository.getMetadata("checkstyle").getName()).isEqualTo("Checkstyle");
//    assertThat(repository.getMetadata("checkstyleextensions").getVersion()).isEqualTo("0.1-SNAPSHOT");
//  }
//
//  @Test
//  public void shouldExcludePluginAndItsExtensions() throws Exception {
//    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
//    RemotePlugin checkstyleExt = new RemotePlugin("checkstyleextensions", false);
//
//    DefaultPluginsRepository downloader = mock(DefaultPluginsRepository.class);
//    when(downloader.pluginFile(checkstyle)).thenReturn(fileFromCache("sonar-checkstyle-plugin-2.8.jar"));
//    when(downloader.pluginFile(checkstyleExt)).thenReturn(fileFromCache("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"));
//
//    Settings settings = new Settings();
//    settings.setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle");
//    repository = new BatchPluginRepository(downloader, settings, mode, new BatchPluginJarInstaller(cache));
//
//    repository.doStart(Arrays.asList(checkstyle, checkstyleExt));
//
//    assertThat(repository.getMetadata()).isEmpty();
//  }
//
//  private File fileFromCache(String filename) throws Exception {
//    File file = new File(Resources.getResource("org/sonar/batch/bootstrap/BatchPluginRepositoryTest/" + filename).toURI());
//    File destDir = new File(userHome, "cache/foomd5");
//    FileUtils.forceMkdir(destDir);
//    FileUtils.copyFileToDirectory(file, destDir);
//    return new File(destDir, filename);
//  }
//
//  @Test
//  public void shouldAlwaysAcceptIfNoWhiteListAndBlackList() {
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(new Settings(), mode);
//    assertThat(filter.accepts("pmd")).isTrue();
//    assertThat(filter.accepts("buildbreaker")).isTrue();
//  }
//
//  @Test
//  public void shouldBlackListBuildBreakerInPreviewMode() {
//    when(mode.isPreview()).thenReturn(true);
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(new Settings(), mode);
//    assertThat(filter.accepts("buildbreaker")).isFalse();
//  }
//
//  @Test
//  public void whiteListShouldTakePrecedenceOverBlackList() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura,pmd");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("pmd")).isTrue();
//  }
//
//  @Test
//  public void corePluginShouldAlwaysBeInWhiteList() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("core")).isTrue();
//  }
//
//  @Test
//  public void corePluginShouldNeverBeInBlackList() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "core,findbugs");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("core")).isTrue();
//  }
//
//  @Test
//  public void check_white_list_with_black_list() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("checkstyle")).isTrue();
//    assertThat(filter.accepts("pmd")).isTrue();
//    assertThat(filter.accepts("cobertura")).isFalse();
//  }
//
//  @Test
//  public void check_white_list_when_plugin_is_in_both_list() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "cobertura,checkstyle,pmd,findbugs")
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("checkstyle")).isTrue();
//    assertThat(filter.accepts("pmd")).isTrue();
//    assertThat(filter.accepts("cobertura")).isTrue();
//  }
//
//  @Test
//  public void check_black_list_if_no_white_list() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("checkstyle")).isFalse();
//    assertThat(filter.accepts("pmd")).isFalse();
//    assertThat(filter.accepts("cobertura")).isTrue();
//  }
//
//  @Test
//  public void should_concatenate_preview_filters() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "cockpit")
//      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "views")
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd");
//    when(mode.isPreview()).thenReturn(true);
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.whites).containsOnly("cockpit");
//    assertThat(filter.blacks).containsOnly("views", "checkstyle", "pmd");
//  }
//
//  @Test
//  public void should_concatenate_deprecated_dry_run_filters() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.DRY_RUN_INCLUDE_PLUGINS, "cockpit")
//      .setProperty(CoreProperties.DRY_RUN_EXCLUDE_PLUGINS, "views")
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd");
//    when(mode.isPreview()).thenReturn(true);
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.whites).containsOnly("cockpit");
//    assertThat(filter.blacks).containsOnly("views", "checkstyle", "pmd");
//  }
//
//  @Test
//  public void inclusions_and_exclusions_should_be_trimmed() {
//    Settings settings = new Settings()
//      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle, pmd, findbugs")
//      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura, pmd");
//    BatchPluginRepository.PluginFilter filter = new BatchPluginRepository.PluginFilter(settings, mode);
//    assertThat(filter.accepts("pmd")).isTrue();
//  }
//
//}
