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
package org.sonar.batch.scan;

import javax.annotation.Nullable;

import org.sonar.batch.bootstrap.BootstrapProperties;
import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.batch.scan.ProjectAnalysisMode;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectAnalysisModeTest {

  @Test
  public void regular_analysis_by_default() {
    ProjectAnalysisMode mode = createMode(null, null);
    assertThat(mode.isPreview()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_inconsistent() {
    createMode(null, CoreProperties.ANALYSIS_MODE_ISSUES);
  }

  @Test
  public void support_analysis_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE_ANALYSIS);

    assertThat(mode.isPreview()).isFalse();
  }

  @Test(expected = IllegalStateException.class)
  public void validate_mode() {
    createMode(CoreProperties.ANALYSIS_MODE_INCREMENTAL);
  }

  @Test
  public void support_preview_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE_PREVIEW);

    assertThat(mode.isPreview()).isTrue();
  }

  @Test
  public void support_issues_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE_ISSUES);

    assertThat(mode.isIssues()).isTrue();
  }

  @Test
  public void support_deprecated_dryrun_property() {
    Map<String, String> bootstrapMap = new HashMap<>();
    Map<String, String> analysisMap = new HashMap<>();

    analysisMap.put(CoreProperties.DRY_RUN, "true");
    bootstrapMap.put(CoreProperties.DRY_RUN, "true");

    ProjectAnalysisMode mode = new ProjectAnalysisMode(new BootstrapProperties(bootstrapMap), new AnalysisProperties(analysisMap));

    assertThat(mode.isPreview()).isTrue();
  }

  private static ProjectAnalysisMode createMode(@Nullable String mode) {
    return createMode(mode, mode);
  }

  private static ProjectAnalysisMode createMode(@Nullable String bootstrapMode, @Nullable String analysisMode) {
    Map<String, String> bootstrapMap = new HashMap<>();
    Map<String, String> analysisMap = new HashMap<>();

    if (bootstrapMode != null) {
      bootstrapMap.put(CoreProperties.ANALYSIS_MODE, bootstrapMode);
    }
    if (analysisMode != null) {
      analysisMap.put(CoreProperties.ANALYSIS_MODE, analysisMode);
    }
    return new ProjectAnalysisMode(new BootstrapProperties(bootstrapMap), new AnalysisProperties(analysisMap));
  }

}
