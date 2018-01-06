/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.bootstrap;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScannerPluginPredicateTest {

  private MapSettings settings = new MapSettings();
  private GlobalAnalysisMode mode = mock(GlobalAnalysisMode.class);

  @Test
  public void accept_if_no_inclusions_nor_exclusions() {
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.getWhites()).isEmpty();
    assertThat(predicate.getBlacks()).isEmpty();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("buildbreaker")).isTrue();
  }

  @Test
  public void exclude_buildbreaker_in_preview_mode() {
    when(mode.isPreview()).thenReturn(true);
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.apply("buildbreaker")).isFalse();
  }

  @Test
  public void inclusions_take_precedence_over_exclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura,pmd");
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.apply("pmd")).isTrue();
  }

  @Test
  public void verify_both_inclusions_and_exclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura");
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.apply("checkstyle")).isTrue();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("cobertura")).isFalse();
  }

  @Test
  public void verify_both_inclusions_and_exclusions_issues() {
    when(mode.isIssues()).thenReturn(true);
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle,pmd,findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura");
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.apply("checkstyle")).isTrue();
    assertThat(predicate.apply("pmd")).isTrue();
    assertThat(predicate.apply("cobertura")).isFalse();
  }

  @Test
  public void test_exclusions_without_any_inclusions() {
    when(mode.isPreview()).thenReturn(true);
    settings.setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "checkstyle,pmd,findbugs");
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.apply("checkstyle")).isFalse();
    assertThat(predicate.apply("pmd")).isFalse();
    assertThat(predicate.apply("cobertura")).isTrue();
  }

  @Test
  public void trim_inclusions_and_exclusions() {
    settings
      .setProperty(CoreProperties.PREVIEW_INCLUDE_PLUGINS, "checkstyle, pmd, findbugs")
      .setProperty(CoreProperties.PREVIEW_EXCLUDE_PLUGINS, "cobertura, pmd");
    ScannerPluginPredicate predicate = new ScannerPluginPredicate(settings.asConfig(), mode);
    assertThat(predicate.apply("pmd")).isTrue();
  }

}
