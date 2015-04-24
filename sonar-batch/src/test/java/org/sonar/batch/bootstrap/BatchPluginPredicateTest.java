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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.home.cache.FileCache;
import org.sonar.home.cache.FileCacheBuilder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPluginPredicateTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DefaultAnalysisMode mode = mock(DefaultAnalysisMode.class);
  FileCache cache;
  File userHome;

  @Before
  public void before() throws IOException {
    userHome = temp.newFolder();
    cache = new FileCacheBuilder().setUserHome(userHome).build();
  }

  @Test
  public void shouldAlwaysAcceptIfNoWhiteListAndBlackList() {
    BatchPluginPredicate predicate = new BatchPluginPredicate(new Settings(), mode);
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("buildbreaker")).isTrue();
  }

  @Test
  public void shouldBlackListBuildBreakerInPreviewMode() {
    when(mode.isPreview()).thenReturn(true);
    BatchPluginPredicate predicate = new BatchPluginPredicate(new Settings(), mode);
    assertThat(predicate.apply("buildbreaker")).isFalse();
  }

  @Test
  public void whiteListShouldTakePrecedenceOverBlackList() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura,pmd");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("pmd")).isTrue();
  }

  @Test
  public void corePluginShouldAlwaysBeInWhiteList() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("core")).isTrue();
  }

  @Test
  public void corePluginShouldNeverBeInBlackList() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "core,findbugs");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("core")).isTrue();
  }

  @Test
  public void check_white_list_with_black_list() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("checkstyle")).isTrue();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("cobertura")).isFalse();
  }

  @Test
  public void check_white_list_when_plugin_is_in_both_list() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "cobertura,checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("checkstyle")).isTrue();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("cobertura")).isTrue();
  }

  @Test
  public void check_black_list_if_no_white_list() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("checkstyle")).isFalse();
    assertThat(predicate.apply("pmd")).isFalse();
    assertThat(predicate.apply("cobertura")).isTrue();
  }

  @Test
  public void should_concatenate_preview_predicates() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "cockpit")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "views")
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd");
    when(mode.isPreview()).thenReturn(true);
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.getWhites()).containsOnly("cockpit");
    assertThat(predicate.getBlacks()).containsOnly("views", "checkstyle", "pmd");
  }

  @Test
  public void should_concatenate_deprecated_dry_run_predicates() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.DRY_RUN_INCLUDE_PLUGINS, "cockpit")
      .setProperty(CoreProperties.DRY_RUN_EXCLUDE_PLUGINS, "views")
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "checkstyle,pmd");
    when(mode.isPreview()).thenReturn(true);
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.getWhites()).containsOnly("cockpit");
    assertThat(predicate.getBlacks()).containsOnly("views", "checkstyle", "pmd");
  }

  @Test
  public void inclusions_and_exclusions_should_be_trimmed() {
    Settings settings = new Settings()
      .setProperty(CoreProperties.BATCH_INCLUDE_PLUGINS, "checkstyle, pmd, findbugs")
      .setProperty(CoreProperties.BATCH_EXCLUDE_PLUGINS, "cobertura, pmd");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("pmd")).isTrue();
  }

}
