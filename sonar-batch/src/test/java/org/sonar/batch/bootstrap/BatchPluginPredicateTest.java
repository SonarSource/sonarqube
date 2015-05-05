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

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BatchPluginPredicateTest {

  Settings settings = new Settings();
  DefaultAnalysisMode mode = mock(DefaultAnalysisMode.class);

  @Test
  public void accept_if_no_inclusions_nor_exclusions() {
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.getWhites()).isEmpty();
    assertThat(predicate.getBlacks()).isEmpty();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("buildbreaker")).isTrue();
  }

  @Test
  public void exclude_buildbreaker_in_preview_mode() {
    when(mode.isPreview()).thenReturn(true);
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("buildbreaker")).isFalse();
  }

  @Test
  public void inclusions_take_precedence_over_exclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura,pmd");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("pmd")).isTrue();
  }

  @Test
  public void accept_core_plugin_even_if_not_in_inclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings.setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("core")).isTrue();
  }

  @Test
  public void accept_core_plugin_even_if_in_exclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings.setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "core,findbugs");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("core")).isTrue();
  }

  @Test
  public void both_inclusions_and_exclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("checkstyle")).isTrue();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("cobertura")).isFalse();
  }

  @Test
  public void only_exclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings.setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("checkstyle")).isFalse();
    assertThat(predicate.apply("pmd")).isFalse();
    assertThat(predicate.apply("cobertura")).isTrue();
  }

  @Test
  public void deprecated_dry_run_settings() {
    when(mode.isPreview()).thenReturn(true);
    settings
      .setProperty(CoreProperties.DRY_RUN_INCLUDE_PLUGINS, "cockpit")
      .setProperty(CoreProperties.DRY_RUN_EXCLUDE_PLUGINS, "views,pmd");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);

    assertThat(predicate.getWhites()).containsOnly("cockpit");
    assertThat(predicate.getBlacks()).containsOnly("views", "pmd");
  }

  @Test
  public void trim_inclusions_and_exclusions() {
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle, pmd, findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura, pmd");
    BatchPluginPredicate predicate = new BatchPluginPredicate(settings, mode);
    assertThat(predicate.apply("pmd")).isTrue();
  }

}
