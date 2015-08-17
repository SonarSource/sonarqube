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
package org.sonar.batch.analysis;

import org.junit.Rule;

import org.junit.rules.ExpectedException;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.analysis.AnalysisProperties;

import javax.annotation.Nullable;

import org.sonar.batch.bootstrap.GlobalProperties;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAnalysisModeTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void regular_analysis_by_default() {
    DefaultAnalysisMode mode = createMode(null, null);
    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isPublish()).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_inconsistent() {
    createMode(null, CoreProperties.ANALYSIS_MODE_ISSUES);
  }

  @Test
  public void support_publish_mode() {
    DefaultAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE_PUBLISH);

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isPublish()).isTrue();
  }

  @Test
  public void incremental_mode_no_longer_valid() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("This mode was removed in SonarQube 5.2");

    createMode(CoreProperties.ANALYSIS_MODE_INCREMENTAL);
  }

  @Test
  public void invalidate_mode() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("[preview, publish, issues]");

    createMode("invalid");
  }

  @Test
  public void preview_mode_fallback_issues() {
    DefaultAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE_PREVIEW);

    assertThat(mode.isIssues()).isTrue();
    assertThat(mode.isPreview()).isFalse();
  }

  @Test
  public void default_publish_mode() {
    DefaultAnalysisMode mode = createMode(null);
    assertThat(mode.isPublish()).isTrue();
  }

  @Test
  public void support_issues_mode() {
    DefaultAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE_ISSUES);

    assertThat(mode.isIssues()).isTrue();
  }

  private static DefaultAnalysisMode createMode(@Nullable String mode) {
    return createMode(mode, mode);
  }

  private static DefaultAnalysisMode createMode(@Nullable String bootstrapMode, @Nullable String analysisMode) {
    Map<String, String> bootstrapMap = new HashMap<>();
    Map<String, String> analysisMap = new HashMap<>();

    if (bootstrapMode != null) {
      bootstrapMap.put(CoreProperties.ANALYSIS_MODE, bootstrapMode);
    }
    if (analysisMode != null) {
      analysisMap.put(CoreProperties.ANALYSIS_MODE, analysisMode);
    }
    return new DefaultAnalysisMode(new GlobalProperties(bootstrapMap), new AnalysisProperties(analysisMap));
  }

}
