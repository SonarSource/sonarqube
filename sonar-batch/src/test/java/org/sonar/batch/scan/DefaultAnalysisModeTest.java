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

import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.batch.scan.ProjectAnalysisMode;
import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAnalysisModeTest {

  @Test
  public void regular_analysis_by_default() {
    ProjectAnalysisMode mode = new ProjectAnalysisMode(new AnalysisProperties(Collections.<String, String>emptyMap()));

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();

    mode = createMode(CoreProperties.ANALYSIS_MODE, "pouet");

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_analysis_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_ANALYSIS);

    assertThat(mode.isPreview()).isFalse();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_preview_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_PREVIEW);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();
  }

  @Test
  public void support_quick_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_QUICK);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();
    assertThat(mode.isQuick()).isTrue();
  }

  @Test
  public void support_incremental_mode() {
    ProjectAnalysisMode mode = createMode(CoreProperties.ANALYSIS_MODE, CoreProperties.ANALYSIS_MODE_INCREMENTAL);

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isTrue();
  }

  @Test
  public void support_deprecated_dryrun_property() {
    ProjectAnalysisMode mode = createMode(CoreProperties.DRY_RUN, "true");

    assertThat(mode.isPreview()).isTrue();
    assertThat(mode.isIncremental()).isFalse();
  }

  private ProjectAnalysisMode createMode(String key, String value) {
    Map<String, String> map = new HashMap<>();
    map.put(key, value);

    return new ProjectAnalysisMode(new AnalysisProperties(map));
  }
}
